package com.herbwise.calculators;

import com.herbwise.data.HerbloreRecipes;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionProfitResult.PriceStatus;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.PriceCalculationMode;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.ItemID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PotionProfitCalculator} verifying profitable potions, loss-making potions,
 * dose variants, alternative recipes, missing prices, stale prices, volatile prices, and untradeable items.
 */
public class PotionProfitCalculatorTest
{
	private static final long TEST_NOW_SEC = 1_760_000_000L;

	private PotionProfitCalculator calculator;

	@Before
	public void setUp()
	{
		calculator = new PotionProfitCalculator(new GeTaxCalculator(), new ProductionTimeCalculator());
	}

	private ItemPrice makePrice(int id, String name, long buy, long sell, long timeSec)
	{
		return ItemPrice.builder()
			.itemId(id)
			.itemName(name)
			.instantBuyPrice(buy)
			.instantBuyTimestampSec(timeSec)
			.instantSellPrice(sell)
			.instantSellTimestampSec(timeSec)
			.avgHighPrice5m(buy)
			.avgLowPrice5m(sell)
			.highVolume5m(250)
			.lowVolume5m(250)
			.avgHighPrice1h(buy)
			.avgLowPrice1h(sell)
			.highVolume1h(1500)
			.lowVolume1h(1500)
			.buyLimit(2000)
			.members(true)
			.fetchedAtEpochSec(timeSec)
			.fromCache(false)
			.build();
	}

	@Test
	public void testLossMakingPotionExactFormulaVerification()
	{
		// Verify Section 4.D / 4.E / 4.F exact math:
		// Super Strength (4): Ingredient Cost = 4,250 GP, Sale Value = 4,100 GP
		// GE Tax = floor(4,100 * 0.02) = 82 GP
		// Net Sale Value = 4,100 - 82 = 4,018 GP
		// Net Profit/Loss = 4,018 - 4,250 = -232 GP
		// XP Per Potion = 125.0 XP
		// Net Loss Per XP = 232 / 125 = 1.856 GP/XP (~1.86 GP/XP)
		PotionRecipe superStr4 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("super_strength_4_unf"))
			.findFirst()
			.orElseThrow(AssertionError::new);

		Map<Integer, ItemPrice> prices = new HashMap<>();
		prices.put(ItemID.KWUARM_POTION_UNF, makePrice(ItemID.KWUARM_POTION_UNF, "Kwuarm potion (unf)", 2_900L, 2_850L, TEST_NOW_SEC));
		prices.put(ItemID.LIMPWURT_ROOT, makePrice(ItemID.LIMPWURT_ROOT, "Limpwurt root", 1_350L, 1_300L, TEST_NOW_SEC));
		prices.put(ItemID.SUPER_STRENGTH4, makePrice(ItemID.SUPER_STRENGTH4, "Super strength(4)", 4_150L, 4_100L, TEST_NOW_SEC));

		PotionProfitResult res = calculator.calculate(
			superStr4,
			prices,
			PriceCalculationMode.INSTANT_CONSERVATIVE,
			true,
			75,
			1_000_000L,
			TEST_NOW_SEC,
			true,
			3600L,
			true,
			12.0
		);

		Assert.assertTrue(res.isHasCompletePriceData());
		Assert.assertTrue(res.isLossMaking());
		Assert.assertFalse(res.isProfitable());
		Assert.assertEquals(PriceStatus.LOSS, res.getPriceStatus());
		Assert.assertEquals(4_250L, res.getTotalIngredientCost());
		Assert.assertEquals(4_100L, res.getFinishedPotionSalePrice());
		Assert.assertEquals(82L, res.getGeTax());
		Assert.assertEquals(4_018L, res.getNetSaleValue());
		Assert.assertEquals(-232L, res.getNetProfitPerPotion());
		Assert.assertEquals(-23_200L, res.getProfitFor100());
		Assert.assertEquals(-232_000L, res.getProfitFor1000());
		Assert.assertEquals(-2_320_000L, res.getProfitFor10000());
		Assert.assertEquals(125.0, res.getXpPerPotion(), 0.001);
		Assert.assertEquals(1.856, res.getNetLossPerXp(), 0.001);
		Assert.assertEquals(34.0, res.getIngredientCostPerXp(), 0.001);
	}

	@Test
	public void testProfitablePotionCalculation()
	{
		PotionRecipe prayer3 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_3_unf"))
			.findFirst()
			.orElseThrow(AssertionError::new);

		Map<Integer, ItemPrice> prices = new HashMap<>();
		prices.put(ItemID.RANARR_POTION_UNF, makePrice(ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)", 6_400L, 6_350L, TEST_NOW_SEC));
		prices.put(ItemID.SNAPE_GRASS, makePrice(ItemID.SNAPE_GRASS, "Snape grass", 350L, 340L, TEST_NOW_SEC));
		prices.put(ItemID.PRAYER_POTION3, makePrice(ItemID.PRAYER_POTION3, "Prayer potion(3)", 7_200L, 7_100L, TEST_NOW_SEC));

		PotionProfitResult res = calculator.calculate(
			prayer3,
			prices,
			PriceCalculationMode.INSTANT_CONSERVATIVE,
			true,
			50,
			500_000L,
			TEST_NOW_SEC,
			true,
			3600L,
			true,
			12.0
		);

		// Cost = 6,400 + 350 = 6,750 GP
		// Gross Sale = 7,100 GP, GE Tax = floor(7,100 * 0.02) = 142 GP
		// Net Sale = 6,958 GP -> Profit = +208 GP/potion
		Assert.assertTrue(res.isProfitable());
		Assert.assertEquals(PriceStatus.PROFITABLE, res.getPriceStatus());
		Assert.assertEquals(6_750L, res.getTotalIngredientCost());
		Assert.assertEquals(7_100L, res.getFinishedPotionSalePrice());
		Assert.assertEquals(142L, res.getGeTax());
		Assert.assertEquals(208L, res.getNetProfitPerPotion());
		Assert.assertEquals(208_000L, res.getProfitFor1000());
		Assert.assertEquals(87.5, res.getXpPerPotion(), 0.001);
		Assert.assertTrue(res.getProfitPerXp() > 2.3);
	}

	@Test
	public void testDoseVariantsUseDistinctMarketPrices()
	{
		PotionRecipe pray4 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_4_unf")).findFirst().orElseThrow(AssertionError::new);
		PotionRecipe pray3 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_3_unf")).findFirst().orElseThrow(AssertionError::new);
		PotionRecipe pray2 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_2_unf")).findFirst().orElseThrow(AssertionError::new);
		PotionRecipe pray1 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_1_unf")).findFirst().orElseThrow(AssertionError::new);

		Assert.assertNotEquals(pray4.getPotionItemId(), pray3.getPotionItemId());
		Assert.assertNotEquals(pray3.getPotionItemId(), pray2.getPotionItemId());
		Assert.assertNotEquals(pray2.getPotionItemId(), pray1.getPotionItemId());

		Map<Integer, ItemPrice> prices = new HashMap<>();
		prices.put(ItemID.RANARR_POTION_UNF, makePrice(ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)", 6_400L, 6_350L, TEST_NOW_SEC));
		prices.put(ItemID.SNAPE_GRASS, makePrice(ItemID.SNAPE_GRASS, "Snape grass", 350L, 340L, TEST_NOW_SEC));
		prices.put(ItemID.PRAYER_POTION4, makePrice(ItemID.PRAYER_POTION4, "Prayer potion(4)", 9_300L, 9_200L, TEST_NOW_SEC));
		prices.put(ItemID.PRAYER_POTION3, makePrice(ItemID.PRAYER_POTION3, "Prayer potion(3)", 7_000L, 6_900L, TEST_NOW_SEC));
		prices.put(ItemID.PRAYER_POTION2, makePrice(ItemID.PRAYER_POTION2, "Prayer potion(2)", 4_600L, 4_500L, TEST_NOW_SEC));
		prices.put(ItemID.PRAYER_POTION1, makePrice(ItemID.PRAYER_POTION1, "Prayer potion(1)", 2_300L, 2_200L, TEST_NOW_SEC));

		PotionProfitResult r4 = calculator.calculate(pray4, prices, PriceCalculationMode.INSTANT_CONSERVATIVE, true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 15.0);
		PotionProfitResult r3 = calculator.calculate(pray3, prices, PriceCalculationMode.INSTANT_CONSERVATIVE, true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 15.0);
		PotionProfitResult r2 = calculator.calculate(pray2, prices, PriceCalculationMode.INSTANT_CONSERVATIVE, true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 15.0);
		PotionProfitResult r1 = calculator.calculate(pray1, prices, PriceCalculationMode.INSTANT_CONSERVATIVE, true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 15.0);

		Assert.assertEquals(9_200L, r4.getFinishedPotionSalePrice());
		Assert.assertEquals(6_900L, r3.getFinishedPotionSalePrice());
		Assert.assertEquals(4_500L, r2.getFinishedPotionSalePrice());
		Assert.assertEquals(2_200L, r1.getFinishedPotionSalePrice());
	}

	@Test
	public void testAlternativeRecipesCleanHerbVsUnfinishedPotion()
	{
		PotionRecipe fromUnf = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_4_unf")).findFirst().orElseThrow(AssertionError::new);
		PotionRecipe fromHerb = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_4_herb")).findFirst().orElseThrow(AssertionError::new);

		Map<Integer, ItemPrice> prices = new HashMap<>();
		prices.put(ItemID.RANARR_POTION_UNF, makePrice(ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)", 6_500L, 6_400L, TEST_NOW_SEC));
		prices.put(ItemID.RANARR_WEED, makePrice(ItemID.RANARR_WEED, "Ranarr weed", 6_200L, 6_100L, TEST_NOW_SEC));
		prices.put(ItemID.VIAL_OF_WATER, makePrice(ItemID.VIAL_OF_WATER, "Vial of water", 5L, 4L, TEST_NOW_SEC));
		prices.put(ItemID.SNAPE_GRASS, makePrice(ItemID.SNAPE_GRASS, "Snape grass", 350L, 340L, TEST_NOW_SEC));
		prices.put(ItemID.PRAYER_POTION4, makePrice(ItemID.PRAYER_POTION4, "Prayer potion(4)", 7_000L, 6_900L, TEST_NOW_SEC));

		PotionProfitResult resUnf = calculator.calculate(fromUnf, prices, PriceCalculationMode.INSTANT_CONSERVATIVE, true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 15.0);
		PotionProfitResult resHerb = calculator.calculate(fromHerb, prices, PriceCalculationMode.INSTANT_CONSERVATIVE, true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 15.0);

		// From Unf: 6,500 + 350 = 6,850 GP
		// From Clean Herb: 6,200 + 5 + 350 = 6,555 GP (saves 295 GP per potion)
		Assert.assertEquals(6_850L, resUnf.getTotalIngredientCost());
		Assert.assertEquals(6_555L, resHerb.getTotalIngredientCost());
		Assert.assertTrue(resHerb.getNetProfitPerPotion() > resUnf.getNetProfitPerPotion());
		// Clean herb requires 2 steps -> lower potions/hour than pre-made unfinished potion
		Assert.assertTrue(resUnf.getEstimatedPotionsPerHour() > resHerb.getEstimatedPotionsPerHour());
	}

	@Test
	public void testMissingMarketPricesNeverHardcodedToZero()
	{
		PotionRecipe prayer3 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_3_unf")).findFirst().orElseThrow(AssertionError::new);

		// Empty price map -> missing data
		Map<Integer, ItemPrice> emptyPrices = new HashMap<>();
		PotionProfitResult res = calculator.calculate(
			prayer3, emptyPrices, PriceCalculationMode.INSTANT_CONSERVATIVE,
			true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 12.0
		);

		Assert.assertFalse(res.isHasCompletePriceData());
		Assert.assertFalse(res.isHasIngredientPriceData());
		Assert.assertEquals(PriceStatus.MISSING_DATA, res.getPriceStatus());
	}

	@Test
	public void testStaleAndVolatilePriceDetection()
	{
		PotionRecipe restore3 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("super_restore_3_unf")).findFirst().orElseThrow(AssertionError::new);

		long staleTime = TEST_NOW_SEC - 7200L; // 2 hours old (> 1 hour threshold)
		Map<Integer, ItemPrice> prices = new HashMap<>();
		prices.put(ItemID.SNAPDRAGON_POTION_UNF, makePrice(ItemID.SNAPDRAGON_POTION_UNF, "Snapdragon potion (unf)", 9_000L, 8_900L, staleTime));
		prices.put(ItemID.RED_SPIDERS_EGGS, makePrice(ItemID.RED_SPIDERS_EGGS, "Red spiders' eggs", 900L, 880L, TEST_NOW_SEC));
		prices.put(ItemID.SUPER_RESTORE3, makePrice(ItemID.SUPER_RESTORE3, "Super restore(3)", 10_200L, 10_100L, TEST_NOW_SEC));

		PotionProfitResult staleRes = calculator.calculate(
			restore3, prices, PriceCalculationMode.INSTANT_CONSERVATIVE,
			true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 12.0
		);
		Assert.assertTrue(staleRes.isStalePriceWarning());
		Assert.assertEquals(PriceStatus.UNCERTAIN_OR_STALE, staleRes.getPriceStatus());

		// Volatile spread test (Buy 15,000 vs Sell 10,000 -> 50% spread > 12% threshold)
		prices.put(ItemID.SNAPDRAGON_POTION_UNF, makePrice(ItemID.SNAPDRAGON_POTION_UNF, "Snapdragon potion (unf)", 9_000L, 8_900L, TEST_NOW_SEC));
		prices.put(ItemID.SUPER_RESTORE3, makePrice(ItemID.SUPER_RESTORE3, "Super restore(3)", 15_000L, 10_000L, TEST_NOW_SEC));

		PotionProfitResult volatileRes = calculator.calculate(
			restore3, prices, PriceCalculationMode.INSTANT_CONSERVATIVE,
			true, 75, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 12.0
		);
		Assert.assertTrue(volatileRes.isVolatilePriceWarning());
		Assert.assertEquals(PriceStatus.UNCERTAIN_OR_STALE, volatileRes.getPriceStatus());
	}

	@Test
	public void testUntradeableCrystalDustDivinePotionHandledGracefully()
	{
		PotionRecipe divineScb4 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("divine_super_combat_4")).findFirst().orElseThrow(AssertionError::new);

		Map<Integer, ItemPrice> prices = new HashMap<>();
		prices.put(ItemID.SUPER_COMBAT_POTION4, makePrice(ItemID.SUPER_COMBAT_POTION4, "Super combat potion(4)", 17_800L, 17_600L, TEST_NOW_SEC));
		prices.put(ItemID.DIVINE_SUPER_COMBAT_POTION4, makePrice(ItemID.DIVINE_SUPER_COMBAT_POTION4, "Divine super combat potion(4)", 18_400L, 18_200L, TEST_NOW_SEC));
		// Note: Crystal dust (ItemID.CRYSTAL_DUST) is untradeable and intentionally absent from GE prices

		PotionProfitResult res = calculator.calculate(
			divineScb4, prices, PriceCalculationMode.INSTANT_CONSERVATIVE,
			true, 97, 1_000_000L, TEST_NOW_SEC, true, 3600L, true, 12.0
		);

		Assert.assertTrue(res.isHasCompletePriceData());
		Assert.assertEquals(17_800L, res.getTotalIngredientCost());
		Assert.assertEquals(18_200L, res.getFinishedPotionSalePrice());
	}
}
