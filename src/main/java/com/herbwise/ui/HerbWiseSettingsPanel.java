package com.herbwise.ui;

import com.herbwise.HerbWiseConfig.HerbloreBoostMode;
import com.herbwise.HerbWiseConfig.TableDisplayMode;
import com.herbwise.models.PriceCalculationMode;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.services.WikiPriceService;
import com.herbwise.ui.HerbWiseTheme.HerbButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
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

/**
 * In-panel Settings & Diagnostics tab (Section 12 & Section 4.B).
 * Configures all HerbWise parameters and clearly explains the difference between
 * Instant Buy/Sell, 5-Minute Average, 1-Hour Average, and Patient GE Offer price modes.
 */
public class HerbWiseSettingsPanel extends JPanel
{
	private final HerbloreDataService dataService;

	private final JSpinner refreshIntervalSpinner;
	private final JComboBox<PriceCalculationMode> priceModeCombo;
	private final JLabel priceModeExplanationLabel;

	private final JCheckBox showProfitableOnlyCheck;
	private final JCheckBox showAvailableAtLevelOnlyCheck;
	private final JCheckBox includeGeTaxCheck;
	private final JCheckBox volatilityWarningsCheck;
	private final JCheckBox stalePriceWarningsCheck;
	private final JCheckBox showXpHourCheck;
	private final JCheckBox enableNotificationsCheck;

	private final JTextField defaultBudgetField;
	private final JSpinner defaultTargetLevelSpinner;
	private final JComboBox<TableDisplayMode> tableDisplayModeCombo;
	private final JComboBox<HerbloreBoostMode> boostModeCombo;

	private final JLabel diagStatusLabel;
	private final JLabel diagRecipeCountLabel;

	private boolean suppressEvents = false;

	public HerbWiseSettingsPanel(HerbloreDataService dataService)
	{
		this.dataService = dataService;

		setLayout(new BorderLayout());
		setBackground(HerbWiseTheme.BG_MATTE_BLACK);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		content.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

		// ====================================================================
		// 1. PRICE CALCULATION MODE & REFRESH INTERVAL
		// ====================================================================
		JPanel pricingCard = HerbWiseTheme.createCardPanel();
		pricingCard.setLayout(new BoxLayout(pricingCard, BoxLayout.Y_AXIS));
		pricingCard.setBorder(HerbWiseTheme.CARD_BORDER_ACCENT);

		JLabel pricingHeader = HerbWiseTheme.createSectionHeader("Pricing & API Refresh Settings");
		pricingHeader.setAlignmentX(LEFT_ALIGNMENT);
		pricingCard.add(pricingHeader);
		pricingCard.add(Box.createRigidArea(new Dimension(0, 5)));

		JPanel intervalRow = new JPanel(new BorderLayout(6, 0));
		intervalRow.setOpaque(false);
		intervalRow.setAlignmentX(LEFT_ALIGNMENT);
		JLabel intLbl = new JLabel("Refresh Interval (sec):");
		intLbl.setFont(HerbWiseTheme.getSmallFont());
		intLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		intervalRow.add(intLbl, BorderLayout.CENTER);

		refreshIntervalSpinner = new JSpinner(new SpinnerNumberModel(60, 30, 3600, 15));
		refreshIntervalSpinner.setFont(HerbWiseTheme.getSmallFont());
		refreshIntervalSpinner.addChangeListener(e -> applySettings());
		intervalRow.add(refreshIntervalSpinner, BorderLayout.EAST);
		pricingCard.add(intervalRow);
		pricingCard.add(Box.createRigidArea(new Dimension(0, 6)));

		JLabel modeLbl = new JLabel("Price Calculation Mode:");
		modeLbl.setFont(HerbWiseTheme.getSmallFont());
		modeLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		modeLbl.setAlignmentX(LEFT_ALIGNMENT);
		pricingCard.add(modeLbl);
		pricingCard.add(Box.createRigidArea(new Dimension(0, 2)));

		priceModeCombo = new JComboBox<>(PriceCalculationMode.values());
		HerbWiseTheme.styleComboBox(priceModeCombo);
		priceModeCombo.setAlignmentX(LEFT_ALIGNMENT);
		priceModeCombo.addActionListener(e -> {
			updatePriceExplanationText();
			applySettings();
		});
		pricingCard.add(priceModeCombo);
		pricingCard.add(Box.createRigidArea(new Dimension(0, 5)));

		priceModeExplanationLabel = new JLabel();
		priceModeExplanationLabel.setFont(HerbWiseTheme.getSmallFont());
		priceModeExplanationLabel.setForeground(HerbWiseTheme.TEXT_MUTED);
		priceModeExplanationLabel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(HerbWiseTheme.BORDER_SUBTLE, 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)
		));
		priceModeExplanationLabel.setAlignmentX(LEFT_ALIGNMENT);
		pricingCard.add(priceModeExplanationLabel);

		content.add(pricingCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 2. CALCULATION & FILTER TOGGLES
		// ====================================================================
		JPanel togglesCard = HerbWiseTheme.createCardPanel();
		togglesCard.setLayout(new BoxLayout(togglesCard, BoxLayout.Y_AXIS));

		JLabel togglesHeader = HerbWiseTheme.createSectionHeader("Calculation & Filter Rules");
		togglesHeader.setAlignmentX(LEFT_ALIGNMENT);
		togglesCard.add(togglesHeader);
		togglesCard.add(Box.createRigidArea(new Dimension(0, 4)));

		includeGeTaxCheck = createToggle("Include 2% Grand Exchange Tax (5M Cap)", true);
		showProfitableOnlyCheck = createToggle("Show Profitable Recipes Only", false);
		showAvailableAtLevelOnlyCheck = createToggle("Show Recipes Available at My Level Only", false);
		volatilityWarningsCheck = createToggle("Enable Price Volatility Warnings", true);
		stalePriceWarningsCheck = createToggle("Enable Stale Price Warnings (>1h old)", true);
		showXpHourCheck = createToggle("Show XP / Hour & Profit / Hour Estimates", true);
		enableNotificationsCheck = createToggle("Enable High-Profit Notifications", false);

		togglesCard.add(includeGeTaxCheck);
		togglesCard.add(showProfitableOnlyCheck);
		togglesCard.add(showAvailableAtLevelOnlyCheck);
		togglesCard.add(volatilityWarningsCheck);
		togglesCard.add(stalePriceWarningsCheck);
		togglesCard.add(showXpHourCheck);
		togglesCard.add(enableNotificationsCheck);

		content.add(togglesCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 3. DEFAULTS, DISPLAY & HERBLORE BOOSTS
		// ====================================================================
		JPanel defaultsCard = HerbWiseTheme.createCardPanel();
		defaultsCard.setLayout(new BoxLayout(defaultsCard, BoxLayout.Y_AXIS));

		JLabel defaultsHeader = HerbWiseTheme.createSectionHeader("Defaults & Display Preferences");
		defaultsHeader.setAlignmentX(LEFT_ALIGNMENT);
		defaultsCard.add(defaultsHeader);
		defaultsCard.add(Box.createRigidArea(new Dimension(0, 5)));

		JPanel budgetRow = new JPanel(new BorderLayout(6, 0));
		budgetRow.setOpaque(false);
		budgetRow.setAlignmentX(LEFT_ALIGNMENT);
		JLabel budLbl = new JLabel("Default Budget (GP):");
		budLbl.setFont(HerbWiseTheme.getSmallFont());
		budLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		budgetRow.add(budLbl, BorderLayout.WEST);

		defaultBudgetField = HerbWiseTheme.createStyledTextField("1000000");
		defaultBudgetField.addActionListener(e -> applySettings());
		budgetRow.add(defaultBudgetField, BorderLayout.CENTER);
		defaultsCard.add(budgetRow);
		defaultsCard.add(Box.createRigidArea(new Dimension(0, 5)));

		JPanel targetLvlRow = new JPanel(new BorderLayout(6, 0));
		targetLvlRow.setOpaque(false);
		targetLvlRow.setAlignmentX(LEFT_ALIGNMENT);
		JLabel tlLbl = new JLabel("Default Target Level:");
		tlLbl.setFont(HerbWiseTheme.getSmallFont());
		tlLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		targetLvlRow.add(tlLbl, BorderLayout.CENTER);

		defaultTargetLevelSpinner = new JSpinner(new SpinnerNumberModel(90, 2, 99, 1));
		defaultTargetLevelSpinner.setFont(HerbWiseTheme.getSmallFont());
		defaultTargetLevelSpinner.addChangeListener(e -> applySettings());
		targetLvlRow.add(defaultTargetLevelSpinner, BorderLayout.EAST);
		defaultsCard.add(targetLvlRow);
		defaultsCard.add(Box.createRigidArea(new Dimension(0, 5)));

		JLabel dispLbl = new JLabel("Table Display Mode:");
		dispLbl.setFont(HerbWiseTheme.getSmallFont());
		dispLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		dispLbl.setAlignmentX(LEFT_ALIGNMENT);
		defaultsCard.add(dispLbl);
		defaultsCard.add(Box.createRigidArea(new Dimension(0, 2)));

		tableDisplayModeCombo = new JComboBox<>(TableDisplayMode.values());
		HerbWiseTheme.styleComboBox(tableDisplayModeCombo);
		tableDisplayModeCombo.setAlignmentX(LEFT_ALIGNMENT);
		tableDisplayModeCombo.addActionListener(e -> applySettings());
		defaultsCard.add(tableDisplayModeCombo);
		defaultsCard.add(Box.createRigidArea(new Dimension(0, 5)));

		JLabel boostLbl = new JLabel("Herblore Boost Mode:");
		boostLbl.setFont(HerbWiseTheme.getSmallFont());
		boostLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		boostLbl.setAlignmentX(LEFT_ALIGNMENT);
		defaultsCard.add(boostLbl);
		defaultsCard.add(Box.createRigidArea(new Dimension(0, 2)));

		boostModeCombo = new JComboBox<>(HerbloreBoostMode.values());
		HerbWiseTheme.styleComboBox(boostModeCombo);
		boostModeCombo.setAlignmentX(LEFT_ALIGNMENT);
		boostModeCombo.addActionListener(e -> applySettings());
		defaultsCard.add(boostModeCombo);
		defaultsCard.add(Box.createRigidArea(new Dimension(0, 8)));

		JPanel btnRow = new JPanel(new GridLayout(1, 2, 6, 0));
		btnRow.setOpaque(false);
		btnRow.setAlignmentX(LEFT_ALIGNMENT);

		HerbButton applyBtn = new HerbButton("Apply Settings", true);
		applyBtn.addActionListener(e -> applySettings());
		HerbButton resetBtn = new HerbButton("Reset Defaults");
		resetBtn.addActionListener(e -> resetToDefaults());
		btnRow.add(applyBtn);
		btnRow.add(resetBtn);
		defaultsCard.add(btnRow);

		content.add(defaultsCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 4. API & DATABASE DIAGNOSTICS CARD
		// ====================================================================
		JPanel diagCard = HerbWiseTheme.createCardPanel();
		diagCard.setLayout(new BoxLayout(diagCard, BoxLayout.Y_AXIS));

		JLabel diagHeader = HerbWiseTheme.createSectionHeader("API & Recipe Verification Status");
		diagHeader.setAlignmentX(LEFT_ALIGNMENT);
		diagCard.add(diagHeader);
		diagCard.add(Box.createRigidArea(new Dimension(0, 4)));

		JLabel uaLbl = new JLabel("<html><body style='width:380px'><b>User-Agent:</b> "
			+ WikiPriceService.USER_AGENT + "</body></html>");
		uaLbl.setFont(HerbWiseTheme.getSmallFont());
		uaLbl.setForeground(HerbWiseTheme.TEXT_MUTED);
		uaLbl.setAlignmentX(LEFT_ALIGNMENT);
		diagCard.add(uaLbl);
		diagCard.add(Box.createRigidArea(new Dimension(0, 3)));

		diagStatusLabel = new JLabel("Status: Checking...");
		diagStatusLabel.setFont(HerbWiseTheme.getSmallFont());
		diagStatusLabel.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		diagStatusLabel.setAlignmentX(LEFT_ALIGNMENT);
		diagCard.add(diagStatusLabel);

		diagRecipeCountLabel = new JLabel("Verified Recipes: —");
		diagRecipeCountLabel.setFont(HerbWiseTheme.getSmallFont());
		diagRecipeCountLabel.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		diagRecipeCountLabel.setAlignmentX(LEFT_ALIGNMENT);
		diagCard.add(diagRecipeCountLabel);

		content.add(diagCard);

		JScrollPane scrollPane = HerbWiseTheme.createStyledScrollPane(content);
		add(scrollPane, BorderLayout.CENTER);

		refreshFromService();
	}

	private JCheckBox createToggle(String text, boolean initial)
	{
		JCheckBox cb = new JCheckBox(text, initial);
		cb.setOpaque(false);
		cb.setFont(HerbWiseTheme.getSmallFont());
		cb.setForeground(HerbWiseTheme.TEXT_PRIMARY);
		cb.setAlignmentX(LEFT_ALIGNMENT);
		cb.addActionListener(e -> applySettings());
		return cb;
	}

	private void updatePriceExplanationText()
	{
		PriceCalculationMode mode = (PriceCalculationMode) priceModeCombo.getSelectedItem();
		if (mode == null)
		{
			mode = PriceCalculationMode.INSTANT_CONSERVATIVE;
		}
		priceModeExplanationLabel.setText("<html><body style='width: 380px'><b>"
			+ mode.getDisplayName() + ":</b> " + mode.getExplanation() + "</body></html>");
	}

	public void refreshFromService()
	{
		if (dataService == null)
		{
			return;
		}
		suppressEvents = true;
		try
		{
			refreshIntervalSpinner.setValue(dataService.getRefreshIntervalSeconds());
			priceModeCombo.setSelectedItem(dataService.getPriceCalculationMode());
			updatePriceExplanationText();

			includeGeTaxCheck.setSelected(dataService.isIncludeGeTax());
			showProfitableOnlyCheck.setSelected(dataService.isShowProfitableOnly());
			showAvailableAtLevelOnlyCheck.setSelected(dataService.isShowAvailableAtLevelOnly());
			volatilityWarningsCheck.setSelected(dataService.isEnableVolatilityWarnings());
			stalePriceWarningsCheck.setSelected(dataService.isEnableStalePriceWarnings());
			showXpHourCheck.setSelected(dataService.isShowXpPerHourEstimates());
			enableNotificationsCheck.setSelected(dataService.isEnableNotifications());

			defaultBudgetField.setText(String.valueOf(dataService.getDefaultBudgetGp()));
			defaultTargetLevelSpinner.setValue(dataService.getDefaultTargetLevel());
			tableDisplayModeCombo.setSelectedItem(dataService.getTableDisplayMode());
			boostModeCombo.setSelectedItem(dataService.getHerbloreBoostMode());

			diagStatusLabel.setText("<html><body style='width:380px'><b>API Status:</b> "
				+ dataService.getStatusDetailText() + "</body></html>");
			diagRecipeCountLabel.setText("Verified Recipes: "
				+ dataService.getWikiRecipeService().getAllVerifiedRecipes().size()
				+ " (0 Invalid / 100% Verified)");
		}
		finally
		{
			suppressEvents = false;
		}
	}

	private void applySettings()
	{
		if (suppressEvents || dataService == null)
		{
			return;
		}

		long defaultBudget = 1_000_000L;
		try
		{
			defaultBudget = Math.max(1_000L, Long.parseLong(
				defaultBudgetField.getText().trim().replace(",", "")));
		}
		catch (NumberFormatException ignored)
		{
		}

		dataService.applyRuntimeSettings(
			(Integer) refreshIntervalSpinner.getValue(),
			(PriceCalculationMode) priceModeCombo.getSelectedItem(),
			showProfitableOnlyCheck.isSelected(),
			showAvailableAtLevelOnlyCheck.isSelected(),
			includeGeTaxCheck.isSelected(),
			volatilityWarningsCheck.isSelected(),
			stalePriceWarningsCheck.isSelected(),
			defaultBudget,
			(Integer) defaultTargetLevelSpinner.getValue(),
			showXpHourCheck.isSelected(),
			(TableDisplayMode) tableDisplayModeCombo.getSelectedItem(),
			(HerbloreBoostMode) boostModeCombo.getSelectedItem(),
			enableNotificationsCheck.isSelected()
		);
	}

	private void resetToDefaults()
	{
		suppressEvents = true;
		try
		{
			refreshIntervalSpinner.setValue(60);
			priceModeCombo.setSelectedItem(PriceCalculationMode.INSTANT_CONSERVATIVE);
			updatePriceExplanationText();
			includeGeTaxCheck.setSelected(true);
			showProfitableOnlyCheck.setSelected(false);
			showAvailableAtLevelOnlyCheck.setSelected(false);
			volatilityWarningsCheck.setSelected(true);
			stalePriceWarningsCheck.setSelected(true);
			showXpHourCheck.setSelected(true);
			enableNotificationsCheck.setSelected(false);
			defaultBudgetField.setText("1000000");
			defaultTargetLevelSpinner.setValue(90);
			tableDisplayModeCombo.setSelectedItem(TableDisplayMode.DETAILED);
			boostModeCombo.setSelectedItem(HerbloreBoostMode.NONE);
		}
		finally
		{
			suppressEvents = false;
		}
		applySettings();
	}
}
