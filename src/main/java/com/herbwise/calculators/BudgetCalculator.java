package com.herbwise.calculators;

import com.herbwise.models.BudgetCalculationResult;
import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Dedicated GP budget and quantity planning calculator for HerbWise.
 * Computes maximum affordable potions, total ingredient costs, expected sale values,
 * net profit/loss, total XP gained, remaining GP, and inventory loads required.
 */
@Singleton
public class BudgetCalculator
{
	public static final long[] BUDGET_PRESETS_GP = new long[]{
		100_000L,
		500_000L,
		1_000_000L,
		5_000_000L,
		10_000_000L,
		25_000_000L
	};

	private final ProductionTimeCalculator productionTimeCalculator;

	@Inject
	public BudgetCalculator(ProductionTimeCalculator productionTimeCalculator)
	{
		this.productionTimeCalculator = productionTimeCalculator != null
			? productionTimeCalculator
			: new ProductionTimeCalculator();
	}

	public BudgetCalculator()
	{
		this(new ProductionTimeCalculator());
	}

	/**
	 * Calculates how many potions the player can produce within {@code budgetGp} based on initial ingredient investment.
	 */
	public BudgetCalculationResult calculateBudget(long budgetGp, PotionProfitResult potionResult)
	{
		long safeBudget = Math.max(0L, budgetGp);
		if (potionResult == null || potionResult.getRecipe() == null || !potionResult.isHasIngredientPriceData())
		{
			return new BudgetCalculationResult(
				safeBudget, potionResult, 0L, 0L, 0L, 0L, 0L, 0L, 0.0, safeBudget, 14, 0L, 0.0, null
			);
		}

		long costPerPotion = potionResult.getTotalIngredientCost();
		if (costPerPotion <= 0L)
		{
			return new BudgetCalculationResult(
				safeBudget, potionResult, 0L, 0L, 0L, 0L, 0L, 0L, 0.0, safeBudget, 14, 0L, 0.0, null
			);
		}

		long maxPotions = safeBudget / costPerPotion;
		return buildForQuantity(safeBudget, maxPotions, potionResult);
	}

	/**
	 * Calculates exact costs, ingredients, and XP for a specific quantity of potions compared against {@code budgetGp}.
	 */
	public BudgetCalculationResult buildForQuantity(long budgetGp, long quantity, PotionProfitResult potionResult)
	{
		long safeBudget = Math.max(0L, budgetGp);
		long safeQty = Math.max(0L, quantity);
		if (potionResult == null || potionResult.getRecipe() == null)
		{
			return new BudgetCalculationResult(
				safeBudget, null, 0L, 0L, 0L, 0L, 0L, 0L, 0.0, safeBudget, 14, 0L, 0.0, null
			);
		}

		PotionRecipe recipe = potionResult.getRecipe();
		long totalIngredientCost = safeMultiply(potionResult.getTotalIngredientCost(), safeQty);
		long expectedGrossSale = potionResult.isHasCompletePriceData()
			? safeMultiply(potionResult.getFinishedPotionSalePrice(), safeQty)
			: 0L;
		long expectedGeTax = potionResult.isHasCompletePriceData()
			? safeMultiply(potionResult.getGeTax(), safeQty)
			: 0L;
		long expectedNetSale = potionResult.isHasCompletePriceData()
			? safeMultiply(potionResult.getNetSaleValue(), safeQty)
			: 0L;
		long expectedProfitLoss = potionResult.isHasCompletePriceData()
			? safeMultiply(potionResult.getNetProfitPerPotion(), safeQty)
			: -totalIngredientCost;

		double totalXp = safeQty * potionResult.getXpPerPotion();
		long remainingGp = Math.max(0L, safeBudget - totalIngredientCost);

		int perLoad = productionTimeCalculator.getPotionsPerInventoryLoad(recipe);
		long loads = productionTimeCalculator.calculateInventoryLoads(recipe, safeQty);
		double minutes = productionTimeCalculator.calculateMinutesRequired(recipe, safeQty);

		Map<PotionIngredient, Long> shoppingList = new LinkedHashMap<>();
		for (PotionIngredient ing : recipe.getIngredients())
		{
			shoppingList.put(ing, safeMultiply(safeQty, ing.getQuantity()));
		}

		return new BudgetCalculationResult(
			safeBudget,
			potionResult,
			safeQty,
			totalIngredientCost,
			expectedGrossSale,
			expectedGeTax,
			expectedNetSale,
			expectedProfitLoss,
			totalXp,
			remainingGp,
			perLoad,
			loads,
			minutes,
			shoppingList
		);
	}

	/**
	 * Recommends the best potion to make within {@code budgetGp} at {@code playerLevel}.
	 */
	public Optional<PotionProfitResult> recommendBestPotionForBudget(
		long budgetGp,
		int playerLevel,
		List<PotionProfitResult> results)
	{
		if (results == null || results.isEmpty() || budgetGp <= 0L)
		{
			return Optional.empty();
		}

		return results.stream()
			.filter(r -> r != null && r.getRecipe() != null)
			.filter(r -> !r.getRecipe().isUnfinishedPotionRecipe())
			.filter(r -> !r.getRecipe().isAlternativeRecipe())
			.filter(PotionProfitResult::isHasCompletePriceData)
			.filter(r -> r.getXpPerPotion() > 10.0)
			.filter(r -> r.getRecipe().getRequiredLevel() <= Math.max(1, playerLevel))
			.filter(r -> r.getTotalIngredientCost() > 0L && r.getTotalIngredientCost() <= budgetGp)
			.max(Comparator.comparingDouble(r -> scoreForBudget(r, budgetGp)));
	}

	private double scoreForBudget(PotionProfitResult r, long budgetGp)
	{
		long affordableCount = budgetGp / Math.max(1L, r.getTotalIngredientCost());
		if (affordableCount <= 0L)
		{
			return -1_000_000.0;
		}
		double baseScore = r.getOverallTrainingScore();
		// Prefer recipes where the player can afford at least a full inventory (14+ potions)
		double batchBonus = Math.min(20.0, Math.log10(affordableCount + 1.0) * 6.5);
		return baseScore + batchBonus;
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
