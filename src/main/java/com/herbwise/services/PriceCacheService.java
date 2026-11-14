package com.herbwise.services;

import com.herbwise.data.ItemIdMappings;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.MarketTrendAnalysis;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

/**
 * Thread-safe price and time-series caching service for HerbWise.
 * Stores live responses from the OSRS Wiki API and provides a verified cached fallback
 * (explicitly marked with {@code fromCache = true}) if the network or API is unavailable.
 */
@Singleton
public class PriceCacheService
{
	/**
	 * Baseline timestamp for bundled fallback snapshot (marked as cached so it is never mistaken for live data).
	 */
	public static final long BUNDLED_SNAPSHOT_EPOCH_SEC = 1_731_500_000L;

	private final Map<Integer, ItemPrice> cachedPrices = new ConcurrentHashMap<>();
	private final Map<String, MarketTrendAnalysis> cachedTrends = new ConcurrentHashMap<>();

	private volatile long lastLiveSaveEpochSec = 0L;
	private volatile boolean usingFallbackSnapshot = false;

	public PriceCacheService()
	{
	}

	/**
	 * Saves a fresh batch of live prices into the cache.
	 */
	public void saveLivePrices(Map<Integer, ItemPrice> livePrices, long fetchedAtEpochSec)
	{
		if (livePrices == null || livePrices.isEmpty())
		{
			return;
		}
		cachedPrices.clear();
		cachedPrices.putAll(livePrices);
		lastLiveSaveEpochSec = fetchedAtEpochSec > 0 ? fetchedAtEpochSec : Instant.now().getEpochSecond();
		usingFallbackSnapshot = false;
	}

	/**
	 * Retrieves cached prices. If no live fetch has succeeded yet, loads the verified
	 * fallback snapshot with {@code fromCache = true} so calculations remain functional
	 * while clearly flagging cached status.
	 */
	public Map<Integer, ItemPrice> getOrLoadFallbackPrices()
	{
		if (!cachedPrices.isEmpty())
		{
			return Collections.unmodifiableMap(cachedPrices);
		}
		Map<Integer, ItemPrice> snapshot = ItemIdMappings.buildVerifiedFallbackCacheSnapshot(BUNDLED_SNAPSHOT_EPOCH_SEC);
		cachedPrices.putAll(snapshot);
		usingFallbackSnapshot = true;
		return Collections.unmodifiableMap(cachedPrices);
	}

	public Map<Integer, ItemPrice> getCurrentCachedPrices()
	{
		return Collections.unmodifiableMap(cachedPrices);
	}

	public ItemPrice getCachedPrice(int itemId)
	{
		return cachedPrices.get(itemId);
	}

	public void cacheTrendAnalysis(int itemId, String timestep, MarketTrendAnalysis trend)
	{
		if (itemId > 0 && trend != null)
		{
			String key = itemId + ":" + (timestep != null ? timestep : "1h");
			cachedTrends.put(key, trend);
		}
	}

	public MarketTrendAnalysis getCachedTrendAnalysis(int itemId, String timestep)
	{
		String key = itemId + ":" + (timestep != null ? timestep : "1h");
		return cachedTrends.get(key);
	}

	public void clearCache()
	{
		cachedPrices.clear();
		cachedTrends.clear();
		lastLiveSaveEpochSec = 0L;
		usingFallbackSnapshot = false;
	}

	public long getLastLiveSaveEpochSec()
	{
		return lastLiveSaveEpochSec;
	}

	public boolean isUsingFallbackSnapshot()
	{
		return usingFallbackSnapshot;
	}

	public int getCachedItemCount()
	{
		return cachedPrices.size();
	}
}
