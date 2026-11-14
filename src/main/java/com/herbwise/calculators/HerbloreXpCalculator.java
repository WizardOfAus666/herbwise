package com.herbwise.calculators;

import com.herbwise.data.HerbloreXpTable;
import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.XpProductionPlan;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Calculates exact Herblore experience requirements, potions required to reach target levels or XP goals,
 * total ingredient shopping lists, total GP investment, expected return after GE tax, and production time.
 */
@Singleton
public class HerbloreXpCalculator
{
	private final ProductionTimeCalculator productionTimeCalculator;

	@Inject
	public HerbloreXpCalculator(ProductionTimeCalculator productionTimeCalculator)
	{
		this.productionTimeCalculator = productionTimeCalculator != null
			? productionTimeCalculator
			: new ProductionTimeCalculator();
	}

	public HerbloreXpCalculator()
	{
		this(new ProductionTimeCalculator());
	}

	/**
	 * Calculates a full production plan between a starting XP amount and a target XP amount.
	 */
	public XpProductionPlan calculateByXpGoal(
		long currentXp,
		long targetXp,
		PotionProfitResult potionResult)
	{
		long safeCurrentXp = Math.max(0L, Math.min(HerbloreXpTable.MAX_XP, currentXp));
		long safeTargetXp = Math.max(safeCurrentXp, Math.min(HerbloreXpTable.MAX_XP, targetXp));
		long xpRequired = Math.max(0L, safeTargetXp - safeCurrentXp);

		int currentLevel = HerbloreXpTable.getLevelForXp(safeCurrentXp);
		int targetLevel = HerbloreXpTable.getLevelForXp(safeTargetXp);

		if (potionResult == null || potionResult.getRecipe() == null || potionResult.getXpPerPotion() <= 0.0)
		{
			return new XpProductionPlan(
				currentLevel, targetLevel, safeCurrentXp, safeTargetXp,
				xpRequired, 0L, 0.0, potionResult,
				0L, 0L, 0L, 0L, 0L, 0.0, 0.0, 0.0, 0L, null
			);
		}

		double xpPerPotion = potionResult.getXpPerPotion();
		long potionsNeeded = xpRequired > 0L
			? (long) Math.ceil((double) xpRequired / xpPerPotion)
			: 0L;

		return buildPlanForPotionQuantity(
			currentLevel,
			targetLevel,
			safeCurrentXp,
			safeTargetXp,
			xpRequired,
			potionsNeeded,
			potionResult
		);
	}

	/**
	 * Calculates a full production plan from a starting Herblore level to a target Herblore level.
	 */
	public XpProductionPlan calculateByLevels(
		int currentLevel,
		int targetLevel,
		PotionProfitResult potionResult)
	{
		int safeStartLevel = Math.max(1, Math.min(HerbloreXpTable.MAX_SKILL_LEVEL, currentLevel));
		int safeTargetLevel = Math.max(safeStartLevel, Math.min(HerbloreXpTable.MAX_SKILL_LEVEL, targetLevel));
		long startXp = HerbloreXpTable.getXpForLevel(safeStartLevel);
		long endXp = HerbloreXpTable.getXpForLevel(safeTargetLevel);
		return calculateByXpGoal(startXp, endXp, potionResult);
	}

	/**
	 * Calculates a production plan for a specific desired XP gain (e.g. +10,000 XP).
	 */
	public XpProductionPlan calculateForDesiredXpGain(
		long currentXp,
		long desiredXpGain,
		PotionProfitResult potionResult)
	{
		long safeCurrent = Math.max(0L, currentXp);
		long safeGain = Math.max(0L, desiredXpGain);
		return calculateByXpGoal(safeCurrent, safeCurrent + safeGain, potionResult);
	}

	/**
	 * Calculates a production plan for an explicit custom quantity of potions.
	 */
	public XpProductionPlan calculateForPotionQuantity(
		long currentXp,
		long customPotionCount,
		PotionProfitResult potionResult)
	{
		long safeCurrentXp = Math.max(0L, currentXp);
		long safeCount = Math.max(0L, customPotionCount);
		int currentLevel = HerbloreXpTable.getLevelForXp(safeCurrentXp);

		if (potionResult == null || potionResult.getRecipe() == null)
		{
			return new XpProductionPlan(
				currentLevel, currentLevel, safeCurrentXp, safeCurrentXp,
				0L, 0L, 0.0, null,
				0L, 0L, 0L, 0L, 0L, 0.0, 0.0, 0.0, 0L, null
			);
		}

		double xpGained = safeCount * potionResult.getXpPerPotion();
		long resultingXp = Math.min(HerbloreXpTable.MAX_XP, safeCurrentXp + Math.round(xpGained));
		int resultingLevel = HerbloreXpTable.getLevelForXp(resultingXp);

		return buildPlanForPotionQuantity(
			currentLevel,
			resultingLevel,
			safeCurrentXp,
			resultingXp,
			Math.round(xpGained),
			safeCount,
			potionResult
		);
	}

	private XpProductionPlan buildPlanForPotionQuantity(
		int currentLevel,
		int targetLevel,
		long currentXp,
		long targetXp,
		long xpRequired,
		long potionsRequired,
		PotionProfitResult potionResult)
	{
		PotionRecipe recipe = potionResult.getRecipe();
		double totalXpGained = potionsRequired * potionResult.getXpPerPotion();

		Map<PotionIngredient, Long> shoppingList = new LinkedHashMap<>();
		for (PotionIngredient ing : recipe.getIngredients())
		{
			long totalIngQty = Math.multiplyExact(potionsRequired, (long) ing.getQuantity());
			shoppingList.put(ing, totalIngQty);
		}

		long totalInvestment = 0L;
		long expectedGrossSale = 0L;
		long expectedTax = 0L;
		long expectedNetSale = 0L;
		long totalProfitLoss = 0L;

		if (potionResult.isHasIngredientPriceData())
		{
			totalInvestment = safeMultiply(potionResult.getTotalIngredientCost(), potionsRequired);
		}
		if (potionResult.isHasCompletePriceData())
		{
			expectedGrossSale = safeMultiply(potionResult.getFinishedPotionSalePrice(), potionsRequired);
			expectedTax = safeMultiply(potionResult.getGeTax(), potionsRequired);
			expectedNetSale = safeMultiply(potionResult.getNetSaleValue(), potionsRequired);
			totalProfitLoss = safeMultiply(potionResult.getNetProfitPerPotion(), potionsRequired);
		}

		double netGpPerXp = totalXpGained > 0.0
			? ((double) totalProfitLoss / totalXpGained)
			: 0.0;

		double hours = productionTimeCalculator.calculateHoursRequired(recipe, potionsRequired);
		double minutes = hours * 60.0;
		long loads = productionTimeCalculator.calculateInventoryLoads(recipe, potionsRequired);

		return new XpProductionPlan(
			currentLevel,
			targetLevel,
			currentXp,
			targetXp,
			xpRequired,
			potionsRequired,
			totalXpGained,
			potionResult,
			totalInvestment,
			expectedGrossSale,
			expectedTax,
			expectedNetSale,
			totalProfitLoss,
			netGpPerXp,
			hours,
			minutes,
			loads,
			shoppingList
		);
	}

	private static long safeMultiply(long a, long b)
	{
		try
		{
			return Math.multiplyExact(a, b);
		}
		catch (ArithmeticException ex)
		{
			return (a > 0 && b > 0) || (a < 0 && b < 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
		}
	}
}
