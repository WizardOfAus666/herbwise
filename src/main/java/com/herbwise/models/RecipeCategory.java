package com.herbwise.models;

/**
 * High-level categorisation of OSRS Herblore recipes.
 * Unfinished potions are explicitly separated from completed potions.
 */
public enum RecipeCategory
{
	COMBAT_POTION("Combat & Stat Potions", false),
	RESTORE_PRAYER("Restore & Prayer", false),
	ANTIPOISON_ANTIVENOM("Antipoison & Antivenom", false),
	ANTIFIRE("Antifire Protection", false),
	SKILLING_ENERGY("Skilling & Energy", false),
	BREW_REMEDY("Brews & Remedies", false),
	DIVINE_POTION("Divine Potions", false),
	WEAPON_POISON("Weapon Poisons", false),
	SPECIAL_QUEST("Special & Minigame", false),
	UNFINISHED_POTION("Unfinished Potions (0 XP)", true);

	private final String displayName;
	private final boolean unfinishedCategory;

	RecipeCategory(String displayName, boolean unfinishedCategory)
	{
		this.displayName = displayName;
		this.unfinishedCategory = unfinishedCategory;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public boolean isUnfinishedCategory()
	{
		return unfinishedCategory;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
