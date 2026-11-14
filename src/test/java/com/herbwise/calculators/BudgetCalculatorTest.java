package com.herbwise.calculators;

import com.herbwise.data.HerbloreRecipes;
import com.herbwise.models.BudgetCalculationResult;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.PriceCalculationMode;
import com.herbwise.services.PriceCacheService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link BudgetCalculator} verifying affordable quantities, remaining GP,
 * inventory loads, shopping lists, and budget recommendations (Section 7 & Section 17).
 */
public class BudgetCalculatorTest
{
	private BudgetCalculator budgetCalculator;
	private PotionProfitCalculator profitCalculator;
	private Map<Integer, ItemPrice> fallbackPrices;

	@Before
	public void setUp()
	{
		ProductionTimeCalculator timeCalc = new ProductionTimeCalculator();
		budgetCalculator = new BudgetCalculator(timeCalc);
		profitCalculator = new PotionProfitCalculator(new GeTaxCalculator(), timeCalc);
		fallbackPrices = new PriceCacheService().getOrLoadFallbackPrices();
	}

	@Test
	public void testBudgetCalculationAcrossStandardTiers()
	{
		PotionRecipe prayer3 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_3_unf")).findFirst().orElseThrow(AssertionError::new);

		PotionProfitResult evaluated = profitCalculator.calculate(
			prayer3, fallbackPrices, PriceCalculationMode.INSTANT_CONSERVATIVE,
			true, 75, 1_000_000L, PriceCacheService.BUNDLED_SNAPSHOT_EPOCH_SEC,
			true, 3600L, true, 12.0
		);

		long unitCost = evaluated.getTotalIngredientCost();
		Assert.assertTrue(unitCost > 0L);

		// Test 1,000,000 GP budget
		long budget = 1_000_000L;
		BudgetCalculationResult res = budgetCalculator.calculateBudget(budget, evaluated);

		long expectedPotions = budget / unitCost;
		Assert.assertEquals(expectedPotions, res.getMaxPotionsAffordable());
		Assert.assertEquals(expectedPotions * unitCost, res.getTotalIngredientCost());
		Assert.assertEquals(budget - (expectedPotions * unitCost), res.getRemainingGp());
		Assert.assertEquals((long) Math.ceil(expectedPotions / 14.0), res.getInventoryLoadsRequired());
		Assert.assertEquals(expectedPotions * 87.5, res.getTotalXpGained(), 0.001);
		Assert.assertFalse(res.getIngredientQuantitiesNeeded().isEmpty());
	}

	@Test
	public void testBudgetSmallerThanSinglePotion()
	{
		PotionRecipe saraBrew3 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("saradomin_brew_3_unf")).findFirst().orElseThrow(AssertionError::new);

		PotionProfitResult evaluated = profitCalculator.calculate(
			saraBrew3, fallbackPrices, PriceCalculationMode.INSTANT_CONSERVATIVE,
			true, 85, 500L, PriceCacheService.BUNDLED_SNAPSHOT_EPOCH_SEC,
			true, 3600L, true, 12.0
		);

		BudgetCalculationResult res = budgetCalculator.calculateBudget(500L, evaluated);
		Assert.assertEquals(0L, res.getMaxPotionsAffordable());
		Assert.assertEquals(0L, res.getTotalIngredientCost());
		Assert.assertEquals(500L, res.getRemainingGp());
		Assert.assertEquals(0L, res.getInventoryLoadsRequired());
	}

	@Test
	public void testAutoRecommendBestForBudgetAndPlayerLevel()
	{
		List<PotionProfitResult> allEvaluated = profitCalculator.calculateAll(
			HerbloreRecipes.getAllRecipes(),
			fallbackPrices,
			PriceCalculationMode.INSTANT_CONSERVATIVE,
			true,
			60,
			500_000L,
			PriceCacheService.BUNDLED_SNAPSHOT_EPOCH_SEC,
			true,
			3600L,
			true,
			12.0
		);

		Optional<PotionProfitResult> recommended = budgetCalculator.recommendBestPotionForBudget(
			500_000L,
			60,
			allEvaluated
		);

		Assert.assertTrue(recommended.isPresent());
		PotionProfitResult best = recommended.get();
		Assert.assertTrue(best.getRecipe().getRequiredLevel() <= 60);
		Assert.assertFalse(best.getRecipe().isUnfinishedPotionRecipe());
		Assert.assertTrue(best.getTotalIngredientCost() <= 500_000L);
	}
}
