package com.herbwise.services;

import com.herbwise.data.HerbloreRecipes;
import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.RecipeCategory;
import com.herbwise.services.WikiRecipeService.ValidationResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.ItemID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link WikiRecipeService} and {@link HerbloreRecipes}
 * verifying the complete potion database (Section 2, Section 3, and Section 17).
 */
public class RecipeValidationTest
{
	private WikiRecipeService recipeService;

	@Before
	public void setUp()
	{
		recipeService = new WikiRecipeService();
	}

	@Test
	public void testAllDatabaseRecipesAreVerifiedWithZeroValidationErrors()
	{
		List<ValidationResult> invalidReports = recipeService.getValidationReport().stream()
			.filter(r -> !r.isValid())
			.collect(Collectors.toList());

		Assert.assertTrue(
			"Rejected recipe count must be 0, but found invalid recipes: " + invalidReports.size(),
			invalidReports.isEmpty()
		);
		Assert.assertEquals(HerbloreRecipes.getAllRecipes().size(), recipeService.getAllVerifiedRecipes().size());
		Assert.assertTrue("Database should contain at least 110 verified recipe entries",
			recipeService.getAllVerifiedRecipes().size() >= 110);
	}

	@Test
	public void testAllRequiredPotionFamiliesAreSupported()
	{
		List<PotionRecipe> all = recipeService.getAllVerifiedRecipes();
		String joinedNames = all.stream()
			.map(r -> (r.getPotionName() + " " + r.getFamilyName()).toLowerCase(Locale.ROOT))
			.collect(Collectors.joining(" | "));

		String[] requiredFamilies = new String[]{
			"attack potion",
			"antipoison",
			"strength potion",
			"serum 207",
			"restore potion",
			"energy potion",
			"defence potion",
			"agility potion",
			"combat potion",
			"prayer potion",
			"super attack",
			"superantipoison",
			"fishing potion",
			"super energy",
			"hunter potion",
			"super strength",
			"weapon poison",
			"super restore",
			"super defence",
			"antifire potion",
			"ranging potion",
			"magic potion",
			"zamorak brew",
			"saradomin brew",
			"anti-venom",
			"extended antifire",
			"divine super combat",
			"bastion potion",
			"battlemage potion",
			"ancient brew",
			"forgotten brew",
			"menaphite remedy",
			"moonlight potion"
		};

		for (String required : requiredFamilies)
		{
			Assert.assertTrue(
				"Missing required potion family from Section 3: " + required,
				joinedNames.contains(required)
			);
		}
	}

	@Test
	public void testFourThreeTwoOneDoseVariantsHaveDistinctItemIds()
	{
		List<PotionRecipe> prayerVariants = recipeService.getAllVerifiedRecipes().stream()
			.filter(r -> r.getFamilyName().equals("Prayer potion") && r.getRecipeId().endsWith("_unf"))
			.collect(Collectors.toList());

		Assert.assertEquals(4, prayerVariants.size());
		Set<Integer> distinctIds = prayerVariants.stream()
			.map(PotionRecipe::getPotionItemId)
			.collect(Collectors.toSet());
		Assert.assertEquals(4, distinctIds.size());
	}

	@Test
	public void testUnfinishedPotionsClearlyCategorisedAndSeparatedFromCompletedRecommendations()
	{
		List<PotionRecipe> unfinished = recipeService.getAllVerifiedRecipes().stream()
			.filter(PotionRecipe::isUnfinishedPotionRecipe)
			.collect(Collectors.toList());
		Assert.assertFalse("Unfinished potion recipes must be present", unfinished.isEmpty());
		for (PotionRecipe r : unfinished)
		{
			Assert.assertEquals(RecipeCategory.UNFINISHED_POTION, r.getCategory());
			Assert.assertEquals(0.0, r.getXpGained(), 0.001);
		}

		List<PotionRecipe> completed = recipeService.getVerifiedCompletedRecipes(true);
		for (PotionRecipe r : completed)
		{
			Assert.assertNotEquals(RecipeCategory.UNFINISHED_POTION, r.getCategory());
			Assert.assertFalse(r.isUnfinishedPotionRecipe());
		}
	}

	@Test
	public void testValidationSystemDetectsIncompleteAndInvalidRecipes()
	{
		// 1. Missing ingredients
		PotionRecipe missingIngr = PotionRecipe.builder()
			.recipeId("broken_no_ingr")
			.potionName("Broken Potion(4)")
			.familyName("Broken")
			.potionItemId(ItemID.PRAYER_POTION4)
			.doses(4)
			.requiredLevel(38)
			.xpGained(87.5)
			.outputQuantity(1)
			.category(RecipeCategory.RESTORE_PRAYER)
			.ingredients(Collections.emptyList())
			.verified(true)
			.build();
		ValidationResult res1 = recipeService.validateRecipe(missingIngr);
		Assert.assertFalse(res1.isValid());

		// 2. Unknown / invalid item ID (-999)
		PotionRecipe invalidItemId = PotionRecipe.builder()
			.recipeId("broken_bad_id")
			.potionName("Broken Potion(4)")
			.familyName("Broken")
			.potionItemId(-999)
			.doses(4)
			.requiredLevel(38)
			.xpGained(87.5)
			.outputQuantity(1)
			.category(RecipeCategory.RESTORE_PRAYER)
			.ingredients(Arrays.asList(new PotionIngredient(ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)", 1)))
			.verified(true)
			.build();
		ValidationResult res2 = recipeService.validateRecipe(invalidItemId);
		Assert.assertFalse(res2.isValid());

		// 3. Invalid Herblore level (level 120)
		PotionRecipe invalidLevel = PotionRecipe.builder()
			.recipeId("broken_bad_lvl")
			.potionName("Broken Potion(4)")
			.familyName("Broken")
			.potionItemId(ItemID.PRAYER_POTION4)
			.doses(4)
			.requiredLevel(120)
			.xpGained(87.5)
			.outputQuantity(1)
			.category(RecipeCategory.RESTORE_PRAYER)
			.ingredients(Arrays.asList(new PotionIngredient(ItemID.RANARR_POTION_UNF, "Ranarr potion (unf)", 1)))
			.verified(true)
			.build();
		ValidationResult res3 = recipeService.validateRecipe(invalidLevel);
		Assert.assertFalse(res3.isValid());
	}
}
