package com.herbwise.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a dynamically scored training or profit recommendation from {@code TrainingRecommendationEngine},
 * including the transparent score and data-driven reasons explaining why the potion was selected.
 */
public class TrainingRecommendation
{
	public enum Category
	{
		BEST_PROFIT("Best Profit", "Highest net GP profit per potion"),
		CHEAPEST_XP("Cheapest XP", "Lowest net GP cost per Herblore XP"),
		BEST_XP_RATE("Best XP Rate", "Highest estimated Herblore XP per hour"),
		BEST_BALANCED("Best Balanced Option", "Optimal blend of XP rate, GP/XP efficiency, and GE liquidity"),
		BEST_FOR_BUDGET("Best For My Budget", "Best training method affordable within your GP budget"),
		BEST_FOR_LEVEL("Best For My Level", "Top recommended recipe unlocked at your current Herblore level");

		private final String title;
		private final String subtitle;

		Category(String title, String subtitle)
		{
			this.title = title;
			this.subtitle = subtitle;
		}

		public String getTitle() { return title; }
		public String getSubtitle() { return subtitle; }
	}

	private final Category category;
	private final PotionProfitResult result;
	private final double score;
	private final String headlineMetric;
	private final List<String> reasons;

	public TrainingRecommendation(
		Category category,
		PotionProfitResult result,
		double score,
		String headlineMetric,
		List<String> reasons)
	{
		this.category = category;
		this.result = result;
		this.score = score;
		this.headlineMetric = headlineMetric != null ? headlineMetric : "";
		this.reasons = reasons != null
			? Collections.unmodifiableList(new ArrayList<>(reasons))
			: Collections.emptyList();
	}

	public Category getCategory()
	{
		return category;
	}

	public PotionProfitResult getResult()
	{
		return result;
	}

	public double getScore()
	{
		return score;
	}

	public String getHeadlineMetric()
	{
		return headlineMetric;
	}

	public List<String> getReasons()
	{
		return reasons;
	}
}
