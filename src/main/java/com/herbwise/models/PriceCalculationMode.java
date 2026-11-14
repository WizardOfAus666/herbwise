package com.herbwise.models;

/**
 * Defines how HerbWise resolves ingredient purchase prices and finished potion sale prices
 * from the OSRS Wiki Real-Time Prices API.
 */
public enum PriceCalculationMode
{
	INSTANT_CONSERVATIVE(
		"Instant Buy / Sell (Conservative)",
		"Buys ingredients at Instant-Buy (High) price and sells finished potions at Instant-Sell (Low) price. Best for immediate Grand Exchange transactions."
	),
	FIVE_MINUTE_AVERAGE(
		"5-Minute Average",
		"Uses 5-minute rolling average high price for buying ingredients and 5-minute average low price for selling potions. Smooths out single-trade spikes."
	),
	ONE_HOUR_AVERAGE(
		"1-Hour Average",
		"Uses 1-hour rolling average prices (/1h) for both ingredients and finished potions. Best for steady training sessions and medium-term production."
	),
	PATIENT_OFFER(
		"Patient GE Offers (Low Buy / High Sell)",
		"Buys ingredients via patient buy offers (Instant-Sell/Low) and sells finished potions via patient sell offers (Instant-Buy/High). Maximises margins when waiting."
	);

	private final String displayName;
	private final String explanation;

	PriceCalculationMode(String displayName, String explanation)
	{
		this.displayName = displayName;
		this.explanation = explanation;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getExplanation()
	{
		return explanation;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
