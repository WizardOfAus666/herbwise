package com.herbwise.calculators;

import com.herbwise.data.HerbloreRecipes;
import com.herbwise.data.HerbloreXpTable;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.PriceCalculationMode;
import com.herbwise.models.XpProductionPlan;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.ItemID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link HerbloreXpCalculator} and {@link HerbloreXpTable} (Section 9 & Section 17).
 */
public class HerbloreXpCalculatorTest
{
	private HerbloreXpCalculator xpCalculator;
	private PotionProfitCalculator profitCalculator;

	@Before
	public void setUp()
	{
		ProductionTimeCalculator timeCalc = new ProductionTimeCalculator();
		xpCalculator = new HerbloreXpCalculator(timeCalc);
		profitCalculator = new PotionProfitCalculator(new GeTaxCalculator(), timeCalc);
	}

	private ItemPrice makePrice(int id, String name, long buy, long sell)
	{
		return ItemPrice.builder()
			.itemId(id)
			.itemName(name)
			.instantBuyPrice(buy)
			.instantBuyTimestampSec(1_760_000_000L)
			.instantSellPrice(sell)
			.instantSellTimestampSec(1_760_000_000L)
			.avgHighPrice5m(buy)
			.avgLowPrice5m(sell)
			.highVolume5m(500)
			.lowVolume5m(500)
			.avgHighPrice1h(buy)
			.avgLowPrice1h(sell)
			.highVolume1h(2000)
			.lowVolume1h(2000)
			.buyLimit(2000)
			.members(true)
			.fetchedAtEpochSec(1_760_000_000L)
			.fromCache(false)
			.build();
	}

	@Test
	public void testOfficialOsrsExperienceTableValues()
	{
		Assert.assertEquals(0L, HerbloreXpTable.getXpForLevel(1));
		Assert.assertEquals(83L, HerbloreXpTable.getXpForLevel(2));
		Assert.assertEquals(174L, HerbloreXpTable.getXpForLevel(3));
		Assert.assertEquals(1_154L, HerbloreXpTable.getXpForLevel(10));
		Assert.assertEquals(30_408L, HerbloreXpTable.getXpForLevel(38)); // Prayer potion unlock
		Assert.assertEquals(737_627L, HerbloreXpTable.getXpForLevel(70));
		Assert.assertEquals(1_210_421L, HerbloreXpTable.getXpForLevel(75));
		Assert.assertEquals(1_986_068L, HerbloreXpTable.getXpForLevel(80));
		Assert.assertEquals(3_258_594L, HerbloreXpTable.getXpForLevel(85));
		Assert.assertEquals(5_346_332L, HerbloreXpTable.getXpForLevel(90));
		Assert.assertEquals(8_771_558L, HerbloreXpTable.getXpForLevel(95));
		Assert.assertEquals(13_034_431L, HerbloreXpTable.getXpForLevel(99));

		Assert.assertEquals(1, HerbloreXpTable.getLevelForXp(0L));
		Assert.assertEquals(38, HerbloreXpTable.getLevelForXp(30_408L));
		Assert.assertEquals(75, HerbloreXpTable.getLevelForXp(1_210_421L));
		Assert.assertEquals(99, HerbloreXpTable.getLevelForXp(13_034_431L));
		Assert.assertEquals(99, HerbloreXpTable.getLevelForXp(200_000_000L));
	}

	@Test
	public void testCalculateToTargetLevelPresets()
	{
		PotionRecipe superAttack3 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("super_attack_3_unf")).findFirst().orElseThrow(AssertionError::new);

		Map<Integer, ItemPrice> prices = new HashMap<>();
		prices.put(ItemID.IRIT_POTION_UNF, makePrice(ItemID.IRIT_POTION_UNF, "Irit potion (unf)", 1_200L, 1_150L));
		prices.put(ItemID.EYE_OF_NEWT, makePrice(ItemID.EYE_OF_NEWT, "Eye of newt", 6L, 5L));
		prices.put(ItemID.SUPER_ATTACK3, makePrice(ItemID.SUPER_ATTACK3, "Super attack(3)", 800L, 760L));

		PotionProfitResult evaluated = profitCalculator.calculate(
			superAttack3, prices, PriceCalculationMode.INSTANT_CONSERVATIVE,
			true, 70, 5_000_000L, 1_760_000_000L, true, 3600L, true, 12.0
		);

		// Level 70 (737,627 XP) to Level 75 (1,210,421 XP):
		// Deficit = 1,210,421 - 737,627 = 472,794 XP
		// Super attack(3) gives 100.0 XP -> ceil(472,794 / 100.0) = 4,728 potions
		XpProductionPlan plan = xpCalculator.calculateByLevels(70, 75, evaluated);

		Assert.assertEquals(472_794L, plan.getXpRequired());
		Assert.assertEquals(4_728L, plan.getPotionsRequired());
		Assert.assertEquals(472_800.0, plan.getTotalXpGained(), 0.001);
		// Cost per potion = 1,200 + 6 = 1,206 GP -> Total = 4,728 * 1,206 = 5,701,968 GP
		Assert.assertEquals(5_701_968L, plan.getTotalInvestmentGp());
		Assert.assertEquals(2, plan.getTotalIngredientsNeeded().size());
		// Inventory loads = ceil(4,728 / 14.0) = 338 loads
		Assert.assertEquals(338L, plan.getInventoryLoadsRequired());
		Assert.assertTrue(plan.getEstimatedHours() > 1.5);
	}

	@Test
	public void testCalculateForTenThousandXpGoal()
	{
		// Section 7 requirement: "If the player wants 10,000 XP, calculate exactly how many potions and ingredients they need."
		PotionRecipe prayer3 = HerbloreRecipes.getAllRecipes().stream()
			.filter(r -> r.getRecipeId().equals("prayer_potion_3_unf")).findFirst().orElseThrow(AssertionError::new);

		Map<Integer, ItemPrice> prices = new HashMap<>();
		prices.put(ItemID.RANARR_POTION_UNF, makePrice(ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)", 6_500L, 6_450L));
		prices.put(ItemID.SNAPE_GRASS, makePrice(ItemID.SNAPE_GRASS, "Snape grass", 380L, 370L));
		prices.put(ItemID.PRAYER_POTION3, makePrice(ItemID.PRAYER_POTION3, "Prayer potion(3)", 6_980L, 6_920L));

		PotionProfitResult evaluated = profitCalculator.calculate(
			prayer3, prices, PriceCalculationMode.INSTANT_CONSERVATIVE,
			true, 75, 1_000_000L, 1_760_000_000L, true, 3600L, true, 12.0
		);

		// 10,000 XP / 87.5 XP per potion = 114.2857 -> ceil = 115 potions
		XpProductionPlan plan = xpCalculator.calculateForDesiredXpGain(100_000L, 10_000L, evaluated);
		Assert.assertEquals(10_000L, plan.getXpRequired());
		Assert.assertEquals(115L, plan.getPotionsRequired());
		Assert.assertEquals(115 * 87.5, plan.getTotalXpGained(), 0.001);
	}
}
