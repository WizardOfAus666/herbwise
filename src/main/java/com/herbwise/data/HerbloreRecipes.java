package com.herbwise.data;

import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.RecipeCategory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.ItemID;

/**
 * Authoritative local database of verified Old School RuneScape Herblore potion recipes.
 * Covers all standard Herblore potions, brews, remedies, antivenoms, antifires, weapon poisons,
 * divine potions, extended potions, alternative from-scratch herb recipes, 4/3/2/1-dose variants,
 * and clearly-categorised unfinished potions.
 */
public final class HerbloreRecipes
{
	private static final List<PotionRecipe> ALL_RECIPES;
	private static final Map<String, PotionRecipe> BY_RECIPE_ID;

	static
	{
		List<PotionRecipe> list = new ArrayList<>();

		// =========================================================================
		// 1. STANDARD HERBLORE POTIONS (4/3/2/1-DOSE + FROM-HERB ALTERNATIVES)
		// =========================================================================

		// Attack potion (Level 3, 25.0 XP)
		addStandardFamily(list, "attack_potion", "Attack potion",
			3, 25.0, RecipeCategory.COMBAT_POTION,
			ItemID.ATTACK_POTION4, ItemID.ATTACK_POTION3, ItemID.ATTACK_POTION2, ItemID.ATTACK_POTION1,
			ItemID.GUAM_POTION_UNF, "Guam potion (unf)",
			ItemID.GUAM_LEAF, "Guam leaf",
			ItemID.EYE_OF_NEWT, "Eye of newt", 1,
			"Requires Druidic Ritual quest completion (unlocks Level 3 Herblore).");

		// Antipoison (Level 5, 37.5 XP)
		addStandardFamily(list, "antipoison", "Antipoison",
			5, 37.5, RecipeCategory.ANTIPOISON_ANTIVENOM,
			ItemID.ANTIPOISON4, ItemID.ANTIPOISON3, ItemID.ANTIPOISON2, ItemID.ANTIPOISON1,
			ItemID.MARRENTILL_POTION_UNF, "Marrentill potion (unf)",
			ItemID.MARRENTILL, "Marrentill",
			ItemID.UNICORN_HORN_DUST, "Unicorn horn dust", 1,
			"Cures poison and provides 90 seconds of poison immunity.");

		// Strength potion (Level 12, 50.0 XP)
		addStandardFamily(list, "strength_potion", "Strength potion",
			12, 50.0, RecipeCategory.COMBAT_POTION,
			ItemID.STRENGTH_POTION4, ItemID.STRENGTH_POTION3, ItemID.STRENGTH_POTION2, ItemID.STRENGTH_POTION1,
			ItemID.TARROMIN_POTION_UNF, "Tarromin potion (unf)",
			ItemID.TARROMIN, "Tarromin",
			ItemID.LIMPWURT_ROOT, "Limpwurt root", 1,
			"Boosts Strength by 3 + 10% of current level.");

		// Serum 207 (Level 15, 50.0 XP — Untradeable finished potion)
		addUntradeableStandardFamily(list, "serum_207", "Serum 207",
			15, 50.0, RecipeCategory.SPECIAL_QUEST,
			ItemID.SERUM_207_4, ItemID.SERUM_207_3, ItemID.SERUM_207_2, ItemID.SERUM_207_1,
			ItemID.TARROMIN_POTION_UNF, "Tarromin potion (unf)",
			ItemID.TARROMIN, "Tarromin",
			ItemID.ASHES, "Ashes", 1,
			"Requires partial completion of Shades of Mort'ton. Output is untradeable; popular for low-cost early XP.");

		// Guthix rest (Level 18, 59.5 XP)
		list.add(PotionRecipe.builder()
			.recipeId("guthix_rest_4")
			.familyName("Guthix rest")
			.potionName("Guthix rest(4)")
			.potionItemId(ItemID.GUTHIX_REST4)
			.doses(4)
			.requiredLevel(18)
			.xpGained(59.5)
			.unfinishedPotionId(ItemID.CUP_OF_HOT_WATER)
			.unfinishedPotionName("Cup of hot water")
			.secondarySummary("2x Guam + Marrentill + Harralander")
			.category(RecipeCategory.BREW_REMEDY)
			.potionsPerHourEstimate(950)
			.preparationNotes("Requires One Small Favour quest step. Combines Cup of hot water with 2x Guam leaf, 1x Marrentill, and 1x Harralander.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.CUP_OF_HOT_WATER, "Cup of hot water", 1, true, "Liquid Base"),
				new PotionIngredient(ItemID.GUAM_LEAF, "Guam leaf", 2, true, "Clean Herb"),
				new PotionIngredient(ItemID.MARRENTILL, "Marrentill", 1, true, "Clean Herb"),
				new PotionIngredient(ItemID.HARRALANDER, "Harralander", 1, true, "Clean Herb")
			))
			.build());
		list.add(PotionRecipe.builder()
			.recipeId("guthix_rest_3")
			.familyName("Guthix rest")
			.potionName("Guthix rest(3)")
			.potionItemId(ItemID.GUTHIX_REST3)
			.doses(3)
			.requiredLevel(18)
			.xpGained(59.5)
			.unfinishedPotionId(ItemID.CUP_OF_HOT_WATER)
			.unfinishedPotionName("Cup of hot water")
			.secondarySummary("2x Guam + Marrentill + Harralander")
			.category(RecipeCategory.BREW_REMEDY)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(950)
			.preparationNotes("Direct 3-dose output from mixing Guthix rest tea.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.CUP_OF_HOT_WATER, "Cup of hot water", 1, true, "Liquid Base"),
				new PotionIngredient(ItemID.GUAM_LEAF, "Guam leaf", 2, true, "Clean Herb"),
				new PotionIngredient(ItemID.MARRENTILL, "Marrentill", 1, true, "Clean Herb"),
				new PotionIngredient(ItemID.HARRALANDER, "Harralander", 1, true, "Clean Herb")
			))
			.build());

		// Compost potion (Level 21, 60.0 XP)
		addStandardFamily(list, "compost_potion", "Compost potion",
			21, 60.0, RecipeCategory.SKILLING_ENERGY,
			ItemID.COMPOST_POTION4, ItemID.COMPOST_POTION3, ItemID.COMPOST_POTION2, ItemID.COMPOST_POTION1,
			ItemID.HARRALANDER_POTION_UNF, "Harralander potion (unf)",
			ItemID.HARRALANDER, "Harralander",
			ItemID.VOLCANIC_ASH, "Volcanic ash", 1,
			"Stackable Volcanic ash allows 27 potions per inventory when using Harralander potion (unf).",
			2700);

		// Restore potion (Level 22, 62.5 XP)
		addStandardFamily(list, "restore_potion", "Restore potion",
			22, 62.5, RecipeCategory.RESTORE_PRAYER,
			ItemID.RESTORE_POTION4, ItemID.RESTORE_POTION3, ItemID.RESTORE_POTION2, ItemID.RESTORE_POTION1,
			ItemID.HARRALANDER_POTION_UNF, "Harralander potion (unf)",
			ItemID.HARRALANDER, "Harralander",
			ItemID.RED_SPIDERS_EGGS, "Red spiders' eggs", 1,
			"Restores lowered combat stats (Attack, Strength, Defence, Ranged, Magic).");

		// Energy potion (Level 26, 67.5 XP)
		addStandardFamily(list, "energy_potion", "Energy potion",
			26, 67.5, RecipeCategory.SKILLING_ENERGY,
			ItemID.ENERGY_POTION4, ItemID.ENERGY_POTION3, ItemID.ENERGY_POTION2, ItemID.ENERGY_POTION1,
			ItemID.HARRALANDER_POTION_UNF, "Harralander potion (unf)",
			ItemID.HARRALANDER, "Harralander",
			ItemID.CHOCOLATE_DUST, "Chocolate dust", 1,
			"Restores 10% run energy per dose.");

		// Defence potion (Level 30, 75.0 XP)
		addStandardFamily(list, "defence_potion", "Defence potion",
			30, 75.0, RecipeCategory.COMBAT_POTION,
			ItemID.DEFENCE_POTION4, ItemID.DEFENCE_POTION3, ItemID.DEFENCE_POTION2, ItemID.DEFENCE_POTION1,
			ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)",
			ItemID.RANARR_WEED, "Ranarr weed",
			ItemID.WHITE_BERRIES, "White berries", 1,
			"Boosts Defence by 3 + 10% of level.");

		// Agility potion (Level 34, 80.0 XP)
		addStandardFamily(list, "agility_potion", "Agility potion",
			34, 80.0, RecipeCategory.SKILLING_ENERGY,
			ItemID.AGILITY_POTION4, ItemID.AGILITY_POTION3, ItemID.AGILITY_POTION2, ItemID.AGILITY_POTION1,
			ItemID.TOADFLAX_POTION_UNF, "Toadflax potion (unf)",
			ItemID.TOADFLAX, "Toadflax",
			ItemID.TOADS_LEGS, "Toad's legs", 1,
			"Boosts Agility by +3 levels.");

		// Combat potion (Level 36, 84.0 XP)
		addStandardFamily(list, "combat_potion", "Combat potion",
			36, 84.0, RecipeCategory.COMBAT_POTION,
			ItemID.COMBAT_POTION4, ItemID.COMBAT_POTION3, ItemID.COMBAT_POTION2, ItemID.COMBAT_POTION1,
			ItemID.HARRALANDER_POTION_UNF, "Harralander potion (unf)",
			ItemID.HARRALANDER, "Harralander",
			ItemID.GOAT_HORN_DUST, "Goat horn dust", 1,
			"Boosts Attack and Strength simultaneously. Very popular mid-level training recipe.");

		// Prayer potion (Level 38, 87.5 XP)
		addStandardFamily(list, "prayer_potion", "Prayer potion",
			38, 87.5, RecipeCategory.RESTORE_PRAYER,
			ItemID.PRAYER_POTION4, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION1,
			ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)",
			ItemID.RANARR_WEED, "Ranarr weed",
			ItemID.SNAPE_GRASS, "Snape grass", 1,
			"High-volume staple potion restoring 7 + 25% of Prayer level per dose.");

		// Super attack (Level 45, 100.0 XP)
		addStandardFamily(list, "super_attack", "Super attack",
			45, 100.0, RecipeCategory.COMBAT_POTION,
			ItemID.SUPER_ATTACK4, ItemID.SUPER_ATTACK3, ItemID.SUPER_ATTACK2, ItemID.SUPER_ATTACK1,
			ItemID.IRIT_POTION_UNF, "Irit potion (unf)",
			ItemID.IRIT_LEAF, "Irit leaf",
			ItemID.EYE_OF_NEWT, "Eye of newt", 1,
			"Fast, inexpensive XP using dirt-cheap Eye of newt secondary.");

		// Superantipoison (Level 48, 106.3 XP)
		addStandardFamily(list, "superantipoison", "Superantipoison",
			48, 106.3, RecipeCategory.ANTIPOISON_ANTIVENOM,
			ItemID.SUPERANTIPOISON4, ItemID.SUPERANTIPOISON3, ItemID.SUPERANTIPOISON2, ItemID.SUPERANTIPOISON1,
			ItemID.IRIT_POTION_UNF, "Irit potion (unf)",
			ItemID.IRIT_LEAF, "Irit leaf",
			ItemID.UNICORN_HORN_DUST, "Unicorn horn dust", 1,
			"Cures poison and grants 6 minutes of poison immunity.");

		// Fishing potion (Level 50, 112.5 XP)
		addStandardFamily(list, "fishing_potion", "Fishing potion",
			50, 112.5, RecipeCategory.SKILLING_ENERGY,
			ItemID.FISHING_POTION4, ItemID.FISHING_POTION3, ItemID.FISHING_POTION2, ItemID.FISHING_POTION1,
			ItemID.AVANTOE_POTION_UNF, "Avantoe potion (unf)",
			ItemID.AVANTOE, "Avantoe",
			ItemID.SNAPE_GRASS, "Snape grass", 1,
			"Boosts Fishing by +3 levels.");

		// Super energy (Level 52, 117.5 XP)
		addStandardFamily(list, "super_energy", "Super energy",
			52, 117.5, RecipeCategory.SKILLING_ENERGY,
			ItemID.SUPER_ENERGY4, ItemID.SUPER_ENERGY3, ItemID.SUPER_ENERGY2, ItemID.SUPER_ENERGY1,
			ItemID.AVANTOE_POTION_UNF, "Avantoe potion (unf)",
			ItemID.AVANTOE, "Avantoe",
			ItemID.MORT_MYRE_FUNGUS, "Mort myre fungus", 1,
			"High demand as the base potion for Stamina potions.");

		// Hunter potion (Level 53, 120.0 XP)
		addStandardFamily(list, "hunter_potion", "Hunter potion",
			53, 120.0, RecipeCategory.SKILLING_ENERGY,
			ItemID.HUNTER_POTION4, ItemID.HUNTER_POTION3, ItemID.HUNTER_POTION2, ItemID.HUNTER_POTION1,
			ItemID.AVANTOE_POTION_UNF, "Avantoe potion (unf)",
			ItemID.AVANTOE, "Avantoe",
			ItemID.KEBBIT_TEETH_DUST, "Kebbit teeth dust", 1,
			"Boosts Hunter by +3 levels.");

		// Goading potion (Level 54, 132.0 XP)
		addStandardFamily(list, "goading_potion", "Goading potion",
			54, 132.0, RecipeCategory.SPECIAL_QUEST,
			ItemID.GOADING_POTION4, ItemID.GOADING_POTION3, ItemID.GOADING_POTION2, ItemID.GOADING_POTION1,
			ItemID.HARRALANDER_POTION_UNF, "Harralander potion (unf)",
			ItemID.HARRALANDER, "Harralander",
			ItemID.ALDARIUM, "Aldarium", 1,
			"Aggros nearby monsters for 6 minutes per dose; uses Aldarium from Mastering Mixology.");

		// Super strength (Level 55, 125.0 XP)
		addStandardFamily(list, "super_strength", "Super strength",
			55, 125.0, RecipeCategory.COMBAT_POTION,
			ItemID.SUPER_STRENGTH4, ItemID.SUPER_STRENGTH3, ItemID.SUPER_STRENGTH2, ItemID.SUPER_STRENGTH1,
			ItemID.KWUARM_POTION_UNF, "Kwuarm potion (unf)",
			ItemID.KWUARM, "Kwuarm",
			ItemID.LIMPWURT_ROOT, "Limpwurt root", 1,
			"Core combat potion and component of Super combat potions.");

		// Prayer regeneration potion (Level 58, 132.0 XP)
		addStandardFamily(list, "prayer_regeneration_potion", "Prayer regeneration potion",
			58, 132.0, RecipeCategory.RESTORE_PRAYER,
			ItemID.PRAYER_REGENERATION_POTION4, ItemID.PRAYER_REGENERATION_POTION3,
			ItemID.PRAYER_REGENERATION_POTION2, ItemID.PRAYER_REGENERATION_POTION1,
			ItemID.HUASCA_POTION_UNF, "Huasca potion (unf)",
			ItemID.HUASCA, "Huasca",
			ItemID.ALDARIUM, "Aldarium", 1,
			"Regenerates 66 Prayer points over 8 minutes per dose.");

		// Weapon poison (Level 60, 137.5 XP)
		list.add(PotionRecipe.builder()
			.recipeId("weapon_poison_unf")
			.familyName("Weapon poison")
			.potionName("Weapon poison")
			.potionItemId(ItemID.WEAPON_POISON)
			.doses(1)
			.requiredLevel(60)
			.xpGained(137.5)
			.unfinishedPotionId(ItemID.KWUARM_POTION_UNF)
			.unfinishedPotionName("Kwuarm potion (unf)")
			.secondarySummary("Dragon scale dust")
			.category(RecipeCategory.WEAPON_POISON)
			.potionsPerHourEstimate(2500)
			.preparationNotes("Standard weapon poison (applies 4 poison damage to daggers, spears, and hastae).")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.KWUARM_POTION_UNF, "Kwuarm potion (unf)", 1, true, "Unfinished Base"),
				new PotionIngredient(ItemID.DRAGON_SCALE_DUST, "Dragon scale dust", 1, true, "Secondary Ingredient")
			))
			.build());
		list.add(PotionRecipe.builder()
			.recipeId("weapon_poison_herb")
			.familyName("Weapon poison")
			.potionName("Weapon poison (From Herb)")
			.potionItemId(ItemID.WEAPON_POISON)
			.doses(1)
			.requiredLevel(60)
			.xpGained(137.5)
			.unfinishedPotionId(ItemID.KWUARM_POTION_UNF)
			.unfinishedPotionName("Kwuarm potion (unf)")
			.secondarySummary("Kwuarm + Vial + Dragon scale dust")
			.category(RecipeCategory.WEAPON_POISON)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(1200)
			.preparationNotes("Two-step preparation from clean Kwuarm, Vial of water, and Dragon scale dust.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.KWUARM, "Kwuarm", 1, true, "Clean Herb"),
				new PotionIngredient(ItemID.VIAL_OF_WATER, "Vial of water", 1, true, "Liquid Base"),
				new PotionIngredient(ItemID.DRAGON_SCALE_DUST, "Dragon scale dust", 1, true, "Secondary Ingredient")
			))
			.build());

		// Super restore (Level 63, 142.5 XP)
		addStandardFamily(list, "super_restore", "Super restore",
			63, 142.5, RecipeCategory.RESTORE_PRAYER,
			ItemID.SUPER_RESTORE4, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE1,
			ItemID.SNAPDRAGON_POTION_UNF, "Snapdragon potion (unf)",
			ItemID.SNAPDRAGON, "Snapdragon",
			ItemID.RED_SPIDERS_EGGS, "Red spiders' eggs", 1,
			"High-liquidity PvM staple restoring all drained stats and Prayer.");

		// Sanfew serum (Level 65, 160.0 XP)
		list.add(PotionRecipe.builder()
			.recipeId("sanfew_serum_4")
			.familyName("Sanfew serum")
			.potionName("Sanfew serum(4)")
			.potionItemId(ItemID.SANFEW_SERUM4)
			.doses(4)
			.requiredLevel(65)
			.xpGained(160.0)
			.unfinishedPotionId(ItemID.SUPER_RESTORE4)
			.unfinishedPotionName("Super restore(4)")
			.secondarySummary("Unicorn horn dust + Snake weed + Nail beast nails")
			.category(RecipeCategory.RESTORE_PRAYER)
			.potionsPerHourEstimate(950)
			.preparationNotes("Three-step mix adding Unicorn horn dust, Snake weed, and Nail beast nails to Super restore(4).")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.SUPER_RESTORE4, "Super restore(4)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.UNICORN_HORN_DUST, "Unicorn horn dust", 1, true, "Step 1 Secondary"),
				new PotionIngredient(ItemID.SNAKE_WEED, "Snake weed", 1, true, "Step 2 Secondary"),
				new PotionIngredient(ItemID.NAIL_BEAST_NAILS, "Nail beast nails", 1, true, "Step 3 Secondary")
			))
			.build());
		list.add(PotionRecipe.builder()
			.recipeId("sanfew_serum_3")
			.familyName("Sanfew serum")
			.potionName("Sanfew serum(3)")
			.potionItemId(ItemID.SANFEW_SERUM3)
			.doses(3)
			.requiredLevel(65)
			.xpGained(160.0)
			.unfinishedPotionId(ItemID.SUPER_RESTORE3)
			.unfinishedPotionName("Super restore(3)")
			.secondarySummary("Unicorn horn dust + Snake weed + Nail beast nails")
			.category(RecipeCategory.RESTORE_PRAYER)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(950)
			.preparationNotes("3-dose variant of Sanfew serum.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.SUPER_RESTORE3, "Super restore(3)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.UNICORN_HORN_DUST, "Unicorn horn dust", 1, true, "Step 1 Secondary"),
				new PotionIngredient(ItemID.SNAKE_WEED, "Snake weed", 1, true, "Step 2 Secondary"),
				new PotionIngredient(ItemID.NAIL_BEAST_NAILS, "Nail beast nails", 1, true, "Step 3 Secondary")
			))
			.build());

		// Super defence (Level 66, 150.0 XP)
		addStandardFamily(list, "super_defence", "Super defence",
			66, 150.0, RecipeCategory.COMBAT_POTION,
			ItemID.SUPER_DEFENCE4, ItemID.SUPER_DEFENCE3, ItemID.SUPER_DEFENCE2, ItemID.SUPER_DEFENCE1,
			ItemID.CADANTINE_POTION_UNF, "Cadantine potion (unf)",
			ItemID.CADANTINE, "Cadantine",
			ItemID.WHITE_BERRIES, "White berries", 1,
			"Boosts Defence by 5 + 15% of level; required component for Super combat potions.");

		// Antidote+ (Level 68, 155.0 XP — Coconut milk + Toadflax + Yew roots)
		addCoconutFamily(list, "antidote_plus", "Antidote+",
			68, 155.0, RecipeCategory.ANTIPOISON_ANTIVENOM,
			ItemID.ANTIDOTE4, ItemID.ANTIDOTE3, ItemID.ANTIDOTE2, ItemID.ANTIDOTE1,
			ItemID.ANTIDOTE_UNF, "Antidote+ (unf)",
			ItemID.TOADFLAX, "Toadflax",
			ItemID.YEW_ROOTS, "Yew roots",
			"Made with Coconut milk, Toadflax, and Yew roots. Grants 9 minutes of poison immunity.");

		// Antifire potion (Level 69, 157.5 XP)
		addStandardFamily(list, "antifire_potion", "Antifire potion",
			69, 157.5, RecipeCategory.ANTIFIRE,
			ItemID.ANTIFIRE_POTION4, ItemID.ANTIFIRE_POTION3, ItemID.ANTIFIRE_POTION2, ItemID.ANTIFIRE_POTION1,
			ItemID.LANTADYME_POTION_UNF, "Lantadyme potion (unf)",
			ItemID.LANTADYME, "Lantadyme",
			ItemID.DRAGON_SCALE_DUST, "Dragon scale dust", 1,
			"Base potion for Extended antifire and Super antifire potions.");

		// Ranging potion (Level 72, 162.5 XP)
		addStandardFamily(list, "ranging_potion", "Ranging potion",
			72, 162.5, RecipeCategory.COMBAT_POTION,
			ItemID.RANGING_POTION4, ItemID.RANGING_POTION3, ItemID.RANGING_POTION2, ItemID.RANGING_POTION1,
			ItemID.DWARF_WEED_POTION_UNF, "Dwarf weed potion (unf)",
			ItemID.DWARF_WEED, "Dwarf weed",
			ItemID.WINE_OF_ZAMORAK, "Wine of zamorak", 1,
			"Boosts Ranged by 4 + 10% of level; heavily traded PvM staple.");

		// Weapon poison(+) (Level 73, 165.0 XP)
		list.add(PotionRecipe.builder()
			.recipeId("weapon_poison_plus_unf")
			.familyName("Weapon poison(+)")
			.potionName("Weapon poison(+)")
			.potionItemId(ItemID.WEAPON_POISON_5937)
			.doses(1)
			.requiredLevel(73)
			.xpGained(165.0)
			.unfinishedPotionId(ItemID.WEAPON_POISON_UNF)
			.unfinishedPotionName("Weapon poison+ (unf)")
			.secondarySummary("Red spiders' eggs")
			.category(RecipeCategory.WEAPON_POISON)
			.potionsPerHourEstimate(2500)
			.preparationNotes("Made by adding Red spiders' eggs to Weapon poison+ (unf) [Coconut milk + Cactus spine].")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.WEAPON_POISON_UNF, "Weapon poison+ (unf)", 1, true, "Unfinished Base"),
				new PotionIngredient(ItemID.RED_SPIDERS_EGGS, "Red spiders' eggs", 1, true, "Secondary Ingredient")
			))
			.build());
		list.add(PotionRecipe.builder()
			.recipeId("weapon_poison_plus_scratch")
			.familyName("Weapon poison(+)")
			.potionName("Weapon poison(+) (From Coconut Milk)")
			.potionItemId(ItemID.WEAPON_POISON_5937)
			.doses(1)
			.requiredLevel(73)
			.xpGained(165.0)
			.unfinishedPotionId(ItemID.WEAPON_POISON_UNF)
			.unfinishedPotionName("Weapon poison+ (unf)")
			.secondarySummary("Coconut milk + Cactus spine + Red spiders' eggs")
			.category(RecipeCategory.WEAPON_POISON)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(1200)
			.preparationNotes("Two-step creation from Coconut milk, Cactus spine, and Red spiders' eggs.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.COCONUT_MILK, "Coconut milk", 1, true, "Liquid Base"),
				new PotionIngredient(ItemID.CACTUS_SPINE, "Cactus spine", 1, true, "Primary Ingredient"),
				new PotionIngredient(ItemID.RED_SPIDERS_EGGS, "Red spiders' eggs", 1, true, "Secondary Ingredient")
			))
			.build());

		// Magic potion (Level 76, 172.5 XP)
		addStandardFamily(list, "magic_potion", "Magic potion",
			76, 172.5, RecipeCategory.COMBAT_POTION,
			ItemID.MAGIC_POTION4, ItemID.MAGIC_POTION3, ItemID.MAGIC_POTION2, ItemID.MAGIC_POTION1,
			ItemID.LANTADYME_POTION_UNF, "Lantadyme potion (unf)",
			ItemID.LANTADYME, "Lantadyme",
			ItemID.POTATO_CACTUS, "Potato cactus", 1,
			"Boosts Magic by +4 levels. Often very cheap GP/XP due to inexpensive Lantadyme and Potato cactus.");

		// Stamina potion (Level 77, 25.5 XP per dose -> 102.0 XP for 4-dose)
		addStackableUpgradeFamily(list, "stamina_potion", "Stamina potion",
			77, 25.5, RecipeCategory.SKILLING_ENERGY,
			ItemID.STAMINA_POTION4, ItemID.STAMINA_POTION3, ItemID.STAMINA_POTION2, ItemID.STAMINA_POTION1,
			ItemID.SUPER_ENERGY4, ItemID.SUPER_ENERGY3, ItemID.SUPER_ENERGY2, ItemID.SUPER_ENERGY1,
			"Super energy", ItemID.AMYLASE_CRYSTAL, "Amylase crystal", 1,
			"Uses 1 Amylase crystal per dose (4 crystals for 4-dose = 102 XP). 27 potions per inventory!");

		// Zamorak brew (Level 78, 175.0 XP)
		addStandardFamily(list, "zamorak_brew", "Zamorak brew",
			78, 175.0, RecipeCategory.BREW_REMEDY,
			ItemID.ZAMORAK_BREW4, ItemID.ZAMORAK_BREW3, ItemID.ZAMORAK_BREW2, ItemID.ZAMORAK_BREW1,
			ItemID.TORSTOL_POTION_UNF, "Torstol potion (unf)",
			ItemID.TORSTOL, "Torstol",
			ItemID.JANGERBERRIES, "Jangerberries", 1,
			"Boosts Attack and Strength while lowering Defence and Hitpoints.");

		// Antidote++ (Level 79, 177.5 XP — Coconut milk + Irit leaf + Magic roots)
		addCoconutFamily(list, "antidote_plusplus", "Antidote++",
			79, 177.5, RecipeCategory.ANTIPOISON_ANTIVENOM,
			ItemID.ANTIDOTE4_5952, ItemID.ANTIDOTE3_5954, ItemID.ANTIDOTE2_5956, ItemID.ANTIDOTE1_5958,
			ItemID.ANTIDOTE_UNF_5951, "Antidote++ (unf)",
			ItemID.IRIT_LEAF, "Irit leaf",
			ItemID.MAGIC_ROOTS, "Magic roots",
			"Grants 12 minutes of poison immunity and serves as the base for Anti-venom potions.");

		// Bastion potion (Level 80, 155.0 XP — Cadantine blood potion (unf) + Wine of Zamorak)
		addBloodPotionFamily(list, "bastion_potion", "Bastion potion",
			80, 155.0, RecipeCategory.COMBAT_POTION,
			ItemID.BASTION_POTION4, ItemID.BASTION_POTION3, ItemID.BASTION_POTION2, ItemID.BASTION_POTION1,
			ItemID.WINE_OF_ZAMORAK, "Wine of zamorak",
			"Combines Ranging potion and Super defence effects using a Vial of blood + Cadantine base.");

		// Battlemage potion (Level 80, 155.0 XP — Cadantine blood potion (unf) + Potato cactus)
		addBloodPotionFamily(list, "battlemage_potion", "Battlemage potion",
			80, 155.0, RecipeCategory.COMBAT_POTION,
			ItemID.BATTLEMAGE_POTION4, ItemID.BATTLEMAGE_POTION3, ItemID.BATTLEMAGE_POTION2, ItemID.BATTLEMAGE_POTION1,
			ItemID.POTATO_CACTUS, "Potato cactus",
			"Combines Magic potion and Super defence effects using a Vial of blood + Cadantine base.");

		// Saradomin brew (Level 81, 180.0 XP)
		addStandardFamily(list, "saradomin_brew", "Saradomin brew",
			81, 180.0, RecipeCategory.BREW_REMEDY,
			ItemID.SARADOMIN_BREW4, ItemID.SARADOMIN_BREW3, ItemID.SARADOMIN_BREW2, ItemID.SARADOMIN_BREW1,
			ItemID.TOADFLAX_POTION_UNF, "Toadflax potion (unf)",
			ItemID.TOADFLAX, "Toadflax",
			ItemID.CRUSHED_NEST, "Crushed nest", 1,
			"Premier high-level Herblore training potion with massive Grand Exchange liquidity.");

		// Surge potion (Level 81, 185.0 XP)
		addStandardFamily(list, "surge_potion", "Surge potion",
			81, 185.0, RecipeCategory.COMBAT_POTION,
			ItemID.SURGE_POTION4, ItemID.SURGE_POTION3, ItemID.SURGE_POTION2, ItemID.SURGE_POTION1,
			ItemID.TORSTOL_POTION_UNF, "Torstol potion (unf)",
			ItemID.TORSTOL, "Torstol",
			ItemID.DEMONIC_TALLOW, "Demonic tallow", 1,
			"Restores 25% special attack energy per dose (5-minute cooldown).");

		// Weapon poison(++) (Level 82, 190.0 XP)
		list.add(PotionRecipe.builder()
			.recipeId("weapon_poison_plusplus_unf")
			.familyName("Weapon poison(++)")
			.potionName("Weapon poison(++)")
			.potionItemId(ItemID.WEAPON_POISON_5940)
			.doses(1)
			.requiredLevel(82)
			.xpGained(190.0)
			.unfinishedPotionId(ItemID.WEAPON_POISON_UNF_5939)
			.unfinishedPotionName("Weapon poison++ (unf)")
			.secondarySummary("Poison ivy berries")
			.category(RecipeCategory.WEAPON_POISON)
			.potionsPerHourEstimate(2500)
			.preparationNotes("Made by adding Poison ivy berries to Weapon poison++ (unf) [Coconut milk + Cave nightshade].")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.WEAPON_POISON_UNF_5939, "Weapon poison++ (unf)", 1, true, "Unfinished Base"),
				new PotionIngredient(ItemID.POISON_IVY_BERRIES, "Poison ivy berries", 1, true, "Secondary Ingredient")
			))
			.build());
		list.add(PotionRecipe.builder()
			.recipeId("weapon_poison_plusplus_scratch")
			.familyName("Weapon poison(++)")
			.potionName("Weapon poison(++) (From Coconut Milk)")
			.potionItemId(ItemID.WEAPON_POISON_5940)
			.doses(1)
			.requiredLevel(82)
			.xpGained(190.0)
			.unfinishedPotionId(ItemID.WEAPON_POISON_UNF_5939)
			.unfinishedPotionName("Weapon poison++ (unf)")
			.secondarySummary("Coconut milk + Cave nightshade + Poison ivy berries")
			.category(RecipeCategory.WEAPON_POISON)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(1200)
			.preparationNotes("Two-step creation from Coconut milk, Cave nightshade, and Poison ivy berries.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.COCONUT_MILK, "Coconut milk", 1, true, "Liquid Base"),
				new PotionIngredient(ItemID.CAVE_NIGHTSHADE, "Cave nightshade", 1, true, "Primary Ingredient"),
				new PotionIngredient(ItemID.POISON_IVY_BERRIES, "Poison ivy berries", 1, true, "Secondary Ingredient")
			))
			.build());

		// Extended antifire (Level 84, 27.5 XP per dose -> 110.0 XP for 4-dose)
		addStackableUpgradeFamily(list, "extended_antifire", "Extended antifire",
			84, 27.5, RecipeCategory.ANTIFIRE,
			ItemID.EXTENDED_ANTIFIRE4, ItemID.EXTENDED_ANTIFIRE3, ItemID.EXTENDED_ANTIFIRE2, ItemID.EXTENDED_ANTIFIRE1,
			ItemID.ANTIFIRE_POTION4, ItemID.ANTIFIRE_POTION3, ItemID.ANTIFIRE_POTION2, ItemID.ANTIFIRE_POTION1,
			"Antifire potion", ItemID.LAVA_SCALE_SHARD, "Lava scale shard", 1,
			"Uses 1 Lava scale shard per dose (4 shards for 4-dose = 110 XP). Fast AFK 27-potion inventories.");

		// Ancient brew (Level 85, 190.0 XP — Dwarf weed potion (unf) + Nihil dust)
		addStandardFamily(list, "ancient_brew", "Ancient brew",
			85, 190.0, RecipeCategory.BREW_REMEDY,
			ItemID.ANCIENT_BREW4, ItemID.ANCIENT_BREW3, ItemID.ANCIENT_BREW2, ItemID.ANCIENT_BREW1,
			ItemID.DWARF_WEED_POTION_UNF, "Dwarf weed potion (unf)",
			ItemID.DWARF_WEED, "Dwarf weed",
			ItemID.NIHIL_DUST, "Nihil dust", 1,
			"Stackable Nihil dust allows 27 potions per inventory when using Dwarf weed potion (unf)!",
			2700);

		// Anti-venom (Level 87, 30.0 XP per dose -> 120.0 XP for 4-dose, 5 Zulrah's scales per dose)
		addStackableUpgradeFamily(list, "anti_venom", "Anti-venom",
			87, 30.0, RecipeCategory.ANTIPOISON_ANTIVENOM,
			ItemID.ANTIVENOM4, ItemID.ANTIVENOM3, ItemID.ANTIVENOM2, ItemID.ANTIVENOM1,
			ItemID.ANTIDOTE4_5952, ItemID.ANTIDOTE3_5954, ItemID.ANTIDOTE2_5956, ItemID.ANTIDOTE1_5958,
			"Antidote++", ItemID.ZULRAHS_SCALES, "Zulrah's scales", 5,
			"Requires 5 Zulrah's scales per dose (20 scales for 4-dose = 120 XP). Very fast 27-potion inventories.");

		// Menaphite remedy (Level 88, 200.0 XP)
		addStandardFamily(list, "menaphite_remedy", "Menaphite remedy",
			88, 200.0, RecipeCategory.BREW_REMEDY,
			ItemID.MENAPHITE_REMEDY4, ItemID.MENAPHITE_REMEDY3, ItemID.MENAPHITE_REMEDY2, ItemID.MENAPHITE_REMEDY1,
			ItemID.DWARF_WEED_POTION_UNF, "Dwarf weed potion (unf)",
			ItemID.DWARF_WEED, "Dwarf weed",
			ItemID.LILY_OF_THE_SANDS, "Lily of the sands", 1,
			"Grants 200 Herblore XP per potion — the highest single-mix herb XP in OSRS.");

		// Super combat potion (Level 90, 150.0 XP)
		list.add(PotionRecipe.builder()
			.recipeId("super_combat_potion_4")
			.familyName("Super combat potion")
			.potionName("Super combat potion(4)")
			.potionItemId(ItemID.SUPER_COMBAT_POTION4)
			.doses(4)
			.requiredLevel(90)
			.xpGained(150.0)
			.unfinishedPotionId(ItemID.TORSTOL)
			.unfinishedPotionName("Torstol")
			.secondarySummary("Super attack(4) + Super strength(4) + Super defence(4)")
			.category(RecipeCategory.COMBAT_POTION)
			.potionsPerHourEstimate(2000)
			.preparationNotes("Combines clean Torstol with 4-dose Super attack, Super strength, and Super defence potions (7 per inventory).")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.TORSTOL, "Torstol", 1, true, "Clean Herb"),
				new PotionIngredient(ItemID.SUPER_ATTACK4, "Super attack(4)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.SUPER_STRENGTH4, "Super strength(4)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.SUPER_DEFENCE4, "Super defence(4)", 1, true, "Base Potion")
			))
			.build());
		list.add(PotionRecipe.builder()
			.recipeId("super_combat_potion_4_unf")
			.familyName("Super combat potion")
			.potionName("Super combat potion(4) (From Torstol Unf)")
			.potionItemId(ItemID.SUPER_COMBAT_POTION4)
			.doses(4)
			.requiredLevel(90)
			.xpGained(150.0)
			.unfinishedPotionId(ItemID.TORSTOL_POTION_UNF)
			.unfinishedPotionName("Torstol potion (unf)")
			.secondarySummary("Torstol potion (unf) + Super attack/strength/defence(4)")
			.category(RecipeCategory.COMBAT_POTION)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(2000)
			.preparationNotes("Alternative recipe using Torstol potion (unf) instead of clean Torstol.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.TORSTOL_POTION_UNF, "Torstol potion (unf)", 1, true, "Unfinished Base"),
				new PotionIngredient(ItemID.SUPER_ATTACK4, "Super attack(4)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.SUPER_STRENGTH4, "Super strength(4)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.SUPER_DEFENCE4, "Super defence(4)", 1, true, "Base Potion")
			))
			.build());

		// Forgotten brew (Level 91, 36.25 XP per dose -> 145.0 XP for 4-dose, 20 Ancient essence per dose)
		addStackableUpgradeFamily(list, "forgotten_brew", "Forgotten brew",
			91, 36.25, RecipeCategory.BREW_REMEDY,
			ItemID.FORGOTTEN_BREW4, ItemID.FORGOTTEN_BREW3, ItemID.FORGOTTEN_BREW2, ItemID.FORGOTTEN_BREW1,
			ItemID.ANCIENT_BREW4, ItemID.ANCIENT_BREW3, ItemID.ANCIENT_BREW2, ItemID.ANCIENT_BREW1,
			"Ancient brew", ItemID.ANCIENT_ESSENCE, "Ancient essence", 20,
			"Uses 20 Ancient essence per dose (80 essence for 4-dose = 145 XP). Stackable secondary allows 27 per inventory.");

		// Super antifire potion (Level 92, 130.0 XP)
		list.add(PotionRecipe.builder()
			.recipeId("super_antifire_potion_4")
			.familyName("Super antifire potion")
			.potionName("Super antifire potion(4)")
			.potionItemId(ItemID.SUPER_ANTIFIRE_POTION4)
			.doses(4)
			.requiredLevel(92)
			.xpGained(130.0)
			.unfinishedPotionId(ItemID.ANTIFIRE_POTION4)
			.unfinishedPotionName("Antifire potion(4)")
			.secondarySummary("Crushed superior dragon bones")
			.category(RecipeCategory.ANTIFIRE)
			.potionsPerHourEstimate(2600)
			.preparationNotes("Requires Dragon Slayer II completion (taught by Myths' Guild apothecary). Stackable Crushed superior dragon bones.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.ANTIFIRE_POTION4, "Antifire potion(4)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.CRUSHED_SUPERIOR_DRAGON_BONES, "Crushed superior dragon bones", 1, true, "Secondary Ingredient")
			))
			.build());
		list.add(PotionRecipe.builder()
			.recipeId("super_antifire_potion_3")
			.familyName("Super antifire potion")
			.potionName("Super antifire potion(3)")
			.potionItemId(ItemID.SUPER_ANTIFIRE_POTION3)
			.doses(3)
			.requiredLevel(92)
			.xpGained(130.0)
			.unfinishedPotionId(ItemID.ANTIFIRE_POTION3)
			.unfinishedPotionName("Antifire potion(3)")
			.secondarySummary("Crushed superior dragon bones")
			.category(RecipeCategory.ANTIFIRE)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(2600)
			.preparationNotes("3-dose variant of Super antifire potion.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.ANTIFIRE_POTION3, "Antifire potion(3)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.CRUSHED_SUPERIOR_DRAGON_BONES, "Crushed superior dragon bones", 1, true, "Secondary Ingredient")
			))
			.build());

		// Anti-venom+ (Level 94, 125.0 XP)
		list.add(PotionRecipe.builder()
			.recipeId("anti_venom_plus_4")
			.familyName("Anti-venom+")
			.potionName("Anti-venom+(4)")
			.potionItemId(ItemID.ANTIVENOM4_12913)
			.doses(4)
			.requiredLevel(94)
			.xpGained(125.0)
			.unfinishedPotionId(ItemID.ANTIVENOM4)
			.unfinishedPotionName("Anti-venom(4)")
			.secondarySummary("Torstol")
			.category(RecipeCategory.ANTIPOISON_ANTIVENOM)
			.potionsPerHourEstimate(2500)
			.preparationNotes("Combines 4-dose Anti-venom(4) with clean Torstol.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.ANTIVENOM4, "Anti-venom(4)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.TORSTOL, "Torstol", 1, true, "Clean Herb")
			))
			.build());
		list.add(PotionRecipe.builder()
			.recipeId("anti_venom_plus_3")
			.familyName("Anti-venom+")
			.potionName("Anti-venom+(3)")
			.potionItemId(ItemID.ANTIVENOM3_12915)
			.doses(3)
			.requiredLevel(94)
			.xpGained(125.0)
			.unfinishedPotionId(ItemID.ANTIVENOM3)
			.unfinishedPotionName("Anti-venom(3)")
			.secondarySummary("Torstol")
			.category(RecipeCategory.ANTIPOISON_ANTIVENOM)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(2500)
			.preparationNotes("3-dose variant of Anti-venom+.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.ANTIVENOM3, "Anti-venom(3)", 1, true, "Base Potion"),
				new PotionIngredient(ItemID.TORSTOL, "Torstol", 1, true, "Clean Herb")
			))
			.build());

		// Extended anti-venom+ (Level 94, 20.0 XP per dose -> 80.0 XP for 4-dose)
		addStackableUpgradeFamily(list, "extended_anti_venom_plus", "Extended anti-venom+",
			94, 20.0, RecipeCategory.ANTIPOISON_ANTIVENOM,
			ItemID.EXTENDED_ANTIVENOM4, ItemID.EXTENDED_ANTIVENOM3, ItemID.EXTENDED_ANTIVENOM2, ItemID.EXTENDED_ANTIVENOM1,
			ItemID.ANTIVENOM4_12913, ItemID.ANTIVENOM3_12915, ItemID.ANTIVENOM2_12917, ItemID.ANTIVENOM1_12919,
			"Anti-venom+", ItemID.ARAXYTE_VENOM_SAC, "Araxyte venom sac", 1,
			"Uses 1 Araxyte venom sac per dose (4 sacs for 4-dose = 80 XP). Doubles venom immunity duration.");

		// Extended super antifire (Level 98, 40.0 XP per dose -> 160.0 XP for 4-dose)
		addStackableUpgradeFamily(list, "extended_super_antifire", "Extended super antifire",
			98, 40.0, RecipeCategory.ANTIFIRE,
			ItemID.EXTENDED_SUPER_ANTIFIRE4, ItemID.EXTENDED_SUPER_ANTIFIRE3,
			ItemID.EXTENDED_SUPER_ANTIFIRE2, ItemID.EXTENDED_SUPER_ANTIFIRE1,
			ItemID.SUPER_ANTIFIRE_POTION4, ItemID.SUPER_ANTIFIRE_POTION3,
			ItemID.SUPER_ANTIFIRE_POTION2, ItemID.SUPER_ANTIFIRE_POTION1,
			"Super antifire potion", ItemID.LAVA_SCALE_SHARD, "Lava scale shard", 1,
			"Uses 1 Lava scale shard per dose (4 shards for 4-dose = 160 XP). Highest-level standard potion in OSRS.");

		// =========================================================================
		// 2. DIVINE POTIONS (Untradeable Crystal dust input + Tradeable output)
		// =========================================================================
		addDivineFamily(list, "divine_super_attack", "Divine super attack potion", 70,
			ItemID.DIVINE_SUPER_ATTACK_POTION4, ItemID.DIVINE_SUPER_ATTACK_POTION3,
			ItemID.DIVINE_SUPER_ATTACK_POTION2, ItemID.DIVINE_SUPER_ATTACK_POTION1,
			ItemID.SUPER_ATTACK4, ItemID.SUPER_ATTACK3, ItemID.SUPER_ATTACK2, ItemID.SUPER_ATTACK1,
			"Super attack");

		addDivineFamily(list, "divine_super_strength", "Divine super strength potion", 70,
			ItemID.DIVINE_SUPER_STRENGTH_POTION4, ItemID.DIVINE_SUPER_STRENGTH_POTION3,
			ItemID.DIVINE_SUPER_STRENGTH_POTION2, ItemID.DIVINE_SUPER_STRENGTH_POTION1,
			ItemID.SUPER_STRENGTH4, ItemID.SUPER_STRENGTH3, ItemID.SUPER_STRENGTH2, ItemID.SUPER_STRENGTH1,
			"Super strength");

		addDivineFamily(list, "divine_super_defence", "Divine super defence potion", 70,
			ItemID.DIVINE_SUPER_DEFENCE_POTION4, ItemID.DIVINE_SUPER_DEFENCE_POTION3,
			ItemID.DIVINE_SUPER_DEFENCE_POTION2, ItemID.DIVINE_SUPER_DEFENCE_POTION1,
			ItemID.SUPER_DEFENCE4, ItemID.SUPER_DEFENCE3, ItemID.SUPER_DEFENCE2, ItemID.SUPER_DEFENCE1,
			"Super defence");

		addDivineFamily(list, "divine_ranging", "Divine ranging potion", 74,
			ItemID.DIVINE_RANGING_POTION4, ItemID.DIVINE_RANGING_POTION3,
			ItemID.DIVINE_RANGING_POTION2, ItemID.DIVINE_RANGING_POTION1,
			ItemID.RANGING_POTION4, ItemID.RANGING_POTION3, ItemID.RANGING_POTION2, ItemID.RANGING_POTION1,
			"Ranging potion");

		addDivineFamily(list, "divine_magic", "Divine magic potion", 78,
			ItemID.DIVINE_MAGIC_POTION4, ItemID.DIVINE_MAGIC_POTION3,
			ItemID.DIVINE_MAGIC_POTION2, ItemID.DIVINE_MAGIC_POTION1,
			ItemID.MAGIC_POTION4, ItemID.MAGIC_POTION3, ItemID.MAGIC_POTION2, ItemID.MAGIC_POTION1,
			"Magic potion");

		addDivineFamily(list, "divine_bastion", "Divine bastion potion", 86,
			ItemID.DIVINE_BASTION_POTION4, ItemID.DIVINE_BASTION_POTION3,
			ItemID.DIVINE_BASTION_POTION2, ItemID.DIVINE_BASTION_POTION1,
			ItemID.BASTION_POTION4, ItemID.BASTION_POTION3, ItemID.BASTION_POTION2, ItemID.BASTION_POTION1,
			"Bastion potion");

		addDivineFamily(list, "divine_battlemage", "Divine battlemage potion", 86,
			ItemID.DIVINE_BATTLEMAGE_POTION4, ItemID.DIVINE_BATTLEMAGE_POTION3,
			ItemID.DIVINE_BATTLEMAGE_POTION2, ItemID.DIVINE_BATTLEMAGE_POTION1,
			ItemID.BATTLEMAGE_POTION4, ItemID.BATTLEMAGE_POTION3, ItemID.BATTLEMAGE_POTION2, ItemID.BATTLEMAGE_POTION1,
			"Battlemage potion");

		addDivineFamily(list, "divine_super_combat", "Divine super combat potion", 97,
			ItemID.DIVINE_SUPER_COMBAT_POTION4, ItemID.DIVINE_SUPER_COMBAT_POTION3,
			ItemID.DIVINE_SUPER_COMBAT_POTION2, ItemID.DIVINE_SUPER_COMBAT_POTION1,
			ItemID.SUPER_COMBAT_POTION4, ItemID.SUPER_COMBAT_POTION3,
			ItemID.SUPER_COMBAT_POTION2, ItemID.SUPER_COMBAT_POTION1,
			"Super combat potion");

		// =========================================================================
		// 3. MOONLIGHT POTION (Moons of Peril dungeon minigame — untradeable)
		// =========================================================================
		list.add(PotionRecipe.builder()
			.recipeId("moonlight_potion_4")
			.familyName("Moonlight potion")
			.potionName("Moonlight potion(4)")
			.potionItemId(ItemID.MOONLIGHT_POTION4)
			.doses(4)
			.requiredLevel(38)
			.xpGained(25.0)
			.unfinishedPotionId(ItemID.VIAL_OF_WATER)
			.unfinishedPotionName("Vial of water")
			.secondarySummary("Moonlight grub paste")
			.category(RecipeCategory.SPECIAL_QUEST)
			.tradeableOutput(false)
			.potionsPerHourEstimate(1800)
			.preparationNotes("Prepared inside Neypotzli (Moons of Peril). Both Moonlight grub paste and Moonlight potion are untradeable minigame supplies.")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.VIAL_OF_WATER, "Vial of water", 1, true, "Liquid Base"),
				new PotionIngredient(ItemID.MOONLIGHT_GRUB_PASTE, "Moonlight grub paste", 1, false, "Minigame Secondary")
			))
			.build());

		// =========================================================================
		// 4. UNFINISHED POTIONS (0 XP — Explicitly categorised as UNFINISHED_POTION)
		// =========================================================================
		addUnfinishedRecipe(list, "guam_unf", "Guam potion (unf)", ItemID.GUAM_POTION_UNF, 3, ItemID.GUAM_LEAF, "Guam leaf", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "marrentill_unf", "Marrentill potion (unf)", ItemID.MARRENTILL_POTION_UNF, 5, ItemID.MARRENTILL, "Marrentill", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "tarromin_unf", "Tarromin potion (unf)", ItemID.TARROMIN_POTION_UNF, 12, ItemID.TARROMIN, "Tarromin", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "harralander_unf", "Harralander potion (unf)", ItemID.HARRALANDER_POTION_UNF, 21, ItemID.HARRALANDER, "Harralander", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "ranarr_unf", "Ranarr potion (unf)", ItemID.RANARR_POTION_UNF, 30, ItemID.RANARR_WEED, "Ranarr weed", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "toadflax_unf", "Toadflax potion (unf)", ItemID.TOADFLAX_POTION_UNF, 34, ItemID.TOADFLAX, "Toadflax", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "irit_unf", "Irit potion (unf)", ItemID.IRIT_POTION_UNF, 45, ItemID.IRIT_LEAF, "Irit leaf", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "avantoe_unf", "Avantoe potion (unf)", ItemID.AVANTOE_POTION_UNF, 50, ItemID.AVANTOE, "Avantoe", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "kwuarm_unf", "Kwuarm potion (unf)", ItemID.KWUARM_POTION_UNF, 55, ItemID.KWUARM, "Kwuarm", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "huasca_unf", "Huasca potion (unf)", ItemID.HUASCA_POTION_UNF, 58, ItemID.HUASCA, "Huasca", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "snapdragon_unf", "Snapdragon potion (unf)", ItemID.SNAPDRAGON_POTION_UNF, 63, ItemID.SNAPDRAGON, "Snapdragon", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "cadantine_unf", "Cadantine potion (unf)", ItemID.CADANTINE_POTION_UNF, 66, ItemID.CADANTINE, "Cadantine", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "lantadyme_unf", "Lantadyme potion (unf)", ItemID.LANTADYME_POTION_UNF, 69, ItemID.LANTADYME, "Lantadyme", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "dwarf_weed_unf", "Dwarf weed potion (unf)", ItemID.DWARF_WEED_POTION_UNF, 72, ItemID.DWARF_WEED, "Dwarf weed", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "torstol_unf", "Torstol potion (unf)", ItemID.TORSTOL_POTION_UNF, 78, ItemID.TORSTOL, "Torstol", ItemID.VIAL_OF_WATER, "Vial of water");
		addUnfinishedRecipe(list, "cadantine_blood_unf", "Cadantine blood potion (unf)", ItemID.CADANTINE_BLOOD_POTION_UNF, 80, ItemID.CADANTINE, "Cadantine", ItemID.VIAL_OF_BLOOD, "Vial of blood");

		ALL_RECIPES = Collections.unmodifiableList(list);
		Map<String, PotionRecipe> map = new LinkedHashMap<>();
		for (PotionRecipe r : ALL_RECIPES)
		{
			map.put(r.getRecipeId(), r);
		}
		BY_RECIPE_ID = Collections.unmodifiableMap(map);
	}

	private HerbloreRecipes()
	{
	}

	private static void addStandardFamily(
		List<PotionRecipe> list,
		String baseKey,
		String familyName,
		int level,
		double xp,
		RecipeCategory category,
		int id4,
		int id3,
		int id2,
		int id1,
		int unfId,
		String unfName,
		int herbId,
		String herbName,
		int secId,
		String secName,
		int secQty,
		String notes)
	{
		addStandardFamily(list, baseKey, familyName, level, xp, category,
			id4, id3, id2, id1, unfId, unfName, herbId, herbName, secId, secName, secQty, notes, 2500);
	}

	private static void addStandardFamily(
		List<PotionRecipe> list,
		String baseKey,
		String familyName,
		int level,
		double xp,
		RecipeCategory category,
		int id4,
		int id3,
		int id2,
		int id1,
		int unfId,
		String unfName,
		int herbId,
		String herbName,
		int secId,
		String secName,
		int secQty,
		String notes,
		int potionsPerHour)
	{
		// Primary 4-dose recipe (From Unfinished Potion + Secondary)
		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_4_unf")
			.familyName(familyName)
			.potionName(familyName + "(4)")
			.potionItemId(id4)
			.doses(4)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(unfId)
			.unfinishedPotionName(unfName)
			.secondarySummary(secName)
			.category(category)
			.alternativeRecipe(false)
			.potionsPerHourEstimate(potionsPerHour)
			.preparationNotes(notes)
			.ingredients(Arrays.asList(
				new PotionIngredient(unfId, unfName, 1, true, "Unfinished Base"),
				new PotionIngredient(secId, secName, secQty, true, "Secondary Ingredient")
			))
			.build());

		// Alternative 4-dose recipe (From Clean Herb + Vial of Water + Secondary)
		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_4_herb")
			.familyName(familyName)
			.potionName(familyName + "(4) (From Herb)")
			.potionItemId(id4)
			.doses(4)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(unfId)
			.unfinishedPotionName(unfName)
			.secondarySummary(herbName + " + Vial + " + secName)
			.category(category)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(1200)
			.preparationNotes("Two-step creation starting from clean " + herbName + " + Vial of water + " + secName + ".")
			.ingredients(Arrays.asList(
				new PotionIngredient(herbId, herbName, 1, true, "Clean Herb"),
				new PotionIngredient(ItemID.VIAL_OF_WATER, "Vial of water", 1, true, "Liquid Base"),
				new PotionIngredient(secId, secName, secQty, true, "Secondary Ingredient")
			))
			.build());

		// 3-dose variant (Direct creation output before decanting)
		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_3_unf")
			.familyName(familyName)
			.potionName(familyName + "(3)")
			.potionItemId(id3)
			.doses(3)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(unfId)
			.unfinishedPotionName(unfName)
			.secondarySummary(secName)
			.category(category)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(potionsPerHour)
			.preparationNotes("Direct 3-dose potion produced upon mixing " + unfName + " with " + secName + ".")
			.ingredients(Arrays.asList(
				new PotionIngredient(unfId, unfName, 1, true, "Unfinished Base"),
				new PotionIngredient(secId, secName, secQty, true, "Secondary Ingredient")
			))
			.build());

		// 2-dose variant (separate tradable ItemID)
		if (id2 > 0)
		{
			list.add(PotionRecipe.builder()
				.recipeId(baseKey + "_2_unf")
				.familyName(familyName)
				.potionName(familyName + "(2)")
				.potionItemId(id2)
				.doses(2)
				.requiredLevel(level)
				.xpGained(xp)
				.unfinishedPotionId(unfId)
				.unfinishedPotionName(unfName)
				.secondarySummary(secName)
				.category(category)
				.alternativeRecipe(true)
				.potionsPerHourEstimate(potionsPerHour)
				.preparationNotes("2-dose tradable variant of " + familyName + ".")
				.ingredients(Arrays.asList(
					new PotionIngredient(unfId, unfName, 1, true, "Unfinished Base"),
					new PotionIngredient(secId, secName, secQty, true, "Secondary Ingredient")
				))
				.build());
		}

		// 1-dose variant (separate tradable ItemID)
		if (id1 > 0)
		{
			list.add(PotionRecipe.builder()
				.recipeId(baseKey + "_1_unf")
				.familyName(familyName)
				.potionName(familyName + "(1)")
				.potionItemId(id1)
				.doses(1)
				.requiredLevel(level)
				.xpGained(xp)
				.unfinishedPotionId(unfId)
				.unfinishedPotionName(unfName)
				.secondarySummary(secName)
				.category(category)
				.alternativeRecipe(true)
				.potionsPerHourEstimate(potionsPerHour)
				.preparationNotes("1-dose tradable variant of " + familyName + ".")
				.ingredients(Arrays.asList(
					new PotionIngredient(unfId, unfName, 1, true, "Unfinished Base"),
					new PotionIngredient(secId, secName, secQty, true, "Secondary Ingredient")
				))
				.build());
		}
	}

	private static void addUntradeableStandardFamily(
		List<PotionRecipe> list,
		String baseKey,
		String familyName,
		int level,
		double xp,
		RecipeCategory category,
		int id4,
		int id3,
		int id2,
		int id1,
		int unfId,
		String unfName,
		int herbId,
		String herbName,
		int secId,
		String secName,
		int secQty,
		String notes)
	{
		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_4_unf")
			.familyName(familyName)
			.potionName(familyName + " (4)")
			.potionItemId(id4)
			.doses(4)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(unfId)
			.unfinishedPotionName(unfName)
			.secondarySummary(secName)
			.category(category)
			.tradeableOutput(false)
			.potionsPerHourEstimate(2500)
			.preparationNotes(notes)
			.ingredients(Arrays.asList(
				new PotionIngredient(unfId, unfName, 1, true, "Unfinished Base"),
				new PotionIngredient(secId, secName, secQty, true, "Secondary Ingredient")
			))
			.build());

		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_3_unf")
			.familyName(familyName)
			.potionName(familyName + " (3)")
			.potionItemId(id3)
			.doses(3)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(unfId)
			.unfinishedPotionName(unfName)
			.secondarySummary(secName)
			.category(category)
			.alternativeRecipe(true)
			.tradeableOutput(false)
			.potionsPerHourEstimate(2500)
			.preparationNotes(notes)
			.ingredients(Arrays.asList(
				new PotionIngredient(unfId, unfName, 1, true, "Unfinished Base"),
				new PotionIngredient(secId, secName, secQty, true, "Secondary Ingredient")
			))
			.build());
	}

	private static void addCoconutFamily(
		List<PotionRecipe> list,
		String baseKey,
		String familyName,
		int level,
		double xp,
		RecipeCategory category,
		int id4,
		int id3,
		int id2,
		int id1,
		int unfId,
		String unfName,
		int herbId,
		String herbName,
		int secId,
		String secName,
		String notes)
	{
		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_4_unf")
			.familyName(familyName)
			.potionName(familyName + "(4)")
			.potionItemId(id4)
			.doses(4)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(unfId)
			.unfinishedPotionName(unfName)
			.secondarySummary(secName)
			.category(category)
			.potionsPerHourEstimate(2500)
			.preparationNotes(notes)
			.ingredients(Arrays.asList(
				new PotionIngredient(unfId, unfName, 1, true, "Unfinished Base"),
				new PotionIngredient(secId, secName, 1, true, "Secondary Ingredient")
			))
			.build());

		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_4_coconut")
			.familyName(familyName)
			.potionName(familyName + "(4) (From Coconut Milk)")
			.potionItemId(id4)
			.doses(4)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(unfId)
			.unfinishedPotionName(unfName)
			.secondarySummary("Coconut milk + " + herbName + " + " + secName)
			.category(category)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(1200)
			.preparationNotes("Two-step creation using Coconut milk + " + herbName + " + " + secName + ".")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.COCONUT_MILK, "Coconut milk", 1, true, "Liquid Base"),
				new PotionIngredient(herbId, herbName, 1, true, "Clean Herb"),
				new PotionIngredient(secId, secName, 1, true, "Secondary Ingredient")
			))
			.build());

		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_3_unf")
			.familyName(familyName)
			.potionName(familyName + "(3)")
			.potionItemId(id3)
			.doses(3)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(unfId)
			.unfinishedPotionName(unfName)
			.secondarySummary(secName)
			.category(category)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(2500)
			.preparationNotes("3-dose variant of " + familyName + ".")
			.ingredients(Arrays.asList(
				new PotionIngredient(unfId, unfName, 1, true, "Unfinished Base"),
				new PotionIngredient(secId, secName, 1, true, "Secondary Ingredient")
			))
			.build());
	}

	private static void addBloodPotionFamily(
		List<PotionRecipe> list,
		String baseKey,
		String familyName,
		int level,
		double xp,
		RecipeCategory category,
		int id4,
		int id3,
		int id2,
		int id1,
		int secId,
		String secName,
		String notes)
	{
		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_4_unf")
			.familyName(familyName)
			.potionName(familyName + "(4)")
			.potionItemId(id4)
			.doses(4)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(ItemID.CADANTINE_BLOOD_POTION_UNF)
			.unfinishedPotionName("Cadantine blood potion (unf)")
			.secondarySummary(secName)
			.category(category)
			.potionsPerHourEstimate(2500)
			.preparationNotes(notes)
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.CADANTINE_BLOOD_POTION_UNF, "Cadantine blood potion (unf)", 1, true, "Unfinished Base"),
				new PotionIngredient(secId, secName, 1, true, "Secondary Ingredient")
			))
			.build());

		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_4_scratch")
			.familyName(familyName)
			.potionName(familyName + "(4) (From Vial of Blood)")
			.potionItemId(id4)
			.doses(4)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(ItemID.CADANTINE_BLOOD_POTION_UNF)
			.unfinishedPotionName("Cadantine blood potion (unf)")
			.secondarySummary("Vial of blood + Cadantine + " + secName)
			.category(category)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(1200)
			.preparationNotes("Two-step creation using Vial of blood + Cadantine + " + secName + ".")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.VIAL_OF_BLOOD, "Vial of blood", 1, true, "Liquid Base"),
				new PotionIngredient(ItemID.CADANTINE, "Cadantine", 1, true, "Clean Herb"),
				new PotionIngredient(secId, secName, 1, true, "Secondary Ingredient")
			))
			.build());

		list.add(PotionRecipe.builder()
			.recipeId(baseKey + "_3_unf")
			.familyName(familyName)
			.potionName(familyName + "(3)")
			.potionItemId(id3)
			.doses(3)
			.requiredLevel(level)
			.xpGained(xp)
			.unfinishedPotionId(ItemID.CADANTINE_BLOOD_POTION_UNF)
			.unfinishedPotionName("Cadantine blood potion (unf)")
			.secondarySummary(secName)
			.category(category)
			.alternativeRecipe(true)
			.potionsPerHourEstimate(2500)
			.preparationNotes("3-dose variant of " + familyName + ".")
			.ingredients(Arrays.asList(
				new PotionIngredient(ItemID.CADANTINE_BLOOD_POTION_UNF, "Cadantine blood potion (unf)", 1, true, "Unfinished Base"),
				new PotionIngredient(secId, secName, 1, true, "Secondary Ingredient")
			))
			.build());
	}

	private static void addStackableUpgradeFamily(
		List<PotionRecipe> list,
		String baseKey,
		String familyName,
		int level,
		double xpPerDose,
		RecipeCategory category,
		int out4,
		int out3,
		int out2,
		int out1,
		int in4,
		int in3,
		int in2,
		int in1,
		String basePotionFamilyName,
		int catalystId,
		String catalystName,
		int catalystPerDose,
		String notes)
	{
		int[] outIds = new int[]{out4, out3, out2, out1};
		int[] inIds = new int[]{in4, in3, in2, in1};
		int[] doses = new int[]{4, 3, 2, 1};

		for (int i = 0; i < 4; i++)
		{
			int d = doses[i];
			int totalCatalyst = d * catalystPerDose;
			double totalXp = Math.round((xpPerDose * d) * 100.0) / 100.0;
			String baseDoseName = basePotionFamilyName + "(" + d + ")";

			list.add(PotionRecipe.builder()
				.recipeId(baseKey + "_" + d)
				.familyName(familyName)
				.potionName(familyName + "(" + d + ")")
				.potionItemId(outIds[i])
				.doses(d)
				.requiredLevel(level)
				.xpGained(totalXp)
				.unfinishedPotionId(inIds[i])
				.unfinishedPotionName(baseDoseName)
				.secondarySummary(totalCatalyst + "x " + catalystName)
				.category(category)
				.alternativeRecipe(d != 4)
				.potionsPerHourEstimate(2700)
				.preparationNotes(notes)
				.ingredients(Arrays.asList(
					new PotionIngredient(inIds[i], baseDoseName, 1, true, "Base Potion"),
					new PotionIngredient(catalystId, catalystName, totalCatalyst, true, "Stackable Secondary")
				))
				.build());
		}
	}

	private static void addDivineFamily(
		List<PotionRecipe> list,
		String baseKey,
		String familyName,
		int level,
		int out4,
		int out3,
		int out2,
		int out1,
		int in4,
		int in3,
		int in2,
		int in1,
		String basePotionFamilyName)
	{
		int[] outIds = new int[]{out4, out3, out2, out1};
		int[] inIds = new int[]{in4, in3, in2, in1};
		int[] doses = new int[]{4, 3, 2, 1};

		for (int i = 0; i < 4; i++)
		{
			int d = doses[i];
			double xp = 0.5 * d;
			String baseDoseName = basePotionFamilyName + "(" + d + ")";

			list.add(PotionRecipe.builder()
				.recipeId(baseKey + "_" + d)
				.familyName(familyName)
				.potionName(familyName + "(" + d + ")")
				.potionItemId(outIds[i])
				.doses(d)
				.requiredLevel(level)
				.xpGained(xp)
				.unfinishedPotionId(inIds[i])
				.unfinishedPotionName(baseDoseName)
				.secondarySummary(d + "x Crystal dust (Untradeable)")
				.category(RecipeCategory.DIVINE_POTION)
				.alternativeRecipe(d != 4)
				.potionsPerHourEstimate(2700)
				.preparationNotes("Requires Song of the Elves. Uses " + d + "x untradeable Crystal dust (1 Crystal shard = 10 dust).")
				.ingredients(Arrays.asList(
					new PotionIngredient(inIds[i], baseDoseName, 1, true, "Base Potion"),
					new PotionIngredient(ItemID.CRYSTAL_DUST, "Crystal dust", d, false, "Untradeable Catalyst")
				))
				.build());
		}
	}

	private static void addUnfinishedRecipe(
		List<PotionRecipe> list,
		String recipeId,
		String unfName,
		int unfItemId,
		int requiredLevel,
		int herbId,
		String herbName,
		int vialId,
		String vialName)
	{
		list.add(PotionRecipe.builder()
			.recipeId(recipeId)
			.familyName(unfName)
			.potionName(unfName)
			.potionItemId(unfItemId)
			.doses(3)
			.requiredLevel(requiredLevel)
			.xpGained(0.0)
			.unfinishedPotionId(vialId)
			.unfinishedPotionName(vialName)
			.secondarySummary(herbName)
			.category(RecipeCategory.UNFINISHED_POTION)
			.unfinishedPotionRecipe(true)
			.potionsPerHourEstimate(2500)
			.preparationNotes("Unfinished potion preparation (0 Herblore XP). Excluded from training recommendations by default.")
			.ingredients(Arrays.asList(
				new PotionIngredient(herbId, herbName, 1, true, "Clean Herb"),
				new PotionIngredient(vialId, vialName, 1, true, "Liquid Base")
			))
			.build());
	}

	/**
	 * Returns all recipes in the local database (including dose variants, alternative methods, and unfinished potions).
	 */
	public static List<PotionRecipe> getAllRecipes()
	{
		return ALL_RECIPES;
	}

	/**
	 * Returns only completed potion recipes (excludes 0-XP Unfinished Potions).
	 */
	public static List<PotionRecipe> getCompletedPotionRecipes(boolean includeAlternativesAndLowerDoses)
	{
		List<PotionRecipe> result = new ArrayList<>();
		for (PotionRecipe r : ALL_RECIPES)
		{
			if (r.isUnfinishedPotionRecipe())
			{
				continue;
			}
			if (!includeAlternativesAndLowerDoses && r.isAlternativeRecipe())
			{
				continue;
			}
			result.add(r);
		}
		return result;
	}

	public static PotionRecipe getByRecipeId(String recipeId)
	{
		return BY_RECIPE_ID.get(recipeId);
	}
}
