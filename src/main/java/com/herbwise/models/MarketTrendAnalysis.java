package com.herbwise.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Historical time-series point and trend analysis summary for the Market Analysis tab.
 */
public class MarketTrendAnalysis
{
	public enum TrendDirection
	{
		RISING("Becoming More Expensive (Rising)", "+"),
		FALLING("Becoming Cheaper (Falling)", ""),
		STABLE("Stable Price Movement", ""),
		INSUFFICIENT_DATA("Insufficient Historical Data", "");

		private final String description;
		private final String prefix;

		TrendDirection(String description, String prefix)
		{
			this.description = description;
			this.prefix = prefix;
		}

		public String getDescription() { return description; }
		public String getPrefix() { return prefix; }
	}

	public static class TimeseriesPoint
	{
		private final long timestampEpochSec;
		private final Long avgHighPrice;
		private final Long avgLowPrice;
		private final int highPriceVolume;
		private final int lowPriceVolume;

		public TimeseriesPoint(
			long timestampEpochSec,
			Long avgHighPrice,
			Long avgLowPrice,
			int highPriceVolume,
			int lowPriceVolume)
		{
			this.timestampEpochSec = timestampEpochSec;
			this.avgHighPrice = avgHighPrice;
			this.avgLowPrice = avgLowPrice;
			this.highPriceVolume = Math.max(0, highPriceVolume);
			this.lowPriceVolume = Math.max(0, lowPriceVolume);
		}

		public long getTimestampEpochSec() { return timestampEpochSec; }
		public Long getAvgHighPrice() { return avgHighPrice; }
		public Long getAvgLowPrice() { return avgLowPrice; }
		public int getHighPriceVolume() { return highPriceVolume; }
		public int getLowPriceVolume() { return lowPriceVolume; }
		public int getTotalVolume() { return highPriceVolume + lowPriceVolume; }

		public long getRepresentativePrice()
		{
			if (avgHighPrice != null && avgHighPrice > 0 && avgLowPrice != null && avgLowPrice > 0)
			{
				return (avgHighPrice + avgLowPrice) / 2L;
			}
			if (avgHighPrice != null && avgHighPrice > 0)
			{
				return avgHighPrice;
			}
			if (avgLowPrice != null && avgLowPrice > 0)
			{
				return avgLowPrice;
			}
			return 0L;
		}
	}

	private final int itemId;
	private final String itemName;
	private final String timestep;
	private final List<TimeseriesPoint> points;
	private final long firstPrice;
	private final long latestPrice;
	private final long minPrice;
	private final long maxPrice;
	private final long averagePrice;
	private final long priceChangeGp;
	private final double priceChangePercent;
	private final double volatilityPercent;
	private final boolean volatileWarning;
	private final TrendDirection direction;
	private final long totalVolumeInWindow;

	public MarketTrendAnalysis(
		int itemId,
		String itemName,
		String timestep,
		List<TimeseriesPoint> points,
		long firstPrice,
		long latestPrice,
		long minPrice,
		long maxPrice,
		long averagePrice,
		long priceChangeGp,
		double priceChangePercent,
		double volatilityPercent,
		boolean volatileWarning,
		TrendDirection direction,
		long totalVolumeInWindow)
	{
		this.itemId = itemId;
		this.itemName = itemName != null ? itemName : ("Item #" + itemId);
		this.timestep = timestep != null ? timestep : "1h";
		this.points = points != null
			? Collections.unmodifiableList(new ArrayList<>(points))
			: Collections.emptyList();
		this.firstPrice = firstPrice;
		this.latestPrice = latestPrice;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.averagePrice = averagePrice;
		this.priceChangeGp = priceChangeGp;
		this.priceChangePercent = priceChangePercent;
		this.volatilityPercent = volatilityPercent;
		this.volatileWarning = volatileWarning;
		this.direction = direction != null ? direction : TrendDirection.INSUFFICIENT_DATA;
		this.totalVolumeInWindow = Math.max(0L, totalVolumeInWindow);
	}

	public int getItemId() { return itemId; }
	public String getItemName() { return itemName; }
	public String getTimestep() { return timestep; }
	public List<TimeseriesPoint> getPoints() { return points; }
	public long getFirstPrice() { return firstPrice; }
	public long getLatestPrice() { return latestPrice; }
	public long getMinPrice() { return minPrice; }
	public long getMaxPrice() { return maxPrice; }
	public long getAveragePrice() { return averagePrice; }
	public long getPriceChangeGp() { return priceChangeGp; }
	public double getPriceChangePercent() { return priceChangePercent; }
	public double getVolatilityPercent() { return volatilityPercent; }
	public boolean isVolatileWarning() { return volatileWarning; }
	public TrendDirection getDirection() { return direction; }
	public long getTotalVolumeInWindow() { return totalVolumeInWindow; }
}
