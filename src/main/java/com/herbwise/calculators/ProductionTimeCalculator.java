package com.herbwise.calculators;

import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionRecipe;
import javax.inject.Singleton;
import net.runelite.api.ItemID;

/**
 * Calculates realistic OSRS Herblore production rates, potions per inventory load,
 * and estimated training duration based on ingredient stackability and recipe steps.
 */
@Singleton
public class ProductionTimeCalculator
{
	public static final int INVENTORY_SLOTS = 28;
	public static final int DEFAULT_STANDARD_POTIONS_PER_HOUR = 2500;
	public static final int DEFAULT_STACKABLE_POTIONS_PER_HOUR = 2700;
	public static final int DEFAULT_TWO_STEP_POTIONS_PER_HOUR = 1200;

	/**
	 * Determines how many potions can be mixed in a single 28-slot OSRS inventory load.
	 * - 1 non-stackable potion + 1 stackable secondary (e.g. Amylase, Zulrah's scales, Lava scale shards): 27 per inventory.
	 * - 1 non-stackable unf potion + 1 non-stackable secondary: 14 per inventory.
	 * - 3 non-stackable items (e.g. Herb + Vial + Secondary): 9 per inventory.
	 * - 4 non-stackable items (e.g. Super combat potion: Torstol + 3 potions): 7 per inventory.
	 */
	public int getPotionsPerInventoryLoad(PotionRecipe recipe)
	{
		if (recipe == null || recipe.getIngredients().isEmpty())
		{
			return 14;
		}

		int nonStackableSlotsPerPotion = 0;
		int stackableTypesCount = 0;

		for (PotionIngredient ing : recipe.getIngredients())
		{
			if (isStackableIngredient(ing.getItemId()))
			{
				stackableTypesCount++;
			}
			else
			{
				nonStackableSlotsPerPotion += Math.max(1, ing.getQuantity());
			}
		}

		if (nonStackableSlotsPerPotion <= 0)
		{
			return 27;
		}

		int availableSlotsForNonStackables = INVENTORY_SLOTS - stackableTypesCount;
		int perLoad = availableSlotsForNonStackables / nonStackableSlotsPerPotion;
		return Math.max(1, perLoad);
	}

	/**
	 * Calculates total bank inventory loads needed to produce {@code totalPotions}.
	 */
	public long calculateInventoryLoads(PotionRecipe recipe, long totalPotions)
	{
		if (totalPotions <= 0L)
		{
			return 0L;
		}
		int perLoad = getPotionsPerInventoryLoad(recipe);
		return (totalPotions + perLoad - 1L) / perLoad;
	}

	/**
	 * Returns the estimated potions mixed per hour for a given recipe.
	 */
	public int getEstimatedPotionsPerHour(PotionRecipe recipe)
	{
		if (recipe == null)
		{
			return DEFAULT_STANDARD_POTIONS_PER_HOUR;
		}
		if (recipe.getPotionsPerHourEstimate() > 0)
		{
			return recipe.getPotionsPerHourEstimate();
		}
		int perLoad = getPotionsPerInventoryLoad(recipe);
		if (perLoad >= 27)
		{
			return DEFAULT_STACKABLE_POTIONS_PER_HOUR;
		}
		if (perLoad <= 9)
		{
			return DEFAULT_TWO_STEP_POTIONS_PER_HOUR;
		}
		return DEFAULT_STANDARD_POTIONS_PER_HOUR;
	}

	/**
	 * Calculates estimated Herblore XP gained per hour.
	 */
	public long calculateEstimatedXpPerHour(PotionRecipe recipe)
	{
		if (recipe == null || recipe.getXpGained() <= 0.0)
		{
			return 0L;
		}
		int perHour = getEstimatedPotionsPerHour(recipe);
		return Math.round(recipe.getXpGained() * perHour);
	}

	/**
	 * Calculates estimated GP profit/loss per hour.
	 */
	public long calculateEstimatedProfitPerHour(PotionRecipe recipe, long netProfitPerPotion)
	{
		if (recipe == null)
		{
			return 0L;
		}
		int perHour = getEstimatedPotionsPerHour(recipe);
		return Math.multiplyExact(netProfitPerPotion, (long) perHour);
	}

	/**
	 * Calculates estimated hours needed to produce {@code totalPotions}.
	 */
	public double calculateHoursRequired(PotionRecipe recipe, long totalPotions)
	{
		if (totalPotions <= 0L)
		{
			return 0.0;
		}
		int perHour = getEstimatedPotionsPerHour(recipe);
		return (double) totalPotions / (double) Math.max(1, perHour);
	}

	/**
	 * Calculates estimated minutes needed to produce {@code totalPotions}.
	 */
	public double calculateMinutesRequired(PotionRecipe recipe, long totalPotions)
	{
		return calculateHoursRequired(recipe, totalPotions) * 60.0;
	}

	/**
	 * Formats a duration in minutes into a concise human-readable string (e.g. "2h 15m" or "18 min").
	 */
	public static String formatDurationMinutes(double totalMinutes)
	{
		if (totalMinutes <= 0.0)
		{
			return "0 min";
		}
		long roundedMin = Math.max(1L, Math.round(totalMinutes));
		if (roundedMin < 60L)
		{
			return roundedMin + " min";
		}
		long hours = roundedMin / 60L;
		long mins = roundedMin % 60L;
		return mins > 0 ? (hours + "h " + mins + "m") : (hours + "h");
	}

	public static boolean isStackableIngredient(int itemId)
	{
		switch (itemId)
		{
			case ItemID.AMYLASE_CRYSTAL:
			case ItemID.LAVA_SCALE_SHARD:
			case ItemID.ZULRAHS_SCALES:
			case ItemID.NIHIL_DUST:
			case ItemID.ANCIENT_ESSENCE:
			case ItemID.VOLCANIC_ASH:
			case ItemID.CRUSHED_SUPERIOR_DRAGON_BONES:
			case ItemID.ARAXYTE_VENOM_SAC:
			case ItemID.CRYSTAL_DUST:
			case ItemID.NAIL_BEAST_NAILS:
			case ItemID.MOONLIGHT_GRUB_PASTE:
				return true;
			default:
				return false;
		}
	}
}
