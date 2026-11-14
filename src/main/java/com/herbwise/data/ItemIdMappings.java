package com.herbwise.data;

import com.herbwise.models.ItemPrice;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.ItemID;

/**
 * Authoritative mapping of OSRS Herblore item IDs to canonical item names, Grand Exchange buy limits,
 * tradeability flags, and verified OSRS Wiki cached snapshot prices used strictly as a clearly-labelled
 * fallback when the live API is unreachable.
 */
public final class ItemIdMappings
{
	public static class ItemDefinition
	{
		private final int itemId;
		private final String name;
		private final int buyLimit;
		private final boolean members;
		private final boolean tradeable;
		private final long cachedHighPrice;
		private final long cachedLowPrice;
		private final int cachedHourlyVolume;

		public ItemDefinition(
			int itemId,
			String name,
			int buyLimit,
			boolean members,
			boolean tradeable,
			long cachedHighPrice,
			long cachedLowPrice,
			int cachedHourlyVolume)
		{
			this.itemId = itemId;
			this.name = name;
			this.buyLimit = buyLimit;
			this.members = members;
			this.tradeable = tradeable;
			this.cachedHighPrice = cachedHighPrice;
			this.cachedLowPrice = cachedLowPrice;
			this.cachedHourlyVolume = cachedHourlyVolume;
		}

		public int getItemId() { return itemId; }
		public String getName() { return name; }
		public int getBuyLimit() { return buyLimit; }
		public boolean isMembers() { return members; }
		public boolean isTradeable() { return tradeable; }
		public long getCachedHighPrice() { return cachedHighPrice; }
		public long getCachedLowPrice() { return cachedLowPrice; }
		public int getCachedHourlyVolume() { return cachedHourlyVolume; }
	}

	private static final Map<Integer, ItemDefinition> ITEMS = new LinkedHashMap<>();

	static
	{
		// Vials & Liquid Bases
		register(ItemID.VIAL_OF_WATER, "Vial of water", 13000, false, true, 5L, 4L, 480000);
		register(ItemID.VIAL, "Vial", 13000, false, true, 2L, 1L, 85000);
		register(ItemID.COCONUT_MILK, "Coconut milk", 10000, true, true, 2150L, 2040L, 6200);
		register(ItemID.VIAL_OF_BLOOD, "Vial of blood", 10000, true, true, 16800L, 16500L, 9500);
		register(ItemID.CUP_OF_HOT_WATER, "Cup of hot water", 2000, true, true, 320L, 280L, 450);

		// Clean Herbs
		register(ItemID.GUAM_LEAF, "Guam leaf", 11000, true, true, 48L, 42L, 38000);
		register(ItemID.MARRENTILL, "Marrentill", 11000, true, true, 52L, 45L, 32000);
		register(ItemID.TARROMIN, "Tarromin", 11000, true, true, 145L, 132L, 41000);
		register(ItemID.HARRALANDER, "Harralander", 11000, true, true, 690L, 665L, 54000);
		register(ItemID.RANARR_WEED, "Ranarr weed", 11000, true, true, 6450L, 6390L, 92000);
		register(ItemID.TOADFLAX, "Toadflax", 11000, true, true, 3420L, 3380L, 88000);
		register(ItemID.IRIT_LEAF, "Irit leaf", 11000, true, true, 1180L, 1140L, 65000);
		register(ItemID.AVANTOE, "Avantoe", 11000, true, true, 2240L, 2190L, 58000);
		register(ItemID.KWUARM, "Kwuarm", 11000, true, true, 2380L, 2330L, 61000);
		register(ItemID.SNAPDRAGON, "Snapdragon", 11000, true, true, 8950L, 8870L, 74000);
		register(ItemID.CADANTINE, "Cadantine", 11000, true, true, 2120L, 2070L, 49000);
		register(ItemID.LANTADYME, "Lantadyme", 11000, true, true, 1560L, 1510L, 44000);
		register(ItemID.DWARF_WEED, "Dwarf weed", 11000, true, true, 2190L, 2140L, 63000);
		register(ItemID.TORSTOL, "Torstol", 11000, true, true, 4150L, 4080L, 52000);
		register(ItemID.HUASCA, "Huasca", 11000, true, true, 9800L, 9650L, 18500);
		register(ItemID.SNAKE_WEED, "Snake weed", 11000, true, true, 480L, 430L, 4200);

		// Grimy Herbs
		register(ItemID.GRIMY_GUAM_LEAF, "Grimy guam leaf", 11000, true, true, 42L, 36L, 29000);
		register(ItemID.GRIMY_MARRENTILL, "Grimy marrentill", 11000, true, true, 44L, 38L, 24000);
		register(ItemID.GRIMY_TARROMIN, "Grimy tarromin", 11000, true, true, 130L, 118L, 31000);
		register(ItemID.GRIMY_HARRALANDER, "Grimy harralander", 11000, true, true, 650L, 625L, 42000);
		register(ItemID.GRIMY_RANARR_WEED, "Grimy ranarr weed", 11000, true, true, 6350L, 6290L, 78000);
		register(ItemID.GRIMY_TOADFLAX, "Grimy toadflax", 11000, true, true, 3340L, 3290L, 71000);
		register(ItemID.GRIMY_IRIT_LEAF, "Grimy irit leaf", 11000, true, true, 1120L, 1080L, 51000);
		register(ItemID.GRIMY_AVANTOE, "Grimy avantoe", 11000, true, true, 2160L, 2110L, 47000);
		register(ItemID.GRIMY_KWUARM, "Grimy kwuarm", 11000, true, true, 2290L, 2240L, 49000);
		register(ItemID.GRIMY_SNAPDRAGON, "Grimy snapdragon", 11000, true, true, 8820L, 8740L, 61000);
		register(ItemID.GRIMY_CADANTINE, "Grimy cadantine", 11000, true, true, 2040L, 1990L, 38000);
		register(ItemID.GRIMY_LANTADYME, "Grimy lantadyme", 11000, true, true, 1490L, 1440L, 35000);
		register(ItemID.GRIMY_DWARF_WEED, "Grimy dwarf weed", 11000, true, true, 2110L, 2060L, 48000);
		register(ItemID.GRIMY_TORSTOL, "Grimy torstol", 11000, true, true, 4040L, 3970L, 39000);
		register(ItemID.GRIMY_HUASCA, "Grimy huasca", 11000, true, true, 9620L, 9480L, 14000);

		// Unfinished Potions
		register(ItemID.GUAM_POTION_UNF, "Guam potion (unf)", 10000, true, true, 115L, 98L, 14000);
		register(ItemID.MARRENTILL_POTION_UNF, "Marrentill potion (unf)", 10000, true, true, 125L, 105L, 11000);
		register(ItemID.TARROMIN_POTION_UNF, "Tarromin potion (unf)", 10000, true, true, 230L, 205L, 16000);
		register(ItemID.HARRALANDER_POTION_UNF, "Harralander potion (unf)", 10000, true, true, 780L, 745L, 28000);
		register(ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)", 10000, true, true, 6580L, 6510L, 68000);
		register(ItemID.TOADFLAX_POTION_UNF, "Toadflax potion (unf)", 10000, true, true, 3560L, 3505L, 64000);
		register(ItemID.IRIT_POTION_UNF, "Irit potion (unf)", 10000, true, true, 1290L, 1245L, 45000);
		register(ItemID.AVANTOE_POTION_UNF, "Avantoe potion (unf)", 10000, true, true, 2360L, 2305L, 42000);
		register(ItemID.KWUARM_POTION_UNF, "Kwuarm potion (unf)", 10000, true, true, 2510L, 2450L, 46000);
		register(ItemID.SNAPDRAGON_POTION_UNF, "Snapdragon potion (unf)", 10000, true, true, 9120L, 9030L, 55000);
		register(ItemID.CADANTINE_POTION_UNF, "Cadantine potion (unf)", 10000, true, true, 2240L, 2180L, 36000);
		register(ItemID.LANTADYME_POTION_UNF, "Lantadyme potion (unf)", 10000, true, true, 1680L, 1625L, 34000);
		register(ItemID.DWARF_WEED_POTION_UNF, "Dwarf weed potion (unf)", 10000, true, true, 2310L, 2255L, 49000);
		register(ItemID.TORSTOL_POTION_UNF, "Torstol potion (unf)", 10000, true, true, 4290L, 4210L, 32000);
		register(ItemID.CADANTINE_BLOOD_POTION_UNF, "Cadantine blood potion (unf)", 10000, true, true, 19050L, 18750L, 12500);
		register(ItemID.HUASCA_POTION_UNF, "Huasca potion (unf)", 10000, true, true, 10050L, 9880L, 13000);
		register(ItemID.ANTIDOTE_UNF, "Antidote+ (unf)", 4000, true, true, 5650L, 5480L, 4100);
		register(ItemID.ANTIDOTE_UNF_5951, "Antidote++ (unf)", 4000, true, true, 3420L, 3290L, 4800);
		register(ItemID.WEAPON_POISON_UNF, "Weapon poison+ (unf)", 4000, true, true, 2850L, 2710L, 2100);
		register(ItemID.WEAPON_POISON_UNF_5939, "Weapon poison++ (unf)", 4000, true, true, 3150L, 2980L, 1900);

		// Secondary Ingredients
		register(ItemID.EYE_OF_NEWT, "Eye of newt", 13000, false, true, 6L, 4L, 210000);
		register(ItemID.UNICORN_HORN_DUST, "Unicorn horn dust", 13000, true, true, 420L, 395L, 34000);
		register(ItemID.UNICORN_HORN, "Unicorn horn", 13000, true, true, 360L, 335L, 18000);
		register(ItemID.LIMPWURT_ROOT, "Limpwurt root", 13000, false, true, 1080L, 1040L, 58000);
		register(ItemID.ASHES, "Ashes", 13000, false, true, 48L, 40L, 42000);
		register(ItemID.VOLCANIC_ASH, "Volcanic ash", 13000, true, true, 92L, 86L, 190000);
		register(ItemID.RED_SPIDERS_EGGS, "Red spiders' eggs", 13000, false, true, 780L, 745L, 62000);
		register(ItemID.CHOCOLATE_DUST, "Chocolate dust", 13000, false, true, 75L, 64L, 38000);
		register(ItemID.CHOCOLATE_BAR, "Chocolate bar", 13000, false, true, 58L, 50L, 25000);
		register(ItemID.WHITE_BERRIES, "White berries", 13000, true, true, 890L, 855L, 44000);
		register(ItemID.TOADS_LEGS, "Toad's legs", 13000, true, true, 340L, 310L, 19000);
		register(ItemID.GOAT_HORN_DUST, "Goat horn dust", 13000, true, true, 290L, 265L, 22000);
		register(ItemID.SNAPE_GRASS, "Snape grass", 13000, true, true, 385L, 368L, 115000);
		register(ItemID.MORT_MYRE_FUNGUS, "Mort myre fungus", 13000, true, true, 940L, 905L, 51000);
		register(ItemID.KEBBIT_TEETH_DUST, "Kebbit teeth dust", 13000, true, true, 260L, 230L, 9500);
		register(ItemID.KEBBIT_TEETH, "Kebbit teeth", 13000, true, true, 210L, 185L, 4500);
		register(ItemID.ALDARIUM, "Aldarium", 10000, true, true, 24500L, 24100L, 16500);
		register(ItemID.DRAGON_SCALE_DUST, "Dragon scale dust", 13000, true, true, 590L, 555L, 39000);
		register(ItemID.BLUE_DRAGON_SCALE, "Blue dragon scale", 13000, true, true, 530L, 495L, 24000);
		register(ItemID.CACTUS_SPINE, "Cactus spine", 11000, true, true, 580L, 540L, 16000);
		register(ItemID.CAVE_NIGHTSHADE, "Cave nightshade", 11000, true, true, 820L, 760L, 5800);
		register(ItemID.POISON_IVY_BERRIES, "Poison ivy berries", 13000, true, true, 490L, 450L, 14000);
		register(ItemID.NAIL_BEAST_NAILS, "Nail beast nails", 13000, true, true, 9400L, 9100L, 4800);
		register(ItemID.YEW_ROOTS, "Yew roots", 11000, true, true, 1650L, 1580L, 12000);
		register(ItemID.WINE_OF_ZAMORAK, "Wine of zamorak", 13000, false, true, 820L, 790L, 67000);
		register(ItemID.POTATO_CACTUS, "Potato cactus", 13000, true, true, 265L, 245L, 48000);
		register(ItemID.AMYLASE_CRYSTAL, "Amylase crystal", 13000, true, true, 1180L, 1155L, 320000);
		register(ItemID.JANGERBERRIES, "Jangerberries", 13000, true, true, 390L, 360L, 27000);
		register(ItemID.MAGIC_ROOTS, "Magic roots", 11000, true, true, 1850L, 1780L, 14500);
		register(ItemID.CRUSHED_NEST, "Crushed nest", 13000, true, true, 6450L, 6360L, 84000);
		register(ItemID.DEMONIC_TALLOW, "Demonic tallow", 10000, true, true, 5400L, 5250L, 11500);
		register(ItemID.LAVA_SCALE_SHARD, "Lava scale shard", 13000, true, true, 425L, 405L, 95000);
		register(ItemID.LAVA_SCALE, "Lava scale", 13000, true, true, 1820L, 1750L, 16000);
		register(ItemID.NIHIL_DUST, "Nihil dust", 13000, true, true, 1650L, 1590L, 42000);
		register(ItemID.ZULRAHS_SCALES, "Zulrah's scales", 30000, true, true, 162L, 159L, 1450000);
		register(ItemID.LILY_OF_THE_SANDS, "Lily of the sands", 10000, true, true, 8400L, 8200L, 9200);
		register(ItemID.ANCIENT_ESSENCE, "Ancient essence", 300000, true, true, 12L, 11L, 3800000);
		register(ItemID.CRUSHED_SUPERIOR_DRAGON_BONES, "Crushed superior dragon bones", 10000, true, true, 14900L, 14650L, 18000);
		register(ItemID.SUPERIOR_DRAGON_BONES, "Superior dragon bones", 10000, true, true, 14750L, 14500L, 29000);
		register(ItemID.ARAXYTE_VENOM_SAC, "Araxyte venom sac", 10000, true, true, 2450L, 2360L, 34000);

		// Untradeable Secondaries & Catalysts
		register(ItemID.CRYSTAL_DUST, "Crystal dust", 0, true, false, 0L, 0L, 0);
		register(ItemID.MOONLIGHT_GRUB, "Moonlight grub", 0, true, false, 0L, 0L, 0);
		register(ItemID.MOONLIGHT_GRUB_PASTE, "Moonlight grub paste", 0, true, false, 0L, 0L, 0);

		// Finished Potions (4-dose, 3-dose, 2-dose, 1-dose)
		// 1. Attack potion
		register(ItemID.ATTACK_POTION4, "Attack potion(4)", 2000, false, true, 48L, 40L, 9500);
		register(ItemID.ATTACK_POTION3, "Attack potion(3)", 2000, false, true, 32L, 25L, 12000);
		register(ItemID.ATTACK_POTION2, "Attack potion(2)", 2000, false, true, 24L, 18L, 1400);
		register(ItemID.ATTACK_POTION1, "Attack potion(1)", 2000, false, true, 18L, 12L, 900);

		// 2. Antipoison
		register(ItemID.ANTIPOISON4, "Antipoison(4)", 2000, false, true, 410L, 380L, 14000);
		register(ItemID.ANTIPOISON3, "Antipoison(3)", 2000, false, true, 295L, 265L, 11000);
		register(ItemID.ANTIPOISON2, "Antipoison(2)", 2000, false, true, 190L, 165L, 1800);
		register(ItemID.ANTIPOISON1, "Antipoison(1)", 2000, false, true, 110L, 90L, 1200);

		// 3. Strength potion
		register(ItemID.STRENGTH_POTION4, "Strength potion(4)", 2000, false, true, 1120L, 1070L, 18500);
		register(ItemID.STRENGTH_POTION3, "Strength potion(3)", 2000, false, true, 820L, 780L, 15000);
		register(ItemID.STRENGTH_POTION2, "Strength potion(2)", 2000, false, true, 540L, 500L, 2400);
		register(ItemID.STRENGTH_POTION1, "Strength potion(1)", 2000, false, true, 280L, 250L, 1600);

		// 4. Serum 207 (Untradeable)
		register(ItemID.SERUM_207_4, "Serum 207 (4)", 0, true, false, 0L, 0L, 0);
		register(ItemID.SERUM_207_3, "Serum 207 (3)", 0, true, false, 0L, 0L, 0);
		register(ItemID.SERUM_207_2, "Serum 207 (2)", 0, true, false, 0L, 0L, 0);
		register(ItemID.SERUM_207_1, "Serum 207 (1)", 0, true, false, 0L, 0L, 0);

		// 5. Guthix rest
		register(ItemID.GUTHIX_REST4, "Guthix rest(4)", 2000, true, true, 2650L, 2520L, 4200);
		register(ItemID.GUTHIX_REST3, "Guthix rest(3)", 2000, true, true, 1950L, 1840L, 3100);
		register(ItemID.GUTHIX_REST2, "Guthix rest(2)", 2000, true, true, 1280L, 1180L, 650);
		register(ItemID.GUTHIX_REST1, "Guthix rest(1)", 2000, true, true, 640L, 580L, 420);

		// 6. Compost potion
		register(ItemID.COMPOST_POTION4, "Compost potion(4)", 2000, true, true, 920L, 865L, 6800);
		register(ItemID.COMPOST_POTION3, "Compost potion(3)", 2000, true, true, 680L, 635L, 8200);
		register(ItemID.COMPOST_POTION2, "Compost potion(2)", 2000, true, true, 440L, 405L, 950);
		register(ItemID.COMPOST_POTION1, "Compost potion(1)", 2000, true, true, 220L, 195L, 600);

		// 7. Restore potion
		register(ItemID.RESTORE_POTION4, "Restore potion(4)", 2000, true, true, 1180L, 1120L, 8900);
		register(ItemID.RESTORE_POTION3, "Restore potion(3)", 2000, true, true, 870L, 820L, 11200);
		register(ItemID.RESTORE_POTION2, "Restore potion(2)", 2000, true, true, 570L, 530L, 1100);
		register(ItemID.RESTORE_POTION1, "Restore potion(1)", 2000, true, true, 290L, 260L, 750);

		// 8. Energy potion
		register(ItemID.ENERGY_POTION4, "Energy potion(4)", 2000, true, true, 520L, 485L, 26000);
		register(ItemID.ENERGY_POTION3, "Energy potion(3)", 2000, true, true, 380L, 350L, 31000);
		register(ItemID.ENERGY_POTION2, "Energy potion(2)", 2000, true, true, 250L, 225L, 2800);
		register(ItemID.ENERGY_POTION1, "Energy potion(1)", 2000, true, true, 130L, 110L, 1900);

		// 9. Defence potion
		register(ItemID.DEFENCE_POTION4, "Defence potion(4)", 2000, true, true, 3950L, 3810L, 4500);
		register(ItemID.DEFENCE_POTION3, "Defence potion(3)", 2000, true, true, 2920L, 2810L, 5200);
		register(ItemID.DEFENCE_POTION2, "Defence potion(2)", 2000, true, true, 1920L, 1820L, 600);
		register(ItemID.DEFENCE_POTION1, "Defence potion(1)", 2000, true, true, 960L, 900L, 400);

		// 10. Agility potion
		register(ItemID.AGILITY_POTION4, "Agility potion(4)", 2000, true, true, 2650L, 2540L, 5600);
		register(ItemID.AGILITY_POTION3, "Agility potion(3)", 2000, true, true, 1960L, 1880L, 6400);
		register(ItemID.AGILITY_POTION2, "Agility potion(2)", 2000, true, true, 1290L, 1210L, 750);
		register(ItemID.AGILITY_POTION1, "Agility potion(1)", 2000, true, true, 650L, 600L, 500);

		// 11. Combat potion
		register(ItemID.COMBAT_POTION4, "Combat potion(4)", 2000, true, true, 640L, 595L, 19500);
		register(ItemID.COMBAT_POTION3, "Combat potion(3)", 2000, true, true, 470L, 435L, 24000);
		register(ItemID.COMBAT_POTION2, "Combat potion(2)", 2000, true, true, 310L, 280L, 2100);
		register(ItemID.COMBAT_POTION1, "Combat potion(1)", 2000, true, true, 160L, 140L, 1400);

		// 12. Prayer potion
		register(ItemID.PRAYER_POTION4, "Prayer potion(4)", 2000, true, true, 9380L, 9310L, 165000);
		register(ItemID.PRAYER_POTION3, "Prayer potion(3)", 2000, true, true, 7010L, 6950L, 112000);
		register(ItemID.PRAYER_POTION2, "Prayer potion(2)", 2000, true, true, 4620L, 4550L, 14000);
		register(ItemID.PRAYER_POTION1, "Prayer potion(1)", 2000, true, true, 2310L, 2260L, 9800);

		// 13. Super attack
		register(ItemID.SUPER_ATTACK4, "Super attack(4)", 2000, true, true, 1080L, 1035L, 54000);
		register(ItemID.SUPER_ATTACK3, "Super attack(3)", 2000, true, true, 795L, 760L, 62000);
		register(ItemID.SUPER_ATTACK2, "Super attack(2)", 2000, true, true, 520L, 490L, 4800);
		register(ItemID.SUPER_ATTACK1, "Super attack(1)", 2000, true, true, 260L, 240L, 3200);

		// 14. Superantipoison
		register(ItemID.SUPERANTIPOISON4, "Superantipoison(4)", 2000, true, true, 1520L, 1455L, 16500);
		register(ItemID.SUPERANTIPOISON3, "Superantipoison(3)", 2000, true, true, 1120L, 1070L, 19500);
		register(ItemID.SUPERANTIPOISON2, "Superantipoison(2)", 2000, true, true, 740L, 695L, 1900);
		register(ItemID.SUPERANTIPOISON1, "Superantipoison(1)", 2000, true, true, 370L, 340L, 1300);

		// 15. Fishing potion
		register(ItemID.FISHING_POTION4, "Fishing potion(4)", 2000, true, true, 1920L, 1840L, 7800);
		register(ItemID.FISHING_POTION3, "Fishing potion(3)", 2000, true, true, 1420L, 1360L, 9600);
		register(ItemID.FISHING_POTION2, "Fishing potion(2)", 2000, true, true, 930L, 880L, 850);
		register(ItemID.FISHING_POTION1, "Fishing potion(1)", 2000, true, true, 465L, 430L, 600);

		// 16. Super energy
		register(ItemID.SUPER_ENERGY4, "Super energy(4)", 2000, true, true, 3190L, 3130L, 72000);
		register(ItemID.SUPER_ENERGY3, "Super energy(3)", 2000, true, true, 2380L, 2330L, 64000);
		register(ItemID.SUPER_ENERGY2, "Super energy(2)", 2000, true, true, 1560L, 1510L, 5400);
		register(ItemID.SUPER_ENERGY1, "Super energy(1)", 2000, true, true, 780L, 750L, 3800);

		// 17. Hunter potion
		register(ItemID.HUNTER_POTION4, "Hunter potion(4)", 2000, true, true, 1680L, 1590L, 5400);
		register(ItemID.HUNTER_POTION3, "Hunter potion(3)", 2000, true, true, 1240L, 1175L, 6800);
		register(ItemID.HUNTER_POTION2, "Hunter potion(2)", 2000, true, true, 810L, 760L, 650);
		register(ItemID.HUNTER_POTION1, "Hunter potion(1)", 2000, true, true, 405L, 375L, 450);

		// 18. Goading potion
		register(ItemID.GOADING_POTION4, "Goading potion(4)", 2000, true, true, 34200L, 33600L, 18500);
		register(ItemID.GOADING_POTION3, "Goading potion(3)", 2000, true, true, 25500L, 25000L, 14200);
		register(ItemID.GOADING_POTION2, "Goading potion(2)", 2000, true, true, 16900L, 16500L, 2200);
		register(ItemID.GOADING_POTION1, "Goading potion(1)", 2000, true, true, 8450L, 8200L, 1500);

		// 19. Super strength
		register(ItemID.SUPER_STRENGTH4, "Super strength(4)", 2000, true, true, 3680L, 3620L, 68000);
		register(ItemID.SUPER_STRENGTH3, "Super strength(3)", 2000, true, true, 2750L, 2700L, 59000);
		register(ItemID.SUPER_STRENGTH2, "Super strength(2)", 2000, true, true, 1810L, 1760L, 6200);
		register(ItemID.SUPER_STRENGTH1, "Super strength(1)", 2000, true, true, 905L, 875L, 4400);

		// 20. Prayer regeneration potion
		register(ItemID.PRAYER_REGENERATION_POTION4, "Prayer regeneration potion(4)", 2000, true, true, 46400L, 45800L, 22000);
		register(ItemID.PRAYER_REGENERATION_POTION3, "Prayer regeneration potion(3)", 2000, true, true, 34700L, 34200L, 16500);
		register(ItemID.PRAYER_REGENERATION_POTION2, "Prayer regeneration potion(2)", 2000, true, true, 23000L, 22600L, 2800);
		register(ItemID.PRAYER_REGENERATION_POTION1, "Prayer regeneration potion(1)", 2000, true, true, 11500L, 11250L, 1900);

		// 21. Weapon poison variants
		register(ItemID.WEAPON_POISON, "Weapon poison", 2000, true, true, 2480L, 2390L, 7400);
		register(ItemID.WEAPON_POISON_5937, "Weapon poison(+)", 2000, true, true, 3450L, 3310L, 4200);
		register(ItemID.WEAPON_POISON_5940, "Weapon poison(++)", 2000, true, true, 4120L, 3960L, 5100);

		// 22. Super restore
		register(ItemID.SUPER_RESTORE4, "Super restore(4)", 2000, true, true, 12450L, 12360L, 142000);
		register(ItemID.SUPER_RESTORE3, "Super restore(3)", 2000, true, true, 9310L, 9240L, 94000);
		register(ItemID.SUPER_RESTORE2, "Super restore(2)", 2000, true, true, 6180L, 6110L, 12500);
		register(ItemID.SUPER_RESTORE1, "Super restore(1)", 2000, true, true, 3090L, 3040L, 8900);

		// 23. Sanfew serum
		register(ItemID.SANFEW_SERUM4, "Sanfew serum(4)", 2000, true, true, 24200L, 23850L, 19500);
		register(ItemID.SANFEW_SERUM3, "Sanfew serum(3)", 2000, true, true, 18100L, 17800L, 11000);
		register(ItemID.SANFEW_SERUM2, "Sanfew serum(2)", 2000, true, true, 12000L, 11750L, 2400);
		register(ItemID.SANFEW_SERUM1, "Sanfew serum(1)", 2000, true, true, 6000L, 5850L, 1600);

		// 24. Super defence
		register(ItemID.SUPER_DEFENCE4, "Super defence(4)", 2000, true, true, 3290L, 3220L, 48000);
		register(ItemID.SUPER_DEFENCE3, "Super defence(3)", 2000, true, true, 2460L, 2405L, 44000);
		register(ItemID.SUPER_DEFENCE2, "Super defence(2)", 2000, true, true, 1620L, 1575L, 4200);
		register(ItemID.SUPER_DEFENCE1, "Super defence(1)", 2000, true, true, 810L, 780L, 2900);

		// 25. Antidote+
		register(ItemID.ANTIDOTE4, "Antidote+(4)", 2000, true, true, 4950L, 4810L, 9200);
		register(ItemID.ANTIDOTE3, "Antidote+(3)", 2000, true, true, 3690L, 3580L, 7400);
		register(ItemID.ANTIDOTE2, "Antidote+(2)", 2000, true, true, 2440L, 2350L, 1100);
		register(ItemID.ANTIDOTE1, "Antidote+(1)", 2000, true, true, 1220L, 1160L, 750);

		// 26. Antifire potion
		register(ItemID.ANTIFIRE_POTION4, "Antifire potion(4)", 2000, true, true, 2240L, 2180L, 52000);
		register(ItemID.ANTIFIRE_POTION3, "Antifire potion(3)", 2000, true, true, 1670L, 1620L, 46000);
		register(ItemID.ANTIFIRE_POTION2, "Antifire potion(2)", 2000, true, true, 1100L, 1060L, 4500);
		register(ItemID.ANTIFIRE_POTION1, "Antifire potion(1)", 2000, true, true, 550L, 525L, 3100);

		// 27. Ranging potion
		register(ItemID.RANGING_POTION4, "Ranging potion(4)", 2000, true, true, 3260L, 3205L, 96000);
		register(ItemID.RANGING_POTION3, "Ranging potion(3)", 2000, true, true, 2440L, 2395L, 74000);
		register(ItemID.RANGING_POTION2, "Ranging potion(2)", 2000, true, true, 1610L, 1575L, 8400);
		register(ItemID.RANGING_POTION1, "Ranging potion(1)", 2000, true, true, 805L, 780L, 5900);

		// 28. Magic potion
		register(ItemID.MAGIC_POTION4, "Magic potion(4)", 2000, true, true, 1640L, 1585L, 38000);
		register(ItemID.MAGIC_POTION3, "Magic potion(3)", 2000, true, true, 1220L, 1175L, 34000);
		register(ItemID.MAGIC_POTION2, "Magic potion(2)", 2000, true, true, 800L, 765L, 3200);
		register(ItemID.MAGIC_POTION1, "Magic potion(1)", 2000, true, true, 400L, 380L, 2100);

		// 29. Stamina potion
		register(ItemID.STAMINA_POTION4, "Stamina potion(4)", 2000, true, true, 7850L, 7790L, 128000);
		register(ItemID.STAMINA_POTION3, "Stamina potion(3)", 2000, true, true, 5870L, 5810L, 62000);
		register(ItemID.STAMINA_POTION2, "Stamina potion(2)", 2000, true, true, 3890L, 3840L, 14500);
		register(ItemID.STAMINA_POTION1, "Stamina potion(1)", 2000, true, true, 1940L, 1910L, 11200);

		// 30. Zamorak brew
		register(ItemID.ZAMORAK_BREW4, "Zamorak brew(4)", 2000, true, true, 4680L, 4560L, 22000);
		register(ItemID.ZAMORAK_BREW3, "Zamorak brew(3)", 2000, true, true, 3490L, 3400L, 24000);
		register(ItemID.ZAMORAK_BREW2, "Zamorak brew(2)", 2000, true, true, 2310L, 2240L, 2600);
		register(ItemID.ZAMORAK_BREW1, "Zamorak brew(1)", 2000, true, true, 1150L, 1110L, 1800);

		// 31. Antidote++
		register(ItemID.ANTIDOTE4_5952, "Antidote++(4)", 2000, true, true, 4420L, 4340L, 38000);
		register(ItemID.ANTIDOTE3_5954, "Antidote++(3)", 2000, true, true, 3310L, 3240L, 29000);
		register(ItemID.ANTIDOTE2_5956, "Antidote++(2)", 2000, true, true, 2190L, 2140L, 3400);
		register(ItemID.ANTIDOTE1_5958, "Antidote++(1)", 2000, true, true, 1090L, 1060L, 2200);

		// 32. Bastion potion
		register(ItemID.BASTION_POTION4, "Bastion potion(4)", 2000, true, true, 24900L, 24600L, 42000);
		register(ItemID.BASTION_POTION3, "Bastion potion(3)", 2000, true, true, 18650L, 18400L, 28000);
		register(ItemID.BASTION_POTION2, "Bastion potion(2)", 2000, true, true, 12350L, 12150L, 4100);
		register(ItemID.BASTION_POTION1, "Bastion potion(1)", 2000, true, true, 6150L, 6020L, 2900);

		// 33. Battlemage potion
		register(ItemID.BATTLEMAGE_POTION4, "Battlemage potion(4)", 2000, true, true, 23800L, 23500L, 26000);
		register(ItemID.BATTLEMAGE_POTION3, "Battlemage potion(3)", 2000, true, true, 17800L, 17550L, 19500);
		register(ItemID.BATTLEMAGE_POTION2, "Battlemage potion(2)", 2000, true, true, 11800L, 11600L, 2800);
		register(ItemID.BATTLEMAGE_POTION1, "Battlemage potion(1)", 2000, true, true, 5900L, 5780L, 1900);

		// 34. Saradomin brew
		register(ItemID.SARADOMIN_BREW4, "Saradomin brew(4)", 2000, true, true, 12850L, 12760L, 185000);
		register(ItemID.SARADOMIN_BREW3, "Saradomin brew(3)", 2000, true, true, 9620L, 9540L, 124000);
		register(ItemID.SARADOMIN_BREW2, "Saradomin brew(2)", 2000, true, true, 6390L, 6320L, 16500);
		register(ItemID.SARADOMIN_BREW1, "Saradomin brew(1)", 2000, true, true, 3190L, 3150L, 11800);

		// 35. Surge potion
		register(ItemID.SURGE_POTION4, "Surge potion(4)", 2000, true, true, 11900L, 11650L, 19000);
		register(ItemID.SURGE_POTION3, "Surge potion(3)", 2000, true, true, 8900L, 8700L, 14000);
		register(ItemID.SURGE_POTION2, "Surge potion(2)", 2000, true, true, 5900L, 5750L, 2100);
		register(ItemID.SURGE_POTION1, "Surge potion(1)", 2000, true, true, 2950L, 2860L, 1400);

		// 36. Extended antifire
		register(ItemID.EXTENDED_ANTIFIRE4, "Extended antifire(4)", 2000, true, true, 3920L, 3850L, 48000);
		register(ItemID.EXTENDED_ANTIFIRE3, "Extended antifire(3)", 2000, true, true, 2930L, 2870L, 26000);
		register(ItemID.EXTENDED_ANTIFIRE2, "Extended antifire(2)", 2000, true, true, 1940L, 1890L, 4200);
		register(ItemID.EXTENDED_ANTIFIRE1, "Extended antifire(1)", 2000, true, true, 970L, 940L, 2900);

		// 37. Ancient brew
		register(ItemID.ANCIENT_BREW4, "Ancient brew(4)", 2000, true, true, 4950L, 4860L, 44000);
		register(ItemID.ANCIENT_BREW3, "Ancient brew(3)", 2000, true, true, 3700L, 3630L, 36000);
		register(ItemID.ANCIENT_BREW2, "Ancient brew(2)", 2000, true, true, 2450L, 2390L, 3800);
		register(ItemID.ANCIENT_BREW1, "Ancient brew(1)", 2000, true, true, 1220L, 1185L, 2500);

		// 38. Anti-venom
		register(ItemID.ANTIVENOM4, "Anti-venom(4)", 2000, true, true, 7620L, 7510L, 46000);
		register(ItemID.ANTIVENOM3, "Anti-venom(3)", 2000, true, true, 5700L, 5610L, 28000);
		register(ItemID.ANTIVENOM2, "Anti-venom(2)", 2000, true, true, 3780L, 3710L, 3900);
		register(ItemID.ANTIVENOM1, "Anti-venom(1)", 2000, true, true, 1890L, 1845L, 2700);

		// 39. Menaphite remedy
		register(ItemID.MENAPHITE_REMEDY4, "Menaphite remedy(4)", 2000, true, true, 13800L, 13550L, 14500);
		register(ItemID.MENAPHITE_REMEDY3, "Menaphite remedy(3)", 2000, true, true, 10300L, 10100L, 11200);
		register(ItemID.MENAPHITE_REMEDY2, "Menaphite remedy(2)", 2000, true, true, 6850L, 6700L, 1800);
		register(ItemID.MENAPHITE_REMEDY1, "Menaphite remedy(1)", 2000, true, true, 3420L, 3340L, 1200);

		// 40. Super combat potion
		register(ItemID.SUPER_COMBAT_POTION4, "Super combat potion(4)", 2000, true, true, 12950L, 12860L, 136000);
		register(ItemID.SUPER_COMBAT_POTION3, "Super combat potion(3)", 2000, true, true, 9690L, 9610L, 68000);
		register(ItemID.SUPER_COMBAT_POTION2, "Super combat potion(2)", 2000, true, true, 6440L, 6370L, 11500);
		register(ItemID.SUPER_COMBAT_POTION1, "Super combat potion(1)", 2000, true, true, 3220L, 3180L, 8400);

		// 41. Forgotten brew
		register(ItemID.FORGOTTEN_BREW4, "Forgotten brew(4)", 2000, true, true, 6150L, 6040L, 32000);
		register(ItemID.FORGOTTEN_BREW3, "Forgotten brew(3)", 2000, true, true, 4600L, 4510L, 21000);
		register(ItemID.FORGOTTEN_BREW2, "Forgotten brew(2)", 2000, true, true, 3050L, 2980L, 3400);
		register(ItemID.FORGOTTEN_BREW1, "Forgotten brew(1)", 2000, true, true, 1520L, 1480L, 2300);

		// 42. Super antifire potion
		register(ItemID.SUPER_ANTIFIRE_POTION4, "Super antifire potion(4)", 2000, true, true, 17400L, 17150L, 24000);
		register(ItemID.SUPER_ANTIFIRE_POTION3, "Super antifire potion(3)", 2000, true, true, 13000L, 12800L, 14500);
		register(ItemID.SUPER_ANTIFIRE_POTION2, "Super antifire potion(2)", 2000, true, true, 8650L, 8500L, 2600);
		register(ItemID.SUPER_ANTIFIRE_POTION1, "Super antifire potion(1)", 2000, true, true, 4320L, 4240L, 1800);

		// 43. Anti-venom+
		register(ItemID.ANTIVENOM4_12913, "Anti-venom+(4)", 2000, true, true, 11850L, 11720L, 49000);
		register(ItemID.ANTIVENOM3_12915, "Anti-venom+(3)", 2000, true, true, 8860L, 8750L, 27000);
		register(ItemID.ANTIVENOM2_12917, "Anti-venom+(2)", 2000, true, true, 5890L, 5810L, 4400);
		register(ItemID.ANTIVENOM1_12919, "Anti-venom+(1)", 2000, true, true, 2940L, 2890L, 3100);

		// 44. Extended anti-venom+
		register(ItemID.EXTENDED_ANTIVENOM4, "Extended anti-venom+(4)", 2000, true, true, 21900L, 21600L, 34000);
		register(ItemID.EXTENDED_ANTIVENOM3, "Extended anti-venom+(3)", 2000, true, true, 16400L, 16150L, 18500);
		register(ItemID.EXTENDED_ANTIVENOM2, "Extended anti-venom+(2)", 2000, true, true, 10900L, 10700L, 3200);
		register(ItemID.EXTENDED_ANTIVENOM1, "Extended anti-venom+(1)", 2000, true, true, 5450L, 5340L, 2100);

		// 45. Extended super antifire
		register(ItemID.EXTENDED_SUPER_ANTIFIRE4, "Extended super antifire(4)", 2000, true, true, 19500L, 19250L, 29000);
		register(ItemID.EXTENDED_SUPER_ANTIFIRE3, "Extended super antifire(3)", 2000, true, true, 14600L, 14400L, 16500);
		register(ItemID.EXTENDED_SUPER_ANTIFIRE2, "Extended super antifire(2)", 2000, true, true, 9700L, 9550L, 2800);
		register(ItemID.EXTENDED_SUPER_ANTIFIRE1, "Extended super antifire(1)", 2000, true, true, 4850L, 4760L, 1900);

		// 46. Divine Potions
		register(ItemID.DIVINE_SUPER_ATTACK_POTION4, "Divine super attack potion(4)", 2000, true, true, 2850L, 2720L, 6800);
		register(ItemID.DIVINE_SUPER_ATTACK_POTION3, "Divine super attack potion(3)", 2000, true, true, 2120L, 2010L, 3400);
		register(ItemID.DIVINE_SUPER_ATTACK_POTION2, "Divine super attack potion(2)", 2000, true, true, 1410L, 1330L, 900);
		register(ItemID.DIVINE_SUPER_ATTACK_POTION1, "Divine super attack potion(1)", 2000, true, true, 705L, 660L, 600);

		register(ItemID.DIVINE_SUPER_STRENGTH_POTION4, "Divine super strength potion(4)", 2000, true, true, 5450L, 5310L, 9200);
		register(ItemID.DIVINE_SUPER_STRENGTH_POTION3, "Divine super strength potion(3)", 2000, true, true, 4080L, 3960L, 4800);
		register(ItemID.DIVINE_SUPER_STRENGTH_POTION2, "Divine super strength potion(2)", 2000, true, true, 2710L, 2620L, 1100);
		register(ItemID.DIVINE_SUPER_STRENGTH_POTION1, "Divine super strength potion(1)", 2000, true, true, 1350L, 1300L, 750);

		register(ItemID.DIVINE_SUPER_DEFENCE_POTION4, "Divine super defence potion(4)", 2000, true, true, 4980L, 4840L, 5200);
		register(ItemID.DIVINE_SUPER_DEFENCE_POTION3, "Divine super defence potion(3)", 2000, true, true, 3720L, 3610L, 2900);
		register(ItemID.DIVINE_SUPER_DEFENCE_POTION2, "Divine super defence potion(2)", 2000, true, true, 2470L, 2390L, 700);
		register(ItemID.DIVINE_SUPER_DEFENCE_POTION1, "Divine super defence potion(1)", 2000, true, true, 1235L, 1190L, 450);

		register(ItemID.DIVINE_RANGING_POTION4, "Divine ranging potion(4)", 2000, true, true, 5120L, 5020L, 64000);
		register(ItemID.DIVINE_RANGING_POTION3, "Divine ranging potion(3)", 2000, true, true, 3830L, 3750L, 32000);
		register(ItemID.DIVINE_RANGING_POTION2, "Divine ranging potion(2)", 2000, true, true, 2540L, 2480L, 5800);
		register(ItemID.DIVINE_RANGING_POTION1, "Divine ranging potion(1)", 2000, true, true, 1270L, 1235L, 4100);

		register(ItemID.DIVINE_MAGIC_POTION4, "Divine magic potion(4)", 2000, true, true, 3480L, 3380L, 12500);
		register(ItemID.DIVINE_MAGIC_POTION3, "Divine magic potion(3)", 2000, true, true, 2600L, 2520L, 6800);
		register(ItemID.DIVINE_MAGIC_POTION2, "Divine magic potion(2)", 2000, true, true, 1720L, 1660L, 1400);
		register(ItemID.DIVINE_MAGIC_POTION1, "Divine magic potion(1)", 2000, true, true, 860L, 825L, 950);

		register(ItemID.DIVINE_BASTION_POTION4, "Divine bastion potion(4)", 2000, true, true, 26900L, 26550L, 38000);
		register(ItemID.DIVINE_BASTION_POTION3, "Divine bastion potion(3)", 2000, true, true, 20150L, 19880L, 21000);
		register(ItemID.DIVINE_BASTION_POTION2, "Divine bastion potion(2)", 2000, true, true, 13400L, 13200L, 3600);
		register(ItemID.DIVINE_BASTION_POTION1, "Divine bastion potion(1)", 2000, true, true, 6700L, 6580L, 2400);

		register(ItemID.DIVINE_BATTLEMAGE_POTION4, "Divine battlemage potion(4)", 2000, true, true, 25700L, 25350L, 19500);
		register(ItemID.DIVINE_BATTLEMAGE_POTION3, "Divine battlemage potion(3)", 2000, true, true, 19250L, 18980L, 11200);
		register(ItemID.DIVINE_BATTLEMAGE_POTION2, "Divine battlemage potion(2)", 2000, true, true, 12800L, 12600L, 2100);
		register(ItemID.DIVINE_BATTLEMAGE_POTION1, "Divine battlemage potion(1)", 2000, true, true, 6400L, 6280L, 1400);

		register(ItemID.DIVINE_SUPER_COMBAT_POTION4, "Divine super combat potion(4)", 2000, true, true, 14950L, 14840L, 118000);
		register(ItemID.DIVINE_SUPER_COMBAT_POTION3, "Divine super combat potion(3)", 2000, true, true, 11200L, 11100L, 54000);
		register(ItemID.DIVINE_SUPER_COMBAT_POTION2, "Divine super combat potion(2)", 2000, true, true, 7450L, 7380L, 9800);
		register(ItemID.DIVINE_SUPER_COMBAT_POTION1, "Divine super combat potion(1)", 2000, true, true, 3720L, 3680L, 6900);

		// 47. Moonlight potion (Moons of Peril minigame — untradeable)
		register(ItemID.MOONLIGHT_POTION4, "Moonlight potion(4)", 0, true, false, 0L, 0L, 0);
		register(ItemID.MOONLIGHT_POTION3, "Moonlight potion(3)", 0, true, false, 0L, 0L, 0);
		register(ItemID.MOONLIGHT_POTION2, "Moonlight potion(2)", 0, true, false, 0L, 0L, 0);
		register(ItemID.MOONLIGHT_POTION1, "Moonlight potion(1)", 0, true, false, 0L, 0L, 0);
	}

	private ItemIdMappings()
	{
	}

	private static void register(
		int itemId,
		String name,
		int buyLimit,
		boolean members,
		boolean tradeable,
		long cachedHighPrice,
		long cachedLowPrice,
		int cachedHourlyVolume)
	{
		ITEMS.put(itemId, new ItemDefinition(
			itemId,
			name,
			buyLimit,
			members,
			tradeable,
			cachedHighPrice,
			cachedLowPrice,
			cachedHourlyVolume
		));
	}

	public static ItemDefinition getDefinition(int itemId)
	{
		return ITEMS.get(itemId);
	}

	public static String getItemName(int itemId)
	{
		ItemDefinition def = ITEMS.get(itemId);
		return def != null ? def.getName() : ("Item #" + itemId);
	}

	public static boolean isKnownItemId(int itemId)
	{
		return itemId > 0 && ITEMS.containsKey(itemId);
	}

	public static boolean isTradeable(int itemId)
	{
		ItemDefinition def = ITEMS.get(itemId);
		return def != null && def.isTradeable();
	}

	public static int getBuyLimit(int itemId)
	{
		ItemDefinition def = ITEMS.get(itemId);
		return def != null ? def.getBuyLimit() : 2000;
	}

	public static Set<Integer> getAllMappedItemIds()
	{
		return Collections.unmodifiableSet(ITEMS.keySet());
	}

	public static Map<Integer, ItemDefinition> getAllDefinitions()
	{
		return Collections.unmodifiableMap(ITEMS);
	}

	/**
	 * Builds a clearly-marked cached snapshot {@link ItemPrice} map (`fromCache = true`)
	 * used only when the live OSRS Wiki API is unreachable and no newer disk cache exists.
	 * Untradeable items are never given artificial prices.
	 */
	public static Map<Integer, ItemPrice> buildVerifiedFallbackCacheSnapshot(long snapshotEpochSec)
	{
		Map<Integer, ItemPrice> fallback = new LinkedHashMap<>();
		for (ItemDefinition def : ITEMS.values())
		{
			if (!def.isTradeable() || def.getCachedHighPrice() <= 0L)
			{
				continue;
			}
			long high = def.getCachedHighPrice();
			long low = def.getCachedLowPrice() > 0L ? def.getCachedLowPrice() : high;
			int vol1h = def.getCachedHourlyVolume();
			int vol5m = Math.max(1, vol1h / 12);

			ItemPrice price = ItemPrice.builder()
				.itemId(def.getItemId())
				.itemName(def.getName())
				.members(def.isMembers())
				.buyLimit(def.getBuyLimit())
				.instantBuyPrice(high)
				.instantBuyTimestampSec(snapshotEpochSec)
				.instantSellPrice(low)
				.instantSellTimestampSec(snapshotEpochSec)
				.avgHighPrice5m(high)
				.avgLowPrice5m(low)
				.highVolume5m(vol5m / 2)
				.lowVolume5m(vol5m - (vol5m / 2))
				.avgHighPrice1h(high)
				.avgLowPrice1h(low)
				.highVolume1h(vol1h / 2)
				.lowVolume1h(vol1h - (vol1h / 2))
				.fetchedAtEpochSec(snapshotEpochSec)
				.fromCache(true)
				.build();
			fallback.put(def.getItemId(), price);
		}
		return fallback;
	}
}
