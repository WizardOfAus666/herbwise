package com.herbwise.models;

/**
 * Holds real-time and rolling-average price and volume information for a single OSRS item
 * retrieved from the OSRS Wiki Real-Time Prices API (/latest, /5m, /1h, /mapping).
 */
public class ItemPrice
{
	private final int itemId;
	private String itemName;
	private String examine;
	private boolean members;
	private int buyLimit;
	private int highAlch;
	private int storeValue;

	private Long instantBuyPrice;
	private Long instantBuyTimestampSec;
	private Long instantSellPrice;
	private Long instantSellTimestampSec;

	private Long avgHighPrice5m;
	private Long avgLowPrice5m;
	private int highVolume5m;
	private int lowVolume5m;

	private Long avgHighPrice1h;
	private Long avgLowPrice1h;
	private int highVolume1h;
	private int lowVolume1h;

	private long fetchedAtEpochSec;
	private boolean fromCache;

	public ItemPrice(int itemId, String itemName)
	{
		this.itemId = itemId;
		this.itemName = itemName != null ? itemName : ("Item #" + itemId);
	}

	private ItemPrice(Builder builder)
	{
		this.itemId = builder.itemId;
		this.itemName = builder.itemName != null ? builder.itemName : ("Item #" + builder.itemId);
		this.examine = builder.examine;
		this.members = builder.members;
		this.buyLimit = builder.buyLimit;
		this.highAlch = builder.highAlch;
		this.storeValue = builder.storeValue;
		this.instantBuyPrice = builder.instantBuyPrice;
		this.instantBuyTimestampSec = builder.instantBuyTimestampSec;
		this.instantSellPrice = builder.instantSellPrice;
		this.instantSellTimestampSec = builder.instantSellTimestampSec;
		this.avgHighPrice5m = builder.avgHighPrice5m;
		this.avgLowPrice5m = builder.avgLowPrice5m;
		this.highVolume5m = builder.highVolume5m;
		this.lowVolume5m = builder.lowVolume5m;
		this.avgHighPrice1h = builder.avgHighPrice1h;
		this.avgLowPrice1h = builder.avgLowPrice1h;
		this.highVolume1h = builder.highVolume1h;
		this.lowVolume1h = builder.lowVolume1h;
		this.fetchedAtEpochSec = builder.fetchedAtEpochSec;
		this.fromCache = builder.fromCache;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public int getItemId() { return itemId; }
	public String getItemName() { return itemName; }
	public void setItemName(String itemName) { this.itemName = itemName; }
	public String getExamine() { return examine; }
	public void setExamine(String examine) { this.examine = examine; }
	public boolean isMembers() { return members; }
	public void setMembers(boolean members) { this.members = members; }
	public int getBuyLimit() { return buyLimit; }
	public void setBuyLimit(int buyLimit) { this.buyLimit = buyLimit; }
	public int getHighAlch() { return highAlch; }
	public void setHighAlch(int highAlch) { this.highAlch = highAlch; }
	public int getStoreValue() { return storeValue; }
	public void setStoreValue(int storeValue) { this.storeValue = storeValue; }

	public Long getInstantBuyPrice() { return instantBuyPrice; }
	public void setInstantBuyPrice(Long instantBuyPrice) { this.instantBuyPrice = instantBuyPrice; }
	public Long getInstantBuyTimestampSec() { return instantBuyTimestampSec; }
	public void setInstantBuyTimestampSec(Long instantBuyTimestampSec) { this.instantBuyTimestampSec = instantBuyTimestampSec; }

	public Long getInstantSellPrice() { return instantSellPrice; }
	public void setInstantSellPrice(Long instantSellPrice) { this.instantSellPrice = instantSellPrice; }
	public Long getInstantSellTimestampSec() { return instantSellTimestampSec; }
	public void setInstantSellTimestampSec(Long instantSellTimestampSec) { this.instantSellTimestampSec = instantSellTimestampSec; }

	public Long getAvgHighPrice5m() { return avgHighPrice5m; }
	public void setAvgHighPrice5m(Long avgHighPrice5m) { this.avgHighPrice5m = avgHighPrice5m; }
	public Long getAvgLowPrice5m() { return avgLowPrice5m; }
	public void setAvgLowPrice5m(Long avgLowPrice5m) { this.avgLowPrice5m = avgLowPrice5m; }
	public int getHighVolume5m() { return highVolume5m; }
	public void setHighVolume5m(int highVolume5m) { this.highVolume5m = highVolume5m; }
	public int getLowVolume5m() { return lowVolume5m; }
	public void setLowVolume5m(int lowVolume5m) { this.lowVolume5m = lowVolume5m; }

	public Long getAvgHighPrice1h() { return avgHighPrice1h; }
	public void setAvgHighPrice1h(Long avgHighPrice1h) { this.avgHighPrice1h = avgHighPrice1h; }
	public Long getAvgLowPrice1h() { return avgLowPrice1h; }
	public void setAvgLowPrice1h(Long avgLowPrice1h) { this.avgLowPrice1h = avgLowPrice1h; }
	public int getHighVolume1h() { return highVolume1h; }
	public void setHighVolume1h(int highVolume1h) { this.highVolume1h = highVolume1h; }
	public int getLowVolume1h() { return lowVolume1h; }
	public void setLowVolume1h(int lowVolume1h) { this.lowVolume1h = lowVolume1h; }

	public long getFetchedAtEpochSec() { return fetchedAtEpochSec; }
	public void setFetchedAtEpochSec(long fetchedAtEpochSec) { this.fetchedAtEpochSec = fetchedAtEpochSec; }
	public boolean isFromCache() { return fromCache; }
	public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }

	public int getTotalVolume5m()
	{
		return Math.max(0, highVolume5m) + Math.max(0, lowVolume5m);
	}

	public int getTotalVolume1h()
	{
		return Math.max(0, highVolume1h) + Math.max(0, lowVolume1h);
	}

	public boolean hasAnyPriceData()
	{
		return isPositive(instantBuyPrice)
			|| isPositive(instantSellPrice)
			|| isPositive(avgHighPrice5m)
			|| isPositive(avgLowPrice5m)
			|| isPositive(avgHighPrice1h)
			|| isPositive(avgLowPrice1h);
	}

	/**
	 * Resolves the realistic buying price (what a player pays for an ingredient) under the chosen mode.
	 * Returns null if no valid positive price exists (never substitutes 0).
	 */
	public Long resolveBuyPrice(PriceCalculationMode mode)
	{
		PriceCalculationMode effective = mode != null ? mode : PriceCalculationMode.INSTANT_CONSERVATIVE;
		switch (effective)
		{
			case FIVE_MINUTE_AVERAGE:
				if (isPositive(avgHighPrice5m)) return avgHighPrice5m;
				if (isPositive(avgLowPrice5m)) return avgLowPrice5m;
				if (isPositive(instantBuyPrice)) return instantBuyPrice;
				return isPositive(instantSellPrice) ? instantSellPrice : null;

			case ONE_HOUR_AVERAGE:
				if (isPositive(avgHighPrice1h)) return avgHighPrice1h;
				if (isPositive(avgLowPrice1h)) return avgLowPrice1h;
				if (isPositive(instantBuyPrice)) return instantBuyPrice;
				return isPositive(instantSellPrice) ? instantSellPrice : null;

			case PATIENT_OFFER:
				if (isPositive(instantSellPrice)) return instantSellPrice;
				if (isPositive(instantBuyPrice)) return instantBuyPrice;
				return null;

			case INSTANT_CONSERVATIVE:
			default:
				if (isPositive(instantBuyPrice)) return instantBuyPrice;
				if (isPositive(instantSellPrice)) return instantSellPrice;
				return null;
		}
	}

	/**
	 * Resolves the realistic selling price (what a player receives before GE tax when selling a finished potion)
	 * under the chosen mode. Returns null if no valid positive price exists.
	 */
	public Long resolveSellPrice(PriceCalculationMode mode)
	{
		PriceCalculationMode effective = mode != null ? mode : PriceCalculationMode.INSTANT_CONSERVATIVE;
		switch (effective)
		{
			case FIVE_MINUTE_AVERAGE:
				if (isPositive(avgLowPrice5m)) return avgLowPrice5m;
				if (isPositive(avgHighPrice5m)) return avgHighPrice5m;
				if (isPositive(instantSellPrice)) return instantSellPrice;
				return isPositive(instantBuyPrice) ? instantBuyPrice : null;

			case ONE_HOUR_AVERAGE:
				if (isPositive(avgLowPrice1h)) return avgLowPrice1h;
				if (isPositive(avgHighPrice1h)) return avgHighPrice1h;
				if (isPositive(instantSellPrice)) return instantSellPrice;
				return isPositive(instantBuyPrice) ? instantBuyPrice : null;

			case PATIENT_OFFER:
				if (isPositive(instantBuyPrice)) return instantBuyPrice;
				if (isPositive(instantSellPrice)) return instantSellPrice;
				return null;

			case INSTANT_CONSERVATIVE:
			default:
				if (isPositive(instantSellPrice)) return instantSellPrice;
				if (isPositive(instantBuyPrice)) return instantBuyPrice;
				return null;
		}
	}

	public long getLatestTradeTimestampSec()
	{
		long highTs = instantBuyTimestampSec != null ? instantBuyTimestampSec : 0L;
		long lowTs = instantSellTimestampSec != null ? instantSellTimestampSec : 0L;
		long best = Math.max(highTs, lowTs);
		return best > 0 ? best : fetchedAtEpochSec;
	}

	public long getAgeSeconds(long nowEpochSec)
	{
		long ts = getLatestTradeTimestampSec();
		if (ts <= 0 || nowEpochSec <= ts)
		{
			return 0L;
		}
		return nowEpochSec - ts;
	}

	public boolean isStale(long nowEpochSec, long maxAgeSeconds)
	{
		if (maxAgeSeconds <= 0)
		{
			return false;
		}
		long ts = getLatestTradeTimestampSec();
		if (ts <= 0)
		{
			return true;
		}
		return (nowEpochSec - ts) > maxAgeSeconds;
	}

	public double getSpreadPercent()
	{
		if (isPositive(instantBuyPrice) && isPositive(instantSellPrice) && instantSellPrice > 0)
		{
			long diff = Math.abs(instantBuyPrice - instantSellPrice);
			return ((double) diff / (double) instantSellPrice) * 100.0;
		}
		return 0.0;
	}

	public boolean isVolatile(double maxSpreadPercent)
	{
		if (getSpreadPercent() > maxSpreadPercent)
		{
			return true;
		}
		if (isPositive(avgHighPrice5m) && isPositive(avgHighPrice1h) && avgHighPrice1h > 0)
		{
			double deltaPct = Math.abs((double) (avgHighPrice5m - avgHighPrice1h) / (double) avgHighPrice1h) * 100.0;
			if (deltaPct > maxSpreadPercent)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isPositive(Long value)
	{
		return value != null && value > 0L;
	}

	public static class Builder
	{
		private int itemId;
		private String itemName;
		private String examine;
		private boolean members = true;
		private int buyLimit;
		private int highAlch;
		private int storeValue;
		private Long instantBuyPrice;
		private Long instantBuyTimestampSec;
		private Long instantSellPrice;
		private Long instantSellTimestampSec;
		private Long avgHighPrice5m;
		private Long avgLowPrice5m;
		private int highVolume5m;
		private int lowVolume5m;
		private Long avgHighPrice1h;
		private Long avgLowPrice1h;
		private int highVolume1h;
		private int lowVolume1h;
		private long fetchedAtEpochSec;
		private boolean fromCache;

		public Builder itemId(int itemId) { this.itemId = itemId; return this; }
		public Builder itemName(String itemName) { this.itemName = itemName; return this; }
		public Builder examine(String examine) { this.examine = examine; return this; }
		public Builder members(boolean members) { this.members = members; return this; }
		public Builder buyLimit(int buyLimit) { this.buyLimit = buyLimit; return this; }
		public Builder highAlch(int highAlch) { this.highAlch = highAlch; return this; }
		public Builder storeValue(int storeValue) { this.storeValue = storeValue; return this; }
		public Builder instantBuyPrice(Long instantBuyPrice) { this.instantBuyPrice = instantBuyPrice; return this; }
		public Builder instantBuyTimestampSec(Long instantBuyTimestampSec) { this.instantBuyTimestampSec = instantBuyTimestampSec; return this; }
		public Builder instantSellPrice(Long instantSellPrice) { this.instantSellPrice = instantSellPrice; return this; }
		public Builder instantSellTimestampSec(Long instantSellTimestampSec) { this.instantSellTimestampSec = instantSellTimestampSec; return this; }
		public Builder avgHighPrice5m(Long avgHighPrice5m) { this.avgHighPrice5m = avgHighPrice5m; return this; }
		public Builder avgLowPrice5m(Long avgLowPrice5m) { this.avgLowPrice5m = avgLowPrice5m; return this; }
		public Builder highVolume5m(int highVolume5m) { this.highVolume5m = highVolume5m; return this; }
		public Builder lowVolume5m(int lowVolume5m) { this.lowVolume5m = lowVolume5m; return this; }
		public Builder avgHighPrice1h(Long avgHighPrice1h) { this.avgHighPrice1h = avgHighPrice1h; return this; }
		public Builder avgLowPrice1h(Long avgLowPrice1h) { this.avgLowPrice1h = avgLowPrice1h; return this; }
		public Builder highVolume1h(int highVolume1h) { this.highVolume1h = highVolume1h; return this; }
		public Builder lowVolume1h(int lowVolume1h) { this.lowVolume1h = lowVolume1h; return this; }
		public Builder fetchedAtEpochSec(long fetchedAtEpochSec) { this.fetchedAtEpochSec = fetchedAtEpochSec; return this; }
		public Builder fromCache(boolean fromCache) { this.fromCache = fromCache; return this; }

		public ItemPrice build()
		{
			return new ItemPrice(this);
		}
	}
}
