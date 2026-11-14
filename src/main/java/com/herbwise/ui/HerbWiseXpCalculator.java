package com.herbwise.ui;

import com.herbwise.data.HerbloreXpTable;
import com.herbwise.models.PlayerHerbloreData;
import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.models.XpProductionPlan;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.ui.HerbWiseTheme.HerbButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * Full Herblore XP & Production Calculator tab (Section 9).
 * Inputs:
 * - Current Herblore level & Current XP (synced with player skill or customizable)
 * - Target Herblore level & Target XP (with presets 70, 75, 80, 85, 90, 95, 99)
 * - Desired potion selector
 * - Optional exact number of potions override
 * Outputs:
 * - XP gained, Total ingredients needed, Total investment, Expected sale value,
 *   GE tax, Total profit/loss, GP per XP, Inventory loads, Estimated time to complete
 */
public class HerbWiseXpCalculator extends JPanel
{
	private final HerbloreDataService dataService;

	private final JSpinner currentLevelSpinner;
	private final JTextField currentXpField;
	private final JSpinner targetLevelSpinner;
	private final JTextField targetXpField;
	private final JComboBox<PotionItem> desiredPotionCombo;
	private final JCheckBox useExactQuantityCheck;
	private final JTextField customQuantityField;

	// Output labels
	private final JLabel xpRequiredLabel;
	private final JLabel potionsNeededLabel;
	private final JLabel xpGainedLabel;
	private final JLabel totalInvestmentLabel;
	private final JLabel expectedSaleValueLabel;
	private final JLabel geTaxLabel;
	private final JLabel totalProfitLossLabel;
	private final JLabel gpPerXpLabel;
	private final JLabel inventoryLoadsLabel;
	private final JLabel estimatedTimeLabel;
	private final JPanel ingredientsNeededPanel;

	private boolean suppressInputEvents = false;

	private static class PotionItem
	{
		private final PotionProfitResult result;

		PotionItem(PotionProfitResult result)
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
			return String.format(Locale.US, "%s (Lvl %d • %.1f XP)",
				r.getPotionName(), r.getRequiredLevel(), r.getXpGained());
		}
	}

	public HerbWiseXpCalculator(HerbloreDataService dataService)
	{
		this.dataService = dataService;

		setLayout(new BorderLayout());
		setBackground(HerbWiseTheme.BG_MATTE_BLACK);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		content.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

		// ====================================================================
		// 1. INPUTS & LEVEL PRESETS CARD
		// ====================================================================
		JPanel inputCard = HerbWiseTheme.createCardPanel();
		inputCard.setLayout(new BoxLayout(inputCard, BoxLayout.Y_AXIS));
		inputCard.setBorder(HerbWiseTheme.CARD_BORDER_ACCENT);

		JPanel headerRow = new JPanel(new BorderLayout(4, 0));
		headerRow.setOpaque(false);
		headerRow.setAlignmentX(LEFT_ALIGNMENT);
		headerRow.add(HerbWiseTheme.createSectionHeader("XP & Production Planner"), BorderLayout.CENTER);

		HerbButton syncPlayerBtn = new HerbButton("Sync Player");
		syncPlayerBtn.setToolTipText("Sync Current Level and XP from active player stats");
		syncPlayerBtn.addActionListener(e -> syncFromPlayerSkill());
		headerRow.add(syncPlayerBtn, BorderLayout.EAST);
		inputCard.add(headerRow);
		inputCard.add(Box.createRigidArea(new Dimension(0, 6)));

		// Current Level & Current XP
		JPanel currentGrid = new JPanel(new GridLayout(2, 2, 6, 4));
		currentGrid.setOpaque(false);
		currentGrid.setAlignmentX(LEFT_ALIGNMENT);

		currentLevelSpinner = new JSpinner(new SpinnerNumberModel(75, 1, 99, 1));
		currentLevelSpinner.setFont(HerbWiseTheme.getSmallFont());
		currentXpField = HerbWiseTheme.createStyledTextField(String.valueOf(HerbloreXpTable.getXpForLevel(75)));

		targetLevelSpinner = new JSpinner(new SpinnerNumberModel(90, 2, 99, 1));
		targetLevelSpinner.setFont(HerbWiseTheme.getSmallFont());
		targetXpField = HerbWiseTheme.createStyledTextField(String.valueOf(HerbloreXpTable.getXpForLevel(90)));

		currentLevelSpinner.addChangeListener(e -> {
			if (!suppressInputEvents)
			{
				suppressInputEvents = true;
				try
				{
					int lvl = (Integer) currentLevelSpinner.getValue();
					currentXpField.setText(String.valueOf(HerbloreXpTable.getXpForLevel(lvl)));
				}
				finally
				{
					suppressInputEvents = false;
				}
				recalculatePlan();
			}
		});

		currentXpField.addActionListener(e -> {
			if (!suppressInputEvents)
			{
				suppressInputEvents = true;
				try
				{
					long xp = parseXp(currentXpField.getText(), HerbloreXpTable.getXpForLevel(75));
					currentLevelSpinner.setValue(HerbloreXpTable.getLevelForXp(xp));
				}
				finally
				{
					suppressInputEvents = false;
				}
				recalculatePlan();
			}
		});

		targetLevelSpinner.addChangeListener(e -> {
			if (!suppressInputEvents)
			{
				suppressInputEvents = true;
				try
				{
					int targetLvl = (Integer) targetLevelSpinner.getValue();
					targetXpField.setText(String.valueOf(HerbloreXpTable.getXpForLevel(targetLvl)));
				}
				finally
				{
					suppressInputEvents = false;
				}
				recalculatePlan();
			}
		});

		targetXpField.addActionListener(e -> {
			if (!suppressInputEvents)
			{
				suppressInputEvents = true;
				try
				{
					long xp = parseXp(targetXpField.getText(), HerbloreXpTable.getXpForLevel(90));
					targetLevelSpinner.setValue(Math.max(2, HerbloreXpTable.getLevelForXp(xp)));
				}
				finally
				{
					suppressInputEvents = false;
				}
				recalculatePlan();
			}
		});

		currentGrid.add(createLabeledComponent("Current Level:", currentLevelSpinner));
		currentGrid.add(createLabeledComponent("Current XP:", currentXpField));
		currentGrid.add(createLabeledComponent("Target Level:", targetLevelSpinner));
		currentGrid.add(createLabeledComponent("Target XP:", targetXpField));
		inputCard.add(currentGrid);
		inputCard.add(Box.createRigidArea(new Dimension(0, 6)));

		useExactQuantityCheck = new JCheckBox("Custom Qty:", false);
		customQuantityField = HerbWiseTheme.createStyledTextField("1000");
		customQuantityField.setEnabled(false);

		// Preset Level Target Buttons: 70, 75, 80, 85, 90, 95, 99 (Section 9)
		JLabel presetsLbl = new JLabel("Target Level Presets:");
		presetsLbl.setFont(HerbWiseTheme.getSmallFont());
		presetsLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		presetsLbl.setAlignmentX(LEFT_ALIGNMENT);
		inputCard.add(presetsLbl);
		inputCard.add(Box.createRigidArea(new Dimension(0, 3)));

		JPanel presetGrid = new JPanel(new GridLayout(1, 7, 3, 0));
		presetGrid.setOpaque(false);
		presetGrid.setAlignmentX(LEFT_ALIGNMENT);
		for (final int presetLevel : HerbloreXpTable.LEVEL_PRESETS)
		{
			HerbButton btn = new HerbButton(String.valueOf(presetLevel));
			btn.setToolTipText("Set Target Level to " + presetLevel + " ("
				+ HerbWiseTheme.formatNumber(HerbloreXpTable.getXpForLevel(presetLevel)) + " XP)");
			btn.addActionListener(e -> {
				useExactQuantityCheck.setSelected(false);
				customQuantityField.setEnabled(false);
				targetLevelSpinner.setValue(presetLevel);
			});
			presetGrid.add(btn);
		}
		inputCard.add(presetGrid);
		inputCard.add(Box.createRigidArea(new Dimension(0, 6)));

		// Desired Potion Selector
		JLabel potionLbl = new JLabel("Desired Potion:");
		potionLbl.setFont(HerbWiseTheme.getSmallFont());
		potionLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		potionLbl.setAlignmentX(LEFT_ALIGNMENT);
		inputCard.add(potionLbl);
		inputCard.add(Box.createRigidArea(new Dimension(0, 2)));

		desiredPotionCombo = new JComboBox<>();
		HerbWiseTheme.styleComboBox(desiredPotionCombo);
		desiredPotionCombo.setAlignmentX(LEFT_ALIGNMENT);
		desiredPotionCombo.addActionListener(e -> {
			if (!suppressInputEvents)
			{
				recalculatePlan();
			}
		});
		inputCard.add(desiredPotionCombo);
		inputCard.add(Box.createRigidArea(new Dimension(0, 6)));

		// Custom Number of Potions Override
		JPanel customQtyRow = new JPanel(new BorderLayout(5, 0));
		customQtyRow.setOpaque(false);
		customQtyRow.setAlignmentX(LEFT_ALIGNMENT);

		useExactQuantityCheck.setOpaque(false);
		useExactQuantityCheck.setFont(HerbWiseTheme.getSmallFont());
		useExactQuantityCheck.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		useExactQuantityCheck.addActionListener(e -> {
			customQuantityField.setEnabled(useExactQuantityCheck.isSelected());
			recalculatePlan();
		});
		customQtyRow.add(useExactQuantityCheck, BorderLayout.WEST);

		customQuantityField.addActionListener(e -> recalculatePlan());
		customQtyRow.add(customQuantityField, BorderLayout.CENTER);

		HerbButton calcBtn = new HerbButton("Calculate", true);
		calcBtn.addActionListener(e -> recalculatePlan());
		customQtyRow.add(calcBtn, BorderLayout.EAST);

		inputCard.add(customQtyRow);
		content.add(inputCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 2. PRODUCTION PLAN RESULTS CARD
		// ====================================================================
		JPanel resultsCard = HerbWiseTheme.createCardPanel();
		resultsCard.setLayout(new BoxLayout(resultsCard, BoxLayout.Y_AXIS));

		JLabel resultsHeader = HerbWiseTheme.createSectionHeader("Production & Financial Summary");
		resultsHeader.setAlignmentX(LEFT_ALIGNMENT);
		resultsCard.add(resultsHeader);
		resultsCard.add(Box.createRigidArea(new Dimension(0, 4)));

		JPanel outGrid = new JPanel(new GridLayout(10, 1, 0, 3));
		outGrid.setOpaque(false);
		outGrid.setAlignmentX(LEFT_ALIGNMENT);

		xpRequiredLabel = createRightLabel(HerbWiseTheme.TEXT_PRIMARY);
		potionsNeededLabel = createRightLabel(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		xpGainedLabel = createRightLabel(HerbWiseTheme.COLOR_INFO_CYAN);
		totalInvestmentLabel = createRightLabel(HerbWiseTheme.TEXT_PRIMARY);
		expectedSaleValueLabel = createRightLabel(HerbWiseTheme.TEXT_PRIMARY);
		geTaxLabel = createRightLabel(HerbWiseTheme.COLOR_STALE_YELLOW);
		totalProfitLossLabel = createRightLabel(HerbWiseTheme.COLOR_PROFIT_GREEN);
		gpPerXpLabel = createRightLabel(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		inventoryLoadsLabel = createRightLabel(HerbWiseTheme.TEXT_SECONDARY);
		estimatedTimeLabel = createRightLabel(HerbWiseTheme.TEXT_SECONDARY);

		outGrid.add(createRow("Target XP Deficit:", xpRequiredLabel));
		outGrid.add(createRow("Potions to Make:", potionsNeededLabel));
		outGrid.add(createRow("Total XP Gained:", xpGainedLabel));
		outGrid.add(createRow("Total Investment:", totalInvestmentLabel));
		outGrid.add(createRow("Expected Sale Value:", expectedSaleValueLabel));
		outGrid.add(createRow("Total GE Tax:", geTaxLabel));
		outGrid.add(createRow("Total Profit / Loss:", totalProfitLossLabel));
		outGrid.add(createRow("Net GP per XP:", gpPerXpLabel));
		outGrid.add(createRow("Inventory Loads (14/inv):", inventoryLoadsLabel));
		outGrid.add(createRow("Estimated Time:", estimatedTimeLabel));

		resultsCard.add(outGrid);
		resultsCard.add(Box.createRigidArea(new Dimension(0, 6)));

		JLabel ingHeader = new JLabel("Total Ingredients Needed:");
		ingHeader.setFont(HerbWiseTheme.getBadgeFont());
		ingHeader.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		ingHeader.setAlignmentX(LEFT_ALIGNMENT);
		resultsCard.add(ingHeader);
		resultsCard.add(Box.createRigidArea(new Dimension(0, 3)));

		ingredientsNeededPanel = new JPanel();
		ingredientsNeededPanel.setLayout(new BoxLayout(ingredientsNeededPanel, BoxLayout.Y_AXIS));
		ingredientsNeededPanel.setOpaque(false);
		ingredientsNeededPanel.setAlignmentX(LEFT_ALIGNMENT);
		resultsCard.add(ingredientsNeededPanel);

		content.add(resultsCard);

		JScrollPane scrollPane = HerbWiseTheme.createStyledScrollPane(content);
		add(scrollPane, BorderLayout.CENTER);

		syncFromPlayerSkill();
		refresh();
	}

	private JPanel createLabeledComponent(String label, java.awt.Component comp)
	{
		JPanel p = new JPanel(new BorderLayout(3, 1));
		p.setOpaque(false);
		JLabel l = new JLabel(label);
		l.setFont(HerbWiseTheme.getSmallFont());
		l.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		p.add(l, BorderLayout.NORTH);
		p.add(comp, BorderLayout.CENTER);
		return p;
	}

	private JLabel createRightLabel(Color fg)
	{
		JLabel l = new JLabel("—");
		l.setFont(HerbWiseTheme.getBoldFont());
		l.setForeground(fg);
		l.setHorizontalAlignment(SwingConstants.RIGHT);
		return l;
	}

	private JPanel createRow(String key, JLabel val)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		JLabel k = new JLabel(key);
		k.setFont(HerbWiseTheme.getSmallFont());
		k.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		row.add(k, BorderLayout.WEST);
		row.add(val, BorderLayout.CENTER);
		return row;
	}

	public void selectPotion(PotionProfitResult result)
	{
		if (result == null || result.getRecipe() == null)
		{
			return;
		}
		refresh();
		String targetId = result.getRecipe().getRecipeId();
		for (int i = 0; i < desiredPotionCombo.getItemCount(); i++)
		{
			PotionItem item = desiredPotionCombo.getItemAt(i);
			if (item != null && item.result != null
				&& targetId.equals(item.result.getRecipe().getRecipeId()))
			{
				desiredPotionCombo.setSelectedIndex(i);
				break;
			}
		}
		recalculatePlan();
	}

	public void syncFromPlayerSkill()
	{
		if (dataService == null)
		{
			return;
		}
		PlayerHerbloreData player = dataService.getPlayerSkillService().getCurrentHerbloreData();
		suppressInputEvents = true;
		try
		{
			int curLvl = player.getCurrentLevel();
			long curXp = player.getCurrentXp();
			currentLevelSpinner.setValue(curLvl);
			currentXpField.setText(String.valueOf(curXp));

			int defaultTarget = Math.max(curLvl + 1, Math.min(99, dataService.getDefaultTargetLevel()));
			targetLevelSpinner.setValue(defaultTarget);
			targetXpField.setText(String.valueOf(HerbloreXpTable.getXpForLevel(defaultTarget)));
		}
		finally
		{
			suppressInputEvents = false;
		}
		recalculatePlan();
	}

	public void refresh()
	{
		if (dataService == null)
		{
			return;
		}
		suppressInputEvents = true;
		try
		{
			PotionItem currentChoice = (PotionItem) desiredPotionCombo.getSelectedItem();
			String prevId = currentChoice != null && currentChoice.result != null
				? currentChoice.result.getRecipe().getRecipeId()
				: null;

			desiredPotionCombo.removeAllItems();

			List<PotionProfitResult> items = new ArrayList<>();
			for (PotionProfitResult r : dataService.getAllEvaluatedResults())
			{
				if (r != null && r.getRecipe() != null
					&& !r.getRecipe().isUnfinishedPotionRecipe()
					&& !r.getRecipe().isAlternativeRecipe()
					&& r.getXpPerPotion() > 0.0)
				{
					items.add(r);
				}
			}

			items.sort(Comparator.comparingInt((PotionProfitResult r) -> r.getRecipe().getRequiredLevel())
				.thenComparing(r -> r.getRecipe().getPotionName()));

			PotionItem toSelect = null;
			for (PotionProfitResult r : items)
			{
				PotionItem item = new PotionItem(r);
				desiredPotionCombo.addItem(item);
				if (prevId != null && prevId.equals(r.getRecipe().getRecipeId()))
				{
					toSelect = item;
				}
				else if (prevId == null && r.getRecipe().getPotionName().equals("Prayer potion(4)"))
				{
					toSelect = item;
				}
			}

			if (toSelect != null)
			{
				desiredPotionCombo.setSelectedItem(toSelect);
			}
			else if (desiredPotionCombo.getItemCount() > 0)
			{
				desiredPotionCombo.setSelectedIndex(0);
			}
		}
		finally
		{
			suppressInputEvents = false;
		}
		recalculatePlan();
	}

	private long parseXp(String raw, long fallback)
	{
		if (raw == null || raw.trim().isEmpty())
		{
			return fallback;
		}
		try
		{
			String cleaned = raw.trim().replace(",", "").replace(" ", "");
			return Math.max(0L, Math.min(HerbloreXpTable.MAX_XP, Long.parseLong(cleaned)));
		}
		catch (NumberFormatException ex)
		{
			return fallback;
		}
	}

	public void recalculatePlan()
	{
		if (dataService == null)
		{
			return;
		}
		PotionItem selectedItem = (PotionItem) desiredPotionCombo.getSelectedItem();
		ingredientsNeededPanel.removeAll();

		if (selectedItem == null || selectedItem.result == null)
		{
			xpRequiredLabel.setText("—");
			potionsNeededLabel.setText("—");
			xpGainedLabel.setText("—");
			totalInvestmentLabel.setText("—");
			expectedSaleValueLabel.setText("—");
			geTaxLabel.setText("—");
			totalProfitLossLabel.setText("—");
			gpPerXpLabel.setText("—");
			inventoryLoadsLabel.setText("—");
			estimatedTimeLabel.setText("—");
			ingredientsNeededPanel.revalidate();
			ingredientsNeededPanel.repaint();
			return;
		}

		long currentXp = parseXp(currentXpField.getText(),
			HerbloreXpTable.getXpForLevel((Integer) currentLevelSpinner.getValue()));
		long targetXp = parseXp(targetXpField.getText(),
			HerbloreXpTable.getXpForLevel((Integer) targetLevelSpinner.getValue()));

		XpProductionPlan plan;
		if (useExactQuantityCheck.isSelected())
		{
			long customQty = Math.max(1L, parseXp(customQuantityField.getText(), 1000L));
			plan = dataService.getXpCalculator().calculateForPotionQuantity(currentXp, customQty, selectedItem.result);
		}
		else
		{
			plan = dataService.getXpCalculator().calculateByXpGoal(currentXp, targetXp, selectedItem.result);
		}

		xpRequiredLabel.setText(HerbWiseTheme.formatNumber(plan.getXpRequired()) + " XP");
		potionsNeededLabel.setText(HerbWiseTheme.formatNumber(plan.getPotionsRequired()) + " potions");
		xpGainedLabel.setText(HerbWiseTheme.formatNumber(Math.round(plan.getTotalXpGained())) + " XP");
		totalInvestmentLabel.setText(HerbWiseTheme.formatGp(plan.getTotalInvestmentGp()));
		expectedSaleValueLabel.setText(HerbWiseTheme.formatGp(plan.getExpectedGrossSaleValueGp()));
		geTaxLabel.setText("-" + HerbWiseTheme.formatGp(plan.getExpectedTotalGeTaxGp()));

		long net = plan.getTotalProfitLossGp();
		totalProfitLossLabel.setText(HerbWiseTheme.formatSignedGp(net));
		totalProfitLossLabel.setForeground(net >= 0 ? HerbWiseTheme.COLOR_PROFIT_GREEN : HerbWiseTheme.COLOR_LOSS_RED);

		if (plan.getNetGpPerXp() >= 0.0)
		{
			gpPerXpLabel.setText(String.format(Locale.US, "+%.2f GP/XP (Profit)", plan.getNetGpPerXp()));
			gpPerXpLabel.setForeground(HerbWiseTheme.COLOR_PROFIT_GREEN);
		}
		else
		{
			gpPerXpLabel.setText(String.format(Locale.US, "%.2f GP/XP", plan.getNetGpPerXp()));
			gpPerXpLabel.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		}

		inventoryLoadsLabel.setText(HerbWiseTheme.formatNumber(plan.getInventoryLoadsRequired()) + " loads");
		estimatedTimeLabel.setText(formatDurationMinutes(plan.getEstimatedMinutes()));

		Map<Integer, Long> unitCosts = selectedItem.result.getIngredientUnitPrices();
		for (Map.Entry<PotionIngredient, Long> entry : plan.getTotalIngredientsNeeded().entrySet())
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
			ingredientsNeededPanel.add(row);
		}

		ingredientsNeededPanel.revalidate();
		ingredientsNeededPanel.repaint();
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
