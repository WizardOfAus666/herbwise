package com.herbwise.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the complete calculation output for the Budget Calculator tab.
 */
public class BudgetCalculationResult
{
	private final long budgetGp;
	private final PotionProfitResult potionResult;
	private final long maxPotionsAffordable;
	private final long totalIngredientCost;
	private final long expectedGrossSaleValue;
	private final long expectedTotalGeTax;
	private final long expectedNetSaleValue;
	private final long expectedTotalProfitLoss;
	private final double totalXpGained;
	private final long remainingGp;
	private final int potionsPerInventoryLoad;
	private final long inventoryLoadsRequired;
	private final double estimatedMinutes;
	private final Map<PotionIngredient, Long> ingredientQuantitiesNeeded;

	public BudgetCalculationResult(
		long budgetGp,
		PotionProfitResult potionResult,
		long maxPotionsAffordable,
		long totalIngredientCost,
		long expectedGrossSaleValue,
		long expectedTotalGeTax,
		long expectedNetSaleValue,
		long expectedTotalProfitLoss,
		double totalXpGained,
		long remainingGp,
		int potionsPerInventoryLoad,
		long inventoryLoadsRequired,
		double estimatedMinutes,
		Map<PotionIngredient, Long> ingredientQuantitiesNeeded)
	{
		this.budgetGp = Math.max(0L, budgetGp);
		this.potionResult = potionResult;
		this.maxPotionsAffordable = Math.max(0L, maxPotionsAffordable);
		this.totalIngredientCost = Math.max(0L, totalIngredientCost);
		this.expectedGrossSaleValue = Math.max(0L, expectedGrossSaleValue);
		this.expectedTotalGeTax = Math.max(0L, expectedTotalGeTax);
		this.expectedNetSaleValue = Math.max(0L, expectedNetSaleValue);
		this.expectedTotalProfitLoss = expectedTotalProfitLoss;
		this.totalXpGained = Math.max(0.0, totalXpGained);
		this.remainingGp = Math.max(0L, remainingGp);
		this.potionsPerInventoryLoad = Math.max(1, potionsPerInventoryLoad);
		this.inventoryLoadsRequired = Math.max(0L, inventoryLoadsRequired);
		this.estimatedMinutes = Math.max(0.0, estimatedMinutes);
		this.ingredientQuantitiesNeeded = ingredientQuantitiesNeeded != null
			? Collections.unmodifiableMap(new LinkedHashMap<>(ingredientQuantitiesNeeded))
			: Collections.emptyMap();
	}

	public long getBudgetGp() { return budgetGp; }
	public PotionProfitResult getPotionResult() { return potionResult; }
	public long getMaxPotionsAffordable() { return maxPotionsAffordable; }
	public long getTotalIngredientCost() { return totalIngredientCost; }
	public long getExpectedGrossSaleValue() { return expectedGrossSaleValue; }
	public long getExpectedTotalGeTax() { return expectedTotalGeTax; }
	public long getExpectedNetSaleValue() { return expectedNetSaleValue; }
	public long getExpectedTotalProfitLoss() { return expectedTotalProfitLoss; }
	public double getTotalXpGained() { return totalXpGained; }
	public long getRemainingGp() { return remainingGp; }
	public int getPotionsPerInventoryLoad() { return potionsPerInventoryLoad; }
	public long getInventoryLoadsRequired() { return inventoryLoadsRequired; }
	public double getEstimatedMinutes() { return estimatedMinutes; }
	public Map<PotionIngredient, Long> getIngredientQuantitiesNeeded() { return ingredientQuantitiesNeeded; }
}
