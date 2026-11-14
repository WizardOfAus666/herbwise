package com.herbwise.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a verified OSRS Herblore potion recipe, including its required Herblore level,
 * XP granted, output potion item ID, dose count, ingredient list, and preparation method.
 */
public class PotionRecipe
{
	private final String recipeId;
	private final String familyName;
	private final String potionName;
	private final int potionItemId;
	private final int doses;
	private final int outputQuantity;
	private final int requiredLevel;
	private final double xpGained;
	private final List<PotionIngredient> ingredients;
	private final int unfinishedPotionId;
	private final String unfinishedPotionName;
	private final String secondarySummary;
	private final RecipeCategory category;
	private final boolean alternativeRecipe;
	private final boolean unfinishedPotionRecipe;
	private final boolean tradeableOutput;
	private final int potionsPerHourEstimate;
	private final String preparationNotes;
	private final boolean verified;

	private PotionRecipe(Builder builder)
	{
		this.recipeId = builder.recipeId;
		this.familyName = builder.familyName != null ? builder.familyName : builder.potionName;
		this.potionName = builder.potionName;
		this.potionItemId = builder.potionItemId;
		this.doses = builder.doses;
		this.outputQuantity = builder.outputQuantity > 0 ? builder.outputQuantity : 1;
		this.requiredLevel = builder.requiredLevel;
		this.xpGained = builder.xpGained;
		this.ingredients = builder.ingredients != null
			? Collections.unmodifiableList(new ArrayList<>(builder.ingredients))
			: Collections.emptyList();
		this.unfinishedPotionId = builder.unfinishedPotionId;
		this.unfinishedPotionName = builder.unfinishedPotionName != null ? builder.unfinishedPotionName : "None";
		this.secondarySummary = builder.secondarySummary != null ? builder.secondarySummary : "";
		this.category = builder.category != null ? builder.category : RecipeCategory.COMBAT_POTION;
		this.alternativeRecipe = builder.alternativeRecipe;
		this.unfinishedPotionRecipe = builder.unfinishedPotionRecipe
			|| (this.category == RecipeCategory.UNFINISHED_POTION);
		this.tradeableOutput = builder.tradeableOutput;
		this.potionsPerHourEstimate = builder.potionsPerHourEstimate > 0 ? builder.potionsPerHourEstimate : 2500;
		this.preparationNotes = builder.preparationNotes != null ? builder.preparationNotes : "";
		this.verified = builder.verified;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public String getRecipeId()
	{
		return recipeId;
	}

	public String getFamilyName()
	{
		return familyName;
	}

	public String getPotionName()
	{
		return potionName;
	}

	public int getPotionItemId()
	{
		return potionItemId;
	}

	public int getDoses()
	{
		return doses;
	}

	public int getOutputQuantity()
	{
		return outputQuantity;
	}

	public int getRequiredLevel()
	{
		return requiredLevel;
	}

	public double getXpGained()
	{
		return xpGained;
	}

	public List<PotionIngredient> getIngredients()
	{
		return ingredients;
	}

	public int getUnfinishedPotionId()
	{
		return unfinishedPotionId;
	}

	public String getUnfinishedPotionName()
	{
		return unfinishedPotionName;
	}

	public String getSecondarySummary()
	{
		return secondarySummary;
	}

	public RecipeCategory getCategory()
	{
		return category;
	}

	public boolean isAlternativeRecipe()
	{
		return alternativeRecipe;
	}

	public boolean isUnfinishedPotionRecipe()
	{
		return unfinishedPotionRecipe;
	}

	public boolean isTradeableOutput()
	{
		return tradeableOutput;
	}

	public int getPotionsPerHourEstimate()
	{
		return potionsPerHourEstimate;
	}

	public String getPreparationNotes()
	{
		return preparationNotes;
	}

	public boolean isVerified()
	{
		return verified;
	}

	/**
	 * Checks whether all ingredients and the finished potion output are tradeable on the Grand Exchange.
	 */
	public boolean isFullyTradeable()
	{
		if (!tradeableOutput)
		{
			return false;
		}
		for (PotionIngredient ing : ingredients)
		{
			if (!ing.isTradeable())
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Formats a concise summary of all ingredients required.
	 */
	public String getFormattedIngredientList()
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ingredients.size(); i++)
		{
			if (i > 0)
			{
				sb.append(" + ");
			}
			sb.append(ingredients.get(i).toString());
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof PotionRecipe)) return false;
		PotionRecipe that = (PotionRecipe) o;
		return Objects.equals(recipeId, that.recipeId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(recipeId);
	}

	@Override
	public String toString()
	{
		return potionName + " (Lvl " + requiredLevel + ", " + xpGained + " XP)";
	}

	public static class Builder
	{
		private String recipeId;
		private String familyName;
		private String potionName;
		private int potionItemId;
		private int doses = 4;
		private int outputQuantity = 1;
		private int requiredLevel = 1;
		private double xpGained = 0.0;
		private List<PotionIngredient> ingredients = new ArrayList<>();
		private int unfinishedPotionId = 0;
		private String unfinishedPotionName = "None";
		private String secondarySummary = "";
		private RecipeCategory category = RecipeCategory.COMBAT_POTION;
		private boolean alternativeRecipe = false;
		private boolean unfinishedPotionRecipe = false;
		private boolean tradeableOutput = true;
		private int potionsPerHourEstimate = 2500;
		private String preparationNotes = "";
		private boolean verified = true;

		public Builder recipeId(String recipeId) { this.recipeId = recipeId; return this; }
		public Builder familyName(String familyName) { this.familyName = familyName; return this; }
		public Builder potionName(String potionName) { this.potionName = potionName; return this; }
		public Builder potionItemId(int potionItemId) { this.potionItemId = potionItemId; return this; }
		public Builder doses(int doses) { this.doses = doses; return this; }
		public Builder outputQuantity(int outputQuantity) { this.outputQuantity = outputQuantity; return this; }
		public Builder requiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; return this; }
		public Builder xpGained(double xpGained) { this.xpGained = xpGained; return this; }
		public Builder ingredients(List<PotionIngredient> ingredients) { this.ingredients = ingredients; return this; }
		public Builder addIngredient(PotionIngredient ingredient) { this.ingredients.add(ingredient); return this; }
		public Builder unfinishedPotionId(int unfinishedPotionId) { this.unfinishedPotionId = unfinishedPotionId; return this; }
		public Builder unfinishedPotionName(String unfinishedPotionName) { this.unfinishedPotionName = unfinishedPotionName; return this; }
		public Builder secondarySummary(String secondarySummary) { this.secondarySummary = secondarySummary; return this; }
		public Builder category(RecipeCategory category) { this.category = category; return this; }
		public Builder alternativeRecipe(boolean alternativeRecipe) { this.alternativeRecipe = alternativeRecipe; return this; }
		public Builder unfinishedPotionRecipe(boolean unfinishedPotionRecipe) { this.unfinishedPotionRecipe = unfinishedPotionRecipe; return this; }
		public Builder tradeableOutput(boolean tradeableOutput) { this.tradeableOutput = tradeableOutput; return this; }
		public Builder potionsPerHourEstimate(int potionsPerHourEstimate) { this.potionsPerHourEstimate = potionsPerHourEstimate; return this; }
		public Builder preparationNotes(String preparationNotes) { this.preparationNotes = preparationNotes; return this; }
		public Builder verified(boolean verified) { this.verified = verified; return this; }

		public PotionRecipe build()
		{
			return new PotionRecipe(this);
		}
	}
}
