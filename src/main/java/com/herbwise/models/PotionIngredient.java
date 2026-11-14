package com.herbwise.models;

import java.util.Objects;

/**
 * Represents a single required ingredient and quantity in a Herblore recipe.
 */
public class PotionIngredient
{
	private final int itemId;
	private final String name;
	private final int quantity;
	private final boolean tradeable;
	private final String role;

	public PotionIngredient(int itemId, String name, int quantity, boolean tradeable, String role)
	{
		this.itemId = itemId;
		this.name = name != null ? name : "Unknown Item";
		this.quantity = quantity;
		this.tradeable = tradeable;
		this.role = role != null ? role : "Ingredient";
	}

	public PotionIngredient(int itemId, String name, int quantity)
	{
		this(itemId, name, quantity, true, "Ingredient");
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getName()
	{
		return name;
	}

	public int getQuantity()
	{
		return quantity;
	}

	public boolean isTradeable()
	{
		return tradeable;
	}

	public String getRole()
	{
		return role;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof PotionIngredient)) return false;
		PotionIngredient that = (PotionIngredient) o;
		return itemId == that.itemId && quantity == that.quantity && tradeable == that.tradeable
			&& Objects.equals(name, that.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(itemId, name, quantity, tradeable);
	}

	@Override
	public String toString()
	{
		return quantity > 1 ? (quantity + "x " + name) : name;
	}
}
