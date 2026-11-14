package com.herbwise.ui;

import com.herbwise.data.ItemIdMappings;
import com.herbwise.data.ItemIdMappings.ItemDefinition;
import com.herbwise.models.ItemPrice;
import com.herbwise.models.MarketTrendAnalysis;
import com.herbwise.models.MarketTrendAnalysis.TimeseriesPoint;
import com.herbwise.models.MarketTrendAnalysis.TrendDirection;
import com.herbwise.models.PriceCalculationMode;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.ui.HerbWiseTheme.HerbButton;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Price History & Market Analysis tab (Section 11).
 * Uses /latest, /5m, /1h, and /timeseries OSRS Wiki endpoints to display:
 * - Current Instant-Buy & Instant-Sell prices + margin spread
 * - 5-Minute Average & 1-Hour Average prices
 * - Historical Trend Direction (Becoming More Expensive / Rising, Becoming Cheaper / Falling, Stable)
 * - Price movement (GP and %), Trading Volume, Volatility / Thin-Trade warnings, and Age of price data
 * - Custom-painted historical price & volume chart
 */
public class HerbWiseMarketPanel extends JPanel
{
	private final HerbloreDataService dataService;

	private final JComboBox<MarketItemChoice> itemSelectorCombo;
	private final JComboBox<String> timestepCombo;
	private final JLabel selectedIconLabel;
	private final JLabel selectedItemTitleLabel;
	private final JLabel trendBadgeLabel;
	private final JLabel warningBannerLabel;

	private final JLabel currentBuyLabel;
	private final JLabel currentSellLabel;
	private final JLabel avg5mLabel;
	private final JLabel avg1hLabel;
	private final JLabel priceMovementLabel;
	private final JLabel volume1hLabel;
	private final JLabel volatilitySpreadLabel;
	private final JLabel priceAgeLabel;

	private final PriceHistoryChartPanel chartPanel;
	private boolean suppressComboEvents = false;

	private static class MarketItemChoice
	{
		private final int itemId;
		private final String itemName;

		MarketItemChoice(int itemId, String itemName)
		{
			this.itemId = itemId;
			this.itemName = itemName;
		}

		@Override
		public String toString()
		{
			return itemName + " (ID " + itemId + ")";
		}
	}

	public HerbWiseMarketPanel(HerbloreDataService dataService)
	{
		this.dataService = dataService;

		setLayout(new BorderLayout());
		setBackground(HerbWiseTheme.BG_MATTE_BLACK);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		content.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));

		// ====================================================================
		// 1. ITEM & TIMESTEP SELECTOR CARD
		// ====================================================================
		JPanel controlCard = HerbWiseTheme.createCardPanel();
		controlCard.setLayout(new BoxLayout(controlCard, BoxLayout.Y_AXIS));
		controlCard.setBorder(HerbWiseTheme.CARD_BORDER_ACCENT);

		JLabel header = HerbWiseTheme.createSectionHeader("OSRS Wiki Price History & Market Analysis");
		header.setAlignmentX(LEFT_ALIGNMENT);
		controlCard.add(header);
		controlCard.add(Box.createRigidArea(new Dimension(0, 5)));

		itemSelectorCombo = new JComboBox<>();
		HerbWiseTheme.styleComboBox(itemSelectorCombo);
		itemSelectorCombo.setAlignmentX(LEFT_ALIGNMENT);
		itemSelectorCombo.addActionListener(e -> {
			if (!suppressComboEvents)
			{
				updateSelectedItemAnalysis();
			}
		});
		controlCard.add(itemSelectorCombo);
		controlCard.add(Box.createRigidArea(new Dimension(0, 4)));

		JPanel stepRow = new JPanel(new BorderLayout(6, 0));
		stepRow.setOpaque(false);
		stepRow.setAlignmentX(LEFT_ALIGNMENT);
		JLabel stepLbl = new JLabel("Step:");
		stepLbl.setFont(HerbWiseTheme.getSmallFont());
		stepLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		stepRow.add(stepLbl, BorderLayout.WEST);

		timestepCombo = new JComboBox<>(new String[]{"5m", "1h", "6h", "24h"});
		HerbWiseTheme.styleComboBox(timestepCombo);
		timestepCombo.setSelectedItem("5m");
		timestepCombo.addActionListener(e -> {
			if (!suppressComboEvents)
			{
				updateSelectedItemAnalysis();
			}
		});
		stepRow.add(timestepCombo, BorderLayout.CENTER);

		HerbButton fetchBtn = new HerbButton("Refresh", true);
		fetchBtn.addActionListener(e -> updateSelectedItemAnalysis());
		stepRow.add(fetchBtn, BorderLayout.EAST);

		controlCard.add(stepRow);
		content.add(controlCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 2. ITEM HEADER & TREND STATUS CARD
		// ====================================================================
		JPanel statusCard = HerbWiseTheme.createCardPanel();
		statusCard.setLayout(new BoxLayout(statusCard, BoxLayout.Y_AXIS));

		JPanel itemTopRow = new JPanel(new BorderLayout(8, 0));
		itemTopRow.setOpaque(false);
		itemTopRow.setAlignmentX(LEFT_ALIGNMENT);

		selectedIconLabel = new JLabel();
		itemTopRow.add(selectedIconLabel, BorderLayout.WEST);

		JPanel nameAndTrendCol = new JPanel(new GridLayout(2, 1, 0, 2));
		nameAndTrendCol.setOpaque(false);
		selectedItemTitleLabel = new JLabel("Prayer potion(4)");
		selectedItemTitleLabel.setFont(HerbWiseTheme.getSectionFont());
		selectedItemTitleLabel.setForeground(HerbWiseTheme.TEXT_PRIMARY);

		trendBadgeLabel = new JLabel("Trend: Stable");
		trendBadgeLabel.setFont(HerbWiseTheme.getBadgeFont());
		trendBadgeLabel.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);

		nameAndTrendCol.add(selectedItemTitleLabel);
		nameAndTrendCol.add(trendBadgeLabel);
		itemTopRow.add(nameAndTrendCol, BorderLayout.CENTER);

		statusCard.add(itemTopRow);
		statusCard.add(Box.createRigidArea(new Dimension(0, 5)));

		warningBannerLabel = new JLabel("Market liquidity & spread healthy");
		warningBannerLabel.setFont(HerbWiseTheme.getSmallFont());
		warningBannerLabel.setForeground(HerbWiseTheme.COLOR_PROFIT_GREEN);
		warningBannerLabel.setAlignmentX(LEFT_ALIGNMENT);
		statusCard.add(warningBannerLabel);

		content.add(statusCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 3. HISTORICAL PRICE CHART CARD
		// ====================================================================
		JPanel chartCard = HerbWiseTheme.createCardPanel();
		chartCard.setLayout(new BorderLayout(0, 4));
		JLabel chartTitle = HerbWiseTheme.createSectionHeader("Historical Price & Spread Chart");
		chartCard.add(chartTitle, BorderLayout.NORTH);

		chartPanel = new PriceHistoryChartPanel();
		chartPanel.setPreferredSize(new Dimension(380, 175));
		chartCard.add(chartPanel, BorderLayout.CENTER);

		content.add(chartCard);
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		// ====================================================================
		// 4. DETAILED MARKET METRICS CARD
		// ====================================================================
		JPanel metricsCard = HerbWiseTheme.createCardPanel();
		metricsCard.setLayout(new BoxLayout(metricsCard, BoxLayout.Y_AXIS));
		JLabel metricsHeader = HerbWiseTheme.createSectionHeader("Real-Time & Average Market Data");
		metricsHeader.setAlignmentX(LEFT_ALIGNMENT);
		metricsCard.add(metricsHeader);
		metricsCard.add(Box.createRigidArea(new Dimension(0, 4)));

		JPanel grid = new JPanel(new GridLayout(8, 1, 0, 3));
		grid.setOpaque(false);
		grid.setAlignmentX(LEFT_ALIGNMENT);

		currentBuyLabel = createMetricLabel(HerbWiseTheme.TEXT_PRIMARY);
		currentSellLabel = createMetricLabel(HerbWiseTheme.TEXT_PRIMARY);
		avg5mLabel = createMetricLabel(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		avg1hLabel = createMetricLabel(HerbWiseTheme.COLOR_INFO_CYAN);
		priceMovementLabel = createMetricLabel(HerbWiseTheme.COLOR_PROFIT_GREEN);
		volume1hLabel = createMetricLabel(HerbWiseTheme.TEXT_SECONDARY);
		volatilitySpreadLabel = createMetricLabel(HerbWiseTheme.TEXT_SECONDARY);
		priceAgeLabel = createMetricLabel(HerbWiseTheme.TEXT_MUTED);

		grid.add(createMetricRow("Current Instant-Buy:", currentBuyLabel));
		grid.add(createMetricRow("Current Instant-Sell:", currentSellLabel));
		grid.add(createMetricRow("5-Minute Average:", avg5mLabel));
		grid.add(createMetricRow("1-Hour Average:", avg1hLabel));
		grid.add(createMetricRow("Price Movement:", priceMovementLabel));
		grid.add(createMetricRow("Trading Volume (1h):", volume1hLabel));
		grid.add(createMetricRow("Bid-Ask Spread:", volatilitySpreadLabel));
		grid.add(createMetricRow("Age of Price Data:", priceAgeLabel));

		metricsCard.add(grid);
		content.add(metricsCard);

		JScrollPane scrollPane = HerbWiseTheme.createStyledScrollPane(content);
		add(scrollPane, BorderLayout.CENTER);

		populateMarketItems();
		refresh();
	}

	private JLabel createMetricLabel(Color color)
	{
		JLabel lbl = new JLabel("—");
		lbl.setFont(HerbWiseTheme.getBoldFont());
		lbl.setForeground(color);
		lbl.setHorizontalAlignment(SwingConstants.RIGHT);
		return lbl;
	}

	private JPanel createMetricRow(String label, JLabel valueLabel)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		JLabel k = new JLabel(label);
		k.setFont(HerbWiseTheme.getSmallFont());
		k.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		row.add(k, BorderLayout.WEST);
		row.add(valueLabel, BorderLayout.CENTER);
		return row;
	}

	private void populateMarketItems()
	{
		suppressComboEvents = true;
		try
		{
			itemSelectorCombo.removeAllItems();
			List<MarketItemChoice> choices = new ArrayList<>();
			for (ItemDefinition def : ItemIdMappings.getAllDefinitions().values())
			{
				if (!def.isTradeable())
				{
					continue;
				}
				choices.add(new MarketItemChoice(def.getItemId(), def.getName()));
			}
			choices.sort(Comparator.comparing(c -> c.itemName));

			MarketItemChoice defaultSelection = null;
			for (MarketItemChoice c : choices)
			{
				itemSelectorCombo.addItem(c);
				if ("Prayer potion(4)".equals(c.itemName))
				{
					defaultSelection = c;
				}
			}
			if (defaultSelection != null)
			{
				itemSelectorCombo.setSelectedItem(defaultSelection);
			}
		}
		finally
		{
			suppressComboEvents = false;
		}
	}

	public void selectItem(int itemId)
	{
		for (int i = 0; i < itemSelectorCombo.getItemCount(); i++)
		{
			MarketItemChoice c = itemSelectorCombo.getItemAt(i);
			if (c != null && c.itemId == itemId)
			{
				itemSelectorCombo.setSelectedIndex(i);
				break;
			}
		}
		updateSelectedItemAnalysis();
	}

	public void refresh()
	{
		updateSelectedItemAnalysis();
	}

	private void updateSelectedItemAnalysis()
	{
		if (dataService == null)
		{
			return;
		}
		MarketItemChoice selected = (MarketItemChoice) itemSelectorCombo.getSelectedItem();
		if (selected == null)
		{
			return;
		}

		int itemId = selected.itemId;
		String itemName = selected.itemName;
		String timestep = (String) timestepCombo.getSelectedItem();
		if (timestep == null)
		{
			timestep = "5m";
		}

		dataService.attachItemIcon(selectedIconLabel, itemId, itemName);
		selectedItemTitleLabel.setText(itemName);

		ItemPrice itemPrice = dataService.getItemPrice(itemId);
		long nowSec = Instant.now().getEpochSecond();

		if (itemPrice != null && itemPrice.hasAnyPriceData())
		{
			Long buyObj = itemPrice.resolveBuyPrice(PriceCalculationMode.INSTANT_CONSERVATIVE);
			Long sellObj = itemPrice.resolveSellPrice(PriceCalculationMode.INSTANT_CONSERVATIVE);
			long buy = buyObj != null ? buyObj : 0L;
			long sell = sellObj != null ? sellObj : buy;

			currentBuyLabel.setText(HerbWiseTheme.formatGp(buy));
			currentSellLabel.setText(HerbWiseTheme.formatGp(sell));

			Long h5 = itemPrice.getAvgHighPrice5m();
			Long l5 = itemPrice.getAvgLowPrice5m();
			long mid5m = (h5 != null && h5 > 0 && l5 != null && l5 > 0) ? (h5 + l5) / 2L : buy;

			Long h1 = itemPrice.getAvgHighPrice1h();
			Long l1 = itemPrice.getAvgLowPrice1h();
			long mid1h = (h1 != null && h1 > 0 && l1 != null && l1 > 0) ? (h1 + l1) / 2L : mid5m;

			avg5mLabel.setText(HerbWiseTheme.formatGp(mid5m));
			avg1hLabel.setText(HerbWiseTheme.formatGp(mid1h));
			volume1hLabel.setText(HerbWiseTheme.formatNumber(itemPrice.getTotalVolume1h()) + " traded");
			long spreadGp = Math.max(0L, buy - sell);
			volatilitySpreadLabel.setText(String.format(Locale.US, "%s (%.1f%%)",
				HerbWiseTheme.formatGp(spreadGp), itemPrice.getSpreadPercent()));

			long ageSec = itemPrice.getAgeSeconds(nowSec);
			priceAgeLabel.setText(HerbloreDataService.formatAgeShort(ageSec)
				+ (itemPrice.isFromCache() ? " (Cached)" : " ago"));

			// Render immediate local snapshot analysis while asynchronous timeseries fetch runs
			MarketTrendAnalysis fallbackAnalysis = buildLocalSnapshotTrend(itemPrice, timestep);
			applyTrendAnalysisToUi(fallbackAnalysis);
		}
		else
		{
			currentBuyLabel.setText("Unavailable");
			currentSellLabel.setText("Unavailable");
			avg5mLabel.setText("Unavailable");
			avg1hLabel.setText("Unavailable");
			priceMovementLabel.setText("—");
			volume1hLabel.setText("0");
			volatilitySpreadLabel.setText("—");
			priceAgeLabel.setText("Unavailable");
		}

		// Trigger non-blocking /timeseries API lookup
		final ItemPrice snapshotPrice = itemPrice;
		final String safeStep = timestep;
		dataService.fetchTimeseriesAsync(
			itemId,
			safeStep,
			analysis -> {
				if (analysis != null && !analysis.getPoints().isEmpty())
				{
					applyTrendAnalysisToUi(analysis);
				}
			},
			err -> {
				if (snapshotPrice != null)
				{
					applyTrendAnalysisToUi(buildLocalSnapshotTrend(snapshotPrice, safeStep));
				}
			}
		);
	}

	private void applyTrendAnalysisToUi(MarketTrendAnalysis analysis)
	{
		if (analysis == null)
		{
			return;
		}
		TrendDirection dir = analysis.getDirection();
		trendBadgeLabel.setText("Trend: " + dir.getDescription());

		if (dir == TrendDirection.RISING)
		{
			trendBadgeLabel.setForeground(HerbWiseTheme.COLOR_PROFIT_GREEN);
		}
		else if (dir == TrendDirection.FALLING)
		{
			trendBadgeLabel.setForeground(HerbWiseTheme.COLOR_LOSS_RED);
		}
		else
		{
			trendBadgeLabel.setForeground(HerbWiseTheme.COLOR_INFO_CYAN);
		}

		priceMovementLabel.setText(String.format(Locale.US, "%s (%+.2f%%)",
			HerbWiseTheme.formatSignedGp(analysis.getPriceChangeGp()),
			analysis.getPriceChangePercent()));
		priceMovementLabel.setForeground(analysis.getPriceChangeGp() >= 0
			? HerbWiseTheme.COLOR_PROFIT_GREEN
			: HerbWiseTheme.COLOR_LOSS_RED);

		boolean thinlyTraded = analysis.getTotalVolumeInWindow() > 0 && analysis.getTotalVolumeInWindow() < 40;
		if (analysis.isVolatileWarning())
		{
			warningBannerLabel.setText(String.format(Locale.US,
				"⚠ Volatile market (%.1f%% swing) — use limit offers", analysis.getVolatilityPercent()));
			warningBannerLabel.setForeground(HerbWiseTheme.COLOR_STALE_YELLOW);
		}
		else if (thinlyTraded)
		{
			warningBannerLabel.setText("⚠ Thinly traded item — prices may move abruptly");
			warningBannerLabel.setForeground(HerbWiseTheme.COLOR_STALE_YELLOW);
		}
		else
		{
			warningBannerLabel.setText("✓ Healthy GE liquidity and stable spread");
			warningBannerLabel.setForeground(HerbWiseTheme.COLOR_PROFIT_GREEN);
		}

		chartPanel.setAnalysis(analysis);
	}

	/**
	 * Builds a local multi-point trend preview from the item's 1h, 5m, and instant prices
	 * when the live /timeseries endpoint is unreachable or before its response arrives.
	 */
	private MarketTrendAnalysis buildLocalSnapshotTrend(ItemPrice p, String timestep)
	{
		long now = Instant.now().getEpochSecond();
		long highNow = p.getInstantBuyPrice() != null ? p.getInstantBuyPrice() : 0L;
		long lowNow = p.getInstantSellPrice() != null ? p.getInstantSellPrice() : highNow;
		long high1h = p.getAvgHighPrice1h() != null ? p.getAvgHighPrice1h() : highNow;
		long low1h = p.getAvgLowPrice1h() != null ? p.getAvgLowPrice1h() : lowNow;

		List<TimeseriesPoint> pts = new ArrayList<>();
		int volStep = Math.max(1, p.getTotalVolume1h() / 12);
		for (int i = 0; i < 12; i++)
		{
			double t = i / 11.0;
			long h = Math.round(high1h * (1.0 - t) + highNow * t);
			long l = Math.round(low1h * (1.0 - t) + lowNow * t);
			pts.add(new TimeseriesPoint(now - (11 - i) * 300L, h, l, volStep / 2, volStep - (volStep / 2)));
		}

		return dataService.getWikiPriceService().analyzeTimeseries(p.getItemId(), p.getItemName(), timestep, pts);
	}

	/**
	 * Custom Swing chart component painting high/low price lines and volume bars.
	 */
	private static class PriceHistoryChartPanel extends JPanel
	{
		private MarketTrendAnalysis analysis;

		PriceHistoryChartPanel()
		{
			setBackground(HerbWiseTheme.BG_INPUT);
			setBorder(BorderFactory.createLineBorder(HerbWiseTheme.BORDER_SUBTLE, 1));
		}

		void setAnalysis(MarketTrendAnalysis analysis)
		{
			this.analysis = analysis;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int w = getWidth();
			int h = getHeight();

			if (analysis == null || analysis.getPoints().isEmpty())
			{
				g2.setColor(HerbWiseTheme.TEXT_MUTED);
				g2.setFont(HerbWiseTheme.getSmallFont());
				String msg = "No timeseries points available";
				FontMetrics fm = g2.getFontMetrics();
				g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
				g2.dispose();
				return;
			}

			List<TimeseriesPoint> pts = analysis.getPoints();
			long minPrice = Long.MAX_VALUE;
			long maxPrice = Long.MIN_VALUE;
			long maxVol = 1L;

			for (TimeseriesPoint pt : pts)
			{
				long hi = pt.getAvgHighPrice() != null && pt.getAvgHighPrice() > 0
					? pt.getAvgHighPrice()
					: pt.getRepresentativePrice();
				long lo = pt.getAvgLowPrice() != null && pt.getAvgLowPrice() > 0
					? pt.getAvgLowPrice()
					: pt.getRepresentativePrice();
				if (hi > 0) maxPrice = Math.max(maxPrice, hi);
				if (lo > 0) minPrice = Math.min(minPrice, lo);
				maxVol = Math.max(maxVol, pt.getTotalVolume());
			}

			if (minPrice == Long.MAX_VALUE || maxPrice <= 0)
			{
				g2.dispose();
				return;
			}
			if (maxPrice <= minPrice)
			{
				maxPrice = minPrice + 10L;
			}

			int padLeft = 8;
			int padRight = 8;
			int padTop = 18;
			int padBottom = 22;
			int plotW = Math.max(20, w - padLeft - padRight);
			int plotH = Math.max(20, h - padTop - padBottom);

			// Subtle horizontal grid lines
			g2.setColor(HerbWiseTheme.BORDER_SUBTLE);
			g2.setStroke(new BasicStroke(1.0f));
			for (int i = 0; i <= 3; i++)
			{
				int gy = padTop + (plotH * i) / 3;
				g2.drawLine(padLeft, gy, padLeft + plotW, gy);
			}

			int n = pts.size();
			// Paint subtle volume bars at bottom 25% of plot
			g2.setColor(new Color(0x22, 0xC5, 0x5E, 45));
			int barW = Math.max(2, plotW / Math.max(1, n * 2));
			for (int i = 0; i < n; i++)
			{
				TimeseriesPoint pt = pts.get(i);
				int x = padLeft + (n == 1 ? plotW / 2 : (i * plotW) / (n - 1));
				int barH = (int) Math.round((pt.getTotalVolume() / (double) maxVol) * (plotH * 0.25));
				g2.fillRect(x - barW / 2, padTop + plotH - barH, barW, barH);
			}

			// Paint High Price Line (Instant-Buy / Green) and Low Price Line (Instant-Sell / Cyan)
			int prevX = -1;
			int prevHighY = -1;
			int prevLowY = -1;

			for (int i = 0; i < n; i++)
			{
				TimeseriesPoint pt = pts.get(i);
				long hi = pt.getAvgHighPrice() != null && pt.getAvgHighPrice() > 0
					? pt.getAvgHighPrice()
					: pt.getRepresentativePrice();
				long lo = pt.getAvgLowPrice() != null && pt.getAvgLowPrice() > 0
					? pt.getAvgLowPrice()
					: pt.getRepresentativePrice();

				int x = padLeft + (n == 1 ? plotW / 2 : (i * plotW) / (n - 1));
				int highY = padTop + plotH - (int) Math.round(((hi - minPrice) / (double) (maxPrice - minPrice)) * plotH);
				int lowY = padTop + plotH - (int) Math.round(((lo - minPrice) / (double) (maxPrice - minPrice)) * plotH);

				if (prevX >= 0)
				{
					g2.setStroke(new BasicStroke(1.8f));
					g2.setColor(HerbWiseTheme.ACCENT_HERB_BRIGHT);
					g2.drawLine(prevX, prevHighY, x, highY);

					g2.setStroke(new BasicStroke(1.4f));
					g2.setColor(HerbWiseTheme.COLOR_INFO_CYAN);
					g2.drawLine(prevX, prevLowY, x, lowY);
				}

				prevX = x;
				prevHighY = highY;
				prevLowY = lowY;
			}

			// Price Range Labels
			g2.setFont(HerbWiseTheme.getBadgeFont());
			g2.setColor(HerbWiseTheme.ACCENT_HERB_BRIGHT);
			g2.drawString("High: " + HerbWiseTheme.formatGp(maxPrice), padLeft + 2, 13);

			g2.setColor(HerbWiseTheme.COLOR_INFO_CYAN);
			g2.drawString("Low: " + HerbWiseTheme.formatGp(minPrice), padLeft + 2, h - 6);

			g2.dispose();
		}
	}
}
