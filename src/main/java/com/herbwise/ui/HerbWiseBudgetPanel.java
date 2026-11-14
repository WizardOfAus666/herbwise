package com.herbwise.ui;

import com.herbwise.models.BudgetCalculationResult;
import com.herbwise.models.PlayerHerbloreData;
import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.XpProductionPlan;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.ui.HerbWiseTheme.HerbButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Dedicated Budget & Goal Quantity Calculator tab (Section 7).
 * Calculates:
 * - Maximum potions affordable within a GP budget
 * - Total ingredient cost, expected sale value, GE tax, and expected profit/loss
 * - Total XP gained, remaining GP, and inventory loads required
 * - Auto-recommends the best potion for the player's budget and level
 * - Includes Goal Quantity Calculator ("Want X XP" or "Reach Level Y")
 */
public class HerbWiseBudgetPanel extends JPanel
{
	private final HerbloreDataService dataService;

	// Budget Controls
	private final JTextField budgetInputField;
	private final JCheckBox autoRecommendCheck;
	private final JCheckBox filterUnlockedOnlyCheck;
	private final JComboBox<PotionChoiceItem> potionSelectorCombo;

	// Budget Results Labels
	private final JLabel selectedPotionLabel;
	private final JLabel maxAffordableLabel;
	private final JLabel totalIngredientCostLabel;
	private final JLabel expectedSaleValueLabel;
	private final JLabel expectedGeTaxLabel;
	private final JLabel expectedNetProfitLabel;
	private final JLabel totalXpGainedLabel;
	private final JLabel remainingGpLabel;
	private final JLabel inventoryLoadsLabel;
	private final JLabel estProductionTimeLabel;
	private final JPanel shoppingListContainer;

	// Quantity / Goal XP / Goal Level Calculator Controls (Section 7 bottom)
	private final JComboBox<String> goalModeCombo;
	private final JTextField goalInputField;
	private final JLabel goalPotionsNeededLabel;
	private final JLabel goalXpGainedLabel;
	private final JLabel goalTotalCostLabel;
	private final JLabel goalNetProfitLabel;
	private final JLabel goalInventoryLoadsLabel;
	private final JPanel goalIngredientsPanel;

	private boolean suppressComboEvents = false;

	private static class PotionChoiceItem
	{
		private final PotionProfitResult result;

		PotionChoiceItem(PotionProfitResult result)
		{
			this.result = result;
		}

		@Override
		public String toString()
		{
			if (result == null || result.getRecipe() == null)
			{
				return "—";
			}
			PotionRecipe r = result.getRecipe();
			return r.getPotionName() + " (Lvl " + r.getRequiredLevel() + " • "
				+ HerbWiseTheme.formatGp(result.getTotalIngredientCost()) + ")";
		}
	}

	public HerbWiseBudgetPanel(HerbloreDataService dataService)
	{
		this.dataService = dataService;

		setLayout(new BorderLayout());
		setBackground(HerbWiseTheme.BG_MATTE_BLACK);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		content.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

		// ====================================================================
		// 1. GP BUDGET INPUT & PRESETS CARD
		// ====================================================================
		JPanel budgetControlCard = HerbWiseTheme.createCardPanel();
		budgetControlCard.setLayout(new BoxLayout(budgetControlCard, BoxLayout.Y_AXIS));
		budgetControlCard.setBorder(HerbWiseTheme.CARD_BORDER_ACCENT);

		JLabel titleLbl = HerbWiseTheme.createSectionHeader("GP Budget Calculator");
		titleLbl.setAlignmentX(LEFT_ALIGNMENT);
		budgetControlCard.add(titleLbl);
		budgetControlCard.add(Box.createRigidArea(new Dimension(0, 5)));

		JPanel inputRow = new JPanel(new BorderLayout(6, 0));
		inputRow.setOpaque(false);
		inputRow.setAlignmentX(LEFT_ALIGNMENT);
		JLabel gpLbl = new JLabel("Budget (GP):");
		gpLbl.setFont(HerbWiseTheme.getSmallFont());
		gpLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		inputRow.add(gpLbl, BorderLayout.WEST);

		long initialBudget = dataService != null ? dataService.getDefaultBudgetGp() : 1_000_000L;
		budgetInputField = HerbWiseTheme.createStyledTextField(String.valueOf(initialBudget));
		budgetInputField.addActionListener(e -> recalculateBudget());
		inputRow.add(budgetInputField, BorderLayout.CENTER);

		HerbButton calcBtn = new HerbButton("Calculate", true);
		calcBtn.addActionListener(e -> recalculateBudget());
		inputRow.add(calcBtn, BorderLayout.EAST);
		budgetControlCard.add(inputRow);
		budgetControlCard.add(Box.createRigidArea(new Dimension(0, 5)));

		// Preset Buttons: 100K, 500K, 1M, 5M, 10M, 25M
		JPanel presetsGrid = new JPanel(new GridLayout(2, 3, 4, 4));
		presetsGrid.setOpaque(false);
		presetsGrid.setAlignmentX(LEFT_ALIGNMENT);

		long[] presetAmounts = new long[]{100_000L, 500_000L, 1_000_000L, 5_000_000L, 10_000_000L, 25_000_000L};
		String[] presetLabels = new String[]{"100K GP", "500K GP", "1M GP", "5M GP", "10M GP", "25M GP"};
		for (int i = 0; i < presetAmounts.length; i++)
		{
			final long amount = presetAmounts[i];
			HerbButton pBtn = new HerbButton(presetLabels[i]);
			pBtn.addActionListener(e -> {
				budgetInputField.setText(String.valueOf(amount));
				recalculateBudget();
			});
			presetsGrid.add(pBtn);
		}
		budgetControlCard.add(presetsGrid);
		budgetControlCard.add(Box.createRigidArea(new Dimension(0, 6)));

		// Auto-Recommend vs Specific Potion Selection
		potionSelectorCombo = new JComboBox<>();
		HerbWiseTheme.styleComboBox(potionSelectorCombo);
		potionSelectorCombo.setEnabled(false);
		potionSelectorCombo.setAlignmentX(LEFT_ALIGNMENT);

		autoRecommendCheck = new JCheckBox("Let HerbWise recommend best potion for budget", true);
		autoRecommendCheck.setOpaque(false);
		autoRecommendCheck.setFont(HerbWiseTheme.getSmallFont());
		autoRecommendCheck.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		autoRecommendCheck.setAlignmentX(LEFT_ALIGNMENT);
		autoRecommendCheck.addActionListener(e -> {
			potionSelectorCombo.setEnabled(!autoRecommendCheck.isSelected());
			recalculateBudget();
		});
		budgetControlCard.add(autoRecommendCheck);

		filterUnlockedOnlyCheck = new JCheckBox("Only include potions available at my Herblore level", true);
		filterUnlockedOnlyCheck.setOpaque(false);
		filterUnlockedOnlyCheck.setFont(HerbWiseTheme.getSmallFont());
		filterUnlockedOnlyCheck.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		filterUnlockedOnlyCheck.setAlignmentX(LEFT_ALIGNMENT);
		filterUnlockedOnlyCheck.addActionListener(e -> {
			populatePotionCombo();
			recalculateBudget();
		});
		budgetControlCard.add(filterUnlockedOnlyCheck);
		budgetControlCard.add(Box.createRigidArea(new Dimension(0, 4)));

		potionSelectorCombo.addActionListener(e -> {
			if (!suppressComboEvents)
			{
				recalculateBudget();
				recalculateGoalQuantity();
			}
		});
		budgetControlCard.add(potionSelectorCombo);

		content.add(budgetControlCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 2. BUDGET CALCULATION OUTPUT CARD
		// ====================================================================
		JPanel outputCard = HerbWiseTheme.createCardPanel();
		outputCard.setLayout(new BoxLayout(outputCard, BoxLayout.Y_AXIS));

		JLabel outHeader = HerbWiseTheme.createSectionHeader("Budget Breakdown");
		outHeader.setAlignmentX(LEFT_ALIGNMENT);
		outputCard.add(outHeader);
		outputCard.add(Box.createRigidArea(new Dimension(0, 4)));

		JPanel metricsGrid = new JPanel(new GridLayout(10, 1, 0, 3));
		metricsGrid.setOpaque(false);
		metricsGrid.setAlignmentX(LEFT_ALIGNMENT);

		selectedPotionLabel = createValueLabel(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		maxAffordableLabel = createValueLabel(HerbWiseTheme.TEXT_PRIMARY);
		totalIngredientCostLabel = createValueLabel(HerbWiseTheme.TEXT_PRIMARY);
		expectedSaleValueLabel = createValueLabel(HerbWiseTheme.TEXT_PRIMARY);
		expectedGeTaxLabel = createValueLabel(HerbWiseTheme.COLOR_STALE_YELLOW);
		expectedNetProfitLabel = createValueLabel(HerbWiseTheme.COLOR_PROFIT_GREEN);
		totalXpGainedLabel = createValueLabel(HerbWiseTheme.COLOR_INFO_CYAN);
		remainingGpLabel = createValueLabel(HerbWiseTheme.TEXT_SECONDARY);
		inventoryLoadsLabel = createValueLabel(HerbWiseTheme.TEXT_SECONDARY);
		estProductionTimeLabel = createValueLabel(HerbWiseTheme.TEXT_SECONDARY);

		metricsGrid.add(createMetricRow("Selected Potion:", selectedPotionLabel));
		metricsGrid.add(createMetricRow("Max Potions Affordable:", maxAffordableLabel));
		metricsGrid.add(createMetricRow("Total Ingredient Cost:", totalIngredientCostLabel));
		metricsGrid.add(createMetricRow("Expected Gross Sale:", expectedSaleValueLabel));
		metricsGrid.add(createMetricRow("Expected GE Tax:", expectedGeTaxLabel));
		metricsGrid.add(createMetricRow("Expected Profit / Loss:", expectedNetProfitLabel));
		metricsGrid.add(createMetricRow("Total XP Gained:", totalXpGainedLabel));
		metricsGrid.add(createMetricRow("Remaining GP:", remainingGpLabel));
		metricsGrid.add(createMetricRow("Inventory Loads (14/inv):", inventoryLoadsLabel));
		metricsGrid.add(createMetricRow("Estimated Crafting Time:", estProductionTimeLabel));

		outputCard.add(metricsGrid);
		outputCard.add(Box.createRigidArea(new Dimension(0, 6)));

		JLabel shopHeader = new JLabel("Ingredient Shopping List:");
		shopHeader.setFont(HerbWiseTheme.getBadgeFont());
		shopHeader.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		shopHeader.setAlignmentX(LEFT_ALIGNMENT);
		outputCard.add(shopHeader);
		outputCard.add(Box.createRigidArea(new Dimension(0, 3)));

		shoppingListContainer = new JPanel();
		shoppingListContainer.setLayout(new BoxLayout(shoppingListContainer, BoxLayout.Y_AXIS));
		shoppingListContainer.setOpaque(false);
		shoppingListContainer.setAlignmentX(LEFT_ALIGNMENT);
		outputCard.add(shoppingListContainer);

		content.add(outputCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 3. QUANTITY / TARGET XP / TARGET LEVEL CALCULATOR (Section 7)
		// ====================================================================
		JPanel goalCard = HerbWiseTheme.createCardPanel();
		goalCard.setLayout(new BoxLayout(goalCard, BoxLayout.Y_AXIS));

		JLabel goalHeader = HerbWiseTheme.createSectionHeader("Target XP & Level Quantity Planner");
		goalHeader.setAlignmentX(LEFT_ALIGNMENT);
		goalCard.add(goalHeader);
		goalCard.add(Box.createRigidArea(new Dimension(0, 4)));

		goalInputField = HerbWiseTheme.createStyledTextField("10000");

		goalModeCombo = new JComboBox<>(new String[]{
			"Target XP Gained (e.g. 10,000 XP)",
			"Reach Target Herblore Level (e.g. Level 90)",
			"Exact Number of Potions (e.g. 500)"
		});
		HerbWiseTheme.styleComboBox(goalModeCombo);
		goalModeCombo.setAlignmentX(LEFT_ALIGNMENT);
		goalModeCombo.addActionListener(e -> {
			int idx = goalModeCombo.getSelectedIndex();
			if (idx == 0) goalInputField.setText("10000");
			else if (idx == 1) goalInputField.setText("90");
			else goalInputField.setText("500");
			recalculateGoalQuantity();
		});
		goalCard.add(goalModeCombo);
		goalCard.add(Box.createRigidArea(new Dimension(0, 4)));

		JPanel goalInputRow = new JPanel(new BorderLayout(5, 0));
		goalInputRow.setOpaque(false);
		goalInputRow.setAlignmentX(LEFT_ALIGNMENT);
		JLabel goalValLbl = new JLabel("Target Value:");
		goalValLbl.setFont(HerbWiseTheme.getSmallFont());
		goalValLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		goalInputRow.add(goalValLbl, BorderLayout.WEST);

		goalInputField.addActionListener(e -> recalculateGoalQuantity());
		goalInputRow.add(goalInputField, BorderLayout.CENTER);

		HerbButton goalCalcBtn = new HerbButton("Plan", true);
		goalCalcBtn.addActionListener(e -> recalculateGoalQuantity());
		goalInputRow.add(goalCalcBtn, BorderLayout.EAST);
		goalCard.add(goalInputRow);
		goalCard.add(Box.createRigidArea(new Dimension(0, 4)));

		// Quick Goal Presets: "10,000 XP", "100,000 XP", "Reach Lvl 90"
		JPanel quickGoalRow = new JPanel(new GridLayout(1, 3, 4, 0));
		quickGoalRow.setOpaque(false);
		quickGoalRow.setAlignmentX(LEFT_ALIGNMENT);

		HerbButton btn10kXp = new HerbButton("10,000 XP");
		btn10kXp.addActionListener(e -> {
			goalModeCombo.setSelectedIndex(0);
			goalInputField.setText("10000");
			recalculateGoalQuantity();
		});
		HerbButton btn100kXp = new HerbButton("100,000 XP");
		btn100kXp.addActionListener(e -> {
			goalModeCombo.setSelectedIndex(0);
			goalInputField.setText("100000");
			recalculateGoalQuantity();
		});
		HerbButton btnLvl90 = new HerbButton("Reach Lvl 90");
		btnLvl90.addActionListener(e -> {
			goalModeCombo.setSelectedIndex(1);
			goalInputField.setText("90");
			recalculateGoalQuantity();
		});
		quickGoalRow.add(btn10kXp);
		quickGoalRow.add(btn100kXp);
		quickGoalRow.add(btnLvl90);
		goalCard.add(quickGoalRow);
		goalCard.add(Box.createRigidArea(new Dimension(0, 6)));

		JPanel goalGrid = new JPanel(new GridLayout(5, 1, 0, 3));
		goalGrid.setOpaque(false);
		goalGrid.setAlignmentX(LEFT_ALIGNMENT);

		goalPotionsNeededLabel = createValueLabel(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		goalXpGainedLabel = createValueLabel(HerbWiseTheme.COLOR_INFO_CYAN);
		goalTotalCostLabel = createValueLabel(HerbWiseTheme.TEXT_PRIMARY);
		goalNetProfitLabel = createValueLabel(HerbWiseTheme.COLOR_PROFIT_GREEN);
		goalInventoryLoadsLabel = createValueLabel(HerbWiseTheme.TEXT_SECONDARY);

		goalGrid.add(createMetricRow("Potions Required:", goalPotionsNeededLabel));
		goalGrid.add(createMetricRow("Total XP Gained:", goalXpGainedLabel));
		goalGrid.add(createMetricRow("Total Investment:", goalTotalCostLabel));
		goalGrid.add(createMetricRow("Est. Net Profit/Loss:", goalNetProfitLabel));
		goalGrid.add(createMetricRow("Inventory Loads:", goalInventoryLoadsLabel));
		goalCard.add(goalGrid);
		goalCard.add(Box.createRigidArea(new Dimension(0, 5)));

		goalIngredientsPanel = new JPanel();
		goalIngredientsPanel.setLayout(new BoxLayout(goalIngredientsPanel, BoxLayout.Y_AXIS));
		goalIngredientsPanel.setOpaque(false);
		goalIngredientsPanel.setAlignmentX(LEFT_ALIGNMENT);
		goalCard.add(goalIngredientsPanel);

		content.add(goalCard);

		JScrollPane scrollPane = HerbWiseTheme.createStyledScrollPane(content);
		add(scrollPane, BorderLayout.CENTER);

		refresh();
	}

	private JLabel createValueLabel(java.awt.Color fg)
	{
		JLabel lbl = new JLabel("—");
		lbl.setFont(HerbWiseTheme.getBoldFont());
		lbl.setForeground(fg);
		lbl.setHorizontalAlignment(SwingConstants.RIGHT);
		return lbl;
	}

	private JPanel createMetricRow(String labelText, JLabel valueLabel)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		JLabel key = new JLabel(labelText);
		key.setFont(HerbWiseTheme.getSmallFont());
		key.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		row.add(key, BorderLayout.WEST);
		row.add(valueLabel, BorderLayout.CENTER);
		return row;
	}

	public void selectPotion(PotionProfitResult result)
	{
		if (result == null || result.getRecipe() == null)
		{
			return;
		}
		autoRecommendCheck.setSelected(false);
		filterUnlockedOnlyCheck.setSelected(false);
		potionSelectorCombo.setEnabled(true);
		populatePotionCombo();

		String targetId = result.getRecipe().getRecipeId();
		for (int i = 0; i < potionSelectorCombo.getItemCount(); i++)
		{
			PotionChoiceItem item = potionSelectorCombo.getItemAt(i);
			if (item != null && item.result != null
				&& targetId.equals(item.result.getRecipe().getRecipeId()))
			{
				potionSelectorCombo.setSelectedIndex(i);
				break;
			}
		}
		recalculateBudget();
		recalculateGoalQuantity();
	}

	public void refresh()
	{
		populatePotionCombo();
		recalculateBudget();
		recalculateGoalQuantity();
	}

	private void populatePotionCombo()
	{
		if (dataService == null)
		{
			return;
		}
		suppressComboEvents = true;
		try
		{
			PotionChoiceItem prev = (PotionChoiceItem) potionSelectorCombo.getSelectedItem();
			String prevRecipeId = prev != null && prev.result != null
				? prev.result.getRecipe().getRecipeId()
				: null;

			potionSelectorCombo.removeAllItems();
			boolean unlockedOnly = filterUnlockedOnlyCheck.isSelected();

			List<PotionProfitResult> candidates = new ArrayList<>();
			for (PotionProfitResult r : dataService.getAllEvaluatedResults())
			{
				if (r == null || r.getRecipe() == null || r.getRecipe().isUnfinishedPotionRecipe())
				{
					continue;
				}
				if (r.getRecipe().isAlternativeRecipe() || !r.isHasIngredientPriceData())
				{
					continue;
				}
				if (unlockedOnly && !r.isAvailableAtPlayerLevel())
				{
					continue;
				}
				candidates.add(r);
			}

			candidates.sort(Comparator.comparingInt((PotionProfitResult r) -> r.getRecipe().getRequiredLevel())
				.thenComparing(r -> r.getRecipe().getPotionName()));

			PotionChoiceItem toSelect = null;
			for (PotionProfitResult r : candidates)
			{
				PotionChoiceItem item = new PotionChoiceItem(r);
				potionSelectorCombo.addItem(item);
				if (prevRecipeId != null && prevRecipeId.equals(r.getRecipe().getRecipeId()))
				{
					toSelect = item;
				}
			}

			if (toSelect != null)
			{
				potionSelectorCombo.setSelectedItem(toSelect);
			}
			else if (potionSelectorCombo.getItemCount() > 0)
			{
				potionSelectorCombo.setSelectedIndex(0);
			}
		}
		finally
		{
			suppressComboEvents = false;
		}
	}

	private long parseInputLong(String raw, long fallback)
	{
		if (raw == null || raw.trim().isEmpty())
		{
			return fallback;
		}
		try
		{
			String cleaned = raw.trim().toLowerCase(Locale.ROOT).replace(",", "").replace(" ", "");
			double multiplier = 1.0;
			if (cleaned.endsWith("b"))
			{
				multiplier = 1_000_000_000.0;
				cleaned = cleaned.substring(0, cleaned.length() - 1);
			}
			else if (cleaned.endsWith("m"))
			{
				multiplier = 1_000_000.0;
				cleaned = cleaned.substring(0, cleaned.length() - 1);
			}
			else if (cleaned.endsWith("k"))
			{
				multiplier = 1_000.0;
				cleaned = cleaned.substring(0, cleaned.length() - 1);
			}
			return Math.max(0L, Math.round(Double.parseDouble(cleaned) * multiplier));
		}
		catch (NumberFormatException ex)
		{
			return fallback;
		}
	}

	private PotionProfitResult resolveSelectedPotion(long budgetGp)
	{
		if (dataService == null)
		{
			return null;
		}
		PlayerHerbloreData player = dataService.getPlayerSkillService().getCurrentHerbloreData();
		int playerLvl = filterUnlockedOnlyCheck.isSelected()
			? player.getEffectiveHerbloreLevel()
			: 99;

		if (autoRecommendCheck.isSelected())
		{
			Optional<PotionProfitResult> best = dataService.getBudgetCalculator().recommendBestPotionForBudget(
				budgetGp,
				playerLvl,
				dataService.getAllEvaluatedResults()
			);
			if (best.isPresent())
			{
				return best.get();
			}
		}

		PotionChoiceItem choice = (PotionChoiceItem) potionSelectorCombo.getSelectedItem();
		return choice != null ? choice.result : null;
	}

	public void recalculateBudget()
	{
		if (dataService == null)
		{
			return;
		}

		long budgetGp = parseInputLong(budgetInputField.getText(), dataService.getDefaultBudgetGp());
		PotionProfitResult selected = resolveSelectedPotion(budgetGp);

		shoppingListContainer.removeAll();

		if (selected == null)
		{
			selectedPotionLabel.setText("None affordable");
			maxAffordableLabel.setText("0 potions");
			totalIngredientCostLabel.setText("0 GP");
			expectedSaleValueLabel.setText("0 GP");
			expectedGeTaxLabel.setText("0 GP");
			expectedNetProfitLabel.setText("0 GP");
			totalXpGainedLabel.setText("0 XP");
			remainingGpLabel.setText(HerbWiseTheme.formatGp(budgetGp));
			inventoryLoadsLabel.setText("0 loads");
			estProductionTimeLabel.setText("0s");
			shoppingListContainer.revalidate();
			shoppingListContainer.repaint();
			return;
		}

		BudgetCalculationResult calc = dataService.getBudgetCalculator().calculateBudget(budgetGp, selected);
		selectedPotionLabel.setText(selected.getRecipe().getPotionName()
			+ (autoRecommendCheck.isSelected() ? " (Recommended)" : ""));
		maxAffordableLabel.setText(HerbWiseTheme.formatNumber(calc.getMaxPotionsAffordable()) + " potions");
		totalIngredientCostLabel.setText(HerbWiseTheme.formatGp(calc.getTotalIngredientCost()));
		expectedSaleValueLabel.setText(HerbWiseTheme.formatGp(calc.getExpectedGrossSaleValue()));
		expectedGeTaxLabel.setText("-" + HerbWiseTheme.formatGp(calc.getExpectedTotalGeTax()));
		expectedNetProfitLabel.setText(HerbWiseTheme.formatSignedGp(calc.getExpectedTotalProfitLoss()));
		expectedNetProfitLabel.setForeground(calc.getExpectedTotalProfitLoss() >= 0
			? HerbWiseTheme.COLOR_PROFIT_GREEN
			: HerbWiseTheme.COLOR_LOSS_RED);
		totalXpGainedLabel.setText(String.format(Locale.US, "%s XP",
			HerbWiseTheme.formatNumber(Math.round(calc.getTotalXpGained()))));
		remainingGpLabel.setText(HerbWiseTheme.formatGp(calc.getRemainingGp()));
		inventoryLoadsLabel.setText(HerbWiseTheme.formatNumber(calc.getInventoryLoadsRequired()) + " loads");
		estProductionTimeLabel.setText(formatDurationMinutes(calc.getEstimatedMinutes()));

		Map<Integer, Long> unitCosts = selected.getIngredientUnitPrices();
		for (Map.Entry<PotionIngredient, Long> entry : calc.getIngredientQuantitiesNeeded().entrySet())
		{
			PotionIngredient ing = entry.getKey();
			long totalQty = entry.getValue();
			long unitCost = unitCosts.getOrDefault(ing.getItemId(), 0L);
			long totalCost = ing.isTradeable() ? totalQty * unitCost : 0L;

			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setOpaque(false);
			row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
			JLabel left = new JLabel("• " + HerbWiseTheme.formatNumber(totalQty)
				+ "x " + ing.getName());
			left.setFont(HerbWiseTheme.getSmallFont());
			left.setForeground(HerbWiseTheme.TEXT_PRIMARY);
			JLabel right = new JLabel(ing.isTradeable()
				? HerbWiseTheme.formatGp(totalCost)
				: "Untradeable");
			right.setFont(HerbWiseTheme.getSmallFont());
			right.setForeground(HerbWiseTheme.TEXT_SECONDARY);
			row.add(left, BorderLayout.CENTER);
			row.add(right, BorderLayout.EAST);
			shoppingListContainer.add(row);
		}

		shoppingListContainer.revalidate();
		shoppingListContainer.repaint();
	}

	public void recalculateGoalQuantity()
	{
		if (dataService == null)
		{
			return;
		}
		long budgetGp = parseInputLong(budgetInputField.getText(), dataService.getDefaultBudgetGp());
		PotionProfitResult selected = resolveSelectedPotion(budgetGp);

		goalIngredientsPanel.removeAll();
		if (selected == null)
		{
			goalPotionsNeededLabel.setText("—");
			goalXpGainedLabel.setText("—");
			goalTotalCostLabel.setText("—");
			goalNetProfitLabel.setText("—");
			goalInventoryLoadsLabel.setText("—");
			goalIngredientsPanel.revalidate();
			goalIngredientsPanel.repaint();
			return;
		}

		PlayerHerbloreData player = dataService.getPlayerSkillService().getCurrentHerbloreData();
		long currentXp = player.getCurrentXp();
		int currentLvl = player.getCurrentLevel();
		int mode = goalModeCombo.getSelectedIndex();
		long inputVal = parseInputLong(goalInputField.getText(), 10000L);

		XpProductionPlan plan;
		if (mode == 0)
		{
			// Target XP Gained (e.g., 10,000 XP)
			plan = dataService.getXpCalculator().calculateForDesiredXpGain(
				currentXp, Math.max(1L, inputVal), selected);
		}
		else if (mode == 1)
		{
			// Reach Target Level (e.g., Level 90)
			int targetLvl = (int) Math.max(2L, Math.min(99L, inputVal));
			plan = dataService.getXpCalculator().calculateByLevels(currentLvl, targetLvl, selected);
		}
		else
		{
			// Exact Number of Potions
			long qty = Math.max(1L, Math.min(1_000_000L, inputVal));
			plan = dataService.getXpCalculator().calculateForPotionQuantity(currentXp, qty, selected);
		}

		goalPotionsNeededLabel.setText(HerbWiseTheme.formatNumber(plan.getPotionsRequired())
			+ "x " + selected.getRecipe().getPotionName());
		goalXpGainedLabel.setText(HerbWiseTheme.formatNumber(Math.round(plan.getTotalXpGained())) + " XP");
		goalTotalCostLabel.setText(HerbWiseTheme.formatGp(plan.getTotalInvestmentGp()));
		goalNetProfitLabel.setText(HerbWiseTheme.formatSignedGp(plan.getTotalProfitLossGp()));
		goalNetProfitLabel.setForeground(plan.getTotalProfitLossGp() >= 0
			? HerbWiseTheme.COLOR_PROFIT_GREEN
			: HerbWiseTheme.COLOR_LOSS_RED);
		goalInventoryLoadsLabel.setText(HerbWiseTheme.formatNumber(plan.getInventoryLoadsRequired()) + " loads ("
			+ formatDurationMinutes(plan.getEstimatedMinutes()) + ")");

		Map<Integer, Long> unitCosts = selected.getIngredientUnitPrices();
		for (Map.Entry<PotionIngredient, Long> entry : plan.getTotalIngredientsNeeded().entrySet())
		{
			PotionIngredient ing = entry.getKey();
			long totalQty = entry.getValue();
			long unitCost = unitCosts.getOrDefault(ing.getItemId(), 0L);
			long totalCost = ing.isTradeable() ? totalQty * unitCost : 0L;

			JPanel row = new JPanel(new BorderLayout(4, 0));
			row.setOpaque(false);
			JLabel left = new JLabel("• " + HerbWiseTheme.formatNumber(totalQty)
				+ "x " + ing.getName());
			left.setFont(HerbWiseTheme.getSmallFont());
			left.setForeground(HerbWiseTheme.TEXT_SECONDARY);
			JLabel right = new JLabel(HerbWiseTheme.formatGp(totalCost));
			right.setFont(HerbWiseTheme.getSmallFont());
			right.setForeground(HerbWiseTheme.TEXT_MUTED);
			row.add(left, BorderLayout.CENTER);
			row.add(right, BorderLayout.EAST);
			goalIngredientsPanel.add(row);
		}

		goalIngredientsPanel.revalidate();
		goalIngredientsPanel.repaint();
	}

	private static String formatDurationMinutes(double minutes)
	{
		if (minutes <= 0.0)
		{
			return "0m";
		}
		if (minutes < 1.0)
		{
			return Math.max(1, Math.round(minutes * 60.0)) + "s";
		}
		long totalMins = Math.round(minutes);
		long hrs = totalMins / 60L;
		long remMins = totalMins % 60L;
		return hrs > 0L ? (hrs + "h " + remMins + "m") : (remMins + "m");
	}
}
