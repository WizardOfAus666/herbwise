package com.herbwise.ui;

import com.herbwise.models.PotionIngredient;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionRecipe;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.ui.HerbWiseTheme.HerbButton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Detailed Recipe & Calculation Breakdown View (Section 11).
 * Shows every ingredient price, GE tax formula step, net profit/loss, GP/XP,
 * and batch totals for 100, 1,000, and 10,000 potions.
 */
public class HerbWiseRecipeDetailPanel extends JPanel
{
	private final HerbloreDataService dataService;
	private final Runnable onBack;
	private final Consumer<PotionProfitResult> onOpenInBudget;
	private final Consumer<PotionProfitResult> onOpenInXpCalc;
	private final Consumer<Integer> onInspectItemMarket;

	private final JPanel contentBox;
	private PotionProfitResult currentResult;

	public HerbWiseRecipeDetailPanel(
		HerbloreDataService dataService,
		Runnable onBack,
		Consumer<PotionProfitResult> onOpenInBudget,
		Consumer<PotionProfitResult> onOpenInXpCalc,
		Consumer<Integer> onInspectItemMarket)
	{
		this.dataService = dataService;
		this.onBack = onBack;
		this.onOpenInBudget = onOpenInBudget;
		this.onOpenInXpCalc = onOpenInXpCalc;
		this.onInspectItemMarket = onInspectItemMarket;

		setLayout(new BorderLayout());
		setBackground(HerbWiseTheme.BG_MATTE_BLACK);

		contentBox = new JPanel();
		contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));
		contentBox.setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		contentBox.setBorder(BorderFactory.createEmptyBorder(6, 6, 10, 6));

		JScrollPane scroll = HerbWiseTheme.createStyledScrollPane(contentBox);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll, BorderLayout.CENTER);
	}

	public void displayRecipe(PotionProfitResult result)
	{
		this.currentResult = result;
		render();
	}

	public PotionProfitResult getCurrentResult()
	{
		return currentResult;
	}

	public void render()
	{
		contentBox.removeAll();

		if (currentResult == null || currentResult.getRecipe() == null)
		{
			JLabel empty = new JLabel("Select a potion from the Dashboard or Database to view its full breakdown.");
			empty.setFont(HerbWiseTheme.getRegularFont());
			empty.setForeground(HerbWiseTheme.TEXT_MUTED);
			contentBox.add(empty);
			revalidate();
			repaint();
			return;
		}

		// Refresh with latest result if available
		dataService.getResultByRecipeId(currentResult.getRecipe().getRecipeId())
			.ifPresent(latest -> this.currentResult = latest);

		PotionProfitResult r = this.currentResult;
		PotionRecipe rec = r.getRecipe();

		// 1. Top Navigation Row
		JPanel navRow = new JPanel(new BorderLayout(6, 0));
		navRow.setOpaque(false);
		navRow.setAlignmentX(LEFT_ALIGNMENT);

		HerbButton backBtn = new HerbButton("← Back to List");
		backBtn.addActionListener(e -> {
			if (onBack != null) onBack.run();
		});
		navRow.add(backBtn, BorderLayout.WEST);

		JLabel catBadge = new JLabel(rec.getCategory().getDisplayName());
		catBadge.setFont(HerbWiseTheme.getBadgeFont());
		catBadge.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		catBadge.setHorizontalAlignment(SwingConstants.RIGHT);
		navRow.add(catBadge, BorderLayout.CENTER);

		contentBox.add(navRow);
		contentBox.add(Box.createRigidArea(new Dimension(0, 6)));

		// 2. Hero Potion Header Card
		JPanel heroCard = HerbWiseTheme.createCardPanel();
		heroCard.setLayout(new BorderLayout(8, 4));
		heroCard.setAlignmentX(LEFT_ALIGNMENT);
		heroCard.setBorder(HerbWiseTheme.CARD_BORDER_ACCENT);

		JLabel iconLabel = new JLabel();
		dataService.attachItemIcon(iconLabel, rec.getPotionItemId(), rec.getPotionName());
		heroCard.add(iconLabel, BorderLayout.WEST);

		JPanel titleStack = new JPanel();
		titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
		titleStack.setOpaque(false);

		JLabel nameLbl = new JLabel(rec.getPotionName());
		nameLbl.setFont(HerbWiseTheme.getTitleFont());
		nameLbl.setForeground(HerbWiseTheme.TEXT_PRIMARY);
		titleStack.add(nameLbl);

		String lvlStatus = r.isAvailableAtPlayerLevel()
			? ("Level " + rec.getRequiredLevel() + " Herblore (Unlocked)")
			: ("Level " + rec.getRequiredLevel() + " Herblore (Need +" + r.getLevelsUntilUnlock() + " lvls)");
		JLabel subLbl = new JLabel(lvlStatus + " • " + String.format(Locale.US, "%.1f XP", r.getXpPerPotion()));
		subLbl.setFont(HerbWiseTheme.getSmallFont());
		subLbl.setForeground(r.isAvailableAtPlayerLevel() ? HerbWiseTheme.ACCENT_HERB_BRIGHT : HerbWiseTheme.COLOR_LOSS_RED);
		titleStack.add(subLbl);

		heroCard.add(titleStack, BorderLayout.CENTER);
		contentBox.add(heroCard);
		contentBox.add(Box.createRigidArea(new Dimension(0, 6)));

		// 3. Quick Planner Action Bar
		JPanel actionGrid = new JPanel(new GridLayout(1, 3, 4, 0));
		actionGrid.setOpaque(false);
		actionGrid.setAlignmentX(LEFT_ALIGNMENT);

		HerbButton toBudgetBtn = new HerbButton("Budget Calc", true);
		toBudgetBtn.addActionListener(e -> {
			if (onOpenInBudget != null) onOpenInBudget.accept(r);
		});
		actionGrid.add(toBudgetBtn);

		HerbButton toXpBtn = new HerbButton("XP Planner", true);
		toXpBtn.addActionListener(e -> {
			if (onOpenInXpCalc != null) onOpenInXpCalc.accept(r);
		});
		actionGrid.add(toXpBtn);

		HerbButton toMarketBtn = new HerbButton("Price Chart");
		toMarketBtn.addActionListener(e -> {
			if (onInspectItemMarket != null) onInspectItemMarket.accept(rec.getPotionItemId());
		});
		actionGrid.add(toMarketBtn);

		contentBox.add(actionGrid);
		contentBox.add(Box.createRigidArea(new Dimension(0, 8)));

		// 4. Ingredients Breakdown Card
		JPanel ingCard = HerbWiseTheme.createCardPanel();
		ingCard.setLayout(new BoxLayout(ingCard, BoxLayout.Y_AXIS));
		ingCard.setAlignmentX(LEFT_ALIGNMENT);

		JLabel ingHeader = HerbWiseTheme.createSectionHeader("A. INGREDIENT COST BREAKDOWN");
		ingHeader.setAlignmentX(LEFT_ALIGNMENT);
		ingCard.add(ingHeader);
		ingCard.add(Box.createRigidArea(new Dimension(0, 4)));

		for (PotionIngredient ing : rec.getIngredients())
		{
			JPanel ingRow = new JPanel(new BorderLayout(6, 0));
			ingRow.setOpaque(false);
			ingRow.setAlignmentX(LEFT_ALIGNMENT);

			JLabel ingIcon = new JLabel();
			dataService.attachItemIcon(ingIcon, ing.getItemId(), ing.getName());
			ingRow.add(ingIcon, BorderLayout.WEST);

			Long unitPrice = r.getIngredientUnitPrices().get(ing.getItemId());
			Long lineCost = r.getIngredientLineCosts().get(ing.getItemId());

			JPanel ingText = new JPanel(new GridLayout(2, 1));
			ingText.setOpaque(false);
			JLabel ingTitle = new JLabel(ing.getQuantity() + "x " + ing.getName() + " [" + ing.getRole() + "]");
			ingTitle.setFont(HerbWiseTheme.getBoldFont());
			ingTitle.setForeground(HerbWiseTheme.TEXT_PRIMARY);
			ingText.add(ingTitle);

			String unitStr = (!ing.isTradeable())
				? "Untradeable item (No GE buy price)"
				: (unitPrice != null ? (HerbWiseTheme.formatGp(unitPrice) + " each") : "Price unavailable");
			JLabel ingSub = new JLabel(unitStr);
			ingSub.setFont(HerbWiseTheme.getSmallFont());
			ingSub.setForeground(HerbWiseTheme.TEXT_MUTED);
			ingText.add(ingSub);
			ingRow.add(ingText, BorderLayout.CENTER);

			JLabel lineTotalLbl = new JLabel(lineCost != null ? HerbWiseTheme.formatGp(lineCost) : "Unavailable");
			lineTotalLbl.setFont(HerbWiseTheme.getBoldFont());
			lineTotalLbl.setForeground(lineCost != null ? HerbWiseTheme.TEXT_SECONDARY : HerbWiseTheme.COLOR_MISSING_GREY);
			ingRow.add(lineTotalLbl, BorderLayout.EAST);

			ingCard.add(ingRow);
			ingCard.add(Box.createRigidArea(new Dimension(0, 4)));
		}

		addMetricRow(ingCard, "Total Ingredient Cost:",
			r.isHasIngredientPriceData() ? HerbWiseTheme.formatGp(r.getTotalIngredientCost()) : "Unavailable",
			HerbWiseTheme.TEXT_PRIMARY, true);

		contentBox.add(ingCard);
		contentBox.add(Box.createRigidArea(new Dimension(0, 6)));

		// 5. Step-by-Step Profit, GE Tax & XP Efficiency Card
		JPanel calcCard = HerbWiseTheme.createCardPanel();
		calcCard.setLayout(new BoxLayout(calcCard, BoxLayout.Y_AXIS));
		calcCard.setAlignmentX(LEFT_ALIGNMENT);

		JLabel calcHeader = HerbWiseTheme.createSectionHeader("B. PROFIT, GE TAX & XP FORMULA");
		calcHeader.setAlignmentX(LEFT_ALIGNMENT);
		calcCard.add(calcHeader);
		calcCard.add(Box.createRigidArea(new Dimension(0, 4)));

		addMetricRow(calcCard, "Price Mode:", r.getPriceMode().getDisplayName(), HerbWiseTheme.COLOR_INFO_CYAN, false);
		addMetricRow(calcCard, "Gross Potion Sale Price:",
			r.isHasCompletePriceData() ? HerbWiseTheme.formatGp(r.getFinishedPotionSalePrice()) : "Unavailable",
			HerbWiseTheme.TEXT_PRIMARY, false);
		addMetricRow(calcCard, "Grand Exchange Tax (2%):",
			r.isHasCompletePriceData() ? ("-" + HerbWiseTheme.formatGp(r.getGeTax())) : "—",
			HerbWiseTheme.COLOR_STALE_YELLOW, false);
		addMetricRow(calcCard, "Net Sale Value (After Tax):",
			r.isHasCompletePriceData() ? HerbWiseTheme.formatGp(r.getNetSaleValue()) : "Unavailable",
			HerbWiseTheme.TEXT_SECONDARY, false);
		addMetricRow(calcCard, "Less Ingredient Cost:",
			r.isHasIngredientPriceData() ? ("-" + HerbWiseTheme.formatGp(r.getTotalIngredientCost())) : "Unavailable",
			HerbWiseTheme.TEXT_SECONDARY, false);

		java.awt.Color profitColor = !r.isHasCompletePriceData()
			? HerbWiseTheme.COLOR_MISSING_GREY
			: (r.getNetProfitPerPotion() >= 0L ? HerbWiseTheme.COLOR_PROFIT_GREEN : HerbWiseTheme.COLOR_LOSS_RED);
		addMetricRow(calcCard, "Net Profit / Loss Per Potion:",
			r.isHasCompletePriceData() ? HerbWiseTheme.formatSignedGp(r.getNetProfitPerPotion()) : "Unavailable",
			profitColor, true);

		addMetricRow(calcCard, "Profit Margin:",
			r.isHasCompletePriceData() ? String.format(Locale.US, "%+.2f%%", r.getProfitMarginPercent()) : "—",
			profitColor, false);
		addMetricRow(calcCard, "XP Per Potion:",
			String.format(Locale.US, "%.1f XP", r.getXpPerPotion()),
			HerbWiseTheme.ACCENT_HERB_BRIGHT, false);
		addMetricRow(calcCard, "Gross Ingredient Cost / XP:",
			r.isHasIngredientPriceData() && r.getXpPerPotion() > 0.0
				? String.format(Locale.US, "%.2f GP/XP", r.getIngredientCostPerXp())
				: "—",
			HerbWiseTheme.TEXT_SECONDARY, false);
		addMetricRow(calcCard, "Net Cost / Profit Per XP:",
			r.isHasCompletePriceData() && r.getXpPerPotion() > 0.0
				? (r.getNetCostPerXp() <= 0.0
					? String.format(Locale.US, "+%.2f GP/XP (Profit)", Math.abs(r.getNetCostPerXp()))
					: String.format(Locale.US, "%.2f GP/XP (Loss)", r.getNetCostPerXp()))
				: "—",
			profitColor, true);
		addMetricRow(calcCard, "Est. Production Speed:",
			HerbWiseTheme.formatNumber(r.getEstimatedPotionsPerHour()) + " pots/hr ("
				+ HerbWiseTheme.formatNumber(r.getEstimatedXpPerHour()) + " XP/hr)",
			HerbWiseTheme.TEXT_SECONDARY, false);

		contentBox.add(calcCard);
		contentBox.add(Box.createRigidArea(new Dimension(0, 6)));

		// 6. Batch Scaling Card (100, 1,000, and 10,000 Potions)
		JPanel batchCard = HerbWiseTheme.createCardPanel();
		batchCard.setLayout(new BoxLayout(batchCard, BoxLayout.Y_AXIS));
		batchCard.setAlignmentX(LEFT_ALIGNMENT);

		JLabel batchHeader = HerbWiseTheme.createSectionHeader("C. BATCH TOTALS (100 / 1,000 / 10,000)");
		batchHeader.setAlignmentX(LEFT_ALIGNMENT);
		batchCard.add(batchHeader);
		batchCard.add(Box.createRigidArea(new Dimension(0, 4)));

		long[] batches = new long[]{100L, 1_000L, 10_000L};
		for (long qty : batches)
		{
			long totalCost = r.isHasIngredientPriceData() ? (r.getTotalIngredientCost() * qty) : 0L;
			long totalProfit = r.isHasCompletePriceData() ? r.getProfitForQuantity(qty) : 0L;
			double totalXp = r.getTotalXpForQuantity(qty);

			String summary = r.isHasCompletePriceData()
				? ("Cost: " + HerbWiseTheme.formatCompactGp(totalCost)
					+ " | Net: " + HerbWiseTheme.formatSignedGp(totalProfit)
					+ " | " + HerbWiseTheme.formatNumber(Math.round(totalXp)) + " XP")
				: ("Cost: " + (r.isHasIngredientPriceData() ? HerbWiseTheme.formatGp(totalCost) : "Unavail.")
					+ " | " + HerbWiseTheme.formatNumber(Math.round(totalXp)) + " XP");

			addMetricRow(batchCard, HerbWiseTheme.formatNumber(qty) + " Potions:", summary, profitColor, false);
		}

		if (rec.getPreparationNotes() != null && !rec.getPreparationNotes().isEmpty())
		{
			batchCard.add(Box.createRigidArea(new Dimension(0, 5)));
			JLabel notesLbl = new JLabel("<html><body style='width:380px;color:#8896a8;'><b>Notes:</b> "
				+ rec.getPreparationNotes() + "</body></html>");
			notesLbl.setFont(HerbWiseTheme.getSmallFont());
			notesLbl.setAlignmentX(LEFT_ALIGNMENT);
			batchCard.add(notesLbl);
		}

		contentBox.add(batchCard);

		revalidate();
		repaint();
	}

	private void addMetricRow(JPanel parent, String label, String value, java.awt.Color valueColor, boolean bold)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel k = new JLabel(label);
		k.setFont(bold ? HerbWiseTheme.getBoldFont() : HerbWiseTheme.getSmallFont());
		k.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		row.add(k, BorderLayout.WEST);

		JLabel v = new JLabel(value);
		v.setFont(bold ? HerbWiseTheme.getBoldFont() : HerbWiseTheme.getSmallFont());
		v.setForeground(valueColor);
		v.setHorizontalAlignment(SwingConstants.RIGHT);
		row.add(v, BorderLayout.CENTER);

		parent.add(row);
		parent.add(Box.createRigidArea(new Dimension(0, 2)));
	}
}
