package com.herbwise.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable result of evaluating a {@link PotionRecipe} through {@code PotionProfitCalculator}.
 * Contains ingredient cost breakdown, finished sale value, GE tax, net profit/loss,
 * XP efficiency metrics, hourly estimates, and market reliability flags.
 */
public class PotionProfitResult
{
	public enum PriceStatus
	{
		PROFITABLE,
		LOSS,
		UNCERTAIN_OR_STALE,
		MISSING_DATA
	}

	private final PotionRecipe recipe;
	private final PriceCalculationMode priceMode;
	private final boolean includeGeTax;
	private final PriceStatus priceStatus;
	private final boolean hasCompletePriceData;
	private final boolean hasIngredientPriceData;
	private final boolean stalePriceWarning;
	private final boolean volatilePriceWarning;
	private final String statusMessage;

	private final Map<Integer, Long> ingredientUnitPrices;
	private final Map<Integer, Long> ingredientLineCosts;
	private final long totalIngredientCost;
	private final long finishedPotionSalePrice;
	private final long geTax;
	private final long netSaleValue;
	private final long netProfitPerPotion;
	private final long profitFor100;
	private final long profitFor1000;
	private final long profitFor10000;
	private final double profitMarginPercent;

	private final double xpPerPotion;
	private final double ingredientCostPerXp;
	private final double netCostPerXp;
	private final double netLossPerXp;
	private final double profitPerXp;

	private final int estimatedPotionsPerHour;
	private final long estimatedXpPerHour;
	private final long estimatedProfitPerHour;
	private final int hourlyVolume;
	private final long maxPriceAgeSeconds;
	private final double overallTrainingScore;

	private final boolean availableAtPlayerLevel;
	private final int levelsUntilUnlock;
	private final boolean affordableWithinBudget;

	private PotionProfitResult(Builder builder)
	{
		this.recipe = builder.recipe;
		this.priceMode = builder.priceMode != null ? builder.priceMode : PriceCalculationMode.INSTANT_CONSERVATIVE;
		this.includeGeTax = builder.includeGeTax;
		this.priceStatus = builder.priceStatus != null ? builder.priceStatus : PriceStatus.MISSING_DATA;
		this.hasCompletePriceData = builder.hasCompletePriceData;
		this.hasIngredientPriceData = builder.hasIngredientPriceData;
		this.stalePriceWarning = builder.stalePriceWarning;
		this.volatilePriceWarning = builder.volatilePriceWarning;
		this.statusMessage = builder.statusMessage != null ? builder.statusMessage : "";
		this.ingredientUnitPrices = builder.ingredientUnitPrices != null
			? Collections.unmodifiableMap(new LinkedHashMap<>(builder.ingredientUnitPrices))
			: Collections.emptyMap();
		this.ingredientLineCosts = builder.ingredientLineCosts != null
			? Collections.unmodifiableMap(new LinkedHashMap<>(builder.ingredientLineCosts))
			: Collections.emptyMap();
		this.totalIngredientCost = builder.totalIngredientCost;
		this.finishedPotionSalePrice = builder.finishedPotionSalePrice;
		this.geTax = builder.geTax;
		this.netSaleValue = builder.netSaleValue;
		this.netProfitPerPotion = builder.netProfitPerPotion;
		this.profitFor100 = builder.profitFor100;
		this.profitFor1000 = builder.profitFor1000;
		this.profitFor10000 = builder.profitFor10000;
		this.profitMarginPercent = builder.profitMarginPercent;
		this.xpPerPotion = builder.xpPerPotion;
		this.ingredientCostPerXp = builder.ingredientCostPerXp;
		this.netCostPerXp = builder.netCostPerXp;
		this.netLossPerXp = builder.netLossPerXp;
		this.profitPerXp = builder.profitPerXp;
		this.estimatedPotionsPerHour = builder.estimatedPotionsPerHour;
		this.estimatedXpPerHour = builder.estimatedXpPerHour;
		this.estimatedProfitPerHour = builder.estimatedProfitPerHour;
		this.hourlyVolume = builder.hourlyVolume;
		this.maxPriceAgeSeconds = builder.maxPriceAgeSeconds;
		this.overallTrainingScore = builder.overallTrainingScore;
		this.availableAtPlayerLevel = builder.availableAtPlayerLevel;
		this.levelsUntilUnlock = builder.levelsUntilUnlock;
		this.affordableWithinBudget = builder.affordableWithinBudget;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public PotionRecipe getRecipe() { return recipe; }
	public PriceCalculationMode getPriceMode() { return priceMode; }
	public boolean isIncludeGeTax() { return includeGeTax; }
	public PriceStatus getPriceStatus() { return priceStatus; }
	public boolean isHasCompletePriceData() { return hasCompletePriceData; }
	public boolean isHasIngredientPriceData() { return hasIngredientPriceData; }
	public boolean isStalePriceWarning() { return stalePriceWarning; }
	public boolean isVolatilePriceWarning() { return volatilePriceWarning; }
	public String getStatusMessage() { return statusMessage; }
	public Map<Integer, Long> getIngredientUnitPrices() { return ingredientUnitPrices; }
	public Map<Integer, Long> getIngredientLineCosts() { return ingredientLineCosts; }
	public long getTotalIngredientCost() { return totalIngredientCost; }
	public long getFinishedPotionSalePrice() { return finishedPotionSalePrice; }
	public long getGeTax() { return geTax; }
	public long getNetSaleValue() { return netSaleValue; }
	public long getNetProfitPerPotion() { return netProfitPerPotion; }
	public long getProfitFor100() { return profitFor100; }
	public long getProfitFor1000() { return profitFor1000; }
	public long getProfitFor10000() { return profitFor10000; }
	public double getProfitMarginPercent() { return profitMarginPercent; }
	public double getXpPerPotion() { return xpPerPotion; }
	public double getIngredientCostPerXp() { return ingredientCostPerXp; }
	public double getNetCostPerXp() { return netCostPerXp; }
	public double getNetLossPerXp() { return netLossPerXp; }
	public double getProfitPerXp() { return profitPerXp; }
	public int getEstimatedPotionsPerHour() { return estimatedPotionsPerHour; }
	public long getEstimatedXpPerHour() { return estimatedXpPerHour; }
	public long getEstimatedProfitPerHour() { return estimatedProfitPerHour; }
	public int getHourlyVolume() { return hourlyVolume; }
	public long getMaxPriceAgeSeconds() { return maxPriceAgeSeconds; }
	public double getOverallTrainingScore() { return overallTrainingScore; }
	public boolean isAvailableAtPlayerLevel() { return availableAtPlayerLevel; }
	public int getLevelsUntilUnlock() { return levelsUntilUnlock; }
	public boolean isAffordableWithinBudget() { return affordableWithinBudget; }

	public boolean isProfitable()
	{
		return hasCompletePriceData && netProfitPerPotion > 0L;
	}

	public boolean isLossMaking()
	{
		return hasCompletePriceData && netProfitPerPotion < 0L;
	}

	public long getProfitForQuantity(long quantity)
	{
		if (quantity <= 0 || !hasCompletePriceData)
		{
			return 0L;
		}
		return Math.multiplyExact(netProfitPerPotion, quantity);
	}

	public double getTotalXpForQuantity(long quantity)
	{
		if (quantity <= 0)
		{
			return 0.0;
		}
		return xpPerPotion * quantity;
	}

	public static class Builder
	{
		private PotionRecipe recipe;
		private PriceCalculationMode priceMode = PriceCalculationMode.INSTANT_CONSERVATIVE;
		private boolean includeGeTax = true;
		private PriceStatus priceStatus = PriceStatus.MISSING_DATA;
		private boolean hasCompletePriceData = false;
		private boolean hasIngredientPriceData = false;
		private boolean stalePriceWarning = false;
		private boolean volatilePriceWarning = false;
		private String statusMessage = "";
		private Map<Integer, Long> ingredientUnitPrices = new LinkedHashMap<>();
		private Map<Integer, Long> ingredientLineCosts = new LinkedHashMap<>();
		private long totalIngredientCost;
		private long finishedPotionSalePrice;
		private long geTax;
		private long netSaleValue;
		private long netProfitPerPotion;
		private long profitFor100;
		private long profitFor1000;
		private long profitFor10000;
		private double profitMarginPercent;
		private double xpPerPotion;
		private double ingredientCostPerXp;
		private double netCostPerXp;
		private double netLossPerXp;
		private double profitPerXp;
		private int estimatedPotionsPerHour;
		private long estimatedXpPerHour;
		private long estimatedProfitPerHour;
		private int hourlyVolume;
		private long maxPriceAgeSeconds;
		private double overallTrainingScore;
		private boolean availableAtPlayerLevel = true;
		private int levelsUntilUnlock = 0;
		private boolean affordableWithinBudget = true;

		public Builder recipe(PotionRecipe recipe) { this.recipe = recipe; return this; }
		public Builder priceMode(PriceCalculationMode priceMode) { this.priceMode = priceMode; return this; }
		public Builder includeGeTax(boolean includeGeTax) { this.includeGeTax = includeGeTax; return this; }
		public Builder priceStatus(PriceStatus priceStatus) { this.priceStatus = priceStatus; return this; }
		public Builder hasCompletePriceData(boolean hasCompletePriceData) { this.hasCompletePriceData = hasCompletePriceData; return this; }
		public Builder hasIngredientPriceData(boolean hasIngredientPriceData) { this.hasIngredientPriceData = hasIngredientPriceData; return this; }
		public Builder stalePriceWarning(boolean stalePriceWarning) { this.stalePriceWarning = stalePriceWarning; return this; }
		public Builder volatilePriceWarning(boolean volatilePriceWarning) { this.volatilePriceWarning = volatilePriceWarning; return this; }
		public Builder statusMessage(String statusMessage) { this.statusMessage = statusMessage; return this; }
		public Builder ingredientUnitPrices(Map<Integer, Long> ingredientUnitPrices) { this.ingredientUnitPrices = ingredientUnitPrices; return this; }
		public Builder ingredientLineCosts(Map<Integer, Long> ingredientLineCosts) { this.ingredientLineCosts = ingredientLineCosts; return this; }
		public Builder totalIngredientCost(long totalIngredientCost) { this.totalIngredientCost = totalIngredientCost; return this; }
		public Builder finishedPotionSalePrice(long finishedPotionSalePrice) { this.finishedPotionSalePrice = finishedPotionSalePrice; return this; }
		public Builder geTax(long geTax) { this.geTax = geTax; return this; }
		public Builder netSaleValue(long netSaleValue) { this.netSaleValue = netSaleValue; return this; }
		public Builder netProfitPerPotion(long netProfitPerPotion) { this.netProfitPerPotion = netProfitPerPotion; return this; }
		public Builder profitFor100(long profitFor100) { this.profitFor100 = profitFor100; return this; }
		public Builder profitFor1000(long profitFor1000) { this.profitFor1000 = profitFor1000; return this; }
		public Builder profitFor10000(long profitFor10000) { this.profitFor10000 = profitFor10000; return this; }
		public Builder profitMarginPercent(double profitMarginPercent) { this.profitMarginPercent = profitMarginPercent; return this; }
		public Builder xpPerPotion(double xpPerPotion) { this.xpPerPotion = xpPerPotion; return this; }
		public Builder ingredientCostPerXp(double ingredientCostPerXp) { this.ingredientCostPerXp = ingredientCostPerXp; return this; }
		public Builder netCostPerXp(double netCostPerXp) { this.netCostPerXp = netCostPerXp; return this; }
		public Builder netLossPerXp(double netLossPerXp) { this.netLossPerXp = netLossPerXp; return this; }
		public Builder profitPerXp(double profitPerXp) { this.profitPerXp = profitPerXp; return this; }
		public Builder estimatedPotionsPerHour(int estimatedPotionsPerHour) { this.estimatedPotionsPerHour = estimatedPotionsPerHour; return this; }
		public Builder estimatedXpPerHour(long estimatedXpPerHour) { this.estimatedXpPerHour = estimatedXpPerHour; return this; }
		public Builder estimatedProfitPerHour(long estimatedProfitPerHour) { this.estimatedProfitPerHour = estimatedProfitPerHour; return this; }
		public Builder hourlyVolume(int hourlyVolume) { this.hourlyVolume = hourlyVolume; return this; }
		public Builder maxPriceAgeSeconds(long maxPriceAgeSeconds) { this.maxPriceAgeSeconds = maxPriceAgeSeconds; return this; }
		public Builder overallTrainingScore(double overallTrainingScore) { this.overallTrainingScore = overallTrainingScore; return this; }
		public Builder availableAtPlayerLevel(boolean availableAtPlayerLevel) { this.availableAtPlayerLevel = availableAtPlayerLevel; return this; }
		public Builder levelsUntilUnlock(int levelsUntilUnlock) { this.levelsUntilUnlock = levelsUntilUnlock; return this; }
		public Builder affordableWithinBudget(boolean affordableWithinBudget) { this.affordableWithinBudget = affordableWithinBudget; return this; }

		public PotionProfitResult build()
		{
			return new PotionProfitResult(this);
		}
	}
}
