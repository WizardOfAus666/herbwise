package com.herbwise.ui;

import com.herbwise.models.PlayerHerbloreData;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.TrainingRecommendation;
import com.herbwise.models.TrainingRecommendation.Category;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.ui.HerbWiseTheme.HerbButton;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * Main Dashboard tab (Sections 5, 6, and 8).
 * Displays:
 * - Player Herblore Level, XP, XP Remaining to Next Level, and Unlockable Potions
 * - Top 4 Summary Statistics (Best Profit/Potion, Cheapest XP, Best XP/GP, Total Available Recipes)
 * - 6 Intelligent Training Recommendations with transparent "Why?" explanations
 * - Main Recommendations Table with 10 sortable columns, filters, and search box
 */
public class HerbWiseDashboard extends JPanel
{
	private final HerbloreDataService dataService;
	private final Consumer<PotionProfitResult> onPotionSelected;

	// Player Herblore Status Card
	private final JLabel playerLevelLabel;
	private final JLabel playerXpLabel;
	private final JLabel xpRemainingLabel;
	private final JLabel unlockedCountLabel;
	private final JLabel nextUnlockLabel;
	private final JProgressBar levelProgressBar;
	private final JPanel offlineLevelRow;
	private final JSpinner offlineLevelSpinner;
	private boolean suppressSpinnerEvent = false;

	// Top 4 Summary Cards
	private final JLabel bestProfitValueLabel;
	private final JLabel bestProfitSubLabel;
	private final JLabel cheapestXpValueLabel;
	private final JLabel cheapestXpSubLabel;
	private final JLabel bestXpPerGpValueLabel;
	private final JLabel bestXpPerGpSubLabel;
	private final JLabel totalAvailableValueLabel;
	private final JLabel totalAvailableSubLabel;

	// Training Recommendation Engine Card (Section 8)
	private final JComboBox<Category> recCategoryCombo;
	private final JLabel recIconLabel;
	private final JLabel recPotionNameLabel;
	private final JLabel recPrimaryMetricLabel;
	private final JLabel recScoreBadgeLabel;
	private final JPanel recReasonsPanel;
	private final HerbButton inspectRecButton;
	private PotionProfitResult activeRecommendedResult;

	// Main Recommendations Table (Section 5)
	private final HerbWisePotionTable mainRecommendationsTable;

	public HerbWiseDashboard(
		HerbloreDataService dataService,
		Consumer<PotionProfitResult> onPotionSelected)
	{
		this.dataService = dataService;
		this.onPotionSelected = onPotionSelected;

		setLayout(new BorderLayout());
		setBackground(HerbWiseTheme.BG_MATTE_BLACK);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		content.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

		// ====================================================================
		// 1. PLAYER HERBLORE SKILL INTEGRATION CARD (Section 6)
		// ====================================================================
		JPanel playerCard = HerbWiseTheme.createCardPanel();
		playerCard.setLayout(new BoxLayout(playerCard, BoxLayout.Y_AXIS));
		playerCard.setBorder(HerbWiseTheme.CARD_BORDER_ACCENT);

		JPanel playerTopRow = new JPanel(new BorderLayout(6, 0));
		playerTopRow.setOpaque(false);
		playerLevelLabel = new JLabel("Herblore Lvl 75");
		playerLevelLabel.setFont(HerbWiseTheme.getSectionFont());
		playerLevelLabel.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		playerTopRow.add(playerLevelLabel, BorderLayout.CENTER);

		unlockedCountLabel = new JLabel("0 / 0 Unlocked");
		unlockedCountLabel.setFont(HerbWiseTheme.getBadgeFont());
		unlockedCountLabel.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		unlockedCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		playerTopRow.add(unlockedCountLabel, BorderLayout.EAST);
		playerCard.add(playerTopRow);
		playerCard.add(Box.createRigidArea(new Dimension(0, 4)));

		JPanel xpRow = new JPanel(new GridLayout(1, 2, 4, 0));
		xpRow.setOpaque(false);
		playerXpLabel = new JLabel("XP: 1,210,421");
		playerXpLabel.setFont(HerbWiseTheme.getSmallFont());
		playerXpLabel.setForeground(HerbWiseTheme.TEXT_PRIMARY);
		xpRemainingLabel = new JLabel("Next Lvl: 125,842 XP");
		xpRemainingLabel.setFont(HerbWiseTheme.getSmallFont());
		xpRemainingLabel.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		xpRemainingLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		xpRow.add(playerXpLabel);
		xpRow.add(xpRemainingLabel);
		playerCard.add(xpRow);
		playerCard.add(Box.createRigidArea(new Dimension(0, 4)));

		levelProgressBar = new JProgressBar(0, 100);
		levelProgressBar.setValue(45);
		levelProgressBar.setStringPainted(true);
		levelProgressBar.setFont(HerbWiseTheme.getBadgeFont());
		levelProgressBar.setBackground(HerbWiseTheme.BG_INPUT);
		levelProgressBar.setForeground(HerbWiseTheme.ACCENT_HERB);
		levelProgressBar.setBorder(BorderFactory.createLineBorder(HerbWiseTheme.BORDER_SUBTLE, 1));
		levelProgressBar.setPreferredSize(new Dimension(180, 14));
		playerCard.add(levelProgressBar);
		playerCard.add(Box.createRigidArea(new Dimension(0, 4)));

		nextUnlockLabel = new JLabel("Next unlock: Checking...");
		nextUnlockLabel.setFont(HerbWiseTheme.getSmallFont());
		nextUnlockLabel.setForeground(HerbWiseTheme.TEXT_MUTED);
		playerCard.add(nextUnlockLabel);

		// Offline-only level control (hidden automatically when logged into RuneLite client)
		offlineLevelRow = new JPanel(new BorderLayout(6, 0));
		offlineLevelRow.setOpaque(false);
		offlineLevelRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		JLabel offlineHint = new JLabel("Offline Preview Level:");
		offlineHint.setFont(HerbWiseTheme.getSmallFont());
		offlineHint.setForeground(HerbWiseTheme.COLOR_STALE_YELLOW);
		offlineLevelRow.add(offlineHint, BorderLayout.CENTER);

		offlineLevelSpinner = new JSpinner(new SpinnerNumberModel(75, 1, 99, 1));
		offlineLevelSpinner.setFont(HerbWiseTheme.getSmallFont());
		offlineLevelSpinner.addChangeListener(e -> {
			if (!suppressSpinnerEvent && dataService != null)
			{
				int lvl = (Integer) offlineLevelSpinner.getValue();
				dataService.getPlayerSkillService().setOfflineManualLevel(lvl);
			}
		});
		offlineLevelRow.add(offlineLevelSpinner, BorderLayout.EAST);
		playerCard.add(offlineLevelRow);

		content.add(playerCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 2. TOP SUMMARY (4 STATISTICS - Section 5)
		// ====================================================================
		JPanel summaryGrid = new JPanel(new GridLayout(2, 2, 6, 5));
		summaryGrid.setOpaque(false);

		bestProfitValueLabel = new JLabel("—");
		bestProfitSubLabel = new JLabel("—");
		summaryGrid.add(createStatCard("1. BEST PROFIT / POTION", bestProfitValueLabel, bestProfitSubLabel));

		cheapestXpValueLabel = new JLabel("—");
		cheapestXpSubLabel = new JLabel("—");
		summaryGrid.add(createStatCard("2. CHEAPEST XP", cheapestXpValueLabel, cheapestXpSubLabel));

		bestXpPerGpValueLabel = new JLabel("—");
		bestXpPerGpSubLabel = new JLabel("—");
		summaryGrid.add(createStatCard("3. BEST XP PER GP", bestXpPerGpValueLabel, bestXpPerGpSubLabel));

		totalAvailableValueLabel = new JLabel("—");
		totalAvailableSubLabel = new JLabel("—");
		summaryGrid.add(createStatCard("4. AVAILABLE RECIPES", totalAvailableValueLabel, totalAvailableSubLabel));

		content.add(summaryGrid);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 3. TRAINING RECOMMENDATION ENGINE (Section 8)
		// ====================================================================
		JPanel recCard = HerbWiseTheme.createCardPanel();
		recCard.setLayout(new BoxLayout(recCard, BoxLayout.Y_AXIS));

		JPanel recHeaderRow = new JPanel(new BorderLayout(4, 0));
		recHeaderRow.setOpaque(false);
		JLabel recHeaderLbl = HerbWiseTheme.createSectionHeader("Smart Recommendation");
		recHeaderRow.add(recHeaderLbl, BorderLayout.CENTER);

		recScoreBadgeLabel = new JLabel("Score: —");
		recScoreBadgeLabel.setFont(HerbWiseTheme.getBadgeFont());
		recScoreBadgeLabel.setForeground(HerbWiseTheme.ACCENT_GOLD);
		recHeaderRow.add(recScoreBadgeLabel, BorderLayout.EAST);
		recCard.add(recHeaderRow);
		recCard.add(Box.createRigidArea(new Dimension(0, 4)));

		recCategoryCombo = new JComboBox<>(Category.values());
		HerbWiseTheme.styleComboBox(recCategoryCombo);
		recCategoryCombo.setSelectedItem(Category.BEST_BALANCED);
		recCategoryCombo.addActionListener(e -> updateRecommendationDisplay());
		recCard.add(recCategoryCombo);
		recCard.add(Box.createRigidArea(new Dimension(0, 6)));

		// Recommended Potion Banner
		JPanel recBanner = new JPanel(new BorderLayout(8, 0));
		recBanner.setBackground(HerbWiseTheme.BG_DEEP_CHARCOAL);
		recBanner.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(HerbWiseTheme.BORDER_HERB_GREEN, 1),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)
		));
		recBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		recBanner.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (activeRecommendedResult != null && onPotionSelected != null)
				{
					onPotionSelected.accept(activeRecommendedResult);
				}
			}
		});

		recIconLabel = new JLabel();
		recBanner.add(recIconLabel, BorderLayout.WEST);

		JPanel recTitleCol = new JPanel(new GridLayout(2, 1, 0, 1));
		recTitleCol.setOpaque(false);
		recPotionNameLabel = new JLabel("Super attack(4)");
		recPotionNameLabel.setFont(HerbWiseTheme.getBoldFont());
		recPotionNameLabel.setForeground(HerbWiseTheme.TEXT_PRIMARY);
		recPrimaryMetricLabel = new JLabel("—");
		recPrimaryMetricLabel.setFont(HerbWiseTheme.getSmallFont());
		recPrimaryMetricLabel.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		recTitleCol.add(recPotionNameLabel);
		recTitleCol.add(recPrimaryMetricLabel);
		recBanner.add(recTitleCol, BorderLayout.CENTER);

		inspectRecButton = new HerbButton("Inspect");
		inspectRecButton.addActionListener(e -> {
			if (activeRecommendedResult != null && onPotionSelected != null)
			{
				onPotionSelected.accept(activeRecommendedResult);
			}
		});
		recBanner.add(inspectRecButton, BorderLayout.EAST);

		recCard.add(recBanner);
		recCard.add(Box.createRigidArea(new Dimension(0, 5)));

		JLabel whyTitle = new JLabel("Why this recommendation?");
		whyTitle.setFont(HerbWiseTheme.getBadgeFont());
		whyTitle.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		whyTitle.setAlignmentX(LEFT_ALIGNMENT);
		recCard.add(whyTitle);
		recCard.add(Box.createRigidArea(new Dimension(0, 3)));

		recReasonsPanel = new JPanel();
		recReasonsPanel.setLayout(new BoxLayout(recReasonsPanel, BoxLayout.Y_AXIS));
		recReasonsPanel.setOpaque(false);
		recCard.add(recReasonsPanel);

		content.add(recCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 4. MAIN RECOMMENDATIONS TABLE (Section 5)
		// ====================================================================
		JPanel tableSection = HerbWiseTheme.createCardPanel();
		tableSection.setLayout(new BorderLayout(0, 4));
		JLabel tableHeader = HerbWiseTheme.createSectionHeader("All Evaluated Potions (Click Row for Breakdown)");
		tableSection.add(tableHeader, BorderLayout.NORTH);

		mainRecommendationsTable = new HerbWisePotionTable(dataService, onPotionSelected, false);
		tableSection.add(mainRecommendationsTable, BorderLayout.CENTER);
		content.add(tableSection);

		JScrollPane scrollPane = HerbWiseTheme.createStyledScrollPane(content);
		add(scrollPane, BorderLayout.CENTER);

		refresh();
	}

	private JPanel createStatCard(String title, JLabel mainValue, JLabel subText)
	{
		JPanel card = HerbWiseTheme.createCardPanel();
		card.setLayout(new GridLayout(3, 1, 0, 1));

		JLabel titleLbl = new JLabel(title);
		titleLbl.setFont(HerbWiseTheme.getBadgeFont());
		titleLbl.setForeground(HerbWiseTheme.TEXT_MUTED);

		mainValue.setFont(HerbWiseTheme.getBoldFont());
		mainValue.setForeground(HerbWiseTheme.TEXT_PRIMARY);

		subText.setFont(HerbWiseTheme.getSmallFont());
		subText.setForeground(HerbWiseTheme.TEXT_SECONDARY);

		card.add(titleLbl);
		card.add(mainValue);
		card.add(subText);
		return card;
	}

	public void refresh()
	{
		if (dataService == null)
		{
			return;
		}

		// 1. Update Player Herblore Skill Data
		PlayerHerbloreData player = dataService.getPlayerSkillService().getCurrentHerbloreData();
		int effectiveLvl = player.getEffectiveHerbloreLevel();

		if (player.getAppliedBoostLevels() > 0 || effectiveLvl > player.getCurrentLevel())
		{
			playerLevelLabel.setText("Herblore Lvl " + player.getCurrentLevel()
				+ " (Boosted: " + effectiveLvl + ")");
		}
		else
		{
			playerLevelLabel.setText("Herblore Lvl " + player.getCurrentLevel());
		}

		playerXpLabel.setText("XP: " + HerbWiseTheme.formatNumber(player.getCurrentXp()));
		if (player.getCurrentLevel() >= 99)
		{
			xpRemainingLabel.setText("Max Level (99)");
		}
		else
		{
			xpRemainingLabel.setText("To " + player.getNextLevel() + ": "
				+ HerbWiseTheme.formatNumber(player.getXpRemainingToNextLevel()) + " XP");
		}

		int pct = (int) Math.round(player.getLevelProgressPercent());
		levelProgressBar.setValue(Math.max(0, Math.min(100, pct)));
		levelProgressBar.setString(String.format(Locale.US, "%.1f%% to Level %d",
			player.getLevelProgressPercent(), player.getNextLevel()));

		// Show manual level spinner ONLY when outside an active game session
		offlineLevelRow.setVisible(!player.isLoggedIn());
		if (!player.isLoggedIn())
		{
			suppressSpinnerEvent = true;
			try
			{
				offlineLevelSpinner.setValue(player.getCurrentLevel());
			}
			finally
			{
				suppressSpinnerEvent = false;
			}
		}

		// 2. Compute Summary Statistics across standard completed potions
		List<PotionProfitResult> allResults = dataService.getAllEvaluatedResults();
		int totalCompletedRecipes = 0;
		int unlockedCompletedRecipes = 0;
		PotionProfitResult bestProfit = null;
		PotionProfitResult cheapestXp = null;
		PotionProfitResult bestXpPerGp = null;
		PotionRecipe nextUnlockRecipe = null;
		int smallestUnlockGap = Integer.MAX_VALUE;

		for (PotionProfitResult r : allResults)
		{
			if (r == null || r.getRecipe() == null)
			{
				continue;
			}
			PotionRecipe rec = r.getRecipe();
			if (rec.isUnfinishedPotionRecipe())
			{
				continue;
			}

			if (!rec.isAlternativeRecipe())
			{
				totalCompletedRecipes++;
				if (r.isAvailableAtPlayerLevel())
				{
					unlockedCompletedRecipes++;
				}
				else
				{
					int gap = r.getLevelsUntilUnlock();
					if (gap > 0 && gap < smallestUnlockGap)
					{
						smallestUnlockGap = gap;
						nextUnlockRecipe = rec;
					}
				}
			}

			boolean allIngredientsTradeable = rec.getIngredients().stream()
				.allMatch(com.herbwise.models.PotionIngredient::isTradeable);

			if (!r.isHasCompletePriceData() || rec.isAlternativeRecipe() || !allIngredientsTradeable)
			{
				continue;
			}

			if (bestProfit == null || r.getNetProfitPerPotion() > bestProfit.getNetProfitPerPotion())
			{
				bestProfit = r;
			}

			if (r.getXpPerPotion() >= 20.0)
			{
				if (cheapestXp == null || r.getNetCostPerXp() < cheapestXp.getNetCostPerXp())
				{
					cheapestXp = r;
				}
				if (bestXpPerGp == null || r.getIngredientCostPerXp() < bestXpPerGp.getIngredientCostPerXp())
				{
					bestXpPerGp = r;
				}
			}
		}

		unlockedCountLabel.setText(unlockedCompletedRecipes + " / " + totalCompletedRecipes + " Unlocked");
		if (nextUnlockRecipe != null)
		{
			nextUnlockLabel.setText("Next unlock in " + smallestUnlockGap + " lvl"
				+ (smallestUnlockGap == 1 ? "" : "s") + ": " + nextUnlockRecipe.getPotionName()
				+ " (Lvl " + nextUnlockRecipe.getRequiredLevel() + ")");
		}
		else
		{
			nextUnlockLabel.setText("All Herblore potion tiers unlocked!");
		}

		// Card 1: Best Profit Per Potion
		if (bestProfit != null)
		{
			bestProfitValueLabel.setText(HerbWiseTheme.formatSignedGp(bestProfit.getNetProfitPerPotion()));
			bestProfitValueLabel.setForeground(bestProfit.getNetProfitPerPotion() >= 0
				? HerbWiseTheme.COLOR_PROFIT_GREEN
				: HerbWiseTheme.COLOR_LOSS_RED);
			bestProfitSubLabel.setText(bestProfit.getRecipe().getPotionName());
		}
		else
		{
			bestProfitValueLabel.setText("Unavailable");
			bestProfitValueLabel.setForeground(HerbWiseTheme.COLOR_MISSING_GREY);
			bestProfitSubLabel.setText("No price data");
		}

		// Card 2: Cheapest XP
		if (cheapestXp != null)
		{
			if (cheapestXp.getNetCostPerXp() <= 0.0)
			{
				cheapestXpValueLabel.setText(String.format(Locale.US, "+%.2f GP/XP", Math.abs(cheapestXp.getNetCostPerXp())));
				cheapestXpValueLabel.setForeground(HerbWiseTheme.COLOR_PROFIT_GREEN);
			}
			else
			{
				cheapestXpValueLabel.setText(String.format(Locale.US, "%.2f GP/XP", cheapestXp.getNetCostPerXp()));
				cheapestXpValueLabel.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
			}
			cheapestXpSubLabel.setText(cheapestXp.getRecipe().getPotionName());
		}
		else
		{
			cheapestXpValueLabel.setText("Unavailable");
			cheapestXpSubLabel.setText("—");
		}

		// Card 3: Best XP Per GP
		if (bestXpPerGp != null && bestXpPerGp.getIngredientCostPerXp() > 0.0)
		{
			double xpPer1000Gp = 1000.0 / bestXpPerGp.getIngredientCostPerXp();
			bestXpPerGpValueLabel.setText(String.format(Locale.US, "%.1f XP / 1K GP", xpPer1000Gp));
			bestXpPerGpValueLabel.setForeground(HerbWiseTheme.COLOR_INFO_CYAN);
			bestXpPerGpSubLabel.setText(bestXpPerGp.getRecipe().getPotionName()
				+ " (" + String.format(Locale.US, "%.2f GP/XP", bestXpPerGp.getIngredientCostPerXp()) + ")");
		}
		else
		{
			bestXpPerGpValueLabel.setText("Unavailable");
			bestXpPerGpSubLabel.setText("—");
		}

		// Card 4: Total Available Recipes
		totalAvailableValueLabel.setText(unlockedCompletedRecipes + " Available");
		totalAvailableValueLabel.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		totalAvailableSubLabel.setText(allResults.size() + " total recipes in DB");

		// 3. Update Recommendation Card
		updateRecommendationDisplay();

		// 4. Refresh Main Table
		mainRecommendationsTable.refreshTable();
	}

	private void updateRecommendationDisplay()
	{
		if (dataService == null)
		{
			return;
		}
		Category selectedCategory = (Category) recCategoryCombo.getSelectedItem();
		if (selectedCategory == null)
		{
			selectedCategory = Category.BEST_BALANCED;
		}

		Map<Category, TrainingRecommendation> recs = dataService.getCurrentRecommendations();
		TrainingRecommendation rec = recs != null ? recs.get(selectedCategory) : null;

		recReasonsPanel.removeAll();

		if (rec == null || rec.getResult() == null)
		{
			activeRecommendedResult = null;
			recPotionNameLabel.setText("No matching recipe for criteria");
			recPrimaryMetricLabel.setText(selectedCategory.getSubtitle());
			recScoreBadgeLabel.setText("Score: —");
			inspectRecButton.setEnabled(false);
			recReasonsPanel.revalidate();
			recReasonsPanel.repaint();
			return;
		}

		activeRecommendedResult = rec.getResult();
		PotionRecipe recipe = activeRecommendedResult.getRecipe();
		inspectRecButton.setEnabled(true);

		dataService.attachItemIcon(recIconLabel, recipe.getPotionItemId(), recipe.getPotionName());
		recPotionNameLabel.setText(recipe.getPotionName() + " (Lvl " + recipe.getRequiredLevel() + ")");
		recPrimaryMetricLabel.setText(rec.getHeadlineMetric());
		recScoreBadgeLabel.setText(String.format(Locale.US, "Score: %.0f/100", rec.getScore()));

		for (String reason : rec.getReasons())
		{
			JLabel bullet = new JLabel("<html><body style='width: 175px'>• " + reason + "</body></html>");
			bullet.setFont(HerbWiseTheme.getSmallFont());
			bullet.setForeground(HerbWiseTheme.TEXT_SECONDARY);
			bullet.setBorder(BorderFactory.createEmptyBorder(1, 2, 2, 2));
			bullet.setAlignmentX(LEFT_ALIGNMENT);
			recReasonsPanel.add(bullet);
		}

		recReasonsPanel.revalidate();
		recReasonsPanel.repaint();
	}

	public HerbWisePotionTable getMainRecommendationsTable()
	{
		return mainRecommendationsTable;
	}
}
