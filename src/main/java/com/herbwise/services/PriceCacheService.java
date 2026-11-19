package com.herbwise.services;

import com.herbwise.data.ItemIdMappings;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.MarketTrendAnalysis;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe, multi-tier price, mapping, and time-series caching service for HerbWise.
 * Implements:
 * - Separate TTL tracking for /latest, /5m, /1h, /mapping, and /timeseries
 * - Stale-while-revalidate preservation (never discards valid cached data on network failure)
 * - Cross-restart persistence via in-JVM shared state and RuneLite disk cache fallback
 */
@Singleton
public class PriceCacheService
{
	private static final Logger log = LoggerFactory.getLogger(PriceCacheService.class);

	/**
	 * Baseline timestamp for bundled fallback snapshot (marked as cached so it is never mistaken for live data).
	 */
	public static final long BUNDLED_SNAPSHOT_EPOCH_SEC = 1_731_500_000L;

	public static final long DEFAULT_LATEST_TTL_SEC = 60L;
	public static final long DEFAULT_5M_TTL_SEC = 300L;
	public static final long DEFAULT_1H_TTL_SEC = 3600L;
	public static final long DEFAULT_MAPPING_TTL_SEC = 86400L;
	public static final long DEFAULT_TIMESERIES_TTL_SEC = 300L;

	// Session-persistent backup so plugin stop/start cycles within a RuneLite session retain warm cache immediately
	private static final Map<Integer, ItemPrice> SESSION_PERSISTENT_PRICES = new ConcurrentHashMap<>();
	private static final Map<Integer, ItemPrice> SESSION_PERSISTENT_MAPPING = new ConcurrentHashMap<>();
	private static volatile long sessionLastLatestEpochSec = 0L;
	private static volatile long sessionLast5mEpochSec = 0L;
	private static volatile long sessionLast1hEpochSec = 0L;
	private static volatile long sessionLastMappingEpochSec = 0L;

	private final Map<Integer, ItemPrice> cachedPrices = new ConcurrentHashMap<>();
	private final Map<Integer, ItemPrice> cachedMapping = new ConcurrentHashMap<>();
	private final Map<String, MarketTrendAnalysis> cachedTrends = new ConcurrentHashMap<>();
	private final Map<String, Long> cachedTrendTimestamps = new ConcurrentHashMap<>();

	private volatile long lastLiveSaveEpochSec = 0L;
	private volatile long last5mSaveEpochSec = 0L;
	private volatile long last1hSaveEpochSec = 0L;
	private volatile long lastMappingSaveEpochSec = 0L;
	private volatile boolean usingFallbackSnapshot = false;

	private volatile long latestTtlSeconds = DEFAULT_LATEST_TTL_SEC;
	private volatile long fiveMinTtlSeconds = DEFAULT_5M_TTL_SEC;
	private volatile long oneHourTtlSeconds = DEFAULT_1H_TTL_SEC;
	private volatile long mappingTtlSeconds = DEFAULT_MAPPING_TTL_SEC;
	private volatile long timeseriesTtlSeconds = DEFAULT_TIMESERIES_TTL_SEC;

	private File persistentCacheFile;

	public PriceCacheService()
	{
		this(resolveDefaultCacheFile());
	}

	public PriceCacheService(File persistentCacheFile)
	{
		this.persistentCacheFile = persistentCacheFile;
		restoreWarmCacheOnStartup();
	}

	private static File resolveDefaultCacheFile()
	{
		try
		{
			if (RuneLite.RUNELITE_DIR != null)
			{
				File dir = new File(RuneLite.RUNELITE_DIR, "herbwise");
				return new File(dir, "price-cache.properties");
			}
		}
		catch (Throwable ignored)
		{
		}
		return null;
	}

	/**
	 * Restores previously cached prices from the session cache or persistent file on plugin startup/restart.
	 */
	public final synchronized void restoreWarmCacheOnStartup()
	{
		if (!SESSION_PERSISTENT_PRICES.isEmpty() && sessionLastLatestEpochSec > 0L)
		{
			for (Map.Entry<Integer, ItemPrice> entry : SESSION_PERSISTENT_PRICES.entrySet())
			{
				cachedPrices.put(entry.getKey(), copyItemPrice(entry.getValue(), true));
			}
			cachedMapping.putAll(SESSION_PERSISTENT_MAPPING);
			lastLiveSaveEpochSec = sessionLastLatestEpochSec;
			last5mSaveEpochSec = sessionLast5mEpochSec;
			last1hSaveEpochSec = sessionLast1hEpochSec;
			lastMappingSaveEpochSec = sessionLastMappingEpochSec;
			usingFallbackSnapshot = false;
			return;
		}

		if (persistentCacheFile != null && persistentCacheFile.isFile())
		{
			loadFromDisk(persistentCacheFile);
		}
	}

	/**
	 * Saves or merges a fresh batch of live prices into the cache without discarding existing 5m/1h/mapping fields.
	 */
	public synchronized void saveLivePrices(Map<Integer, ItemPrice> livePrices, long fetchedAtEpochSec)
	{
		if (livePrices == null || livePrices.isEmpty())
		{
			return;
		}
		long effectiveEpoch = fetchedAtEpochSec > 0L ? fetchedAtEpochSec : Instant.now().getEpochSecond();

		for (Map.Entry<Integer, ItemPrice> entry : livePrices.entrySet())
		{
			Integer id = entry.getKey();
			ItemPrice incoming = entry.getValue();
			if (id == null || id <= 0 || incoming == null || !incoming.hasAnyPriceData())
			{
				continue;
			}

			ItemPrice existing = cachedPrices.get(id);
			ItemPrice merged = mergeItemPrice(existing, incoming, effectiveEpoch);
			cachedPrices.put(id, merged);
			SESSION_PERSISTENT_PRICES.put(id, copyItemPrice(merged, true));
		}

		lastLiveSaveEpochSec = effectiveEpoch;
		sessionLastLatestEpochSec = effectiveEpoch;
		usingFallbackSnapshot = false;
		persistToDiskAsyncSafe();
	}

	private ItemPrice mergeItemPrice(ItemPrice existing, ItemPrice incoming, long fetchedAtEpochSec)
	{
		if (existing == null)
		{
			return copyItemPrice(incoming, false);
		}

		return ItemPrice.builder()
			.itemId(incoming.getItemId())
			.itemName(nonEmpty(incoming.getItemName(), existing.getItemName()))
			.examine(nonEmpty(incoming.getExamine(), existing.getExamine()))
			.members(incoming.isMembers())
			.buyLimit(incoming.getBuyLimit() > 0 ? incoming.getBuyLimit() : existing.getBuyLimit())
			.highAlch(incoming.getHighAlch() > 0 ? incoming.getHighAlch() : existing.getHighAlch())
			.storeValue(incoming.getStoreValue() > 0 ? incoming.getStoreValue() : existing.getStoreValue())
			.instantBuyPrice(incoming.getInstantBuyPrice() != null ? incoming.getInstantBuyPrice() : existing.getInstantBuyPrice())
			.instantBuyTimestampSec(incoming.getInstantBuyTimestampSec() != null ? incoming.getInstantBuyTimestampSec() : existing.getInstantBuyTimestampSec())
			.instantSellPrice(incoming.getInstantSellPrice() != null ? incoming.getInstantSellPrice() : existing.getInstantSellPrice())
			.instantSellTimestampSec(incoming.getInstantSellTimestampSec() != null ? incoming.getInstantSellTimestampSec() : existing.getInstantSellTimestampSec())
			.avgHighPrice5m(incoming.getAvgHighPrice5m() != null ? incoming.getAvgHighPrice5m() : existing.getAvgHighPrice5m())
			.avgLowPrice5m(incoming.getAvgLowPrice5m() != null ? incoming.getAvgLowPrice5m() : existing.getAvgLowPrice5m())
			.highVolume5m(incoming.getHighVolume5m() > 0 ? incoming.getHighVolume5m() : existing.getHighVolume5m())
			.lowVolume5m(incoming.getLowVolume5m() > 0 ? incoming.getLowVolume5m() : existing.getLowVolume5m())
			.avgHighPrice1h(incoming.getAvgHighPrice1h() != null ? incoming.getAvgHighPrice1h() : existing.getAvgHighPrice1h())
			.avgLowPrice1h(incoming.getAvgLowPrice1h() != null ? incoming.getAvgLowPrice1h() : existing.getAvgLowPrice1h())
			.highVolume1h(incoming.getHighVolume1h() > 0 ? incoming.getHighVolume1h() : existing.getHighVolume1h())
			.lowVolume1h(incoming.getLowVolume1h() > 0 ? incoming.getLowVolume1h() : existing.getLowVolume1h())
			.fetchedAtEpochSec(fetchedAtEpochSec)
			.fromCache(incoming.isFromCache())
			.build();
	}

	private static String nonEmpty(String preferred, String fallback)
	{
		if (preferred != null && !preferred.isEmpty() && !preferred.startsWith("Item #"))
		{
			return preferred;
		}
		return fallback != null ? fallback : preferred;
	}

	private static ItemPrice copyItemPrice(ItemPrice src, boolean markFromCache)
	{
		if (src == null)
		{
			return null;
		}
		return ItemPrice.builder()
			.itemId(src.getItemId())
			.itemName(src.getItemName())
			.examine(src.getExamine())
			.members(src.isMembers())
			.buyLimit(src.getBuyLimit())
			.highAlch(src.getHighAlch())
			.storeValue(src.getStoreValue())
			.instantBuyPrice(src.getInstantBuyPrice())
			.instantBuyTimestampSec(src.getInstantBuyTimestampSec())
			.instantSellPrice(src.getInstantSellPrice())
			.instantSellTimestampSec(src.getInstantSellTimestampSec())
			.avgHighPrice5m(src.getAvgHighPrice5m())
			.avgLowPrice5m(src.getAvgLowPrice5m())
			.highVolume5m(src.getHighVolume5m())
			.lowVolume5m(src.getLowVolume5m())
			.avgHighPrice1h(src.getAvgHighPrice1h())
			.avgLowPrice1h(src.getAvgLowPrice1h())
			.highVolume1h(src.getHighVolume1h())
			.lowVolume1h(src.getLowVolume1h())
			.fetchedAtEpochSec(src.getFetchedAtEpochSec())
			.fromCache(markFromCache || src.isFromCache())
			.build();
	}

	public synchronized void mark5mUpdated(long epochSec)
	{
		this.last5mSaveEpochSec = epochSec;
		sessionLast5mEpochSec = epochSec;
	}

	public synchronized void mark1hUpdated(long epochSec)
	{
		this.last1hSaveEpochSec = epochSec;
		sessionLast1hEpochSec = epochSec;
	}

	public synchronized void saveMappingMetadata(Map<Integer, ItemPrice> mapping, long epochSec)
	{
		if (mapping == null || mapping.isEmpty())
		{
			return;
		}
		cachedMapping.clear();
		cachedMapping.putAll(mapping);
		SESSION_PERSISTENT_MAPPING.clear();
		SESSION_PERSISTENT_MAPPING.putAll(mapping);
		this.lastMappingSaveEpochSec = epochSec;
		sessionLastMappingEpochSec = epochSec;
	}

	public Map<Integer, ItemPrice> getCachedMappingMetadata()
	{
		return Collections.unmodifiableMap(cachedMapping);
	}

	public boolean isLatestCacheFresh(long nowEpochSec)
	{
		return isLatestCacheFresh(nowEpochSec, latestTtlSeconds);
	}

	public boolean isLatestCacheFresh(long nowEpochSec, long customTtlSec)
	{
		if (usingFallbackSnapshot || lastLiveSaveEpochSec <= 0L || cachedPrices.isEmpty())
		{
			return false;
		}
		long ttl = Math.max(1L, customTtlSec);
		return (nowEpochSec - lastLiveSaveEpochSec) < ttl;
	}

	public boolean is5mCacheFresh(long nowEpochSec)
	{
		return last5mSaveEpochSec > 0L && (nowEpochSec - last5mSaveEpochSec) < fiveMinTtlSeconds;
	}

	public boolean is1hCacheFresh(long nowEpochSec)
	{
		return last1hSaveEpochSec > 0L && (nowEpochSec - last1hSaveEpochSec) < oneHourTtlSeconds;
	}

	public boolean isMappingCacheFresh(long nowEpochSec)
	{
		return !cachedMapping.isEmpty()
			&& lastMappingSaveEpochSec > 0L
			&& (nowEpochSec - lastMappingSaveEpochSec) < mappingTtlSeconds;
	}

	public boolean isTrendCacheFresh(int itemId, String timestep, long nowEpochSec)
	{
		String key = buildTrendKey(itemId, timestep);
		Long savedAt = cachedTrendTimestamps.get(key);
		return savedAt != null
			&& cachedTrends.containsKey(key)
			&& (nowEpochSec - savedAt) < timeseriesTtlSeconds;
	}

	/**
	 * Retrieves cached prices. If no live fetch has succeeded yet, loads the verified
	 * fallback snapshot with {@code fromCache = true} so calculations remain functional
	 * while clearly flagging cached status.
	 */
	public synchronized Map<Integer, ItemPrice> getOrLoadFallbackPrices()
	{
		if (!cachedPrices.isEmpty())
		{
			return Collections.unmodifiableMap(new HashMap<>(cachedPrices));
		}
		Map<Integer, ItemPrice> snapshot = ItemIdMappings.buildVerifiedFallbackCacheSnapshot(BUNDLED_SNAPSHOT_EPOCH_SEC);
		cachedPrices.putAll(snapshot);
		usingFallbackSnapshot = true;
		return Collections.unmodifiableMap(new HashMap<>(cachedPrices));
	}

	public Map<Integer, ItemPrice> getCurrentCachedPrices()
	{
		return Collections.unmodifiableMap(new HashMap<>(cachedPrices));
	}

	public Map<Integer, ItemPrice> getCachedPricesForIds(Collection<Integer> itemIds)
	{
		if (itemIds == null || itemIds.isEmpty())
		{
			return Collections.emptyMap();
		}
		Map<Integer, ItemPrice> subset = new LinkedHashMap<>(itemIds.size());
		for (Integer id : itemIds)
		{
			if (id != null)
			{
				ItemPrice price = cachedPrices.get(id);
				if (price != null)
				{
					subset.put(id, price);
				}
			}
		}
		return subset;
	}

	public ItemPrice getCachedPrice(int itemId)
	{
		return cachedPrices.get(itemId);
	}

	public void cacheTrendAnalysis(int itemId, String timestep, MarketTrendAnalysis trend)
	{
		cacheTrendAnalysis(itemId, timestep, trend, Instant.now().getEpochSecond());
	}

	public void cacheTrendAnalysis(int itemId, String timestep, MarketTrendAnalysis trend, long nowEpochSec)
	{
		if (itemId > 0 && trend != null)
		{
			String key = buildTrendKey(itemId, timestep);
			cachedTrends.put(key, trend);
			cachedTrendTimestamps.put(key, nowEpochSec);
		}
	}

	public MarketTrendAnalysis getCachedTrendAnalysis(int itemId, String timestep)
	{
		return cachedTrends.get(buildTrendKey(itemId, timestep));
	}

	private static String buildTrendKey(int itemId, String timestep)
	{
		return itemId + ":" + (timestep != null && !timestep.trim().isEmpty() ? timestep.trim() : "1h");
	}

	public synchronized void clearCache()
	{
		cachedPrices.clear();
		cachedMapping.clear();
		cachedTrends.clear();
		cachedTrendTimestamps.clear();
		lastLiveSaveEpochSec = 0L;
		last5mSaveEpochSec = 0L;
		last1hSaveEpochSec = 0L;
		lastMappingSaveEpochSec = 0L;
		usingFallbackSnapshot = false;
	}

	public static synchronized void clearSessionPersistentCache()
	{
		SESSION_PERSISTENT_PRICES.clear();
		SESSION_PERSISTENT_MAPPING.clear();
		sessionLastLatestEpochSec = 0L;
		sessionLast5mEpochSec = 0L;
		sessionLast1hEpochSec = 0L;
		sessionLastMappingEpochSec = 0L;
	}

	private void persistToDiskAsyncSafe()
	{
		if (persistentCacheFile == null)
		{
			return;
		}
		saveToDisk(persistentCacheFile);
	}

	public synchronized void saveToDisk(File targetFile)
	{
		if (targetFile == null || cachedPrices.isEmpty() || usingFallbackSnapshot)
		{
			return;
		}
		try
		{
			File parent = targetFile.getParentFile();
			if (parent != null && !parent.exists())
			{
				parent.mkdirs();
			}
			Properties props = new Properties();
			props.setProperty("meta.lastLiveSaveEpochSec", String.valueOf(lastLiveSaveEpochSec));
			props.setProperty("meta.last5mSaveEpochSec", String.valueOf(last5mSaveEpochSec));
			props.setProperty("meta.last1hSaveEpochSec", String.valueOf(last1hSaveEpochSec));

			for (ItemPrice p : cachedPrices.values())
			{
				if (p == null || p.getItemId() <= 0 || !p.hasAnyPriceData())
				{
					continue;
				}
				String prefix = "item." + p.getItemId() + ".";
				if (p.getInstantBuyPrice() != null)
				{
					props.setProperty(prefix + "high", String.valueOf(p.getInstantBuyPrice()));
				}
				if (p.getInstantBuyTimestampSec() != null)
				{
					props.setProperty(prefix + "highTime", String.valueOf(p.getInstantBuyTimestampSec()));
				}
				if (p.getInstantSellPrice() != null)
				{
					props.setProperty(prefix + "low", String.valueOf(p.getInstantSellPrice()));
				}
				if (p.getInstantSellTimestampSec() != null)
				{
					props.setProperty(prefix + "lowTime", String.valueOf(p.getInstantSellTimestampSec()));
				}
				if (p.getAvgHighPrice5m() != null)
				{
					props.setProperty(prefix + "high5m", String.valueOf(p.getAvgHighPrice5m()));
				}
				if (p.getAvgLowPrice5m() != null)
				{
					props.setProperty(prefix + "low5m", String.valueOf(p.getAvgLowPrice5m()));
				}
				if (p.getHighVolume5m() > 0)
				{
					props.setProperty(prefix + "hVol5m", String.valueOf(p.getHighVolume5m()));
				}
				if (p.getLowVolume5m() > 0)
				{
					props.setProperty(prefix + "lVol5m", String.valueOf(p.getLowVolume5m()));
				}
				if (p.getAvgHighPrice1h() != null)
				{
					props.setProperty(prefix + "high1h", String.valueOf(p.getAvgHighPrice1h()));
				}
				if (p.getAvgLowPrice1h() != null)
				{
					props.setProperty(prefix + "low1h", String.valueOf(p.getAvgLowPrice1h()));
				}
				if (p.getHighVolume1h() > 0)
				{
					props.setProperty(prefix + "hVol1h", String.valueOf(p.getHighVolume1h()));
				}
				if (p.getLowVolume1h() > 0)
				{
					props.setProperty(prefix + "lVol1h", String.valueOf(p.getLowVolume1h()));
				}
			}

			try (FileOutputStream fos = new FileOutputStream(targetFile))
			{
				props.store(fos, "HerbWise Persistent Price Cache");
			}
		}
		catch (Exception ex)
		{
			log.debug("[HerbWise] Could not persist price cache to disk: {}", ex.getMessage());
		}
	}

	public synchronized boolean loadFromDisk(File sourceFile)
	{
		if (sourceFile == null || !sourceFile.isFile())
		{
			return false;
		}
		try
		{
			Properties props = new Properties();
			try (FileInputStream fis = new FileInputStream(sourceFile))
			{
				props.load(fis);
			}

			long savedAt = parseLongProp(props, "meta.lastLiveSaveEpochSec", 0L);
			if (savedAt <= 0L)
			{
				return false;
			}

			Map<Integer, ItemPrice> loaded = new HashMap<>();
			for (String key : props.stringPropertyNames())
			{
				if (!key.startsWith("item.") || !key.endsWith(".high") && !key.endsWith(".low"))
				{
					continue;
				}
				String[] parts = key.split("\\.");
				if (parts.length < 3)
				{
					continue;
				}
				int itemId;
				try
				{
					itemId = Integer.parseInt(parts[1]);
				}
				catch (NumberFormatException e)
				{
					continue;
				}
				if (itemId <= 0 || loaded.containsKey(itemId))
				{
					continue;
				}

				String prefix = "item." + itemId + ".";
				Long high = parseNullableLongProp(props, prefix + "high");
				Long highTime = parseNullableLongProp(props, prefix + "highTime");
				Long low = parseNullableLongProp(props, prefix + "low");
				Long lowTime = parseNullableLongProp(props, prefix + "lowTime");
				Long high5m = parseNullableLongProp(props, prefix + "high5m");
				Long low5m = parseNullableLongProp(props, prefix + "low5m");
				int hVol5m = (int) parseLongProp(props, prefix + "hVol5m", 0L);
				int lVol5m = (int) parseLongProp(props, prefix + "lVol5m", 0L);
				Long high1h = parseNullableLongProp(props, prefix + "high1h");
				Long low1h = parseNullableLongProp(props, prefix + "low1h");
				int hVol1h = (int) parseLongProp(props, prefix + "hVol1h", 0L);
				int lVol1h = (int) parseLongProp(props, prefix + "lVol1h", 0L);

				ItemPrice itemPrice = ItemPrice.builder()
					.itemId(itemId)
					.itemName(ItemIdMappings.getItemName(itemId))
					.buyLimit(ItemIdMappings.getBuyLimit(itemId))
					.instantBuyPrice(high)
					.instantBuyTimestampSec(highTime)
					.instantSellPrice(low)
					.instantSellTimestampSec(lowTime)
					.avgHighPrice5m(high5m)
					.avgLowPrice5m(low5m)
					.highVolume5m(hVol5m)
					.lowVolume5m(lVol5m)
					.avgHighPrice1h(high1h)
					.avgLowPrice1h(low1h)
					.highVolume1h(hVol1h)
					.lowVolume1h(lVol1h)
					.fetchedAtEpochSec(savedAt)
					.fromCache(true)
					.build();
				loaded.put(itemId, itemPrice);
			}

			if (!loaded.isEmpty())
			{
				cachedPrices.putAll(loaded);
				SESSION_PERSISTENT_PRICES.putAll(loaded);
				lastLiveSaveEpochSec = savedAt;
				last5mSaveEpochSec = parseLongProp(props, "meta.last5mSaveEpochSec", 0L);
				last1hSaveEpochSec = parseLongProp(props, "meta.last1hSaveEpochSec", 0L);
				sessionLastLatestEpochSec = lastLiveSaveEpochSec;
				sessionLast5mEpochSec = last5mSaveEpochSec;
				sessionLast1hEpochSec = last1hSaveEpochSec;
				usingFallbackSnapshot = false;
				return true;
			}
		}
		catch (Exception ex)
		{
			log.debug("[HerbWise] Failed to load persistent price cache: {}", ex.getMessage());
		}
		return false;
	}

	private static long parseLongProp(Properties props, String key, long def)
	{
		String val = props.getProperty(key);
		if (val == null || val.trim().isEmpty())
		{
			return def;
		}
		try
		{
			return Long.parseLong(val.trim());
		}
		catch (NumberFormatException ex)
		{
			return def;
		}
	}

	private static Long parseNullableLongProp(Properties props, String key)
	{
		long v = parseLongProp(props, key, -1L);
		return v > 0L ? v : null;
	}

	public long getLastLiveSaveEpochSec() { return lastLiveSaveEpochSec; }
	public long getLast5mSaveEpochSec() { return last5mSaveEpochSec; }
	public long getLast1hSaveEpochSec() { return last1hSaveEpochSec; }
	public long getLastMappingSaveEpochSec() { return lastMappingSaveEpochSec; }

	public boolean isUsingFallbackSnapshot() { return usingFallbackSnapshot; }
	public int getCachedItemCount() { return cachedPrices.size(); }

	public long getLatestTtlSeconds() { return latestTtlSeconds; }
	public void setLatestTtlSeconds(long ttlSeconds) { this.latestTtlSeconds = Math.max(1L, ttlSeconds); }

	public long getFiveMinTtlSeconds() { return fiveMinTtlSeconds; }
	public void setFiveMinTtlSeconds(long ttlSeconds) { this.fiveMinTtlSeconds = Math.max(1L, ttlSeconds); }

	public long getOneHourTtlSeconds() { return oneHourTtlSeconds; }
	public void setOneHourTtlSeconds(long ttlSeconds) { this.oneHourTtlSeconds = Math.max(1L, ttlSeconds); }

	public long getMappingTtlSeconds() { return mappingTtlSeconds; }
	public void setMappingTtlSeconds(long ttlSeconds) { this.mappingTtlSeconds = Math.max(1L, ttlSeconds); }

	public long getTimeseriesTtlSeconds() { return timeseriesTtlSeconds; }
	public void setTimeseriesTtlSeconds(long ttlSeconds) { this.timeseriesTtlSeconds = Math.max(1L, ttlSeconds); }
}
