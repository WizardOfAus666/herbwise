package com.herbwise.calculators;

import com.herbwise.data.GeTaxRules;
import javax.inject.Singleton;

/**
 * Dedicated Grand Exchange tax calculation engine implementing official OSRS GE tax rules:
 * - 2% tax on eligible finished item sales, rounded down per individual item.
 * - 0 GP tax on items selling for under 50 GP.
 * - 5,000,000 GP maximum tax cap per individual item (sales >= 250,000,000 GP).
 * - 0 GP tax on explicitly exempt or untradeable items.
 * - 0 GP tax on all ingredient purchases (buyer never pays GE tax).
 */
@Singleton
public class GeTaxCalculator
{
	/**
	 * Calculates the Grand Exchange tax deducted per single item sold.
	 */
	public long calculateTaxPerItem(int itemId, long salePricePerItem, boolean includeTax)
	{
		if (!includeTax || salePricePerItem < GeTaxRules.MIN_TAXABLE_PRICE_GP)
		{
			return 0L;
		}
		if (GeTaxRules.isItemTaxExempt(itemId))
		{
			return 0L;
		}
		long rawTax = salePricePerItem / GeTaxRules.TAX_DIVISOR;
		return Math.min(GeTaxRules.MAX_TAX_PER_ITEM_GP, rawTax);
	}

	/**
	 * Calculates the total Grand Exchange tax for selling {@code quantity} of an item.
	 * Per OSRS mechanics, tax is rounded down per individual item first, then multiplied by quantity.
	 */
	public long calculateTotalTax(int itemId, long salePricePerItem, long quantity, boolean includeTax)
	{
		if (quantity <= 0L)
		{
			return 0L;
		}
		long taxPerItem = calculateTaxPerItem(itemId, salePricePerItem, includeTax);
		if (taxPerItem <= 0L)
		{
			return 0L;
		}
		return Math.multiplyExact(taxPerItem, quantity);
	}

	/**
	 * Calculates the net GP received per item after deducting applicable Grand Exchange tax.
	 */
	public long calculateNetSalePricePerItem(int itemId, long grossSalePricePerItem, boolean includeTax)
	{
		if (grossSalePricePerItem <= 0L)
		{
			return 0L;
		}
		long tax = calculateTaxPerItem(itemId, grossSalePricePerItem, includeTax);
		return Math.max(0L, grossSalePricePerItem - tax);
	}

	/**
	 * Buying ingredients on the Grand Exchange never incurs tax for the buyer.
	 */
	public long calculateIngredientPurchaseTax(int itemId, long purchasePrice)
	{
		return 0L;
	}
}
