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
import com.herbwise.models.PriceCalculationMode;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central OSRS Wiki Real-Time Prices API service for HerbWise.
 *
 * <p>All OSRS Wiki API traffic in HerbWise passes through this singleton service.
 * Provides:
 * <ul>
 *   <li>Single canonical base URL ({@code https://prices.runescape.wiki/api/v1/osrs}) with descriptive {@code User-Agent}</li>
 *   <li>Multi-tier caching with stale-while-revalidate preservation via {@link PriceCacheService}</li>
 *   <li>On-demand lazy loading of {@code /5m}, {@code /1h}, {@code /mapping}, and {@code /timeseries}</li>
 *   <li>Shared in-flight request deduplication so concurrent callers share a single HTTP request</li>
 *   <li>HTTP 429 {@code Retry-After} compliance, exponential backoff with jitter, and circuit breaker protection</li>
 *   <li>Internal API traffic diagnostics (requests per endpoint, cache hits/misses, deduplicated count, 429s, latency)</li>
 * </ul>
 */
@Singleton
public class WikiPriceService
{
	private static final Logger log = LoggerFactory.getLogger(WikiPriceService.class);

	public static final String V1_BASE_URL = "https://prices.runescape.wiki/api/v1/osrs";
	public static final String V2_BASE_URL = "https://prices.runescape.wiki/api/v1/osrs";
	public static final String USER_AGENT = "HerbWise-RuneLite-Plugin/1.0.0 (+https://github.com/WizardOfAus666/herbwise; OSRS Herblore Profit & Training Assistant)";

	public static final int DEFAULT_CIRCUIT_BREAKER_THRESHOLD = 3;
	public static final long DEFAULT_CIRCUIT_BREAKER_COOLDOWN_MS = 120_000L;
	public static final long DEFAULT_INITIAL_BACKOFF_MS = 2_000L;
	public static final long DEFAULT_MAX_BACKOFF_MS = 120_000L;
	public static final long DEFAULT_RATE_LIMIT_FALLBACK_SEC = 60L;

	/**
	 * Immutable snapshot of internal API traffic and cache diagnostics.
	 */
	public static final class ApiDiagnostics
	{
		private final long totalApiRequests;
		private final Map<String, Long> requestsByEndpoint;
		private final long cacheHits;
		private final long cacheMisses;
		private final long deduplicatedRequests;
		private final long failedRequests;
		private final long http429Responses;
		private final long retryAttempts;
		private final long averageResponseTimeMs;
		private final int pendingRequests;
		private final boolean circuitBreakerOpen;
		private final boolean rateLimited;
		private final long remainingCooldownSeconds;

		public ApiDiagnostics(
			long totalApiRequests,
			Map<String, Long> requestsByEndpoint,
			long cacheHits,
			long cacheMisses,
			long deduplicatedRequests,
			long failedRequests,
			long http429Responses,
			long retryAttempts,
			long averageResponseTimeMs,
			int pendingRequests,
			boolean circuitBreakerOpen,
			boolean rateLimited,
			long remainingCooldownSeconds)
		{
			this.totalApiRequests = totalApiRequests;
			this.requestsByEndpoint = Collections.unmodifiableMap(new LinkedHashMap<>(requestsByEndpoint));
			this.cacheHits = cacheHits;
			this.cacheMisses = cacheMisses;
			this.deduplicatedRequests = deduplicatedRequests;
			this.failedRequests = failedRequests;
			this.http429Responses = http429Responses;
			this.retryAttempts = retryAttempts;
			this.averageResponseTimeMs = averageResponseTimeMs;
			this.pendingRequests = pendingRequests;
			this.circuitBreakerOpen = circuitBreakerOpen;
			this.rateLimited = rateLimited;
			this.remainingCooldownSeconds = remainingCooldownSeconds;
		}

		public long getTotalApiRequests() { return totalApiRequests; }
		public Map<String, Long> getRequestsByEndpoint() { return requestsByEndpoint; }
		public long getCacheHits() { return cacheHits; }
		public long getCacheMisses() { return cacheMisses; }
		public long getDeduplicatedRequests() { return deduplicatedRequests; }
		public long getFailedRequests() { return failedRequests; }
		public long getHttp429Responses() { return http429Responses; }
		public long getRetryAttempts() { return retryAttempts; }
		public long getAverageResponseTimeMs() { return averageResponseTimeMs; }
		public int getPendingRequests() { return pendingRequests; }
		public boolean isCircuitBreakerOpen() { return circuitBreakerOpen; }
		public boolean isRateLimited() { return rateLimited; }
		public long getRemainingCooldownSeconds() { return remainingCooldownSeconds; }
	}

	private static final class EndpointFlightState
	{
		final Object lock = new Object();
		final AtomicInteger activeCallers = new AtomicInteger(0);
		volatile long completedAtMs = 0L;
		volatile String lastResponse = null;
		volatile IOException lastError = null;
	}

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final PriceCacheService priceCacheService;

	private final ConcurrentHashMap<String, EndpointFlightState> flightStates = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, AtomicLong> endpointRequestCounts = new ConcurrentHashMap<>();

	// Diagnostics counters
	private final AtomicLong totalApiRequests = new AtomicLong(0L);
	private final AtomicLong cacheHits = new AtomicLong(0L);
	private final AtomicLong cacheMisses = new AtomicLong(0L);
	private final AtomicLong deduplicatedRequests = new AtomicLong(0L);
	private final AtomicLong failedRequests = new AtomicLong(0L);
	private final AtomicLong http429Responses = new AtomicLong(0L);
	private final AtomicLong retryAttempts = new AtomicLong(0L);
	private final AtomicLong totalResponseTimeMs = new AtomicLong(0L);
	private final AtomicLong completedTimedRequests = new AtomicLong(0L);
	private final AtomicInteger pendingRequests = new AtomicInteger(0);

	// Rate-limit, backoff, and circuit-breaker state
	private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
	private volatile long rateLimitedUntilEpochMs = 0L;
	private volatile long circuitBreakerOpenUntilEpochMs = 0L;
	private volatile long nextBackoffAllowedEpochMs = 0L;

	private volatile int circuitBreakerThreshold = DEFAULT_CIRCUIT_BREAKER_THRESHOLD;
	private volatile long circuitBreakerCooldownMs = DEFAULT_CIRCUIT_BREAKER_COOLDOWN_MS;
	private volatile long initialBackoffMs = DEFAULT_INITIAL_BACKOFF_MS;
	private volatile long maxBackoffMs = DEFAULT_MAX_BACKOFF_MS;

	private volatile LongSupplier clockMillis = System::currentTimeMillis;
	private volatile String activeBaseUrl = V1_BASE_URL;
	private volatile boolean connected = false;
	private volatile String connectionStatusMessage = "Initialising OSRS Wiki API...";

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

	public void setClockMillis(LongSupplier clockMillis)
	{
		if (clockMillis != null)
		{
			this.clockMillis = clockMillis;
		}
	}

	public long currentTimeMillis()
	{
		return clockMillis.getAsLong();
	}

	public long currentEpochSeconds()
	{
		return currentTimeMillis() / 1000L;
	}

	public void setCircuitBreakerConfig(int threshold, long cooldownMs, long initialBackoffMs, long maxBackoffMs)
	{
		this.circuitBreakerThreshold = Math.max(1, threshold);
		this.circuitBreakerCooldownMs = Math.max(100L, cooldownMs);
		this.initialBackoffMs = Math.max(50L, initialBackoffMs);
		this.maxBackoffMs = Math.max(this.initialBackoffMs, maxBackoffMs);
	}

	public boolean isRateLimited()
	{
		return currentTimeMillis() < rateLimitedUntilEpochMs;
	}

	public boolean isCircuitBreakerOpen()
	{
		return currentTimeMillis() < circuitBreakerOpenUntilEpochMs;
	}

	public boolean isInBackoffWindow()
	{
		return currentTimeMillis() < nextBackoffAllowedEpochMs;
	}

	public long getRemainingCooldownSeconds()
	{
		long nowMs = currentTimeMillis();
		long untilMs = Math.max(rateLimitedUntilEpochMs, Math.max(circuitBreakerOpenUntilEpochMs, nextBackoffAllowedEpochMs));
		if (untilMs <= nowMs)
		{
			return 0L;
		}
		return Math.max(1L, (untilMs - nowMs + 999L) / 1000L);
	}

	/**
	 * Resets rate-limit and circuit-breaker counters (used in unit tests).
	 */
	public void resetDiagnosticsAndCircuitState()
	{
		totalApiRequests.set(0L);
		endpointRequestCounts.clear();
		cacheHits.set(0L);
		cacheMisses.set(0L);
		deduplicatedRequests.set(0L);
		failedRequests.set(0L);
		http429Responses.set(0L);
		retryAttempts.set(0L);
		totalResponseTimeMs.set(0L);
		completedTimedRequests.set(0L);
		pendingRequests.set(0);
		consecutiveFailures.set(0);
		rateLimitedUntilEpochMs = 0L;
		circuitBreakerOpenUntilEpochMs = 0L;
		nextBackoffAllowedEpochMs = 0L;
	}

	public ApiDiagnostics getDiagnostics()
	{
		Map<String, Long> byEndpoint = new LinkedHashMap<>();
		byEndpoint.put("/latest", getEndpointRequestCount("/latest"));
		byEndpoint.put("/5m", getEndpointRequestCount("/5m"));
		byEndpoint.put("/1h", getEndpointRequestCount("/1h"));
		byEndpoint.put("/mapping", getEndpointRequestCount("/mapping"));
		byEndpoint.put("/timeseries", getEndpointRequestCount("/timeseries"));
		for (Map.Entry<String, AtomicLong> e : endpointRequestCounts.entrySet())
		{
			byEndpoint.putIfAbsent(e.getKey(), e.getValue().get());
		}

		long completed = completedTimedRequests.get();
		long avgMs = completed > 0L ? (totalResponseTimeMs.get() / completed) : 0L;

		return new ApiDiagnostics(
			totalApiRequests.get(),
			byEndpoint,
			cacheHits.get(),
			cacheMisses.get(),
			deduplicatedRequests.get(),
			failedRequests.get(),
			http429Responses.get(),
			retryAttempts.get(),
			avgMs,
			pendingRequests.get(),
			isCircuitBreakerOpen(),
			isRateLimited(),
			getRemainingCooldownSeconds()
		);
	}

	public long getEndpointRequestCount(String endpointKey)
	{
		AtomicLong count = endpointRequestCounts.get(endpointKey);
		return count != null ? count.get() : 0L;
	}

	/**
	 * Retrieves the price for a single item ID through the central cache, refreshing {@code /latest}
	 * at most once if the cache is expired. Never triggers per-item HTTP requests.
	 */
	public ItemPrice getOrFetchItemPrice(int itemId) throws IOException
	{
		long nowSec = currentEpochSeconds();
		if (priceCacheService.isLatestCacheFresh(nowSec))
		{
			cacheHits.incrementAndGet();
			return priceCacheService.getCachedPrice(itemId);
		}
		Map<Integer, ItemPrice> map = fetchAllHerblorePrices();
		return map.get(itemId);
	}

	/**
	 * Retrieves prices for a collection of unique item IDs through the central cache,
	 * sharing a single underlying {@code /latest} fetch if the cache is expired.
	 */
	public Map<Integer, ItemPrice> getOrFetchPricesForItems(Collection<Integer> itemIds) throws IOException
	{
		long nowSec = currentEpochSeconds();
		if (priceCacheService.isLatestCacheFresh(nowSec))
		{
			cacheHits.incrementAndGet();
			return priceCacheService.getCachedPricesForIds(itemIds);
		}
		fetchAllHerblorePrices();
		return priceCacheService.getCachedPricesForIds(itemIds);
	}

	/**
	 * Fetches or returns cached bulk market data using {@link PriceCalculationMode#INSTANT_CONSERVATIVE}
	 * (which only requires {@code /latest} and local item metadata).
	 */
	public Map<Integer, ItemPrice> fetchAllHerblorePrices() throws IOException
	{
		return fetchAllHerblorePrices(PriceCalculationMode.INSTANT_CONSERVATIVE, false);
	}

	/**
	 * Central bulk price retrieval method with tiered cache validation, lazy historical loading,
	 * in-flight request deduplication, rate-limit compliance, and stale-while-revalidate fallback.
	 *
	 * @param activeMode   Current price calculation mode (determines whether {@code /5m} or {@code /1h} is required)
	 * @param forceRefresh If true, bypasses the {@code /latest} TTL check unless rate-limited or circuit-broken
	 */
	public Map<Integer, ItemPrice> fetchAllHerblorePrices(PriceCalculationMode activeMode, boolean forceRefresh) throws IOException
	{
		long nowSec = currentEpochSeconds();
		PriceCalculationMode safeMode = activeMode != null ? activeMode : PriceCalculationMode.INSTANT_CONSERVATIVE;

		boolean need5m = (safeMode == PriceCalculationMode.FIVE_MINUTE_AVERAGE) && !priceCacheService.is5mCacheFresh(nowSec);
		boolean need1h = (safeMode == PriceCalculationMode.ONE_HOUR_AVERAGE) && !priceCacheService.is1hCacheFresh(nowSec);
		boolean latestFresh = !forceRefresh && priceCacheService.isLatestCacheFresh(nowSec);

		if (latestFresh && !need5m && !need1h)
		{
			cacheHits.incrementAndGet();
			Map<Integer, ItemPrice> cached = priceCacheService.getCurrentCachedPrices();
			connected = true;
			connectionStatusMessage = "Connected (" + cached.size() + " cached items)";
			return cached;
		}

		cacheMisses.incrementAndGet();

		try
		{
			Map<Integer, ItemPrice> workingMap;
			if (latestFresh)
			{
				workingMap = new HashMap<>(priceCacheService.getCurrentCachedPrices());
			}
			else
			{
				String latestJson = executeDeduplicatedEndpoint("/latest");
				long fetchedAtSec = currentEpochSeconds();
				Map<Integer, ItemPrice> parsedLatest = parseLatestJson(latestJson, fetchedAtSec);
				if (parsedLatest.isEmpty())
				{
					throw new IOException("OSRS Wiki /latest returned 0 valid item prices.");
				}
				// Merge with existing cache so previously fetched 5m/1h/mapping fields are preserved
				workingMap = new HashMap<>(priceCacheService.getCurrentCachedPrices());
				for (Map.Entry<Integer, ItemPrice> entry : parsedLatest.entrySet())
				{
					workingMap.put(entry.getKey(), entry.getValue());
				}
				mergeMappingMetadata(workingMap, priceCacheService.getCachedMappingMetadata());
				enrichWithLocalDefinitions(workingMap);
				priceCacheService.saveLivePrices(workingMap, fetchedAtSec);
			}

			// Lazy on-demand /5m fetch ONLY when PriceCalculationMode.AVERAGE_5M is active and 5m cache expired
			if (need5m)
			{
				try
				{
					String fiveMinJson = executeDeduplicatedEndpoint("/5m");
					long fetchedAtSec = currentEpochSeconds();
					merge5mJson(workingMap, fiveMinJson, fetchedAtSec);
					priceCacheService.saveLivePrices(workingMap, fetchedAtSec);
					priceCacheService.mark5mUpdated(fetchedAtSec);
				}
				catch (Exception ex)
				{
					log.debug("[HerbWise] Non-fatal: /5m on-demand fetch skipped ({}); preserving existing prices.", ex.getMessage());
				}
			}

			// Lazy on-demand /1h fetch ONLY when PriceCalculationMode.AVERAGE_1H is active and 1h cache expired
			if (need1h)
			{
				try
				{
					String oneHourJson = executeDeduplicatedEndpoint("/1h");
					long fetchedAtSec = currentEpochSeconds();
					merge1hJson(workingMap, oneHourJson, fetchedAtSec);
					priceCacheService.saveLivePrices(workingMap, fetchedAtSec);
					priceCacheService.mark1hUpdated(fetchedAtSec);
				}
				catch (Exception ex)
				{
					log.debug("[HerbWise] Non-fatal: /1h on-demand fetch skipped ({}); preserving existing prices.", ex.getMessage());
				}
			}

			Map<Integer, ItemPrice> finalMap = priceCacheService.getCurrentCachedPrices();
			connected = true;
			connectionStatusMessage = "Connected (" + finalMap.size() + " items via OSRS Wiki API)";
			return finalMap;
		}
		catch (IOException ex)
		{
			connected = false;
			if (isRateLimited())
			{
				connectionStatusMessage = "Rate Limited — Retry in " + getRemainingCooldownSeconds() + "s (Using Cached Data)";
			}
			else if (isCircuitBreakerOpen())
			{
				connectionStatusMessage = "Circuit Breaker Active — Cooldown " + getRemainingCooldownSeconds() + "s (Using Cached Data)";
			}
			else
			{
				connectionStatusMessage = "API Offline — Using Cached Prices (" + ex.getMessage() + ")";
			}
			throw ex;
		}
	}

	/**
	 * Fetches {@code /5m} and/or {@code /1h} rolling averages on demand (e.g. when the user opens Market Analysis)
	 * if and only if their respective 5-minute or 1-hour cache TTL has expired.
	 */
	public Map<Integer, ItemPrice> ensureHistoricalAveragesLoaded(boolean require5m, boolean require1h) throws IOException
	{
		long nowSec = currentEpochSeconds();
		boolean need5m = require5m && !priceCacheService.is5mCacheFresh(nowSec);
		boolean need1h = require1h && !priceCacheService.is1hCacheFresh(nowSec);

		if (!need5m && !need1h)
		{
			cacheHits.incrementAndGet();
			return priceCacheService.getCurrentCachedPrices();
		}

		cacheMisses.incrementAndGet();
		Map<Integer, ItemPrice> workingMap = new HashMap<>(priceCacheService.getOrLoadFallbackPrices());
		long fetchedAtSec = currentEpochSeconds();

		if (need5m)
		{
			String fiveMinJson = executeDeduplicatedEndpoint("/5m");
			merge5mJson(workingMap, fiveMinJson, fetchedAtSec);
			priceCacheService.mark5mUpdated(fetchedAtSec);
		}
		if (need1h)
		{
			String oneHourJson = executeDeduplicatedEndpoint("/1h");
			merge1hJson(workingMap, oneHourJson, fetchedAtSec);
			priceCacheService.mark1hUpdated(fetchedAtSec);
		}

		enrichWithLocalDefinitions(workingMap);
		priceCacheService.saveLivePrices(workingMap, fetchedAtSec);
		return priceCacheService.getCurrentCachedPrices();
	}

	/**
	 * Fetches {@code /mapping} metadata on demand if the 24-hour mapping cache is expired.
	 * Cached results are reused across calls without duplicate network requests.
	 */
	public Map<Integer, ItemPrice> fetchMappingIfNeeded() throws IOException
	{
		long nowSec = currentEpochSeconds();
		if (priceCacheService.isMappingCacheFresh(nowSec))
		{
			cacheHits.incrementAndGet();
			return priceCacheService.getCachedMappingMetadata();
		}

		cacheMisses.incrementAndGet();
		String mappingJson = executeDeduplicatedEndpoint("/mapping");
		Map<Integer, ItemPrice> parsed = parseMappingJson(mappingJson);
		priceCacheService.saveMappingMetadata(parsed, currentEpochSeconds());

		// Also enrich any existing cached prices with the freshly fetched mapping metadata
		Map<Integer, ItemPrice> current = new HashMap<>(priceCacheService.getCurrentCachedPrices());
		if (!current.isEmpty())
		{
			mergeMappingMetadata(current, parsed);
			priceCacheService.saveLivePrices(current, priceCacheService.getLastLiveSaveEpochSec());
		}
		return parsed;
	}

	/**
	 * Fetches historical time-series data for a single item from {@code /timeseries?timestep={step}&id={itemId}},
	 * returning the cached analysis immediately if within the 5-minute timeseries cache TTL.
	 */
	public MarketTrendAnalysis fetchTimeseries(int itemId, String timestep) throws IOException
	{
		String safeStep = (timestep == null || timestep.trim().isEmpty()) ? "5m" : timestep.trim();
		String itemName = ItemIdMappings.getItemName(itemId);

		if (itemId <= 0)
		{
			return new MarketTrendAnalysis(
				itemId, itemName, safeStep, Collections.emptyList(),
				0L, 0L, 0L, 0L, 0L, 0L, 0.0, 0.0, false,
				TrendDirection.INSUFFICIENT_DATA, 0L
			);
		}

		long nowSec = currentEpochSeconds();
		if (priceCacheService.isTrendCacheFresh(itemId, safeStep, nowSec))
		{
			MarketTrendAnalysis cached = priceCacheService.getCachedTrendAnalysis(itemId, safeStep);
			if (cached != null)
			{
				cacheHits.incrementAndGet();
				return cached;
			}
		}

		cacheMisses.incrementAndGet();
		try
		{
			String queryPath = "/timeseries?timestep=" + safeStep + "&id=" + itemId;
			String json = executeDeduplicatedEndpoint(queryPath);
			List<TimeseriesPoint> points = parseTimeseriesJson(json);
			MarketTrendAnalysis analysis = analyzeTimeseries(itemId, itemName, safeStep, points);
			priceCacheService.cacheTrendAnalysis(itemId, safeStep, analysis, currentEpochSeconds());
			return analysis;
		}
		catch (IOException ex)
		{
			MarketTrendAnalysis cached = priceCacheService.getCachedTrendAnalysis(itemId, safeStep);
			if (cached != null)
			{
				return cached;
			}
			ItemPrice cachedPrice = priceCacheService.getCachedPrice(itemId);
			if (cachedPrice != null && cachedPrice.hasAnyPriceData())
			{
				MarketTrendAnalysis fallbackTrend = buildFallbackTrendFromItemPrice(cachedPrice, safeStep);
				priceCacheService.cacheTrendAnalysis(itemId, safeStep, fallbackTrend, currentEpochSeconds());
				return fallbackTrend;
			}
			throw ex;
		}
	}

	/**
	 * Executes an endpoint request with shared in-flight deduplication, circuit-breaker protection,
	 * HTTP 429 Retry-After handling, and exponential backoff with jitter.
	 */
	String executeDeduplicatedEndpoint(String endpointPath) throws IOException
	{
		checkRateLimitAndCircuitBreaker(endpointPath);

		EndpointFlightState state = flightStates.computeIfAbsent(endpointPath, k -> new EndpointFlightState());
		int callersBefore = state.activeCallers.getAndIncrement();
		if (callersBefore > 0)
		{
			deduplicatedRequests.incrementAndGet();
		}

		long arrivalMs = currentTimeMillis();
		try
		{
			synchronized (state.lock)
			{
				// If a concurrent leader request completed while we were waiting on state.lock, reuse its result!
				if (callersBefore > 0 && state.completedAtMs >= arrivalMs)
				{
					if (state.lastResponse != null)
					{
						cacheHits.incrementAndGet();
						return state.lastResponse;
					}
					if (state.lastError != null)
					{
						throw state.lastError;
					}
				}

				checkRateLimitAndCircuitBreaker(endpointPath);

				try
				{
					String response = executeHttpCall(V1_BASE_URL + endpointPath, endpointPath);
					activeBaseUrl = V1_BASE_URL;
					state.lastResponse = response;
					state.lastError = null;
					state.completedAtMs = currentTimeMillis();
					return response;
				}
				catch (IOException ex)
				{
					state.lastResponse = null;
					state.lastError = ex;
					state.completedAtMs = currentTimeMillis();
					throw ex;
				}
			}
		}
		finally
		{
			state.activeCallers.decrementAndGet();
		}
	}

	private void checkRateLimitAndCircuitBreaker(String endpointPath) throws IOException
	{
		long nowMs = currentTimeMillis();
		if (nowMs < rateLimitedUntilEpochMs)
		{
			long waitSec = Math.max(1L, (rateLimitedUntilEpochMs - nowMs + 999L) / 1000L);
			throw new IOException("HTTP 429 rate limit active for " + endpointPath + " (retry after " + waitSec + "s)");
		}
		if (nowMs < circuitBreakerOpenUntilEpochMs)
		{
			long waitSec = Math.max(1L, (circuitBreakerOpenUntilEpochMs - nowMs + 999L) / 1000L);
			throw new IOException("Circuit breaker open for " + endpointPath + " (cooldown " + waitSec + "s remaining)");
		}
		if (nowMs < nextBackoffAllowedEpochMs)
		{
			long waitMs = Math.max(1L, nextBackoffAllowedEpochMs - nowMs);
			throw new IOException("Exponential backoff active for " + endpointPath + " (" + waitMs + "ms remaining)");
		}
	}

	String executeWithRetry(String fullUrl) throws IOException
	{
		String endpointKey = extractEndpointCategory(fullUrl);
		return executeHttpCall(fullUrl, endpointKey);
	}

	private String executeHttpCall(String fullUrl, String endpointPath) throws IOException
	{
		String category = extractEndpointCategory(endpointPath);
		totalApiRequests.incrementAndGet();
		endpointRequestCounts.computeIfAbsent(category, k -> new AtomicLong(0L)).incrementAndGet();
		if (consecutiveFailures.get() > 0)
		{
			retryAttempts.incrementAndGet();
		}

		pendingRequests.incrementAndGet();
		long startMs = currentTimeMillis();

		Request request = new Request.Builder()
			.url(fullUrl)
			.header("User-Agent", USER_AGENT)
			.header("Accept", "application/json")
			.get()
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			long elapsedMs = Math.max(0L, currentTimeMillis() - startMs);
			totalResponseTimeMs.addAndGet(elapsedMs);
			completedTimedRequests.incrementAndGet();

			int code = response.code();
			if (response.isSuccessful() && response.body() != null)
			{
				ResponseBody body = response.body();
				String payload = body.string();
				onRequestSucceeded();
				return payload;
			}

			if (code == 429)
			{
				http429Responses.incrementAndGet();
				failedRequests.incrementAndGet();
				long retryAfterSec = parseRetryAfterSeconds(response.header("Retry-After"));
				rateLimitedUntilEpochMs = currentTimeMillis() + (retryAfterSec * 1000L);
				connected = false;
				connectionStatusMessage = "Rate Limited (HTTP 429) — Retry-After " + retryAfterSec + "s";
				throw new IOException("HTTP 429 Too Many Requests from " + fullUrl + " (Retry-After: " + retryAfterSec + "s)");
			}

			failedRequests.incrementAndGet();
			if (code == 500 || code == 502 || code == 503 || code == 504)
			{
				recordFailureForBackoffAndCircuitBreaker();
			}
			throw new IOException("HTTP " + code + " from " + fullUrl);
		}
		catch (IOException ex)
		{
			if (!ex.getMessage().startsWith("HTTP "))
			{
				failedRequests.incrementAndGet();
				recordFailureForBackoffAndCircuitBreaker();
			}
			throw ex;
		}
		finally
		{
			pendingRequests.decrementAndGet();
		}
	}

	private void onRequestSucceeded()
	{
		consecutiveFailures.set(0);
		circuitBreakerOpenUntilEpochMs = 0L;
		nextBackoffAllowedEpochMs = 0L;
	}

	private void recordFailureForBackoffAndCircuitBreaker()
	{
		int failures = consecutiveFailures.incrementAndGet();
		long nowMs = currentTimeMillis();

		int shift = Math.min(6, Math.max(0, failures - 1));
		long expDelayMs = Math.min(maxBackoffMs, initialBackoffMs * (1L << shift));
		long jitterBound = Math.max(1L, expDelayMs / 4L);
		long jitterMs = ThreadLocalRandom.current().nextLong(0L, jitterBound + 1L);
		nextBackoffAllowedEpochMs = nowMs + expDelayMs + jitterMs;

		if (failures >= circuitBreakerThreshold)
		{
			circuitBreakerOpenUntilEpochMs = nowMs + circuitBreakerCooldownMs;
			log.warn("[HerbWise] Circuit breaker opened after {} consecutive API failures (cooldown {}s).",
				failures, circuitBreakerCooldownMs / 1000L);
		}
	}

	public static long parseRetryAfterSeconds(String retryAfterHeader)
	{
		if (retryAfterHeader == null || retryAfterHeader.trim().isEmpty())
		{
			return DEFAULT_RATE_LIMIT_FALLBACK_SEC;
		}
		String trimmed = retryAfterHeader.trim();
		try
		{
			long seconds = Long.parseLong(trimmed);
			return Math.max(1L, Math.min(3600L, seconds));
		}
		catch (NumberFormatException ignored)
		{
		}
		try
		{
			ZonedDateTime date = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
			long diffSec = date.toEpochSecond() - Instant.now().getEpochSecond();
			return Math.max(1L, Math.min(3600L, diffSec));
		}
		catch (Exception ignored)
		{
		}
		return DEFAULT_RATE_LIMIT_FALLBACK_SEC;
	}

	private static String extractEndpointCategory(String pathOrUrl)
	{
		if (pathOrUrl == null)
		{
			return "unknown";
		}
		if (pathOrUrl.contains("/latest"))
		{
			return "/latest";
		}
		if (pathOrUrl.contains("/5m"))
		{
			return "/5m";
		}
		if (pathOrUrl.contains("/1h"))
		{
			return "/1h";
		}
		if (pathOrUrl.contains("/mapping"))
		{
			return "/mapping";
		}
		if (pathOrUrl.contains("/timeseries"))
		{
			return "/timeseries";
		}
		return pathOrUrl;
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
		long now = price.getLatestTradeTimestampSec() > 0 ? price.getLatestTradeTimestampSec() : currentEpochSeconds();
		long high = price.getInstantBuyPrice() != null ? price.getInstantBuyPrice() : 0L;
		long low = price.getInstantSellPrice() != null ? price.getInstantSellPrice() : high;
		int vol1h = price.getTotalVolume1h();
		int stepVol = Math.max(1, vol1h / 12);

		List<TimeseriesPoint> synthetic = new ArrayList<>(12);
		for (int i = 11; i >= 0; i--)
		{
			long ts = now - (i * 3600L);
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
