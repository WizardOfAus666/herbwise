package com.herbwise.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the complete production, XP, ingredient shopping list, cost, and time breakdown
 * for the Production & XP Calculator tab.
 */
public class XpProductionPlan
{
	private final int currentLevel;
	private final int targetLevel;
	private final long currentXp;
	private final long targetXp;
	private final long xpRequired;
	private final long potionsRequired;
	private final double totalXpGained;
	private final PotionProfitResult potionResult;
	private final long totalInvestmentGp;
	private final long expectedGrossSaleValueGp;
	private final long expectedTotalGeTaxGp;
	private final long expectedNetSaleValueGp;
	private final long totalProfitLossGp;
	private final double netGpPerXp;
	private final double estimatedHours;
	private final double estimatedMinutes;
	private final long inventoryLoadsRequired;
	private final Map<PotionIngredient, Long> totalIngredientsNeeded;

	public XpProductionPlan(
		int currentLevel,
		int targetLevel,
		long currentXp,
		long targetXp,
		long xpRequired,
		long potionsRequired,
		double totalXpGained,
		PotionProfitResult potionResult,
		long totalInvestmentGp,
		long expectedGrossSaleValueGp,
		long expectedTotalGeTaxGp,
		long expectedNetSaleValueGp,
		long totalProfitLossGp,
		double netGpPerXp,
		double estimatedHours,
		double estimatedMinutes,
		long inventoryLoadsRequired,
		Map<PotionIngredient, Long> totalIngredientsNeeded)
	{
		this.currentLevel = currentLevel;
		this.targetLevel = targetLevel;
		this.currentXp = currentXp;
		this.targetXp = targetXp;
		this.xpRequired = Math.max(0L, xpRequired);
		this.potionsRequired = Math.max(0L, potionsRequired);
		this.totalXpGained = Math.max(0.0, totalXpGained);
		this.potionResult = potionResult;
		this.totalInvestmentGp = Math.max(0L, totalInvestmentGp);
		this.expectedGrossSaleValueGp = Math.max(0L, expectedGrossSaleValueGp);
		this.expectedTotalGeTaxGp = Math.max(0L, expectedTotalGeTaxGp);
		this.expectedNetSaleValueGp = Math.max(0L, expectedNetSaleValueGp);
		this.totalProfitLossGp = totalProfitLossGp;
		this.netGpPerXp = netGpPerXp;
		this.estimatedHours = Math.max(0.0, estimatedHours);
		this.estimatedMinutes = Math.max(0.0, estimatedMinutes);
		this.inventoryLoadsRequired = Math.max(0L, inventoryLoadsRequired);
		this.totalIngredientsNeeded = totalIngredientsNeeded != null
			? Collections.unmodifiableMap(new LinkedHashMap<>(totalIngredientsNeeded))
			: Collections.emptyMap();
	}

	public int getCurrentLevel() { return currentLevel; }
	public int getTargetLevel() { return targetLevel; }
	public long getCurrentXp() { return currentXp; }
	public long getTargetXp() { return targetXp; }
	public long getXpRequired() { return xpRequired; }
	public long getPotionsRequired() { return potionsRequired; }
	public double getTotalXpGained() { return totalXpGained; }
	public PotionProfitResult getPotionResult() { return potionResult; }
	public long getTotalInvestmentGp() { return totalInvestmentGp; }
	public long getExpectedGrossSaleValueGp() { return expectedGrossSaleValueGp; }
	public long getExpectedTotalGeTaxGp() { return expectedTotalGeTaxGp; }
	public long getExpectedNetSaleValueGp() { return expectedNetSaleValueGp; }
	public long getTotalProfitLossGp() { return totalProfitLossGp; }
	public double getNetGpPerXp() { return netGpPerXp; }
	public double getEstimatedHours() { return estimatedHours; }
	public double getEstimatedMinutes() { return estimatedMinutes; }
	public long getInventoryLoadsRequired() { return inventoryLoadsRequired; }
	public Map<PotionIngredient, Long> getTotalIngredientsNeeded() { return totalIngredientsNeeded; }
}
