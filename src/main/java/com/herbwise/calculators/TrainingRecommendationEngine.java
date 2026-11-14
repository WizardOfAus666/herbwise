package com.herbwise.calculators;

import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.TrainingRecommendation;
import com.herbwise.models.TrainingRecommendation.Category;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Singleton;

/**
 * Intelligent Herblore training and profit recommendation engine.
 * Evaluates verified recipes across six distinct categories using transparent scoring
 * and generates dynamic, data-backed explanations for every recommendation.
 */
@Singleton
public class TrainingRecommendationEngine
{
	private static final NumberFormat GP_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

	/**
	 * Generates all 6 recommendation categories based on the current market results,
	 * player Herblore level, and GP budget.
	 */
	public Map<Category, TrainingRecommendation> generateRecommendations(
		List<PotionProfitResult> allResults,
		int playerHerbloreLevel,
		long playerBudgetGp,
		boolean restrictAllToPlayerLevel)
	{
		Map<Category, TrainingRecommendation> map = new LinkedHashMap<>();
		if (allResults == null || allResults.isEmpty())
		{
			return map;
		}

		int effectiveLevel = Math.max(1, playerHerbloreLevel);
		long effectiveBudget = Math.max(0L, playerBudgetGp);

		// Base candidate pool: verified, completed potions with complete market data and tradeable GE ingredients
		List<PotionProfitResult> validCompleted = allResults.stream()
			.filter(r -> r != null && r.getRecipe() != null && r.getRecipe().isVerified())
			.filter(r -> !r.getRecipe().isUnfinishedPotionRecipe())
			.filter(r -> !r.getRecipe().isAlternativeRecipe())
			.filter(r -> r.getRecipe().getIngredients().stream().allMatch(com.herbwise.models.PotionIngredient::isTradeable))
			.filter(PotionProfitResult::isHasCompletePriceData)
			.collect(Collectors.toList());

		if (validCompleted.isEmpty())
		{
			return map;
		}

		List<PotionProfitResult> levelFilteredPool = validCompleted.stream()
			.filter(r -> r.getRecipe().getRequiredLevel() <= effectiveLevel)
			.collect(Collectors.toList());

		List<PotionProfitResult> generalPool = restrictAllToPlayerLevel && !levelFilteredPool.isEmpty()
			? levelFilteredPool
			: validCompleted;

		// A. BEST PROFIT
		findBestProfit(generalPool, effectiveLevel).ifPresent(rec -> map.put(Category.BEST_PROFIT, rec));

		// B. CHEAPEST XP (requires meaningful training XP > 15 XP per potion)
		findCheapestXp(generalPool, effectiveLevel).ifPresent(rec -> map.put(Category.CHEAPEST_XP, rec));

		// C. BEST XP RATE
		findBestXpRate(generalPool, effectiveLevel).ifPresent(rec -> map.put(Category.BEST_XP_RATE, rec));

		// D. BEST BALANCED OPTION
		findBestBalanced(generalPool, effectiveLevel).ifPresent(rec -> map.put(Category.BEST_BALANCED, rec));

		// E. BEST FOR MY BUDGET
		List<PotionProfitResult> budgetPool = !levelFilteredPool.isEmpty() ? levelFilteredPool : generalPool;
		findBestForBudget(budgetPool, effectiveLevel, effectiveBudget)
			.ifPresent(rec -> map.put(Category.BEST_FOR_BUDGET, rec));

		// F. BEST FOR MY LEVEL
		List<PotionProfitResult> myLevelPool = !levelFilteredPool.isEmpty() ? levelFilteredPool : validCompleted;
		findBestForLevel(myLevelPool, effectiveLevel)
			.ifPresent(rec -> map.put(Category.BEST_FOR_LEVEL, rec));

		return map;
	}

	private Optional<TrainingRecommendation> findBestProfit(List<PotionProfitResult> pool, int playerLevel)
	{
		return pool.stream()
			.max(Comparator.comparingLong(PotionProfitResult::getNetProfitPerPotion)
				.thenComparingInt(PotionProfitResult::getHourlyVolume))
			.map(best -> {
				PotionRecipe rec = best.getRecipe();
				List<String> reasons = new ArrayList<>();
				String profitStr = formatSignedGp(best.getNetProfitPerPotion());
				reasons.add("Highest net profit at " + profitStr + " per potion ("
					+ formatSignedGp(best.getProfitFor1000()) + " per 1,000 made).");
				reasons.add(String.format(Locale.US,
					"Ingredient cost %s GP vs %s GP net sale after %s GP GE tax (%.1f%% margin).",
					GP_FORMAT.format(best.getTotalIngredientCost()),
					GP_FORMAT.format(best.getNetSaleValue()),
					GP_FORMAT.format(best.getGeTax()),
					best.getProfitMarginPercent()
				));
				reasons.add(buildLevelReason(rec, playerLevel));
				reasons.add(buildLiquidityReason(best));

				String headline = profitStr + "/potion (" + formatSignedGp(best.getEstimatedProfitPerHour()) + "/hr)";
				double score = Math.min(100.0, Math.max(0.0, 50.0 + (best.getNetProfitPerPotion() / 40.0)));
				return new TrainingRecommendation(Category.BEST_PROFIT, best, score, headline, reasons);
			});
	}

	private Optional<TrainingRecommendation> findCheapestXp(List<PotionProfitResult> pool, int playerLevel)
	{
		List<PotionProfitResult> trainingCandidates = pool.stream()
			.filter(r -> r.getXpPerPotion() >= 20.0)
			.collect(Collectors.toList());
		if (trainingCandidates.isEmpty())
		{
			trainingCandidates = pool;
		}

		return trainingCandidates.stream()
			.min(Comparator.comparingDouble(PotionProfitResult::getNetCostPerXp)
				.thenComparing(Comparator.comparingDouble(PotionProfitResult::getXpPerPotion).reversed()))
			.map(best -> {
				PotionRecipe rec = best.getRecipe();
				List<String> reasons = new ArrayList<>();
				if (best.getNetCostPerXp() <= 0.0)
				{
					reasons.add(String.format(Locale.US,
						"Profitable training! Earns +%.2f GP per XP (%s GP profit per potion) while granting %.1f XP.",
						Math.abs(best.getNetCostPerXp()),
						GP_FORMAT.format(best.getNetProfitPerPotion()),
						best.getXpPerPotion()
					));
				}
				else
				{
					reasons.add(String.format(Locale.US,
						"Lowest net training cost at %.2f GP per XP (%s GP loss for %.1f XP).",
						best.getNetCostPerXp(),
						GP_FORMAT.format(Math.abs(best.getNetProfitPerPotion())),
						best.getXpPerPotion()
					));
				}
				reasons.add("Raw ingredient outlay is only " + GP_FORMAT.format(best.getTotalIngredientCost())
					+ " GP per potion (" + String.format(Locale.US, "%.2f GP/XP gross", best.getIngredientCostPerXp()) + ").");
				reasons.add(buildLevelReason(rec, playerLevel));
				reasons.add(buildLiquidityReason(best));

				String headline = best.getNetCostPerXp() <= 0.0
					? String.format(Locale.US, "+%.2f GP/XP (Profitable)", Math.abs(best.getNetCostPerXp()))
					: String.format(Locale.US, "%.2f GP/XP", best.getNetCostPerXp());
				return new TrainingRecommendation(Category.CHEAPEST_XP, best, best.getOverallTrainingScore(), headline, reasons);
			});
	}

	private Optional<TrainingRecommendation> findBestXpRate(List<PotionProfitResult> pool, int playerLevel)
	{
		return pool.stream()
			.max(Comparator.comparingLong(PotionProfitResult::getEstimatedXpPerHour)
				.thenComparingDouble(PotionProfitResult::getXpPerPotion))
			.map(best -> {
				PotionRecipe rec = best.getRecipe();
				List<String> reasons = new ArrayList<>();
				reasons.add("Delivers an estimated " + GP_FORMAT.format(best.getEstimatedXpPerHour())
					+ " XP/hr at ~" + GP_FORMAT.format(best.getEstimatedPotionsPerHour()) + " potions/hr.");
				reasons.add(String.format(Locale.US,
					"Grants %.1f Herblore XP per potion with %s GP/potion net %s.",
					best.getXpPerPotion(),
					GP_FORMAT.format(Math.abs(best.getNetProfitPerPotion())),
					best.getNetProfitPerPotion() >= 0 ? "profit" : "cost"
				));
				reasons.add(buildLevelReason(rec, playerLevel));
				reasons.add(buildLiquidityReason(best));

				String headline = GP_FORMAT.format(best.getEstimatedXpPerHour()) + " XP/hr ("
					+ String.format(Locale.US, "%.1f XP each", best.getXpPerPotion()) + ")";
				return new TrainingRecommendation(Category.BEST_XP_RATE, best, best.getOverallTrainingScore(), headline, reasons);
			});
	}

	private Optional<TrainingRecommendation> findBestBalanced(List<PotionProfitResult> pool, int playerLevel)
	{
		List<PotionProfitResult> trainingPool = pool.stream()
			.filter(r -> r.getXpPerPotion() >= 25.0)
			.collect(Collectors.toList());
		if (trainingPool.isEmpty())
		{
			trainingPool = pool;
		}

		return trainingPool.stream()
			.max(Comparator.comparingDouble(PotionProfitResult::getOverallTrainingScore))
			.map(best -> {
				PotionRecipe rec = best.getRecipe();
				List<String> reasons = new ArrayList<>();
				reasons.add(String.format(Locale.US,
					"Highest composite training score (%.1f / 100) balancing XP speed, GP/XP efficiency, and liquidity.",
					best.getOverallTrainingScore()
				));
				reasons.add(String.format(Locale.US,
					"Combines %s XP/hr with %s (%s GP per potion).",
					GP_FORMAT.format(best.getEstimatedXpPerHour()),
					best.getNetCostPerXp() <= 0.0
						? String.format(Locale.US, "+%.2f GP/XP profit", Math.abs(best.getNetCostPerXp()))
						: String.format(Locale.US, "%.2f GP/XP net cost", best.getNetCostPerXp()),
					formatSignedGp(best.getNetProfitPerPotion())
				));
				reasons.add(buildLevelReason(rec, playerLevel));
				reasons.add(buildLiquidityReason(best));

				String headline = String.format(Locale.US, "Score %.1f/100 • %s XP/hr",
					best.getOverallTrainingScore(),
					GP_FORMAT.format(best.getEstimatedXpPerHour()));
				return new TrainingRecommendation(Category.BEST_BALANCED, best, best.getOverallTrainingScore(), headline, reasons);
			});
	}

	private Optional<TrainingRecommendation> findBestForBudget(
		List<PotionProfitResult> pool,
		int playerLevel,
		long budgetGp)
	{
		List<PotionProfitResult> affordable = pool.stream()
			.filter(r -> r.getXpPerPotion() >= 20.0)
			.filter(r -> r.getTotalIngredientCost() > 0L && r.getTotalIngredientCost() <= budgetGp)
			.collect(Collectors.toList());

		if (affordable.isEmpty())
		{
			// Fallback to lowest-cost recipe in pool if budget is very small
			affordable = pool.stream()
				.filter(r -> r.getTotalIngredientCost() > 0L)
				.sorted(Comparator.comparingLong(PotionProfitResult::getTotalIngredientCost))
				.limit(3)
				.collect(Collectors.toList());
		}

		return affordable.stream()
			.max(Comparator.comparingDouble(r -> {
				long count = budgetGp > 0 ? (budgetGp / Math.max(1L, r.getTotalIngredientCost())) : 0L;
				return r.getOverallTrainingScore() + Math.min(20.0, Math.log10(count + 1.0) * 6.5);
			}))
			.map(best -> {
				long maxAffordable = budgetGp > 0L
					? (budgetGp / Math.max(1L, best.getTotalIngredientCost()))
					: 0L;
				double totalXp = maxAffordable * best.getXpPerPotion();
				List<String> reasons = new ArrayList<>();
				reasons.add("Your " + GP_FORMAT.format(budgetGp) + " GP budget affords "
					+ GP_FORMAT.format(maxAffordable) + " potions ("
					+ GP_FORMAT.format(Math.round(totalXp)) + " total Herblore XP).");
				reasons.add("Costs " + GP_FORMAT.format(best.getTotalIngredientCost())
					+ " GP per potion in ingredients and returns "
					+ GP_FORMAT.format(best.getNetSaleValue()) + " GP after GE tax.");
				reasons.add(buildLevelReason(best.getRecipe(), playerLevel));
				reasons.add(buildLiquidityReason(best));

				String headline = GP_FORMAT.format(maxAffordable) + " potions ("
					+ GP_FORMAT.format(Math.round(totalXp)) + " XP)";
				return new TrainingRecommendation(Category.BEST_FOR_BUDGET, best, best.getOverallTrainingScore(), headline, reasons);
			});
	}

	private Optional<TrainingRecommendation> findBestForLevel(List<PotionProfitResult> pool, int playerLevel)
	{
		List<PotionProfitResult> unlocked = pool.stream()
			.filter(r -> r.getRecipe().getRequiredLevel() <= playerLevel)
			.filter(r -> r.getXpPerPotion() >= 25.0)
			.collect(Collectors.toList());

		if (unlocked.isEmpty())
		{
			unlocked = pool;
		}

		return unlocked.stream()
			.max(Comparator.comparingDouble((PotionProfitResult r) -> {
				// Favour recipes close to the player's current tier that also have strong balanced score
				int levelGap = Math.max(0, playerLevel - r.getRecipe().getRequiredLevel());
				double tierProximityBonus = Math.max(0.0, 18.0 - (levelGap * 0.45));
				return r.getOverallTrainingScore() + tierProximityBonus;
			}))
			.map(best -> {
				PotionRecipe rec = best.getRecipe();
				List<String> reasons = new ArrayList<>();
				reasons.add("Available now at your current Herblore level (" + playerLevel
					+ " vs Level " + rec.getRequiredLevel() + " requirement).");
				reasons.add(String.format(Locale.US,
					"Provides %.1f XP per potion (%s XP/hr) at %s.",
					best.getXpPerPotion(),
					GP_FORMAT.format(best.getEstimatedXpPerHour()),
					best.getNetCostPerXp() <= 0.0
						? (formatSignedGp(best.getNetProfitPerPotion()) + " profit/potion")
						: String.format(Locale.US, "%.2f GP/XP", best.getNetCostPerXp())
				));
				reasons.add("Uses " + rec.getFormattedIngredientList() + ".");
				reasons.add(buildLiquidityReason(best));

				String headline = "Lvl " + rec.getRequiredLevel() + " • "
					+ String.format(Locale.US, "%.1f XP", best.getXpPerPotion()) + " ("
					+ formatSignedGp(best.getNetProfitPerPotion()) + ")";
				return new TrainingRecommendation(Category.BEST_FOR_LEVEL, best, best.getOverallTrainingScore(), headline, reasons);
			});
	}

	private static String buildLevelReason(PotionRecipe recipe, int playerLevel)
	{
		if (playerLevel >= recipe.getRequiredLevel())
		{
			return "Unlocked at your current Herblore level (Requires Lvl " + recipe.getRequiredLevel()
				+ ", you are Lvl " + playerLevel + ").";
		}
		int diff = recipe.getRequiredLevel() - playerLevel;
		return "Requires Level " + recipe.getRequiredLevel() + " Herblore (" + diff
			+ (diff == 1 ? " level" : " levels") + " above your current Lvl " + playerLevel + ").";
	}

	private static String buildLiquidityReason(PotionProfitResult result)
	{
		int vol = result.getHourlyVolume();
		String freshness = result.isStalePriceWarning()
			? "cached/older market snapshot"
			: "fresh live GE data";
		if (vol >= 50_000)
		{
			return "High Grand Exchange liquidity (~" + GP_FORMAT.format(vol) + " doses/potions traded per hour, " + freshness + ").";
		}
		if (vol >= 10_000)
		{
			return "Good market liquidity (~" + GP_FORMAT.format(vol) + " traded per hour, " + freshness + ").";
		}
		if (vol > 0)
		{
			return "Moderate trading volume (~" + GP_FORMAT.format(vol) + " traded per hour, " + freshness + ").";
		}
		return "Low recent trading volume; verify Grand Exchange offers before bulk buying.";
	}

	public static String formatSignedGp(long gp)
	{
		if (gp > 0L)
		{
			return "+" + GP_FORMAT.format(gp) + " GP";
		}
		return GP_FORMAT.format(gp) + " GP";
	}
}
