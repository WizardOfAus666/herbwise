package com.herbwise.services;

import com.herbwise.HerbWiseConfig;
import com.herbwise.HerbWiseConfig.HerbloreBoostMode;
import com.herbwise.HerbWiseConfig.TableDisplayMode;
import com.herbwise.calculators.BudgetCalculator;
import com.herbwise.calculators.GeTaxCalculator;
import com.herbwise.calculators.HerbloreXpCalculator;
import com.herbwise.calculators.PotionProfitCalculator;
import com.herbwise.calculators.ProductionTimeCalculator;
import com.herbwise.calculators.TrainingRecommendationEngine;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.MarketTrendAnalysis;
import com.herbwise.models.PlayerHerbloreData;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.PriceCalculationMode;
import com.herbwise.models.TrainingRecommendation;
import com.herbwise.models.TrainingRecommendation.Category;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central data, calculation, and background refresh orchestrator for HerbWise.
 * Coordinates OSRS Wiki price updates, local caching, recipe validation, player skill tracking,
 * profit/XP calculations, and UI state updates without blocking the RuneLite client thread.
 */
@Singleton
public class HerbloreDataService
{
	private static final Logger log = LoggerFactory.getLogger(HerbloreDataService.class);
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")
		.withZone(ZoneId.systemDefault());

	public enum ConnectionState
	{
		LIVE("LIVE API", new Color(0x22, 0xC5, 0x5E)),
		CACHED_FALLBACK("CACHED DATA", new Color(0xF5, 0x9E, 0x0B)),
		REFRESHING("REFRESHING...", new Color(0x38, 0xBD, 0xF8)),
		ERROR("API OFFLINE", new Color(0xEF, 0x44, 0x44));

		private final String badgeText;
		private final Color badgeColor;

		ConnectionState(String badgeText, Color badgeColor)
		{
			this.badgeText = badgeText;
			this.badgeColor = badgeColor;
		}

		public String getBadgeText() { return badgeText; }
		public Color getBadgeColor() { return badgeColor; }
	}

	public enum FilterMode
	{
		ALL_POTIONS("All Potions"),
		PROFITABLE_ONLY("Profitable Only"),
		LOSS_MAKING_ONLY("Loss-Making Only"),
		AFFORDABLE_RECIPES("Affordable Within Budget"),
		AVAILABLE_AT_MY_LEVEL("Available At My Level"),
		BEST_XP_METHODS("Best XP Methods (130+ XP)"),
		BEST_MONEY_MAKING("Best Money-Making Methods"),
		UNFINISHED_POTIONS("Unfinished Potions (0 XP)");

		private final String label;

		FilterMode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	public enum SortMode
	{
		BEST_OVERALL_TRAINING("Best Overall Training Value"),
		HIGHEST_PROFIT("Highest Profit"),
		LOWEST_LOSS("Lowest Loss / Highest Net"),
		CHEAPEST_XP("Cheapest XP (Lowest GP/XP)"),
		HIGHEST_XP("Highest XP Per Potion"),
		HIGHEST_XP_HOUR("Highest XP / Hour"),
		LOWEST_INGREDIENT_COST("Lowest Ingredient Cost"),
		LEVEL_ASCENDING("Required Level (Low -> High)");

		private final String label;

		SortMode(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final WikiPriceService wikiPriceService;
	private final WikiRecipeService wikiRecipeService;
	private final PriceCacheService priceCacheService;
	private final PlayerSkillService playerSkillService;
	private final PotionProfitCalculator profitCalculator;
	private final GeTaxCalculator geTaxCalculator;
	private final HerbloreXpCalculator xpCalculator;
	private final BudgetCalculator budgetCalculator;
	private final TrainingRecommendationEngine recommendationEngine;
	private final ProductionTimeCalculator productionTimeCalculator;
	private final HerbWiseConfig config;
	private final ItemManager itemManager;
	private final Notifier notifier;

	private final Map<Integer, ItemPrice> activePrices = new ConcurrentHashMap<>();
	private volatile List<PotionProfitResult> allEvaluatedResults = Collections.emptyList();
	private volatile Map<Category, TrainingRecommendation> currentRecommendations = Collections.emptyMap();

	private final List<Runnable> uiUpdateListeners = new CopyOnWriteArrayList<>();
	private final AtomicBoolean refreshRunning = new AtomicBoolean(false);

	@Inject
	private ScheduledExecutorService executorService;
	private ScheduledFuture<?> scheduledRefreshFuture;
	private ScheduledFuture<?> scheduledRetryFuture;

	// Runtime settings synchronized from HerbWiseConfig and editable in Settings panel
	private volatile int refreshIntervalSeconds = 60;
	private volatile PriceCalculationMode priceCalculationMode = PriceCalculationMode.INSTANT_CONSERVATIVE;
	private volatile boolean showProfitableOnly = false;
	private volatile boolean showAvailableAtLevelOnly = false;
	private volatile boolean includeGeTax = true;
	private volatile boolean enableVolatilityWarnings = true;
	private volatile boolean enableStalePriceWarnings = true;
	private volatile long defaultBudgetGp = 1_000_000L;
	private volatile int defaultTargetLevel = 90;
	private volatile boolean showXpPerHourEstimates = true;
	private volatile TableDisplayMode tableDisplayMode = TableDisplayMode.DETAILED;
	private volatile HerbloreBoostMode herbloreBoostMode = HerbloreBoostMode.NONE;
	private volatile boolean enableNotifications = false;

	private volatile ConnectionState connectionState = ConnectionState.CACHED_FALLBACK;
	private volatile long lastUpdatedEpochSec = 0L;
	private volatile String statusDetailText = "Ready";

	@Inject
	public HerbloreDataService(
		WikiPriceService wikiPriceService,
		WikiRecipeService wikiRecipeService,
		PriceCacheService priceCacheService,
		PlayerSkillService playerSkillService,
		PotionProfitCalculator profitCalculator,
		GeTaxCalculator geTaxCalculator,
		HerbloreXpCalculator xpCalculator,
		BudgetCalculator budgetCalculator,
		TrainingRecommendationEngine recommendationEngine,
		ProductionTimeCalculator productionTimeCalculator,
		HerbWiseConfig config,
		ItemManager itemManager,
		Notifier notifier)
	{
		this.wikiPriceService = wikiPriceService;
		this.wikiRecipeService = wikiRecipeService != null ? wikiRecipeService : new WikiRecipeService();
		this.priceCacheService = priceCacheService != null ? priceCacheService : new PriceCacheService();
		this.playerSkillService = playerSkillService != null ? playerSkillService : new PlayerSkillService();
		this.geTaxCalculator = geTaxCalculator != null ? geTaxCalculator : new GeTaxCalculator();
		this.productionTimeCalculator = productionTimeCalculator != null ? productionTimeCalculator : new ProductionTimeCalculator();
		this.profitCalculator = profitCalculator != null ? profitCalculator : new PotionProfitCalculator(this.geTaxCalculator, this.productionTimeCalculator);
		this.xpCalculator = xpCalculator != null ? xpCalculator : new HerbloreXpCalculator(this.productionTimeCalculator);
		this.budgetCalculator = budgetCalculator != null ? budgetCalculator : new BudgetCalculator(this.productionTimeCalculator);
		this.recommendationEngine = recommendationEngine != null ? recommendationEngine : new TrainingRecommendationEngine();
		this.config = config;
		this.itemManager = itemManager;
		this.notifier = notifier;

		syncSettingsFromConfig();

		// Prime immediately with cached fallback so UI is never blank before first network response completes
		Map<Integer, ItemPrice> initialCache = this.priceCacheService.getOrLoadFallbackPrices();
		this.activePrices.putAll(initialCache);
		this.lastUpdatedEpochSec = PriceCacheService.BUNDLED_SNAPSHOT_EPOCH_SEC;
		recalculateAll(Instant.now().getEpochSecond());

		this.playerSkillService.addListener(skillData -> {
			recalculateAll(Instant.now().getEpochSecond());
			notifyUiListeners();
		});
	}

	public synchronized void syncSettingsFromConfig()
	{
		if (config == null)
		{
			return;
		}
		this.refreshIntervalSeconds = Math.max(30, config.priceRefreshIntervalSeconds());
		this.priceCalculationMode = config.priceCalculationMode() != null
			? config.priceCalculationMode()
			: PriceCalculationMode.INSTANT_CONSERVATIVE;
		this.showProfitableOnly = config.showProfitableOnly();
		this.showAvailableAtLevelOnly = config.showAvailableAtLevelOnly();
		this.includeGeTax = config.includeGeTax();
		this.enableVolatilityWarnings = config.enableVolatilityWarnings();
		this.enableStalePriceWarnings = config.enableStalePriceWarnings();
		this.defaultBudgetGp = Math.max(1_000L, config.defaultBudgetGp());
		this.defaultTargetLevel = Math.max(2, Math.min(99, config.defaultTargetLevel()));
		this.showXpPerHourEstimates = config.showXpPerHourEstimates();
		this.tableDisplayMode = config.tableDisplayMode() != null
			? config.tableDisplayMode()
			: TableDisplayMode.DETAILED;
		this.herbloreBoostMode = config.herbloreBoostMode() != null
			? config.herbloreBoostMode()
			: HerbloreBoostMode.NONE;
		this.enableNotifications = config.enableNotifications();
		this.playerSkillService.setBoostMode(this.herbloreBoostMode);
	}

	public synchronized void applyRuntimeSettings(
		int refreshIntervalSec,
		PriceCalculationMode priceMode,
		boolean profitableOnly,
		boolean availableAtLevelOnly,
		boolean includeTax,
		boolean volatilityWarnings,
		boolean staleWarnings,
		long defaultBudget,
		int defaultTargetLvl,
		boolean showXpHour,
		TableDisplayMode displayMode,
		HerbloreBoostMode boostMode,
		boolean notifications)
	{
		this.refreshIntervalSeconds = Math.max(30, refreshIntervalSec);
		this.priceCalculationMode = priceMode != null ? priceMode : PriceCalculationMode.INSTANT_CONSERVATIVE;
		this.showProfitableOnly = profitableOnly;
		this.showAvailableAtLevelOnly = availableAtLevelOnly;
		this.includeGeTax = includeTax;
		this.enableVolatilityWarnings = volatilityWarnings;
		this.enableStalePriceWarnings = staleWarnings;
		this.defaultBudgetGp = Math.max(1_000L, defaultBudget);
		this.defaultTargetLevel = Math.max(2, Math.min(99, defaultTargetLvl));
		this.showXpPerHourEstimates = showXpHour;
		this.tableDisplayMode = displayMode != null ? displayMode : TableDisplayMode.DETAILED;
		this.herbloreBoostMode = boostMode != null ? boostMode : HerbloreBoostMode.NONE;
		this.enableNotifications = notifications;

		this.playerSkillService.setBoostMode(this.herbloreBoostMode);
		rescheduleBackgroundUpdates();
		recalculateAll(Instant.now().getEpochSecond());
		notifyUiListeners();
	}

	public void setExecutorService(ScheduledExecutorService executorService)
	{
		this.executorService = executorService;
	}

	/**
	 * Starts background price updates and triggers an initial asynchronous fetch.
	 */
	public synchronized void startUp()
	{
		rescheduleBackgroundUpdates();
		refreshPricesAsync();
	}

	/**
	 * Cleanly cancels scheduled tasks when the plugin stops.
	 */
	public synchronized void shutDown()
	{
		if (scheduledRefreshFuture != null)
		{
			scheduledRefreshFuture.cancel(false);
			scheduledRefreshFuture = null;
		}
		if (scheduledRetryFuture != null)
		{
			scheduledRetryFuture.cancel(false);
			scheduledRetryFuture = null;
		}
		refreshRunning.set(false);
	}

	private synchronized void rescheduleBackgroundUpdates()
	{
		if (scheduledRefreshFuture != null)
		{
			scheduledRefreshFuture.cancel(false);
			scheduledRefreshFuture = null;
		}
		if (executorService != null && !executorService.isShutdown())
		{
			int delaySec = Math.max(30, refreshIntervalSeconds);
			scheduledRefreshFuture = executorService.scheduleWithFixedDelay(
				this::refreshPricesNowSafe,
				delaySec,
				delaySec,
				TimeUnit.SECONDS
			);
		}
	}

	/**
	 * Triggers a non-blocking manual or scheduled price refresh on the background executor.
	 */
	public void refreshPricesAsync()
	{
		if (executorService != null && !executorService.isShutdown())
		{
			executorService.submit(this::refreshPricesNowSafe);
		}
	}

	private void refreshPricesNowSafe()
	{
		try
		{
			refreshPricesNow();
		}
		catch (Throwable t)
		{
			log.warn("[HerbWise] Background price update error: {}", t.getMessage());
		}
	}

	/**
	 * Synchronously fetches bulk prices from the OSRS Wiki API, falling back to cached prices on failure.
	 */
	public void refreshPricesNow()
	{
		if (!refreshRunning.compareAndSet(false, true))
		{
			return;
		}

		connectionState = ConnectionState.REFRESHING;
		statusDetailText = "Fetching OSRS Wiki real-time prices...";
		notifyUiListeners();

		long nowSec = Instant.now().getEpochSecond();
		try
		{
			Map<Integer, ItemPrice> livePrices = wikiPriceService.fetchAllHerblorePrices();
			if (livePrices != null && !livePrices.isEmpty())
			{
				activePrices.clear();
				activePrices.putAll(livePrices);
				lastUpdatedEpochSec = nowSec;
				connectionState = ConnectionState.LIVE;
				statusDetailText = wikiPriceService.getConnectionStatusMessage();
			}
		}
		catch (Exception ex)
		{
			log.debug("[HerbWise] Live API fetch unavailable ({}); using cached fallback.", ex.getMessage());
			Map<Integer, ItemPrice> fallback = priceCacheService.getOrLoadFallbackPrices();
			if (!fallback.isEmpty())
			{
				activePrices.clear();
				activePrices.putAll(fallback);
				connectionState = ConnectionState.CACHED_FALLBACK;
				lastUpdatedEpochSec = priceCacheService.getLastLiveSaveEpochSec() > 0
					? priceCacheService.getLastLiveSaveEpochSec()
					: PriceCacheService.BUNDLED_SNAPSHOT_EPOCH_SEC;
				statusDetailText = "Using cached price snapshot (Live API unreachable)";
			}
			else
			{
				connectionState = ConnectionState.ERROR;
				statusDetailText = "API Error: " + ex.getMessage();
			}
		}
		finally
		{
			try
			{
				recalculateAll(nowSec);
				checkHighProfitNotifications();
			}
			finally
			{
				refreshRunning.set(false);
				notifyUiListeners();
			}
		}
	}

	/**
	 * Loads an explicit price map directly (used for deterministic unit testing).
	 */
	public synchronized void loadTestPrices(Map<Integer, ItemPrice> testPriceMap, long nowEpochSec)
	{
		activePrices.clear();
		if (testPriceMap != null)
		{
			activePrices.putAll(testPriceMap);
		}
		this.lastUpdatedEpochSec = nowEpochSec;
		this.connectionState = ConnectionState.LIVE;
		this.statusDetailText = "Test fixture data loaded";
		recalculateAll(nowEpochSec);
	}

	/**
	 * Recalculates all verified recipes and training recommendations using the current price map and settings.
	 */
	public synchronized void recalculateAll(long nowEpochSec)
	{
		PlayerHerbloreData playerData = playerSkillService.getCurrentHerbloreData();
		int effectiveLevel = playerData != null ? playerData.getEffectiveHerbloreLevel() : 75;

		List<PotionRecipe> allVerified = wikiRecipeService.getAllVerifiedRecipes();
		List<PotionProfitResult> results = profitCalculator.calculateAll(
			allVerified,
			activePrices,
			priceCalculationMode,
			includeGeTax,
			effectiveLevel,
			defaultBudgetGp,
			nowEpochSec,
			enableStalePriceWarnings,
			PotionProfitCalculator.DEFAULT_STALE_THRESHOLD_SECONDS,
			enableVolatilityWarnings,
			PotionProfitCalculator.DEFAULT_VOLATILITY_SPREAD_PERCENT
		);

		this.allEvaluatedResults = Collections.unmodifiableList(results);
		this.currentRecommendations = Collections.unmodifiableMap(
			recommendationEngine.generateRecommendations(
				results,
				effectiveLevel,
				defaultBudgetGp,
				showAvailableAtLevelOnly
			)
		);
	}

	/**
	 * Filters and sorts evaluated recipes according to user selection, search text, and active settings.
	 */
	public List<PotionProfitResult> filterAndSortResults(
		FilterMode filterMode,
		SortMode sortMode,
		String searchQuery,
		boolean includeVariantsAndAlternatives)
	{
		FilterMode safeFilter = filterMode != null ? filterMode : FilterMode.ALL_POTIONS;
		SortMode safeSort = sortMode != null ? sortMode : SortMode.BEST_OVERALL_TRAINING;
		String query = searchQuery != null ? searchQuery.trim().toLowerCase(Locale.ROOT) : "";

		List<PotionProfitResult> filtered = allEvaluatedResults.stream()
			.filter(r -> r != null && r.getRecipe() != null)
			.filter(r -> {
				PotionRecipe rec = r.getRecipe();
				if (safeFilter == FilterMode.UNFINISHED_POTIONS)
				{
					if (!rec.isUnfinishedPotionRecipe())
					{
						return false;
					}
				}
				else
				{
					// Exclude 0-XP Unfinished Potions from completed potion recommendations
					if (rec.isUnfinishedPotionRecipe())
					{
						return false;
					}
					if (!includeVariantsAndAlternatives && rec.isAlternativeRecipe())
					{
						return false;
					}
				}

				if (!query.isEmpty())
				{
					boolean matchesName = rec.getPotionName().toLowerCase(Locale.ROOT).contains(query)
						|| rec.getFamilyName().toLowerCase(Locale.ROOT).contains(query)
						|| rec.getFormattedIngredientList().toLowerCase(Locale.ROOT).contains(query)
						|| rec.getCategory().getDisplayName().toLowerCase(Locale.ROOT).contains(query);
					if (!matchesName)
					{
						return false;
					}
				}

				if (showProfitableOnly && !r.isProfitable())
				{
					return false;
				}
				if (showAvailableAtLevelOnly && !r.isAvailableAtPlayerLevel())
				{
					return false;
				}

				switch (safeFilter)
				{
					case PROFITABLE_ONLY:
						return r.isProfitable();
					case LOSS_MAKING_ONLY:
						return r.isLossMaking();
					case AFFORDABLE_RECIPES:
						return r.isAffordableWithinBudget();
					case AVAILABLE_AT_MY_LEVEL:
						return r.isAvailableAtPlayerLevel();
					case BEST_XP_METHODS:
						return r.getXpPerPotion() >= 130.0;
					case BEST_MONEY_MAKING:
						return r.isHasCompletePriceData() && r.getNetProfitPerPotion() > -150L;
					case UNFINISHED_POTIONS:
					case ALL_POTIONS:
					default:
						return true;
				}
			})
			.collect(Collectors.toCollection(ArrayList::new));

		Comparator<PotionProfitResult> comparator = buildComparator(safeSort);
		filtered.sort(comparator);
		return filtered;
	}

	private Comparator<PotionProfitResult> buildComparator(SortMode sortMode)
	{
		// Always push items with missing price data to the bottom when sorting by financial metrics
		Comparator<PotionProfitResult> hasDataFirst = Comparator.comparing(
			(PotionProfitResult r) -> r.isHasCompletePriceData() ? 0 : 1
		);

		switch (sortMode)
		{
			case HIGHEST_PROFIT:
			case LOWEST_LOSS:
				return hasDataFirst.thenComparing(
					Comparator.comparingLong(PotionProfitResult::getNetProfitPerPotion).reversed()
				);

			case CHEAPEST_XP:
				return hasDataFirst
					.thenComparing((PotionProfitResult r) -> r.getXpPerPotion() > 0.0 ? 0 : 1)
					.thenComparingDouble(PotionProfitResult::getNetCostPerXp);

			case HIGHEST_XP:
				return Comparator.comparingDouble(PotionProfitResult::getXpPerPotion).reversed()
					.thenComparing(Comparator.comparingLong(PotionProfitResult::getNetProfitPerPotion).reversed());

			case HIGHEST_XP_HOUR:
				return Comparator.comparingLong(PotionProfitResult::getEstimatedXpPerHour).reversed()
					.thenComparingDouble(PotionProfitResult::getOverallTrainingScore);

			case LOWEST_INGREDIENT_COST:
				return Comparator.comparing((PotionProfitResult r) -> r.isHasIngredientPriceData() ? 0 : 1)
					.thenComparingLong(PotionProfitResult::getTotalIngredientCost);

			case LEVEL_ASCENDING:
				return Comparator.comparingInt((PotionProfitResult r) -> r.getRecipe().getRequiredLevel())
					.thenComparingInt((PotionProfitResult r) -> -r.getRecipe().getDoses());

			case BEST_OVERALL_TRAINING:
			default:
				return hasDataFirst.thenComparing(
					Comparator.comparingDouble(PotionProfitResult::getOverallTrainingScore).reversed()
				);
		}
	}

	/**
	 * Fetches time-series market analysis asynchronously for the Market Analysis tab.
	 */
	public void fetchTimeseriesAsync(
		int itemId,
		String timestep,
		java.util.function.Consumer<MarketTrendAnalysis> onSuccess,
		java.util.function.Consumer<String> onError)
	{
		Runnable task = () -> {
			try
			{
				MarketTrendAnalysis analysis = wikiPriceService.fetchTimeseries(itemId, timestep);
				SwingUtilities.invokeLater(() -> {
					if (onSuccess != null)
					{
						onSuccess.accept(analysis);
					}
				});
			}
			catch (IOException ex)
			{
				SwingUtilities.invokeLater(() -> {
					if (onError != null)
					{
						onError.accept(ex.getMessage());
					}
				});
			}
		};

		if (executorService != null && !executorService.isShutdown())
		{
			executorService.submit(task);
		}
	}

	/**
	 * Attaches an OSRS item icon to a {@link JLabel} via RuneLite's {@link ItemManager},
	 * falling back to a custom-painted OSRS potion vial icon if running outside a live client cache.
	 */
	public void attachItemIcon(JLabel label, int itemId, String itemName)
	{
		if (label == null)
		{
			return;
		}
		label.setPreferredSize(new Dimension(28, 28));
		label.setMinimumSize(new Dimension(28, 28));
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setVerticalAlignment(SwingConstants.CENTER);

		if (itemManager != null && itemId > 0)
		{
			try
			{
				AsyncBufferedImage img = itemManager.getImage(itemId);
				if (img != null)
				{
					img.addTo(label);
					return;
				}
			}
			catch (Throwable ignored)
			{
			}
		}

		label.setIcon(new ImageIcon(createPotionVialFallbackIcon(itemName)));
	}

	public static BufferedImage createPotionVialFallbackIcon(String itemName)
	{
		BufferedImage img = new BufferedImage(26, 26, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Dark charcoal badge
		g.setColor(new Color(0x1E, 0x24, 0x2B));
		g.fillRoundRect(1, 1, 24, 24, 6, 6);

		// Subtle herblore green border
		Color liquidColor = pickPotionTint(itemName);
		g.setColor(liquidColor);
		g.setStroke(new BasicStroke(1.2f));
		g.drawRoundRect(1, 1, 23, 23, 6, 6);

		// Draw miniature potion flask silhouette
		g.setColor(new Color(0x94, 0xA3, 0xB8));
		g.fillRect(10, 4, 6, 3); // Cork/Neck
		g.setColor(liquidColor);
		g.fillRoundRect(6, 8, 14, 13, 6, 6); // Liquid bulb

		String badge = "P";
		if (itemName != null && !itemName.isEmpty())
		{
			if (itemName.contains("(4)")) badge = "4";
			else if (itemName.contains("(3)")) badge = "3";
			else if (itemName.contains("(2)")) badge = "2";
			else if (itemName.contains("(1)")) badge = "1";
			else badge = itemName.substring(0, 1).toUpperCase(Locale.ROOT);
		}

		g.setFont(new Font("SansSerif", Font.BOLD, 9));
		g.setColor(new Color(0x0D, 0x11, 0x17));
		FontMetrics fm = g.getFontMetrics();
		int tx = 6 + (14 - fm.stringWidth(badge)) / 2;
		int ty = 8 + ((13 - fm.getHeight()) / 2) + fm.getAscent();
		g.drawString(badge, tx, ty);

		g.dispose();
		return img;
	}

	private static Color pickPotionTint(String name)
	{
		if (name == null) return new Color(0x22, 0xC5, 0x5E);
		String lower = name.toLowerCase(Locale.ROOT);
		if (lower.contains("prayer") || lower.contains("ranging") || lower.contains("antivenom"))
		{
			return new Color(0x2D, 0xD4, 0xBF); // Teal/Cyan
		}
		if (lower.contains("restore") || lower.contains("zamorak") || lower.contains("bastion") || lower.contains("blood"))
		{
			return new Color(0xF4, 0x3F, 0x5E); // Crimson/Pink
		}
		if (lower.contains("saradomin") || lower.contains("strength"))
		{
			return new Color(0xFA, 0xCC, 0x15); // Gold/Yellow
		}
		if (lower.contains("antifire") || lower.contains("ancient") || lower.contains("forgotten"))
		{
			return new Color(0xA8, 0x55, 0xF7); // Purple
		}
		if (lower.contains("attack") || lower.contains("magic"))
		{
			return new Color(0x38, 0xBD, 0xF8); // Sky Blue
		}
		return new Color(0x22, 0xC5, 0x5E); // Herblore Emerald
	}

	private void checkHighProfitNotifications()
	{
		if (!enableNotifications || notifier == null)
		{
			return;
		}
		TrainingRecommendation bestProfit = currentRecommendations.get(Category.BEST_PROFIT);
		if (bestProfit != null && bestProfit.getResult() != null
			&& bestProfit.getResult().getNetProfitPerPotion() >= 500L)
		{
			PotionProfitResult r = bestProfit.getResult();
			notifier.notify("[HerbWise] High-profit potion: " + r.getRecipe().getPotionName()
				+ " (+" + r.getNetProfitPerPotion() + " GP/potion)");
		}
	}

	public String getFormattedLastUpdated()
	{
		if (lastUpdatedEpochSec <= 0L)
		{
			return "Never";
		}
		long ageSec = Math.max(0L, Instant.now().getEpochSecond() - lastUpdatedEpochSec);
		if (connectionState == ConnectionState.CACHED_FALLBACK)
		{
			return "Cached Snapshot (" + formatAgeShort(ageSec) + " old)";
		}
		return TIME_FMT.format(Instant.ofEpochSecond(lastUpdatedEpochSec)) + " (" + formatAgeShort(ageSec) + " ago)";
	}

	public static String formatAgeShort(long seconds)
	{
		if (seconds < 60L)
		{
			return seconds + "s";
		}
		long mins = seconds / 60L;
		if (mins < 60L)
		{
			return mins + "m";
		}
		long hours = mins / 60L;
		if (hours < 48L)
		{
			return hours + "h";
		}
		return (hours / 24L) + "d";
	}

	public void addUiUpdateListener(Runnable listener)
	{
		if (listener != null)
		{
			uiUpdateListeners.add(listener);
		}
	}

	private void notifyUiListeners()
	{
		SwingUtilities.invokeLater(() -> {
			for (Runnable r : uiUpdateListeners)
			{
				try
				{
					r.run();
				}
				catch (Exception ignored)
				{
				}
			}
		});
	}

	public List<PotionProfitResult> getAllEvaluatedResults() { return allEvaluatedResults; }
	public Map<Category, TrainingRecommendation> getCurrentRecommendations() { return currentRecommendations; }
	public Map<Integer, ItemPrice> getActivePrices() { return Collections.unmodifiableMap(activePrices); }
	public ItemPrice getItemPrice(int itemId) { return activePrices.get(itemId); }
	public Optional<PotionProfitResult> getResultByRecipeId(String recipeId)
	{
		if (recipeId == null) return Optional.empty();
		return allEvaluatedResults.stream()
			.filter(r -> recipeId.equals(r.getRecipe().getRecipeId()))
			.findFirst();
	}

	public WikiPriceService getWikiPriceService() { return wikiPriceService; }
	public WikiRecipeService getWikiRecipeService() { return wikiRecipeService; }
	public PriceCacheService getPriceCacheService() { return priceCacheService; }
	public PlayerSkillService getPlayerSkillService() { return playerSkillService; }
	public PotionProfitCalculator getProfitCalculator() { return profitCalculator; }
	public GeTaxCalculator getGeTaxCalculator() { return geTaxCalculator; }
	public HerbloreXpCalculator getXpCalculator() { return xpCalculator; }
	public BudgetCalculator getBudgetCalculator() { return budgetCalculator; }
	public TrainingRecommendationEngine getRecommendationEngine() { return recommendationEngine; }
	public ProductionTimeCalculator getProductionTimeCalculator() { return productionTimeCalculator; }

	public ConnectionState getConnectionState() { return connectionState; }
	public String getStatusDetailText() { return statusDetailText; }
	public long getLastUpdatedEpochSec() { return lastUpdatedEpochSec; }
	public boolean isRefreshRunning() { return refreshRunning.get(); }

	public int getRefreshIntervalSeconds() { return refreshIntervalSeconds; }
	public PriceCalculationMode getPriceCalculationMode() { return priceCalculationMode; }
	public void setPriceCalculationMode(PriceCalculationMode mode)
	{
		if (mode != null)
		{
			this.priceCalculationMode = mode;
			recalculateAll(Instant.now().getEpochSecond());
			notifyUiListeners();
		}
	}
	public boolean isShowProfitableOnly() { return showProfitableOnly; }
	public boolean isShowAvailableAtLevelOnly() { return showAvailableAtLevelOnly; }
	public boolean isIncludeGeTax() { return includeGeTax; }
	public void setIncludeGeTax(boolean includeGeTax)
	{
		this.includeGeTax = includeGeTax;
		recalculateAll(Instant.now().getEpochSecond());
		notifyUiListeners();
	}
	public boolean isEnableVolatilityWarnings() { return enableVolatilityWarnings; }
	public boolean isEnableStalePriceWarnings() { return enableStalePriceWarnings; }
	public long getDefaultBudgetGp() { return defaultBudgetGp; }
	public int getDefaultTargetLevel() { return defaultTargetLevel; }
	public boolean isShowXpPerHourEstimates() { return showXpPerHourEstimates; }
	public TableDisplayMode getTableDisplayMode() { return tableDisplayMode; }
	public HerbloreBoostMode getHerbloreBoostMode() { return herbloreBoostMode; }
	public boolean isEnableNotifications() { return enableNotifications; }
}
