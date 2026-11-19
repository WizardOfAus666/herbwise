package com.herbwise;

import com.herbwise.models.PotionProfitResult;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.services.HerbloreDataService.ConnectionState;
import com.herbwise.ui.HerbWiseBudgetPanel;
import com.herbwise.ui.HerbWiseDashboard;
import com.herbwise.ui.HerbWiseMarketPanel;
import com.herbwise.ui.HerbWisePotionTable;
import com.herbwise.ui.HerbWiseRecipeDetailPanel;
import com.herbwise.ui.HerbWiseSettingsPanel;
import com.herbwise.ui.HerbWiseTheme;
import com.herbwise.ui.HerbWiseTheme.HerbButton;
import com.herbwise.ui.HerbWiseXpCalculator;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;

/**
 * Main RuneLite sidebar panel for HerbWise.
 * Organizes the 6 required tabs (Dashboard, Potion Database, Budget Calculator,
 * XP Calculator, Market Analysis, and Settings) plus the interactive Recipe Detail View.
 */
public class HerbWisePanel extends PluginPanel
{
	public static final String TAB_DASHBOARD = "DASHBOARD";
	public static final String TAB_DATABASE = "DATABASE";
	public static final String TAB_BUDGET = "BUDGET";
	public static final String TAB_XP_CALC = "XP_CALC";
	public static final String TAB_MARKET = "MARKET";
	public static final String TAB_SETTINGS = "SETTINGS";
	public static final String TAB_RECIPE_DETAIL = "RECIPE_DETAIL";

	public static final int DEFAULT_WIDE_PANEL_WIDTH = 580;
	public static final int DEFAULT_WIDE_PANEL_HEIGHT = 820;

	private final HerbloreDataService dataService;
	private final Runnable uiListenerRef = this::refreshAllTabs;
	private javax.swing.JFrame popoutFrame;

	// Header status widgets
	private final JLabel statusBadgeLabel;
	private final JLabel lastUpdatedLabel;
	private final HerbButton manualRefreshButton;

	// Navigation buttons
	private final HerbButton btnDashboard;
	private final HerbButton btnDatabase;
	private final HerbButton btnBudget;
	private final HerbButton btnXpCalc;
	private final HerbButton btnMarket;
	private final HerbButton btnSettings;

	// Card layout & child tabs
	private final CardLayout cardLayout;
	private final JPanel cardContainer;
	private final HerbWiseDashboard dashboardPanel;
	private final JPanel databaseTabWrapper;
	private final HerbWisePotionTable databaseTable;
	private final HerbWiseBudgetPanel budgetPanel;
	private final HerbWiseXpCalculator xpCalculatorPanel;
	private final HerbWiseMarketPanel marketPanel;
	private final HerbWiseSettingsPanel settingsPanel;
	private final HerbWiseRecipeDetailPanel recipeDetailPanel;

	private String currentActiveTab = TAB_DASHBOARD;
	private String previousTabBeforeDetail = TAB_DASHBOARD;

	@Inject
	public HerbWisePanel(HerbloreDataService dataService)
	{
		super(false);
		this.dataService = dataService;

		setLayout(new BorderLayout(0, 0));
		setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		// ====================================================================
		// TOP HEADER & NAVIGATION BAR
		// ====================================================================
		JPanel topContainer = new JPanel();
		topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
		topContainer.setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		topContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

		// Brand & API Status Card
		JPanel headerCard = new JPanel();
		headerCard.setLayout(new BoxLayout(headerCard, BoxLayout.Y_AXIS));
		headerCard.setBackground(HerbWiseTheme.BG_DEEP_CHARCOAL);
		headerCard.setBorder(HerbWiseTheme.CARD_BORDER_ACCENT);

		JPanel titleRow = new JPanel(new BorderLayout(6, 0));
		titleRow.setOpaque(false);

		JPanel brandLeft = new JPanel(new BorderLayout(6, 0));
		brandLeft.setOpaque(false);
		JLabel iconLabel = new JLabel(new ImageIcon(HerbWiseTheme.createHeaderIcon()));
		brandLeft.add(iconLabel, BorderLayout.WEST);

		JPanel titleTextCol = new JPanel(new GridLayout(2, 1, 0, 0));
		titleTextCol.setOpaque(false);
		JLabel titleLbl = new JLabel("HERBWISE");
		titleLbl.setFont(HerbWiseTheme.getTitleFont());
		titleLbl.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		JLabel subLbl = new JLabel("Herblore Profit & XP Assistant");
		subLbl.setFont(HerbWiseTheme.getSmallFont());
		subLbl.setForeground(HerbWiseTheme.TEXT_MUTED);
		titleTextCol.add(titleLbl);
		titleTextCol.add(subLbl);
		brandLeft.add(titleTextCol, BorderLayout.CENTER);
		titleRow.add(brandLeft, BorderLayout.CENTER);

		manualRefreshButton = new HerbButton("↻ Refresh");
		manualRefreshButton.setToolTipText("Request a price refresh via the central rate-limited scheduler");
		manualRefreshButton.addActionListener(e -> {
			if (dataService != null)
			{
				dataService.manualRefreshAsync();
			}
		});

		HerbButton widePopoutBtn = new HerbButton("⤢ Pop Out", true);
		widePopoutBtn.setToolTipText("Open HerbWise in a standalone resizable window");
		widePopoutBtn.addActionListener(e -> openWidePopoutWindow());

		JPanel headerActions = new JPanel(new GridLayout(1, 2, 4, 0));
		headerActions.setOpaque(false);
		headerActions.add(widePopoutBtn);
		headerActions.add(manualRefreshButton);

		titleRow.add(headerActions, BorderLayout.EAST);
		headerCard.add(titleRow);
		headerCard.add(Box.createRigidArea(new Dimension(0, 5)));

		// API Connection Status & Last Updated Row (Section 2)
		JPanel statusRow = new JPanel(new BorderLayout(4, 0));
		statusRow.setOpaque(false);

		statusBadgeLabel = new JLabel("● LIVE API");
		statusBadgeLabel.setFont(HerbWiseTheme.getBadgeFont());
		statusBadgeLabel.setForeground(HerbWiseTheme.COLOR_PROFIT_GREEN);
		statusRow.add(statusBadgeLabel, BorderLayout.WEST);

		lastUpdatedLabel = new JLabel("Updated: Just now");
		lastUpdatedLabel.setFont(HerbWiseTheme.getSmallFont());
		lastUpdatedLabel.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		lastUpdatedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		statusRow.add(lastUpdatedLabel, BorderLayout.CENTER);
		headerCard.add(statusRow);

		topContainer.add(headerCard);
		topContainer.add(Box.createRigidArea(new Dimension(0, 5)));

		// 6 Navigation Tabs arranged in a compact 2x3 grid
		JPanel navGrid = new JPanel(new GridLayout(2, 3, 4, 4));
		navGrid.setOpaque(false);

		btnDashboard = new HerbButton("1. Dashboard");
		btnDatabase = new HerbButton("2. Database");
		btnBudget = new HerbButton("3. Budget");
		btnXpCalc = new HerbButton("4. XP Calc");
		btnMarket = new HerbButton("5. Market");
		btnSettings = new HerbButton("6. Settings");

		btnDashboard.addActionListener(e -> switchTab(TAB_DASHBOARD));
		btnDatabase.addActionListener(e -> switchTab(TAB_DATABASE));
		btnBudget.addActionListener(e -> switchTab(TAB_BUDGET));
		btnXpCalc.addActionListener(e -> switchTab(TAB_XP_CALC));
		btnMarket.addActionListener(e -> switchTab(TAB_MARKET));
		btnSettings.addActionListener(e -> switchTab(TAB_SETTINGS));

		navGrid.add(btnDashboard);
		navGrid.add(btnDatabase);
		navGrid.add(btnBudget);
		navGrid.add(btnXpCalc);
		navGrid.add(btnMarket);
		navGrid.add(btnSettings);

		topContainer.add(navGrid);
		add(topContainer, BorderLayout.NORTH);

		// ====================================================================
		// TAB CONTENT CARDS
		// ====================================================================
		cardLayout = new CardLayout();
		cardContainer = new JPanel(cardLayout);
		cardContainer.setBackground(HerbWiseTheme.BG_MATTE_BLACK);

		dashboardPanel = new HerbWiseDashboard(dataService, this::openRecipeDetail);

		// Dedicated Potion Database Tab (includes 1-4 dose variants & alt recipes by default)
		databaseTabWrapper = new JPanel(new BorderLayout(0, 4));
		databaseTabWrapper.setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		databaseTabWrapper.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
		JLabel dbHeader = HerbWiseTheme.createSectionHeader("Complete Verified Herblore Recipe Database");
		databaseTabWrapper.add(dbHeader, BorderLayout.NORTH);
		databaseTable = new HerbWisePotionTable(dataService, this::openRecipeDetail, true);
		databaseTabWrapper.add(databaseTable, BorderLayout.CENTER);

		budgetPanel = new HerbWiseBudgetPanel(dataService);
		xpCalculatorPanel = new HerbWiseXpCalculator(dataService);
		marketPanel = new HerbWiseMarketPanel(dataService);
		settingsPanel = new HerbWiseSettingsPanel(dataService);
		recipeDetailPanel = new HerbWiseRecipeDetailPanel(
			dataService,
			() -> switchTab(previousTabBeforeDetail),
			result -> {
				budgetPanel.selectPotion(result);
				switchTab(TAB_BUDGET);
			},
			result -> {
				xpCalculatorPanel.selectPotion(result);
				switchTab(TAB_XP_CALC);
			},
			itemId -> {
				marketPanel.selectItem(itemId);
				switchTab(TAB_MARKET);
			}
		);

		cardContainer.add(dashboardPanel, TAB_DASHBOARD);
		cardContainer.add(databaseTabWrapper, TAB_DATABASE);
		cardContainer.add(budgetPanel, TAB_BUDGET);
		cardContainer.add(xpCalculatorPanel, TAB_XP_CALC);
		cardContainer.add(marketPanel, TAB_MARKET);
		cardContainer.add(settingsPanel, TAB_SETTINGS);
		cardContainer.add(recipeDetailPanel, TAB_RECIPE_DETAIL);

		add(cardContainer, BorderLayout.CENTER);

		if (this.dataService != null)
		{
			this.dataService.addUiUpdateListener(uiListenerRef);
		}

		switchTab(TAB_DASHBOARD);
		refreshHeaderStatus();
		HerbWiseTheme.normalizeBoxAlignment(this);
	}

	public void disposePanelListeners()
	{
		if (this.dataService != null)
		{
			this.dataService.removeUiUpdateListener(uiListenerRef);
		}
	}

	public void switchTab(String tabKey)
	{
		if (tabKey == null)
		{
			return;
		}
		if (!TAB_RECIPE_DETAIL.equals(tabKey))
		{
			this.previousTabBeforeDetail = tabKey;
		}
		this.currentActiveTab = tabKey;

		btnDashboard.setActive(TAB_DASHBOARD.equals(tabKey));
		btnDatabase.setActive(TAB_DATABASE.equals(tabKey));
		btnBudget.setActive(TAB_BUDGET.equals(tabKey));
		btnXpCalc.setActive(TAB_XP_CALC.equals(tabKey));
		btnMarket.setActive(TAB_MARKET.equals(tabKey));
		btnSettings.setActive(TAB_SETTINGS.equals(tabKey));

		cardLayout.show(cardContainer, tabKey);
		HerbWiseTheme.normalizeBoxAlignment(this);
		revalidate();
		repaint();
	}

	public void openRecipeDetail(PotionProfitResult result)
	{
		if (result == null)
		{
			return;
		}
		recipeDetailPanel.displayRecipe(result);
		switchTab(TAB_RECIPE_DETAIL);
	}

	public void refreshAllTabs()
	{
		refreshHeaderStatus();
		dashboardPanel.refresh();
		databaseTable.refreshTable();
		budgetPanel.refresh();
		xpCalculatorPanel.refresh();
		marketPanel.refresh();
		settingsPanel.refreshFromService();
		recipeDetailPanel.render();
		revalidate();
		repaint();
	}

	private void refreshHeaderStatus()
	{
		if (dataService == null)
		{
			return;
		}
		ConnectionState state = dataService.getConnectionState();
		statusBadgeLabel.setText("● " + state.getBadgeText());
		statusBadgeLabel.setForeground(state.getBadgeColor());
		statusBadgeLabel.setToolTipText(dataService.getStatusDetailText());

		lastUpdatedLabel.setText(dataService.getCompactApiStatusSummary());
		lastUpdatedLabel.setToolTipText(dataService.getStatusDetailText());
		manualRefreshButton.setEnabled(!dataService.isRefreshRunning());
		manualRefreshButton.setText(dataService.isRefreshRunning() ? "Updating..." : "↻ Refresh");
	}

	/**
	 * Overrides RuneLite's default 225px PluginPanel width so HerbWise renders at a compact
	 * yet fully readable 520px width where all 10 columns and cards fit cleanly without hogging the screen.
	 */
	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(DEFAULT_WIDE_PANEL_WIDTH, DEFAULT_WIDE_PANEL_HEIGHT);
	}

	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(320, 560);
	}

	public void openWidePopoutWindow()
	{
		if (popoutFrame != null && popoutFrame.isDisplayable())
		{
			popoutFrame.setVisible(true);
			popoutFrame.toFront();
			popoutFrame.requestFocus();
			return;
		}
		HerbWisePanel widePanel = new HerbWisePanel(dataService);
		popoutFrame = new javax.swing.JFrame("HerbWise — OSRS Herblore Profit & Training Assistant");
		popoutFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		popoutFrame.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosed(java.awt.event.WindowEvent e)
			{
				widePanel.disposePanelListeners();
			}
		});
		popoutFrame.setIconImage(HerbWiseTheme.createHeaderIcon());
		popoutFrame.getContentPane().setBackground(HerbWiseTheme.BG_MATTE_BLACK);
		popoutFrame.getContentPane().setLayout(new BorderLayout());
		popoutFrame.getContentPane().add(widePanel, BorderLayout.CENTER);
		popoutFrame.setSize(new Dimension(600, 860));
		popoutFrame.setMinimumSize(new Dimension(480, 620));
		popoutFrame.setLocationRelativeTo(this);
		popoutFrame.setVisible(true);
	}

	public String getCurrentActiveTab() { return currentActiveTab; }
	public HerbWiseDashboard getDashboardPanel() { return dashboardPanel; }
	public HerbWisePotionTable getDatabaseTable() { return databaseTable; }
	public HerbWiseBudgetPanel getBudgetPanel() { return budgetPanel; }
	public HerbWiseXpCalculator getXpCalculatorPanel() { return xpCalculatorPanel; }
	public HerbWiseMarketPanel getMarketPanel() { return marketPanel; }
	public HerbWiseSettingsPanel getSettingsPanel() { return settingsPanel; }
	public HerbWiseRecipeDetailPanel getRecipeDetailPanel() { return recipeDetailPanel; }
}
