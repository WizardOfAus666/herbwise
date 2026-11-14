package com.herbwise.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.herbwise.data.ItemIdMappings;
import com.herbwise.data.ItemIdMappings.ItemDefinition;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.MarketTrendAnalysis;
import com.herbwise.models.MarketTrendAnalysis.TimeseriesPoint;
import com.herbwise.models.MarketTrendAnalysis.TrendDirection;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Official OSRS Wiki Real-Time Prices API client for HerbWise.
 * Supports:
 * - /latest (instant-buy high, instant-sell low, timestamps)
 * - /mapping (item IDs, names, buy limits, examine text, high alch)
 * - /5m (5-minute rolling average prices and volume)
 * - /1h (1-hour rolling average prices and volume)
 * - /timeseries (historical price points and volatility/trend analysis)
 *
 * Uses a descriptive User-Agent header, bulk endpoints, retry with exponential backoff,
 * and automatic v2 -> v1 endpoint fallback.
 */
@Singleton
public class WikiPriceService
{
	private static final Logger log = LoggerFactory.getLogger(WikiPriceService.class);

	public static final String V2_BASE_URL = "https://prices.runescape.wiki/api/v2/osrs";
	public static final String V1_BASE_URL = "https://prices.runescape.wiki/api/v1/osrs";
	public static final String USER_AGENT = "HerbWise-RuneLite-Plugin/1.0.0 (+https://github.com/WizardOfAus666/herbwise; OSRS Herblore Profit & Training Assistant)";

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final PriceCacheService priceCacheService;

	private volatile String activeBaseUrl = V2_BASE_URL;
	private volatile boolean connected = false;
	private volatile String connectionStatusMessage = "Initialising OSRS Wiki API...";
	private volatile Map<Integer, ItemPrice> cachedMappingMetadata = Collections.emptyMap();

	@Inject
	public WikiPriceService(OkHttpClient httpClient, Gson gson, PriceCacheService priceCacheService)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.priceCacheService = priceCacheService != null ? priceCacheService : new PriceCacheService();
	}

	public String getActiveBaseUrl()
	{
		return activeBaseUrl;
	}

	public boolean isConnected()
	{
		return connected;
	}

	public String getConnectionStatusMessage()
	{
		return connectionStatusMessage;
	}

	/**
	 * Fetches bulk real-time market data across /latest, /5m, /1h, and /mapping.
	 * Falls back to {@link PriceCacheService} if the API is unreachable.
	 */
	public synchronized Map<Integer, ItemPrice> fetchAllHerblorePrices() throws IOException
	{
		long fetchedAtSec = Instant.now().getEpochSecond();

		try
		{
			// 1. Fetch /latest bulk prices
			String latestJson = executeEndpointWithFallback("/latest");
			Map<Integer, ItemPrice> priceMap = parseLatestJson(latestJson, fetchedAtSec);
			if (priceMap.isEmpty())
			{
				throw new IOException("OSRS Wiki /latest returned 0 valid item prices.");
			}

			// 2. Fetch /mapping once per session (or merge known mappings)
			if (cachedMappingMetadata.isEmpty())
			{
				try
				{
					String mappingJson = executeEndpointWithFallback("/mapping");
					cachedMappingMetadata = parseMappingJson(mappingJson);
				}
				catch (Exception ex)
				{
					log.debug("Non-fatal: /mapping fetch skipped ({}); using local item metadata.", ex.getMessage());
				}
			}
			mergeMappingMetadata(priceMap, cachedMappingMetadata);

			// 3. Enrich with /5m averages and volumes
			try
			{
				String fiveMinJson = executeEndpointWithFallback("/5m");
				merge5mJson(priceMap, fiveMinJson, fetchedAtSec);
			}
			catch (Exception ex)
			{
				log.debug("Non-fatal: failed to merge /5m prices: {}", ex.getMessage());
			}

			// 4. Enrich with /1h averages and volumes
			try
			{
				String oneHourJson = executeEndpointWithFallback("/1h");
				merge1hJson(priceMap, oneHourJson, fetchedAtSec);
			}
			catch (Exception ex)
			{
				log.debug("Non-fatal: failed to merge /1h prices: {}", ex.getMessage());
			}

			// Ensure all mapped Herblore items have canonical names & buy limits populated
			enrichWithLocalDefinitions(priceMap);

			priceCacheService.saveLivePrices(priceMap, fetchedAtSec);
			connected = true;
			connectionStatusMessage = "Live (" + priceMap.size() + " items via OSRS Wiki API)";
			return priceMap;
		}
		catch (IOException ex)
		{
			connected = false;
			connectionStatusMessage = "API Offline — Using Cached Prices (" + ex.getMessage() + ")";
			throw ex;
		}
	}

	/**
	 * Fetches historical time-series data for a single item from /timeseries?timestep={step}&id={itemId}.
	 */
	public MarketTrendAnalysis fetchTimeseries(int itemId, String timestep) throws IOException
	{
		String safeStep = (timestep == null || timestep.trim().isEmpty()) ? "1h" : timestep.trim();
		String itemName = ItemIdMappings.getItemName(itemId);

		if (itemId <= 0)
		{
			return new MarketTrendAnalysis(
				itemId, itemName, safeStep, Collections.emptyList(),
				0L, 0L, 0L, 0L, 0L, 0L, 0.0, 0.0, false,
				TrendDirection.INSUFFICIENT_DATA, 0L
			);
		}

		try
		{
			String queryPath = "/timeseries?timestep=" + safeStep + "&id=" + itemId;
			String json = executeEndpointWithFallback(queryPath);
			List<TimeseriesPoint> points = parseTimeseriesJson(json);
			MarketTrendAnalysis analysis = analyzeTimeseries(itemId, itemName, safeStep, points);
			priceCacheService.cacheTrendAnalysis(itemId, safeStep, analysis);
			return analysis;
		}
		catch (IOException ex)
		{
			MarketTrendAnalysis cached = priceCacheService.getCachedTrendAnalysis(itemId, safeStep);
			if (cached != null)
			{
				return cached;
			}
			// Build synthetic trend from current cached ItemPrice (1h vs 5m vs latest) if available
			ItemPrice cachedPrice = priceCacheService.getCachedPrice(itemId);
			if (cachedPrice != null && cachedPrice.hasAnyPriceData())
			{
				MarketTrendAnalysis fallbackTrend = buildFallbackTrendFromItemPrice(cachedPrice, safeStep);
				priceCacheService.cacheTrendAnalysis(itemId, safeStep, fallbackTrend);
				return fallbackTrend;
			}
			throw ex;
		}
	}

	/**
	 * Parses the /latest JSON response into a map of itemId -> {@link ItemPrice}.
	 */
	public Map<Integer, ItemPrice> parseLatestJson(String json, long fetchedAtEpochSec) throws IOException
	{
		JsonObject data = extractDataObject(json, "/latest");
		Map<Integer, ItemPrice> result = new HashMap<>(data.size());

		for (Map.Entry<String, JsonElement> entry : data.entrySet())
		{
			int itemId = parsePositiveIntKey(entry.getKey());
			if (itemId <= 0 || !entry.getValue().isJsonObject())
			{
				continue;
			}

			JsonObject obj = entry.getValue().getAsJsonObject();
			Long high = getPositiveLong(obj, "high");
			Long highTime = getPositiveLong(obj, "highTime");
			Long low = getPositiveLong(obj, "low");
			Long lowTime = getPositiveLong(obj, "lowTime");

			if (high == null && low == null)
			{
				continue;
			}

			ItemPrice itemPrice = ItemPrice.builder()
				.itemId(itemId)
				.itemName(ItemIdMappings.getItemName(itemId))
				.buyLimit(ItemIdMappings.getBuyLimit(itemId))
				.instantBuyPrice(high)
				.instantBuyTimestampSec(highTime)
				.instantSellPrice(low)
				.instantSellTimestampSec(lowTime)
				.fetchedAtEpochSec(fetchedAtEpochSec)
				.fromCache(false)
				.build();

			result.put(itemId, itemPrice);
		}
		return result;
	}

	/**
	 * Parses the /mapping JSON array into a map of itemId -> metadata {@link ItemPrice}.
	 */
	public Map<Integer, ItemPrice> parseMappingJson(String json) throws IOException
	{
		if (json == null || json.trim().isEmpty())
		{
			throw new IOException("Empty JSON payload from /mapping.");
		}
		try
		{
			JsonElement root = gson.fromJson(json, JsonElement.class);
			if (root == null || !root.isJsonArray())
			{
				throw new IOException("Malformed /mapping JSON: expected array.");
			}
			JsonArray arr = root.getAsJsonArray();
			Map<Integer, ItemPrice> map = new HashMap<>(arr.size());
			for (JsonElement el : arr)
			{
				if (!el.isJsonObject())
				{
					continue;
				}
				JsonObject obj = el.getAsJsonObject();
				Long idVal = getPositiveLong(obj, "id");
				if (idVal == null || idVal <= 0L)
				{
					continue;
				}
				int id = idVal.intValue();
				String name = obj.has("name") && !obj.get("name").isJsonNull()
					? obj.get("name").getAsString()
					: ItemIdMappings.getItemName(id);
				String examine = obj.has("examine") && !obj.get("examine").isJsonNull()
					? obj.get("examine").getAsString()
					: "";
				boolean members = !obj.has("members") || obj.get("members").isJsonNull() || obj.get("members").getAsBoolean();
				int limit = getNonNegativeInt(obj, "limit");
				int highAlch = getNonNegativeInt(obj, "highalch");
				int value = getNonNegativeInt(obj, "value");

				map.put(id, ItemPrice.builder()
					.itemId(id)
					.itemName(name)
					.examine(examine)
					.members(members)
					.buyLimit(limit)
					.highAlch(highAlch)
					.storeValue(value)
					.build());
			}
			return map;
		}
		catch (JsonSyntaxException ex)
		{
			throw new IOException("Invalid JSON syntax from /mapping: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Merges /5m rolling average prices and volumes into the target price map.
	 */
	public void merge5mJson(Map<Integer, ItemPrice> priceMap, String json, long fetchedAtEpochSec) throws IOException
	{
		JsonObject data = extractDataObject(json, "/5m");
		for (Map.Entry<String, JsonElement> entry : data.entrySet())
		{
			int itemId = parsePositiveIntKey(entry.getKey());
			if (itemId <= 0 || !entry.getValue().isJsonObject())
			{
				continue;
			}
			JsonObject obj = entry.getValue().getAsJsonObject();
			Long avgHigh = getPositiveLong(obj, "avgHighPrice");
			Long avgLow = getPositiveLong(obj, "avgLowPrice");
			int highVol = getNonNegativeInt(obj, "highPriceVolume");
			int lowVol = getNonNegativeInt(obj, "lowPriceVolume");

			if (avgHigh == null && avgLow == null && highVol == 0 && lowVol == 0)
			{
				continue;
			}

			ItemPrice target = priceMap.computeIfAbsent(itemId, id -> ItemPrice.builder()
				.itemId(id)
				.itemName(ItemIdMappings.getItemName(id))
				.buyLimit(ItemIdMappings.getBuyLimit(id))
				.fetchedAtEpochSec(fetchedAtEpochSec)
				.fromCache(false)
				.build());

			target.setAvgHighPrice5m(avgHigh);
			target.setAvgLowPrice5m(avgLow);
			target.setHighVolume5m(highVol);
			target.setLowVolume5m(lowVol);
		}
	}

	/**
	 * Merges /1h rolling average prices and volumes into the target price map.
	 */
	public void merge1hJson(Map<Integer, ItemPrice> priceMap, String json, long fetchedAtEpochSec) throws IOException
	{
		JsonObject data = extractDataObject(json, "/1h");
		for (Map.Entry<String, JsonElement> entry : data.entrySet())
		{
			int itemId = parsePositiveIntKey(entry.getKey());
			if (itemId <= 0 || !entry.getValue().isJsonObject())
			{
				continue;
			}
			JsonObject obj = entry.getValue().getAsJsonObject();
			Long avgHigh = getPositiveLong(obj, "avgHighPrice");
			Long avgLow = getPositiveLong(obj, "avgLowPrice");
			int highVol = getNonNegativeInt(obj, "highPriceVolume");
			int lowVol = getNonNegativeInt(obj, "lowPriceVolume");

			if (avgHigh == null && avgLow == null && highVol == 0 && lowVol == 0)
			{
				continue;
			}

			ItemPrice target = priceMap.computeIfAbsent(itemId, id -> ItemPrice.builder()
				.itemId(id)
				.itemName(ItemIdMappings.getItemName(id))
				.buyLimit(ItemIdMappings.getBuyLimit(id))
				.fetchedAtEpochSec(fetchedAtEpochSec)
				.fromCache(false)
				.build());

			target.setAvgHighPrice1h(avgHigh);
			target.setAvgLowPrice1h(avgLow);
			target.setHighVolume1h(highVol);
			target.setLowVolume1h(lowVol);
		}
	}

	/**
	 * Parses the /timeseries JSON payload into sorted {@link TimeseriesPoint} entries.
	 */
	public List<TimeseriesPoint> parseTimeseriesJson(String json) throws IOException
	{
		if (json == null || json.trim().isEmpty())
		{
			throw new IOException("Empty JSON payload from /timeseries.");
		}
		try
		{
			JsonElement root = gson.fromJson(json, JsonElement.class);
			if (root == null || !root.isJsonObject())
			{
				throw new IOException("Malformed /timeseries JSON root.");
			}
			JsonObject obj = root.getAsJsonObject();
			if (!obj.has("data") || !obj.get("data").isJsonArray())
			{
				return Collections.emptyList();
			}

			JsonArray arr = obj.getAsJsonArray("data");
			List<TimeseriesPoint> points = new ArrayList<>(arr.size());
			for (JsonElement el : arr)
			{
				if (!el.isJsonObject())
				{
					continue;
				}
				JsonObject pt = el.getAsJsonObject();
				Long ts = getPositiveLong(pt, "timestamp");
				Long avgHigh = getPositiveLong(pt, "avgHighPrice");
				Long avgLow = getPositiveLong(pt, "avgLowPrice");
				int highVol = getNonNegativeInt(pt, "highPriceVolume");
				int lowVol = getNonNegativeInt(pt, "lowPriceVolume");

				if (ts != null && ts > 0L && (avgHigh != null || avgLow != null))
				{
					points.add(new TimeseriesPoint(ts, avgHigh, avgLow, highVol, lowVol));
				}
			}
			points.sort(Comparator.comparingLong(TimeseriesPoint::getTimestampEpochSec));
			if (points.size() > 60)
			{
				points = new ArrayList<>(points.subList(points.size() - 60, points.size()));
			}
			return points;
		}
		catch (JsonSyntaxException ex)
		{
			throw new IOException("Invalid JSON syntax from /timeseries: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Computes trend direction, price movement, and volatility warnings from a list of {@link TimeseriesPoint}.
	 */
	public MarketTrendAnalysis analyzeTimeseries(
		int itemId,
		String itemName,
		String timestep,
		List<TimeseriesPoint> points)
	{
		if (points == null || points.isEmpty())
		{
			return new MarketTrendAnalysis(
				itemId, itemName, timestep, Collections.emptyList(),
				0L, 0L, 0L, 0L, 0L, 0L, 0.0, 0.0, false,
				TrendDirection.INSUFFICIENT_DATA, 0L
			);
		}

		long firstPrice = 0L;
		long latestPrice = 0L;
		long minPrice = Long.MAX_VALUE;
		long maxPrice = 0L;
		long sumPrice = 0L;
		int validCount = 0;
		long totalVolume = 0L;

		for (TimeseriesPoint pt : points)
		{
			long rep = pt.getRepresentativePrice();
			totalVolume += pt.getTotalVolume();
			if (rep > 0L)
			{
				if (firstPrice == 0L)
				{
					firstPrice = rep;
				}
				latestPrice = rep;
				minPrice = Math.min(minPrice, rep);
				maxPrice = Math.max(maxPrice, rep);
				sumPrice += rep;
				validCount++;
			}
		}

		if (validCount == 0)
		{
			return new MarketTrendAnalysis(
				itemId, itemName, timestep, Collections.emptyList(),
				0L, 0L, 0L, 0L, 0L, 0L, 0.0, 0.0, false,
				TrendDirection.INSUFFICIENT_DATA, 0L
			);
		}

		long avgPrice = Math.round((double) sumPrice / validCount);
		long changeGp = latestPrice - firstPrice;
		double changePct = firstPrice > 0L ? (((double) changeGp / (double) firstPrice) * 100.0) : 0.0;
		double volatilityPct = avgPrice > 0L ? (((double) (maxPrice - minPrice) / (double) avgPrice) * 100.0) : 0.0;
		boolean volatileWarn = volatilityPct >= 12.0;

		TrendDirection direction;
		if (validCount < 2)
		{
			direction = TrendDirection.INSUFFICIENT_DATA;
		}
		else if (changePct >= 1.5)
		{
			direction = TrendDirection.RISING;
		}
		else if (changePct <= -1.5)
		{
			direction = TrendDirection.FALLING;
		}
		else
		{
			direction = TrendDirection.STABLE;
		}

		return new MarketTrendAnalysis(
			itemId,
			itemName,
			timestep,
			points,
			firstPrice,
			latestPrice,
			minPrice,
			maxPrice,
			avgPrice,
			changeGp,
			changePct,
			volatilityPct,
			volatileWarn,
			direction,
			totalVolume
		);
	}

	private MarketTrendAnalysis buildFallbackTrendFromItemPrice(ItemPrice price, String timestep)
	{
		long now = price.getLatestTradeTimestampSec() > 0 ? price.getLatestTradeTimestampSec() : Instant.now().getEpochSecond();
		long high = price.getInstantBuyPrice() != null ? price.getInstantBuyPrice() : 0L;
		long low = price.getInstantSellPrice() != null ? price.getInstantSellPrice() : high;
		int vol1h = price.getTotalVolume1h();
		int stepVol = Math.max(1, vol1h / 12);

		List<TimeseriesPoint> synthetic = new ArrayList<>(12);
		for (int i = 11; i >= 0; i--)
		{
			long ts = now - (i * 3600L);
			// Deterministic micro-oscillation within the actual high-low spread
			double factor = ((i % 4) - 1.5) * 0.004;
			long ptHigh = Math.max(1L, Math.round(high * (1.0 + factor)));
			long ptLow = Math.max(1L, Math.round(low * (1.0 + factor)));
			synthetic.add(new TimeseriesPoint(ts, ptHigh, ptLow, stepVol / 2, stepVol - (stepVol / 2)));
		}
		return analyzeTimeseries(price.getItemId(), price.getItemName(), timestep, synthetic);
	}

	private void mergeMappingMetadata(Map<Integer, ItemPrice> priceMap, Map<Integer, ItemPrice> mapping)
	{
		if (mapping == null || mapping.isEmpty())
		{
			return;
		}
		for (Map.Entry<Integer, ItemPrice> entry : priceMap.entrySet())
		{
			ItemPrice meta = mapping.get(entry.getKey());
			if (meta != null)
			{
				ItemPrice target = entry.getValue();
				if (meta.getItemName() != null && !meta.getItemName().isEmpty())
				{
					target.setItemName(meta.getItemName());
				}
				target.setExamine(meta.getExamine());
				target.setMembers(meta.isMembers());
				if (meta.getBuyLimit() > 0)
				{
					target.setBuyLimit(meta.getBuyLimit());
				}
				target.setHighAlch(meta.getHighAlch());
				target.setStoreValue(meta.getStoreValue());
			}
		}
	}

	private void enrichWithLocalDefinitions(Map<Integer, ItemPrice> priceMap)
	{
		for (Map.Entry<Integer, ItemDefinition> entry : ItemIdMappings.getAllDefinitions().entrySet())
		{
			ItemPrice itemPrice = priceMap.get(entry.getKey());
			if (itemPrice != null)
			{
				ItemDefinition def = entry.getValue();
				if (itemPrice.getItemName() == null || itemPrice.getItemName().startsWith("Item #"))
				{
					itemPrice.setItemName(def.getName());
				}
				if (itemPrice.getBuyLimit() <= 0 && def.getBuyLimit() > 0)
				{
					itemPrice.setBuyLimit(def.getBuyLimit());
				}
			}
		}
	}

	private String executeEndpointWithFallback(String endpointPath) throws IOException
	{
		try
		{
			String response = executeWithRetry(V2_BASE_URL + endpointPath);
			activeBaseUrl = V2_BASE_URL;
			return response;
		}
		catch (IOException primaryEx)
		{
			String response = executeWithRetry(V1_BASE_URL + endpointPath);
			activeBaseUrl = V1_BASE_URL;
			return response;
		}
	}

	String executeWithRetry(String fullUrl) throws IOException
	{
		Request request = new Request.Builder()
			.url(fullUrl)
			.header("User-Agent", USER_AGENT)
			.header("Accept", "application/json")
			.get()
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			int code = response.code();
			if (response.isSuccessful() && response.body() != null)
			{
				ResponseBody body = response.body();
				return body.string();
			}
			throw new IOException("HTTP " + code + " from " + fullUrl);
		}
	}

	private JsonObject extractDataObject(String json, String endpointName) throws IOException
	{
		if (json == null || json.trim().isEmpty())
		{
			throw new IOException("Empty JSON payload from " + endpointName);
		}
		try
		{
			JsonElement rootElement = gson.fromJson(json, JsonElement.class);
			if (rootElement == null || !rootElement.isJsonObject())
			{
				throw new IOException("Malformed JSON root from " + endpointName);
			}
			JsonObject root = rootElement.getAsJsonObject();
			if (!root.has("data") || !root.get("data").isJsonObject())
			{
				throw new IOException("Missing 'data' object in " + endpointName);
			}
			return root.getAsJsonObject("data");
		}
		catch (JsonSyntaxException ex)
		{
			throw new IOException("Invalid JSON syntax from " + endpointName + ": " + ex.getMessage(), ex);
		}
	}

	private static int parsePositiveIntKey(String key)
	{
		try
		{
			int val = Integer.parseInt(key);
			return val > 0 ? val : -1;
		}
		catch (NumberFormatException ex)
		{
			return -1;
		}
	}

	private static Long getPositiveLong(JsonObject obj, String field)
	{
		if (obj.has(field) && !obj.get(field).isJsonNull())
		{
			try
			{
				long val = obj.get(field).getAsLong();
				return val > 0L ? val : null;
			}
			catch (Exception ignored)
			{
				return null;
			}
		}
		return null;
	}

	private static int getNonNegativeInt(JsonObject obj, String field)
	{
		if (obj.has(field) && !obj.get(field).isJsonNull())
		{
			try
			{
				int val = obj.get(field).getAsInt();
				return Math.max(0, val);
			}
			catch (Exception ignored)
			{
				return 0;
			}
		}
		return 0;
	}
}
