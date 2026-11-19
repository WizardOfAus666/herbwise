package com.herbwise;

import com.herbwise.models.PriceCalculationMode;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * RuneLite configuration interface for HerbWise.
 * All settings persist between RuneLite sessions via RuneLite's ConfigManager.
 */
@ConfigGroup(HerbWiseConfig.CONFIG_GROUP)
public interface HerbWiseConfig extends Config
{
	String CONFIG_GROUP = "herbwise";

	enum TableDisplayMode
	{
		DETAILED("Detailed Table"),
		COMPACT("Compact Table");

		private final String label;

		TableDisplayMode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	enum HerbloreBoostMode
	{
		NONE("No Boost (+0)", 0),
		BOTANICAL_PIE("Botanical Pie (+4)", 4),
		SPICY_STEW("Spicy Stew (+5 Max)", 5);

		private final String label;
		private final int boostLevels;

		HerbloreBoostMode(String label, int boostLevels)
		{
			this.label = label;
			this.boostLevels = boostLevels;
		}

		public int getBoostLevels()
		{
			return boostLevels;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigSection(
		name = "Price & Market Settings",
		description = "Configure OSRS Wiki Real-Time Prices API polling, price calculation mode, and Grand Exchange tax.",
		position = 0
	)
	String SECTION_PRICES = "prices";

	@ConfigSection(
		name = "Filtering & Display",
		description = "Configure default dashboard filters, table density, and XP/hr display.",
		position = 1
	)
	String SECTION_DISPLAY = "display";

	@ConfigSection(
		name = "Planner Defaults",
		description = "Configure default GP budget, target Herblore level, skill boost, and notifications.",
		position = 2
	)
	String SECTION_DEFAULTS = "defaults";

	@Range(min = 30, max = 3600)
	@ConfigItem(
		keyName = "priceRefreshIntervalSeconds",
		name = "Price refresh interval (s)",
		description = "Background refresh interval in seconds for fetching OSRS Wiki prices (30 to 3600s, default 120s).",
		section = SECTION_PRICES,
		position = 1
	)
	default int priceRefreshIntervalSeconds()
	{
		return 120;
	}

	@ConfigItem(
		keyName = "priceCalculationMode",
		name = "Price calculation mode",
		description = "Select how ingredient buying prices and finished potion selling prices are calculated.",
		section = SECTION_PRICES,
		position = 2
	)
	default PriceCalculationMode priceCalculationMode()
	{
		return PriceCalculationMode.INSTANT_CONSERVATIVE;
	}

	@ConfigItem(
		keyName = "includeGeTax",
		name = "Include Grand Exchange tax",
		description = "Deduct the 2% OSRS Grand Exchange seller tax (capped at 5M GP, 0 GP under 50 GP) from finished potion sales.",
		section = SECTION_PRICES,
		position = 3
	)
	default boolean includeGeTax()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableVolatilityWarnings",
		name = "Enable price volatility warnings",
		description = "Warn when an ingredient or potion has a wide instant buy/sell spread or sharp 5m vs 1h swing.",
		section = SECTION_PRICES,
		position = 4
	)
	default boolean enableVolatilityWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableStalePriceWarnings",
		name = "Enable stale price warnings",
		description = "Warn when an item's most recent Grand Exchange trade timestamp is older than 3 hours or from cache.",
		section = SECTION_PRICES,
		position = 5
	)
	default boolean enableStalePriceWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showProfitableOnly",
		name = "Show profitable recipes only",
		description = "Filter the main recommendations table to show only profitable potions by default.",
		section = SECTION_DISPLAY,
		position = 6
	)
	default boolean showProfitableOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showAvailableAtLevelOnly",
		name = "Show available at current level only",
		description = "Only display potions that your current Herblore level (plus active boost) can make.",
		section = SECTION_DISPLAY,
		position = 7
	)
	default boolean showAvailableAtLevelOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showXpPerHourEstimates",
		name = "Show XP per hour estimates",
		description = "Display estimated Herblore XP/hr and GP/hr rates across tables and recommendation cards.",
		section = SECTION_DISPLAY,
		position = 8
	)
	default boolean showXpPerHourEstimates()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tableDisplayMode",
		name = "Table display mode",
		description = "Switch between a detailed multi-metric view and a compact single-line table.",
		section = SECTION_DISPLAY,
		position = 9
	)
	default TableDisplayMode tableDisplayMode()
	{
		return TableDisplayMode.DETAILED;
	}

	@Range(min = 1000, max = 2147483647)
	@ConfigItem(
		keyName = "defaultBudgetGp",
		name = "Default budget (GP)",
		description = "Default Grand Exchange GP budget used in the Budget Calculator and affordability checks.",
		section = SECTION_DEFAULTS,
		position = 10
	)
	default int defaultBudgetGp()
	{
		return 1_000_000;
	}

	@Range(min = 2, max = 99)
	@ConfigItem(
		keyName = "defaultTargetLevel",
		name = "Default target level",
		description = "Default target Herblore level (2..99) in the Production & XP Calculator.",
		section = SECTION_DEFAULTS,
		position = 11
	)
	default int defaultTargetLevel()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "herbloreBoostMode",
		name = "Supported Herblore boost",
		description = "Optional temporary Herblore boost (Botanical pie +4 or Spicy stew +5) applied to level unlock checks.",
		section = SECTION_DEFAULTS,
		position = 12
	)
	default HerbloreBoostMode herbloreBoostMode()
	{
		return HerbloreBoostMode.NONE;
	}

	@ConfigItem(
		keyName = "enableNotifications",
		name = "Enable notifications",
		description = "Send a RuneLite notification when a high-profit potion opportunity is detected on refresh.",
		section = SECTION_DEFAULTS,
		position = 13
	)
	default boolean enableNotifications()
	{
		return false;
	}
}
