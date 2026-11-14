package com.herbwise.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.ItemID;

/**
 * Defines Old School RuneScape Grand Exchange tax rules:
 * - Seller pays 2% tax (rounded down to the nearest whole GP per item).
 * - Items sold for under 50 GP incur 0 GP tax.
 * - Tax is capped at 5,000,000 GP per single item (sales >= 250,000,000 GP).
 * - Buying items on the Grand Exchange never incurs GE tax.
 * - Specific exempt items (such as Old School Bonds and untradeable items) are tax-exempt.
 */
public final class GeTaxRules
{
	/**
	 * Official OSRS GE tax rate (2% = 1 / 50).
	 */
	public static final double TAX_RATE = 0.02;

	/**
	 * Integer divisor equivalent to floor(price * 0.02) = price / 50.
	 */
	public static final long TAX_DIVISOR = 50L;

	/**
	 * Minimum sale price per item before any GE tax applies (items sold for < 50 GP pay 0 tax).
	 */
	public static final long MIN_TAXABLE_PRICE_GP = 50L;

	/**
	 * Maximum GE tax charged on a single item (5,000,000 GP cap).
	 */
	public static final long MAX_TAX_PER_ITEM_GP = 5_000_000L;

	/**
	 * Sale price threshold where the 5,000,000 GP cap is reached (250,000,000 GP).
	 */
	public static final long TAX_CAP_PRICE_THRESHOLD_GP = 250_000_000L;

	private static final Set<Integer> EXEMPT_ITEM_IDS;

	static
	{
		Set<Integer> exempt = new HashSet<>();
		// Old School Bonds and basic exempt tools/consumables per OSRS Wiki GE tax exemption list
		exempt.add(ItemID.OLD_SCHOOL_BOND);
		exempt.add(ItemID.CHISEL);
		exempt.add(ItemID.GARDENING_TROWEL);
		exempt.add(ItemID.HAMMER);
		exempt.add(ItemID.NEEDLE);
		exempt.add(ItemID.PESTLE_AND_MORTAR);
		exempt.add(ItemID.RAKE);
		exempt.add(ItemID.SAW);
		exempt.add(ItemID.SECATEURS);
		exempt.add(ItemID.SEED_DIBBER);
		exempt.add(ItemID.SHEARS);
		exempt.add(ItemID.SPADE);
		exempt.add(ItemID.WATERING_CAN);
		// Untradeable Herblore quest/minigame items cannot be taxed on the GE
		exempt.add(ItemID.SERUM_207_4);
		exempt.add(ItemID.SERUM_207_3);
		exempt.add(ItemID.SERUM_207_2);
		exempt.add(ItemID.SERUM_207_1);
		exempt.add(ItemID.MOONLIGHT_POTION4);
		exempt.add(ItemID.MOONLIGHT_POTION3);
		exempt.add(ItemID.MOONLIGHT_POTION2);
		exempt.add(ItemID.MOONLIGHT_POTION1);
		exempt.add(ItemID.MOONLIGHT_GRUB);
		exempt.add(ItemID.MOONLIGHT_GRUB_PASTE);
		exempt.add(ItemID.CRYSTAL_DUST);
		EXEMPT_ITEM_IDS = Collections.unmodifiableSet(exempt);
	}

	private GeTaxRules()
	{
	}

	/**
	 * Returns true if the given item ID is explicitly exempt from Grand Exchange tax.
	 */
	public static boolean isItemTaxExempt(int itemId)
	{
		if (itemId <= 0)
		{
			return true;
		}
		if (EXEMPT_ITEM_IDS.contains(itemId))
		{
			return true;
		}
		return !ItemIdMappings.isTradeable(itemId);
	}

	public static Set<Integer> getExemptItemIds()
	{
		return EXEMPT_ITEM_IDS;
	}
}
