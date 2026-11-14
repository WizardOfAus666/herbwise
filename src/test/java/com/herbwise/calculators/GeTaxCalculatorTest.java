package com.herbwise.calculators;

import com.herbwise.data.GeTaxRules;
import net.runelite.api.ItemID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link GeTaxCalculator} verifying all OSRS Grand Exchange tax rules (Section 4.C & Section 17).
 */
public class GeTaxCalculatorTest
{
	private GeTaxCalculator taxCalculator;

	@Before
	public void setUp()
	{
		taxCalculator = new GeTaxCalculator();
	}

	@Test
	public void testItemsUnder50GpAreTaxExempt()
	{
		Assert.assertEquals(0L, taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 0L, true));
		Assert.assertEquals(0L, taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 1L, true));
		Assert.assertEquals(0L, taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 25L, true));
		Assert.assertEquals(0L, taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 49L, true));
		// At 50 GP, 2% is exactly 1 GP
		Assert.assertEquals(1L, taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 50L, true));
	}

	@Test
	public void testStandardTwoPercentTaxRoundsDownPerUnit()
	{
		// 99 GP * 0.02 = 1.98 -> floors to 1 GP
		Assert.assertEquals(1L, taxCalculator.calculateTaxPerItem(ItemID.SUPER_RESTORE4, 99L, true));
		// 100 GP * 0.02 = 2.00 -> 2 GP
		Assert.assertEquals(2L, taxCalculator.calculateTaxPerItem(ItemID.SUPER_RESTORE4, 100L, true));
		// 4,100 GP (Super strength(4) example) * 0.02 = 82 GP
		Assert.assertEquals(82L, taxCalculator.calculateTaxPerItem(ItemID.SUPER_STRENGTH4, 4_100L, true));
		// 9,255 GP * 0.02 = 185.1 -> floors to 185 GP
		Assert.assertEquals(185L, taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 9_255L, true));
	}

	@Test
	public void testFiveMillionGpTaxCapPerItem()
	{
		// 250,000,000 GP * 0.02 = 5,000,000 GP (exact cap)
		Assert.assertEquals(GeTaxRules.MAX_TAX_PER_ITEM_GP,
			taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 250_000_000L, true));

		// 500,000,000 GP -> capped at 5,000,000 GP
		Assert.assertEquals(5_000_000L,
			taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 500_000_000L, true));

		// 2,147,483,647 GP -> capped at 5,000,000 GP
		Assert.assertEquals(5_000_000L,
			taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 2_147_483_647L, true));
	}

	@Test
	public void testExplicitTaxExemptItemsPayZeroTax()
	{
		Assert.assertTrue(GeTaxRules.isItemTaxExempt(ItemID.OLD_SCHOOL_BOND));
		Assert.assertEquals(0L, taxCalculator.calculateTaxPerItem(ItemID.OLD_SCHOOL_BOND, 14_000_000L, true));
		Assert.assertEquals(0L, taxCalculator.calculateTaxPerItem(ItemID.CHISEL, 500L, true));
		Assert.assertEquals(0L, taxCalculator.calculateTaxPerItem(ItemID.PESTLE_AND_MORTAR, 500L, true));
	}

	@Test
	public void testIncludeTaxFlagDisabledReturnsZero()
	{
		Assert.assertEquals(0L, taxCalculator.calculateTaxPerItem(ItemID.PRAYER_POTION4, 10_000L, false));
		Assert.assertEquals(10_000L, taxCalculator.calculateNetSalePricePerItem(ItemID.PRAYER_POTION4, 10_000L, false));
	}

	@Test
	public void testIngredientPurchasesNeverChargedGeTax()
	{
		Assert.assertEquals(0L, taxCalculator.calculateIngredientPurchaseTax(ItemID.RANARR_WEED, 6_400L));
		Assert.assertEquals(0L, taxCalculator.calculateIngredientPurchaseTax(ItemID.SNAPE_GRASS, 380L));
		Assert.assertEquals(0L, taxCalculator.calculateIngredientPurchaseTax(ItemID.VIAL_OF_WATER, 5L));
	}

	@Test
	public void testBatchTaxAndNetSaleValueCalculation()
	{
		// 99 GP per unit -> 1 GP tax per unit (not 2% of 99,000 = 1,980!)
		// OSRS GE taxes each item individually rounded down: 1 GP * 1,000 = 1,000 GP
		Assert.assertEquals(1_000L, taxCalculator.calculateTotalTax(ItemID.ATTACK_POTION4, 99L, 1_000L, true));
		Assert.assertEquals(98L, taxCalculator.calculateNetSalePricePerItem(ItemID.ATTACK_POTION4, 99L, true));
	}
}
