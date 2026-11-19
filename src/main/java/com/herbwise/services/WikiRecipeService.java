package com.herbwise.services;

import com.herbwise.data.HerbloreRecipes;
import com.herbwise.data.ItemIdMappings;
import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Singleton;

/**
 * Manages OSRS Wiki Herblore recipe data and executes strict structural and item-ID validation.
 * Any recipe that fails validation (missing ingredients, invalid item IDs, invalid level/XP, or unverified flag)
 * is excluded from confirmed recommendations.
 */
@Singleton
public class WikiRecipeService
{
	public static class ValidationResult
	{
		private final PotionRecipe recipe;
		private final boolean valid;
		private final List<String> errors;

		public ValidationResult(PotionRecipe recipe, boolean valid, List<String> errors)
		{
			this.recipe = recipe;
			this.valid = valid;
			this.errors = errors != null
				? Collections.unmodifiableList(new ArrayList<>(errors))
				: Collections.emptyList();
		}

		public PotionRecipe getRecipe() { return recipe; }
		public boolean isValid() { return valid; }
		public List<String> getErrors() { return errors; }
	}

	private final List<PotionRecipe> verifiedRecipes;
	private final List<ValidationResult> validationReport;
	private final Set<Integer> requiredUniqueItemIds;

	public WikiRecipeService()
	{
		List<PotionRecipe> raw = HerbloreRecipes.getAllRecipes();
		List<PotionRecipe> validList = new ArrayList<>(raw.size());
		List<ValidationResult> reportList = new ArrayList<>(raw.size());
		Set<Integer> uniqueIds = new LinkedHashSet<>();

		for (PotionRecipe recipe : raw)
		{
			ValidationResult res = validateRecipe(recipe);
			reportList.add(res);
			if (res.isValid())
			{
				validList.add(recipe);
				uniqueIds.add(recipe.getPotionItemId());
				for (PotionIngredient ing : recipe.getIngredients())
				{
					if (ing != null && ing.getItemId() > 0 && ing.isTradeable())
					{
						uniqueIds.add(ing.getItemId());
					}
				}
			}
		}

		this.verifiedRecipes = Collections.unmodifiableList(validList);
		this.validationReport = Collections.unmodifiableList(reportList);
		this.requiredUniqueItemIds = Collections.unmodifiableSet(uniqueIds);
	}

	/**
	 * Validates a single {@link PotionRecipe} for missing ingredients, invalid item IDs,
	 * invalid quantities, out-of-range Herblore levels, or negative XP.
	 */
	public ValidationResult validateRecipe(PotionRecipe recipe)
	{
		List<String> errors = new ArrayList<>();
		if (recipe == null)
		{
			errors.add("Recipe is null.");
			return new ValidationResult(null, false, errors);
		}
		if (!recipe.isVerified())
		{
			errors.add("Recipe is marked as unverified.");
		}
		if (recipe.getRecipeId() == null || recipe.getRecipeId().trim().isEmpty())
		{
			errors.add("Recipe ID is missing.");
		}
		if (recipe.getPotionName() == null || recipe.getPotionName().trim().isEmpty())
		{
			errors.add("Potion name is missing.");
		}
		if (recipe.getPotionItemId() <= 0 || !ItemIdMappings.isKnownItemId(recipe.getPotionItemId()))
		{
			errors.add("Invalid or unknown output potion item ID: " + recipe.getPotionItemId());
		}
		if (recipe.getDoses() < 1 || recipe.getDoses() > 4)
		{
			errors.add("Invalid dose count: " + recipe.getDoses());
		}
		if (recipe.getOutputQuantity() <= 0)
		{
			errors.add("Output quantity must be >= 1.");
		}
		if (recipe.getRequiredLevel() < 1 || recipe.getRequiredLevel() > 99)
		{
			errors.add("Required Herblore level out of range (1..99): " + recipe.getRequiredLevel());
		}
		if (recipe.isUnfinishedPotionRecipe())
		{
			if (recipe.getXpGained() < 0.0)
			{
				errors.add("Unfinished potion XP cannot be negative.");
			}
		}
		else
		{
			if (recipe.getXpGained() <= 0.0)
			{
				errors.add("Completed potion recipe must grant > 0 Herblore XP.");
			}
		}

		List<PotionIngredient> ingredients = recipe.getIngredients();
		if (ingredients == null || ingredients.isEmpty())
		{
			errors.add("Recipe has no ingredients.");
		}
		else
		{
			for (PotionIngredient ing : ingredients)
			{
				if (ing == null)
				{
					errors.add("Null ingredient entry.");
					continue;
				}
				if (ing.getName() == null || ing.getName().trim().isEmpty())
				{
					errors.add("Ingredient name is empty for item ID " + ing.getItemId());
				}
				if (ing.getItemId() <= 0 || !ItemIdMappings.isKnownItemId(ing.getItemId()))
				{
					errors.add("Invalid or unmapped ingredient item ID: " + ing.getItemId());
				}
				if (ing.getQuantity() <= 0)
				{
					errors.add("Ingredient quantity must be > 0 for " + ing.getName());
				}
			}
		}

		return new ValidationResult(recipe, errors.isEmpty(), errors);
	}

	/**
	 * Returns all verified recipes (including dose variants, alternative methods, and unfinished potions).
	 */
	public List<PotionRecipe> getAllVerifiedRecipes()
	{
		return verifiedRecipes;
	}

	/**
	 * Returns verified completed potion recipes (excludes 0-XP unfinished potions).
	 */
	public List<PotionRecipe> getVerifiedCompletedRecipes(boolean includeAlternativesAndDoseVariants)
	{
		List<PotionRecipe> list = new ArrayList<>();
		for (PotionRecipe r : verifiedRecipes)
		{
			if (r.isUnfinishedPotionRecipe())
			{
				continue;
			}
			if (!includeAlternativesAndDoseVariants && r.isAlternativeRecipe())
			{
				continue;
			}
			list.add(r);
		}
		return list;
	}

	public List<ValidationResult> getValidationReport()
	{
		return validationReport;
	}

	/**
	 * Returns the deduplicated set of all tradeable item IDs required across all verified Herblore recipes.
	 */
	public Set<Integer> getRequiredUniqueItemIds()
	{
		return requiredUniqueItemIds;
	}
}
