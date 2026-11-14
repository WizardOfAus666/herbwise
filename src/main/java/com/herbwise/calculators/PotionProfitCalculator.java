package com.herbwise.calculators;

import com.herbwise.models.ItemPrice;
import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionProfitResult.PriceStatus;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.PriceCalculationMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Core profit, loss, and XP efficiency engine for HerbWise.
 * Evaluates every {@link PotionRecipe} using real-time or rolling-average prices,
 * applies OSRS Grand Exchange tax rules, and flags missing, stale, or volatile market data.
 */
@Singleton
public class PotionProfitCalculator
{
	public static final long DEFAULT_STALE_THRESHOLD_SECONDS = 3 * 3600L; // 3 hours
	public static final double DEFAULT_VOLATILITY_SPREAD_PERCENT = 15.0; // 15% high/low spread

	private final GeTaxCalculator geTaxCalculator;
	private final ProductionTimeCalculator productionTimeCalculator;

	@Inject
	public PotionProfitCalculator(
		GeTaxCalculator geTaxCalculator,
		ProductionTimeCalculator productionTimeCalculator)
	{
		this.geTaxCalculator = geTaxCalculator != null ? geTaxCalculator : new GeTaxCalculator();
		this.productionTimeCalculator = productionTimeCalculator != null
			? productionTimeCalculator
			: new ProductionTimeCalculator();
	}

	public PotionProfitCalculator()
	{
		this(new GeTaxCalculator(), new ProductionTimeCalculator());
	}

	/**
	 * Evaluates a single {@link PotionRecipe} with default staleness/volatility thresholds.
	 */
	public PotionProfitResult calculate(
		PotionRecipe recipe,
		Map<Integer, ItemPrice> priceMap,
		PriceCalculationMode priceMode,
		boolean includeGeTax,
		int playerHerbloreLevel,
		long playerBudgetGp,
		long nowEpochSec)
	{
		return calculate(
			recipe,
			priceMap,
			priceMode,
			includeGeTax,
			playerHerbloreLevel,
			playerBudgetGp,
			nowEpochSec,
			true,
			DEFAULT_STALE_THRESHOLD_SECONDS,
			true,
			DEFAULT_VOLATILITY_SPREAD_PERCENT
		);
	}

	/**
	 * Evaluates a single {@link PotionRecipe} with full configuration controls.
	 */
	public PotionProfitResult calculate(
		PotionRecipe recipe,
		Map<Integer, ItemPrice> priceMap,
		PriceCalculationMode priceMode,
		boolean includeGeTax,
		int playerHerbloreLevel,
		long playerBudgetGp,
		long nowEpochSec,
		boolean enableStaleWarnings,
		long maxStaleAgeSeconds,
		boolean enableVolatilityWarnings,
		double maxVolatilitySpreadPct)
	{
		PriceCalculationMode safeMode = priceMode != null ? priceMode : PriceCalculationMode.INSTANT_CONSERVATIVE;
		int safePlayerLevel = Math.max(1, playerHerbloreLevel);

		if (recipe == null || !recipe.isVerified() || recipe.getPotionItemId() <= 0 || recipe.getIngredients().isEmpty())
		{
			return PotionProfitResult.builder()
				.recipe(recipe)
				.priceMode(safeMode)
				.includeGeTax(includeGeTax)
				.priceStatus(PriceStatus.MISSING_DATA)
				.hasCompletePriceData(false)
				.hasIngredientPriceData(false)
				.statusMessage("Unverified or incomplete recipe definition")
				.availableAtPlayerLevel(false)
				.affordableWithinBudget(false)
				.build();
		}

		boolean availableAtLevel = safePlayerLevel >= recipe.getRequiredLevel();
		int levelsUntilUnlock = Math.max(0, recipe.getRequiredLevel() - safePlayerLevel);
		int potionsPerHour = productionTimeCalculator.getEstimatedPotionsPerHour(recipe);
		long xpPerHour = productionTimeCalculator.calculateEstimatedXpPerHour(recipe);

		if (priceMap == null || priceMap.isEmpty())
		{
			return PotionProfitResult.builder()
				.recipe(recipe)
				.priceMode(safeMode)
				.includeGeTax(includeGeTax)
				.priceStatus(PriceStatus.MISSING_DATA)
				.hasCompletePriceData(false)
				.hasIngredientPriceData(false)
				.statusMessage("Market price data unavailable")
				.xpPerPotion(recipe.getXpGained())
				.estimatedPotionsPerHour(potionsPerHour)
				.estimatedXpPerHour(xpPerHour)
				.availableAtPlayerLevel(availableAtLevel)
				.levelsUntilUnlock(levelsUntilUnlock)
				.affordableWithinBudget(false)
				.build();
		}

		Map<Integer, Long> unitPrices = new LinkedHashMap<>();
		Map<Integer, Long> lineCosts = new LinkedHashMap<>();
		long totalIngredientCost = 0L;
		boolean allIngredientsPriced = true;
		boolean anyStale = false;
		boolean anyVolatile = false;
		long maxAgeSec = 0L;
		String missingNote = null;

		for (PotionIngredient ing : recipe.getIngredients())
		{
			if (ing == null || ing.getItemId() <= 0 || ing.getQuantity() <= 0)
			{
				allIngredientsPriced = false;
				missingNote = "Invalid ingredient specification";
				break;
			}
			if (!ing.isTradeable())
			{
				unitPrices.put(ing.getItemId(), 0L);
				lineCosts.put(ing.getItemId(), 0L);
				missingNote = "Includes untradeable catalyst: " + ing.getName() + " (valued at 0 GP direct GE cost)";
				continue;
			}

			ItemPrice ingPrice = priceMap.get(ing.getItemId());
			Long resolvedBuy = ingPrice != null ? ingPrice.resolveBuyPrice(safeMode) : null;
			if (resolvedBuy == null || resolvedBuy <= 0L)
			{
				allIngredientsPriced = false;
				missingNote = "Missing market price for " + ing.getName();
				continue;
			}

			try
			{
				long lineCost = Math.multiplyExact(resolvedBuy, (long) ing.getQuantity());
				totalIngredientCost = Math.addExact(totalIngredientCost, lineCost);
				unitPrices.put(ing.getItemId(), resolvedBuy);
				lineCosts.merge(ing.getItemId(), lineCost, Math::addExact);
			}
			catch (ArithmeticException ex)
			{
				allIngredientsPriced = false;
				missingNote = "Price calculation overflow for " + ing.getName();
				break;
			}

			long age = ingPrice.getAgeSeconds(nowEpochSec);
			maxAgeSec = Math.max(maxAgeSec, age);
			if (ingPrice.isStale(nowEpochSec, maxStaleAgeSeconds) || ingPrice.isFromCache())
			{
				anyStale = true;
			}
			if (ingPrice.isVolatile(maxVolatilitySpreadPct))
			{
				anyVolatile = true;
			}
		}

		double xp = Math.max(0.0, recipe.getXpGained());
		double ingredientCostPerXp = (allIngredientsPriced && xp > 0.0)
			? ((double) totalIngredientCost / xp)
			: 0.0;
		boolean affordable = allIngredientsPriced && playerBudgetGp >= totalIngredientCost && totalIngredientCost > 0L;

		// Check finished potion price
		if (!recipe.isTradeableOutput())
		{
			return PotionProfitResult.builder()
				.recipe(recipe)
				.priceMode(safeMode)
				.includeGeTax(includeGeTax)
				.priceStatus(PriceStatus.MISSING_DATA)
				.hasCompletePriceData(false)
				.hasIngredientPriceData(allIngredientsPriced)
				.stalePriceWarning(enableStaleWarnings && anyStale)
				.volatilePriceWarning(enableVolatilityWarnings && anyVolatile)
				.statusMessage(allIngredientsPriced
					? "Untradeable finished potion (cannot be sold on GE)"
					: (missingNote != null ? missingNote : "Untradeable recipe"))
				.ingredientUnitPrices(unitPrices)
				.ingredientLineCosts(lineCosts)
				.totalIngredientCost(allIngredientsPriced ? totalIngredientCost : 0L)
				.xpPerPotion(xp)
				.ingredientCostPerXp(ingredientCostPerXp)
				.netCostPerXp(ingredientCostPerXp)
				.netLossPerXp(ingredientCostPerXp)
				.profitPerXp(-ingredientCostPerXp)
				.estimatedPotionsPerHour(potionsPerHour)
				.estimatedXpPerHour(xpPerHour)
				.maxPriceAgeSeconds(maxAgeSec)
				.availableAtPlayerLevel(availableAtLevel)
				.levelsUntilUnlock(levelsUntilUnlock)
				.affordableWithinBudget(affordable)
				.build();
		}

		ItemPrice finishedPriceObj = priceMap.get(recipe.getPotionItemId());
		Long unitSellPrice = finishedPriceObj != null ? finishedPriceObj.resolveSellPrice(safeMode) : null;

		if (!allIngredientsPriced || unitSellPrice == null || unitSellPrice <= 0L)
		{
			String msg = missingNote != null
				? missingNote
				: ("Missing market price for " + recipe.getPotionName());
			return PotionProfitResult.builder()
				.recipe(recipe)
				.priceMode(safeMode)
				.includeGeTax(includeGeTax)
				.priceStatus(PriceStatus.MISSING_DATA)
				.hasCompletePriceData(false)
				.hasIngredientPriceData(allIngredientsPriced)
				.stalePriceWarning(enableStaleWarnings && anyStale)
				.volatilePriceWarning(enableVolatilityWarnings && anyVolatile)
				.statusMessage(msg)
				.ingredientUnitPrices(unitPrices)
				.ingredientLineCosts(lineCosts)
				.totalIngredientCost(allIngredientsPriced ? totalIngredientCost : 0L)
				.xpPerPotion(xp)
				.ingredientCostPerXp(ingredientCostPerXp)
				.estimatedPotionsPerHour(potionsPerHour)
				.estimatedXpPerHour(xpPerHour)
				.maxPriceAgeSeconds(maxAgeSec)
				.availableAtPlayerLevel(availableAtLevel)
				.levelsUntilUnlock(levelsUntilUnlock)
				.affordableWithinBudget(affordable)
				.build();
		}

		long ageOut = finishedPriceObj.getAgeSeconds(nowEpochSec);
		maxAgeSec = Math.max(maxAgeSec, ageOut);
		if (finishedPriceObj.isStale(nowEpochSec, maxStaleAgeSeconds) || finishedPriceObj.isFromCache())
		{
			anyStale = true;
		}
		if (finishedPriceObj.isVolatile(maxVolatilitySpreadPct))
		{
			anyVolatile = true;
		}

		int outputQty = Math.max(1, recipe.getOutputQuantity());
		long grossSaleValue;
		long geTax;
		long netSaleValue;
		long netProfit;
		long profit100;
		long profit1000;
		long profit10000;
		long profitPerHour;

		try
		{
			grossSaleValue = Math.multiplyExact(unitSellPrice, (long) outputQty);
			geTax = geTaxCalculator.calculateTotalTax(recipe.getPotionItemId(), unitSellPrice, outputQty, includeGeTax);
			netSaleValue = Math.subtractExact(grossSaleValue, geTax);
			netProfit = Math.subtractExact(netSaleValue, totalIngredientCost);
			profit100 = Math.multiplyExact(netProfit, 100L);
			profit1000 = Math.multiplyExact(netProfit, 1_000L);
			profit10000 = Math.multiplyExact(netProfit, 10_000L);
			profitPerHour = productionTimeCalculator.calculateEstimatedProfitPerHour(recipe, netProfit);
		}
		catch (ArithmeticException ex)
		{
			return PotionProfitResult.builder()
				.recipe(recipe)
				.priceMode(safeMode)
				.includeGeTax(includeGeTax)
				.priceStatus(PriceStatus.MISSING_DATA)
				.hasCompletePriceData(false)
				.statusMessage("Arithmetic overflow during profit calculation")
				.build();
		}

		double profitMarginPct = totalIngredientCost > 0L
			? (((double) netProfit / (double) totalIngredientCost) * 100.0)
			: 0.0;

		double netCostPerXp = 0.0;
		double netLossPerXp = 0.0;
		double profitPerXp = 0.0;
		if (xp > 0.0)
		{
			// Positive netCostPerXp means it costs GP per XP; negative means you profit per XP
			netCostPerXp = (double) (totalIngredientCost - netSaleValue) / xp;
			netLossPerXp = netProfit < 0L ? ((double) (-netProfit) / xp) : 0.0;
			profitPerXp = (double) netProfit / xp;
		}

		int hourlyVol = finishedPriceObj.getTotalVolume1h();
		boolean warnStale = enableStaleWarnings && anyStale;
		boolean warnVolatile = enableVolatilityWarnings && anyVolatile;

		PriceStatus status;
		String statusMsg;
		if (warnStale || warnVolatile)
		{
			status = PriceStatus.UNCERTAIN_OR_STALE;
			if (warnStale && warnVolatile)
			{
				statusMsg = "Stale & volatile market prices";
			}
			else if (warnStale)
			{
				statusMsg = finishedPriceObj.isFromCache() ? "Cached price snapshot" : "Stale price data";
			}
			else
			{
				statusMsg = "High price spread / volatility";
			}
		}
		else if (netProfit >= 0L)
		{
			status = PriceStatus.PROFITABLE;
			statusMsg = "Profitable";
		}
		else
		{
			status = PriceStatus.LOSS;
			statusMsg = "Training loss";
		}

		double trainingScore = computeBalancedTrainingScore(
			recipe,
			netProfit,
			netCostPerXp,
			xpPerHour,
			hourlyVol,
			warnStale,
			warnVolatile
		);

		return PotionProfitResult.builder()
			.recipe(recipe)
			.priceMode(safeMode)
			.includeGeTax(includeGeTax)
			.priceStatus(status)
			.hasCompletePriceData(true)
			.hasIngredientPriceData(true)
			.stalePriceWarning(warnStale)
			.volatilePriceWarning(warnVolatile)
			.statusMessage(statusMsg)
			.ingredientUnitPrices(unitPrices)
			.ingredientLineCosts(lineCosts)
			.totalIngredientCost(totalIngredientCost)
			.finishedPotionSalePrice(grossSaleValue)
			.geTax(geTax)
			.netSaleValue(netSaleValue)
			.netProfitPerPotion(netProfit)
			.profitFor100(profit100)
			.profitFor1000(profit1000)
			.profitFor10000(profit10000)
			.profitMarginPercent(profitMarginPct)
			.xpPerPotion(xp)
			.ingredientCostPerXp(ingredientCostPerXp)
			.netCostPerXp(netCostPerXp)
			.netLossPerXp(netLossPerXp)
			.profitPerXp(profitPerXp)
			.estimatedPotionsPerHour(potionsPerHour)
			.estimatedXpPerHour(xpPerHour)
			.estimatedProfitPerHour(profitPerHour)
			.hourlyVolume(hourlyVol)
			.maxPriceAgeSeconds(maxAgeSec)
			.overallTrainingScore(trainingScore)
			.availableAtPlayerLevel(availableAtLevel)
			.levelsUntilUnlock(levelsUntilUnlock)
			.affordableWithinBudget(affordable)
			.build();
	}

	/**
	 * Evaluates a batch of recipes and returns all results in deterministic order.
	 */
	public List<PotionProfitResult> calculateAll(
		List<PotionRecipe> recipes,
		Map<Integer, ItemPrice> priceMap,
		PriceCalculationMode priceMode,
		boolean includeGeTax,
		int playerHerbloreLevel,
		long playerBudgetGp,
		long nowEpochSec,
		boolean enableStaleWarnings,
		long maxStaleAgeSeconds,
		boolean enableVolatilityWarnings,
		double maxVolatilitySpreadPct)
	{
		if (recipes == null || recipes.isEmpty())
		{
			return Collections.emptyList();
		}
		List<PotionProfitResult> results = new ArrayList<>(recipes.size());
		for (PotionRecipe recipe : recipes)
		{
			results.add(calculate(
				recipe,
				priceMap,
				priceMode,
				includeGeTax,
				playerHerbloreLevel,
				playerBudgetGp,
				nowEpochSec,
				enableStaleWarnings,
				maxStaleAgeSeconds,
				enableVolatilityWarnings,
				maxVolatilitySpreadPct
			));
		}
		return results;
	}

	private double computeBalancedTrainingScore(
		PotionRecipe recipe,
		long netProfit,
		double netCostPerXp,
		long xpPerHour,
		int hourlyVolume,
		boolean stale,
		boolean volatilePrice)
	{
		if (recipe.isUnfinishedPotionRecipe() || recipe.getXpGained() <= 0.0)
		{
			return 0.0;
		}

		// 1. XP Rate Score (0..35 points, normalized against 450,000 XP/hr ceiling)
		double xpRateNorm = Math.min(1.0, (double) xpPerHour / 450_000.0);
		double xpComponent = xpRateNorm * 35.0;

		// 2. Cost/Profit Efficiency Score (0..35 points)
		// Profitable or break-even gets 28..35 pts; 10 GP/XP loss gets ~17.5 pts; >=25 GP/XP loss gets 0 pts
		double efficiencyComponent;
		if (netCostPerXp <= 0.0)
		{
			double bonus = Math.min(7.0, (-netCostPerXp) * 1.5);
			efficiencyComponent = 28.0 + bonus;
		}
		else
		{
			efficiencyComponent = Math.max(0.0, 28.0 * (1.0 - (netCostPerXp / 25.0)));
		}

		// 3. Market Liquidity Score (0..20 points, logarithmic on hourly volume)
		double volumeComponent = 0.0;
		if (hourlyVolume > 0)
		{
			double logVol = Math.log10(hourlyVolume + 1.0); // log10(100,000) = 5.0
			volumeComponent = Math.min(20.0, (logVol / 5.0) * 20.0);
		}

		// 4. Reliability & Freshness Score (0..10 points)
		double reliabilityComponent = 10.0;
		if (stale)
		{
			reliabilityComponent -= 5.0;
		}
		if (volatilePrice)
		{
			reliabilityComponent -= 4.0;
		}
		if (netProfit > -500L && netProfit < 500L)
		{
			reliabilityComponent += 1.0;
		}
		reliabilityComponent = Math.max(0.0, Math.min(10.0, reliabilityComponent));

		double total = xpComponent + efficiencyComponent + volumeComponent + reliabilityComponent;
		return Math.round(total * 10.0) / 10.0;
	}
}
