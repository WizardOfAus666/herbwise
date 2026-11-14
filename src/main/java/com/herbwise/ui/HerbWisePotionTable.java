package com.herbwise.ui;

import com.herbwise.HerbWiseConfig.TableDisplayMode;
import com.herbwise.models.PotionProfitResult;
import com.herbwise.models.PotionProfitResult.PriceStatus;
import com.herbwise.models.PotionRecipe;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.services.HerbloreDataService.FilterMode;
import com.herbwise.services.HerbloreDataService.SortMode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Reusable interactive recommendations and potion database table component.
 * Displays all 10 required columns:
 * 0: Potion Icon
 * 1: Potion Name
 * 2: Required Level
 * 3: XP Per Potion
 * 4: Ingredient Cost
 * 5: Sale Price
 * 6: GE Tax
 * 7: Profit/Loss
 * 8: Cost Per XP
 * 9: Profit Margin
 */
public class HerbWisePotionTable extends JPanel
{
	private final HerbloreDataService dataService;
	private final Consumer<PotionProfitResult> onPotionClicked;

	private final JTextField searchField;
	private final JComboBox<FilterMode> filterCombo;
	private final JComboBox<SortMode> sortCombo;
	private final JCheckBox includeVariantsCheck;
	private final JLabel countSummaryLabel;

	private final PotionTableModel tableModel;
	private final JTable table;
	private final JScrollPane tableScrollPane;

	private boolean suppressEvents = false;

	public HerbWisePotionTable(
		HerbloreDataService dataService,
		Consumer<PotionProfitResult> onPotionClicked,
		boolean defaultShowVariants)
	{
		this.dataService = dataService;
		this.onPotionClicked = onPotionClicked;

		setLayout(new BorderLayout(0, 5));
		setBackground(HerbWiseTheme.BG_MATTE_BLACK);

		// Controls Header
		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBackground(HerbWiseTheme.BG_DEEP_CHARCOAL);
		controlsPanel.setBorder(HerbWiseTheme.CARD_BORDER);

		// Search Box Row
		JPanel searchRow = new JPanel(new BorderLayout(4, 0));
		searchRow.setOpaque(false);
		JLabel searchLbl = new JLabel("Search:");
		searchLbl.setFont(HerbWiseTheme.getSmallFont());
		searchLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		searchRow.add(searchLbl, BorderLayout.WEST);

		searchField = HerbWiseTheme.createStyledTextField("");
		searchField.setToolTipText("Search by potion name, herb, or secondary ingredient...");
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e) { refreshTable(); }
			@Override
			public void removeUpdate(DocumentEvent e) { refreshTable(); }
			@Override
			public void changedUpdate(DocumentEvent e) { refreshTable(); }
		});
		searchRow.add(searchField, BorderLayout.CENTER);
		controlsPanel.add(searchRow);
		controlsPanel.add(Box.createRigidArea(new Dimension(0, 4)));

		// Filter & Sort Row side-by-side for compact vertical height
		JPanel filterSortGrid = new JPanel(new GridLayout(1, 2, 6, 0));
		filterSortGrid.setOpaque(false);

		JPanel filterRow = new JPanel(new BorderLayout(4, 0));
		filterRow.setOpaque(false);
		JLabel filterLbl = new JLabel("Filter:");
		filterLbl.setFont(HerbWiseTheme.getSmallFont());
		filterLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		filterCombo = new JComboBox<>(FilterMode.values());
		HerbWiseTheme.styleComboBox(filterCombo);
		filterCombo.addActionListener(e -> {
			if (!suppressEvents) refreshTable();
		});
		filterRow.add(filterLbl, BorderLayout.WEST);
		filterRow.add(filterCombo, BorderLayout.CENTER);
		filterSortGrid.add(filterRow);

		JPanel sortRow = new JPanel(new BorderLayout(4, 0));
		sortRow.setOpaque(false);
		JLabel sortLbl = new JLabel("Sort:");
		sortLbl.setFont(HerbWiseTheme.getSmallFont());
		sortLbl.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		sortCombo = new JComboBox<>(SortMode.values());
		HerbWiseTheme.styleComboBox(sortCombo);
		sortCombo.addActionListener(e -> {
			if (!suppressEvents) refreshTable();
		});
		sortRow.add(sortLbl, BorderLayout.WEST);
		sortRow.add(sortCombo, BorderLayout.CENTER);
		filterSortGrid.add(sortRow);

		controlsPanel.add(filterSortGrid);
		controlsPanel.add(Box.createRigidArea(new Dimension(0, 4)));

		// Variants Checkbox + Count Row
		JPanel bottomControlRow = new JPanel(new BorderLayout(4, 0));
		bottomControlRow.setOpaque(false);

		includeVariantsCheck = new JCheckBox("Include 1-3 Dose Variants & Alternative Recipes", defaultShowVariants);
		includeVariantsCheck.setOpaque(false);
		includeVariantsCheck.setFont(HerbWiseTheme.getSmallFont());
		includeVariantsCheck.setForeground(HerbWiseTheme.TEXT_SECONDARY);
		includeVariantsCheck.addActionListener(e -> refreshTable());
		bottomControlRow.add(includeVariantsCheck, BorderLayout.CENTER);

		countSummaryLabel = new JLabel("0 recipes");
		countSummaryLabel.setFont(HerbWiseTheme.getSmallFont());
		countSummaryLabel.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		countSummaryLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		bottomControlRow.add(countSummaryLabel, BorderLayout.EAST);

		controlsPanel.add(bottomControlRow);
		add(controlsPanel, BorderLayout.NORTH);

		// Table Setup
		tableModel = new PotionTableModel();
		table = new JTable(tableModel);
		table.setBackground(HerbWiseTheme.BG_CARD);
		table.setForeground(HerbWiseTheme.TEXT_PRIMARY);
		table.setGridColor(HerbWiseTheme.BORDER_SUBTLE);
		table.setRowHeight(28);
		table.setFont(HerbWiseTheme.getSmallFont());
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setSelectionBackground(HerbWiseTheme.BG_CARD_HOVER);
		table.setSelectionForeground(HerbWiseTheme.TEXT_PRIMARY);
		table.setFillsViewportHeight(true);
		table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		JTableHeader header = table.getTableHeader();
		header.setBackground(HerbWiseTheme.BG_DEEP_CHARCOAL);
		header.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
		header.setFont(HerbWiseTheme.getBadgeFont());
		header.setReorderingAllowed(false);

		// Header click sorting
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				int col = table.columnAtPoint(e.getPoint());
				handleColumnHeaderSort(col);
			}
		});

		// Row click opens Recipe Detail View
		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				int viewRow = table.rowAtPoint(e.getPoint());
				if (viewRow >= 0)
				{
					int modelRow = table.convertRowIndexToModel(viewRow);
					PotionProfitResult selected = tableModel.getResultAt(modelRow);
					if (selected != null && onPotionClicked != null)
					{
						onPotionClicked.accept(selected);
					}
				}
			}
		});

		configureColumnRenderersAndWidths();

		tableScrollPane = HerbWiseTheme.createStyledScrollPane(table);
		tableScrollPane.setBorder(BorderFactory.createLineBorder(HerbWiseTheme.BORDER_SUBTLE, 1));
		tableScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		tableScrollPane.setPreferredSize(new Dimension(380, 360));
		add(tableScrollPane, BorderLayout.CENTER);
	}

	private void handleColumnHeaderSort(int colIndex)
	{
		suppressEvents = true;
		try
		{
			switch (colIndex)
			{
				case 2:
					sortCombo.setSelectedItem(SortMode.LEVEL_ASCENDING);
					break;
				case 3:
					sortCombo.setSelectedItem(SortMode.HIGHEST_XP);
					break;
				case 4:
					sortCombo.setSelectedItem(SortMode.LOWEST_INGREDIENT_COST);
					break;
				case 7:
				case 9:
					sortCombo.setSelectedItem(SortMode.HIGHEST_PROFIT);
					break;
				case 8:
					sortCombo.setSelectedItem(SortMode.CHEAPEST_XP);
					break;
				default:
					sortCombo.setSelectedItem(SortMode.BEST_OVERALL_TRAINING);
					break;
			}
		}
		finally
		{
			suppressEvents = false;
		}
		refreshTable();
	}

	private void configureColumnRenderersAndWidths()
	{
		PotionCellRenderer cellRenderer = new PotionCellRenderer();
		TableColumnModel colModel = table.getColumnModel();
		int[] widths = new int[]{28, 145, 42, 42, 56, 56, 44, 62, 52, 48};

		for (int i = 0; i < colModel.getColumnCount(); i++)
		{
			TableColumn col = colModel.getColumn(i);
			col.setPreferredWidth(widths[i]);
			col.setMinWidth(i == 0 ? 22 : 28);
			col.setCellRenderer(cellRenderer);
		}
	}

	public void applyDisplayMode(TableDisplayMode mode)
	{
		boolean compact = mode == TableDisplayMode.COMPACT;
		TableColumnModel colModel = table.getColumnModel();
		if (colModel.getColumnCount() < 10)
		{
			return;
		}
		int[] detailedWidths = new int[]{28, 145, 42, 42, 56, 56, 44, 62, 52, 48};
		int[] compactWidths = new int[]{26, 130, 38, 38, 50, 50, 40, 56, 48, 44};
		int[] target = compact ? compactWidths : detailedWidths;
		for (int i = 0; i < 10; i++)
		{
			colModel.getColumn(i).setPreferredWidth(target[i]);
		}
		table.setRowHeight(compact ? 24 : 26);
	}

	public void setFilterMode(FilterMode mode)
	{
		if (mode != null)
		{
			filterCombo.setSelectedItem(mode);
			refreshTable();
		}
	}

	public void setSortMode(SortMode mode)
	{
		if (mode != null)
		{
			sortCombo.setSelectedItem(mode);
			refreshTable();
		}
	}

	public void refreshTable()
	{
		if (dataService == null)
		{
			return;
		}
		applyDisplayMode(dataService.getTableDisplayMode());

		FilterMode filter = (FilterMode) filterCombo.getSelectedItem();
		SortMode sort = (SortMode) sortCombo.getSelectedItem();
		String query = searchField.getText();
		boolean showVariants = includeVariantsCheck.isSelected();

		List<PotionProfitResult> filtered = dataService.filterAndSortResults(filter, sort, query, showVariants);
		tableModel.setRows(filtered);
		countSummaryLabel.setText(filtered.size() + (filtered.size() == 1 ? " recipe" : " recipes"));
	}

	public JTable getTable()
	{
		return table;
	}

	public List<PotionProfitResult> getDisplayedResults()
	{
		return tableModel.getRows();
	}

	/**
	 * Table model exposing the 10 required columns from Section 5.
	 */
	public static class PotionTableModel extends AbstractTableModel
	{
		private static final String[] COLUMNS = new String[]{
			"Icon",
			"Potion",
			"Lvl",
			"XP",
			"Cost",
			"Sale",
			"Tax",
			"Profit",
			"GP/XP",
			"Margin"
		};

		private List<PotionProfitResult> rows = Collections.emptyList();

		public void setRows(List<PotionProfitResult> newRows)
		{
			this.rows = newRows != null ? new ArrayList<>(newRows) : Collections.emptyList();
			fireTableDataChanged();
		}

		public List<PotionProfitResult> getRows()
		{
			return Collections.unmodifiableList(rows);
		}

		public PotionProfitResult getResultAt(int rowIndex)
		{
			if (rowIndex < 0 || rowIndex >= rows.size())
			{
				return null;
			}
			return rows.get(rowIndex);
		}

		@Override
		public int getRowCount()
		{
			return rows.size();
		}

		@Override
		public int getColumnCount()
		{
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			PotionProfitResult r = getResultAt(rowIndex);
			if (r == null || r.getRecipe() == null)
			{
				return "";
			}
			PotionRecipe rec = r.getRecipe();
			switch (columnIndex)
			{
				case 0:
					return rec.getPotionItemId();
				case 1:
					return rec.getPotionName();
				case 2:
					return r.isAvailableAtPlayerLevel()
						? String.valueOf(rec.getRequiredLevel())
						: (rec.getRequiredLevel() + " (-" + r.getLevelsUntilUnlock() + ")");
				case 3:
					return String.format(Locale.US, "%.1f", r.getXpPerPotion());
				case 4:
					return r.isHasIngredientPriceData()
						? HerbWiseTheme.formatNumber(r.getTotalIngredientCost())
						: "Unavail.";
				case 5:
					return r.isHasCompletePriceData()
						? HerbWiseTheme.formatNumber(r.getFinishedPotionSalePrice())
						: "Unavail.";
				case 6:
					return r.isHasCompletePriceData()
						? HerbWiseTheme.formatNumber(r.getGeTax())
						: "—";
				case 7:
					if (!r.isHasCompletePriceData())
					{
						return "—";
					}
					long prof = r.getNetProfitPerPotion();
					return (prof > 0 ? "+" : "") + HerbWiseTheme.formatNumber(prof);
				case 8:
					if (!r.isHasCompletePriceData() || r.getXpPerPotion() <= 0.0)
					{
						return r.isHasIngredientPriceData() && r.getXpPerPotion() > 0.0
							? String.format(Locale.US, "%.2f", r.getIngredientCostPerXp())
							: "—";
					}
					return r.getNetCostPerXp() <= 0.0
						? String.format(Locale.US, "+%.2f", Math.abs(r.getNetCostPerXp()))
						: String.format(Locale.US, "%.2f", r.getNetCostPerXp());
				case 9:
					return r.isHasCompletePriceData()
						? String.format(Locale.US, "%+.1f%%", r.getProfitMarginPercent())
						: "—";
				default:
					return "";
			}
		}
	}

	private class PotionCellRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(
			JTable tbl,
			Object value,
			boolean isSelected,
			boolean hasFocus,
			int row,
			int column)
		{
			JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
			lbl.setIcon(null);
			lbl.setFont(HerbWiseTheme.getSmallFont());
			lbl.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

			int modelRow = tbl.convertRowIndexToModel(row);
			PotionProfitResult r = tableModel.getResultAt(modelRow);
			if (r == null || r.getRecipe() == null)
			{
				return lbl;
			}

			PotionRecipe rec = r.getRecipe();
			boolean unlocked = r.isAvailableAtPlayerLevel();

			if (!isSelected)
			{
				lbl.setBackground(row % 2 == 0 ? HerbWiseTheme.BG_CARD : HerbWiseTheme.BG_DEEP_CHARCOAL);
			}

			// Icon column
			if (column == 0)
			{
				lbl.setText("");
				lbl.setHorizontalAlignment(SwingConstants.CENTER);
				lbl.setIcon(new ImageIcon(HerbloreDataService.createPotionVialFallbackIcon(rec.getPotionName())));
				return lbl;
			}

			// Alignment
			if (column == 1)
			{
				lbl.setHorizontalAlignment(SwingConstants.LEFT);
				lbl.setFont(unlocked ? HerbWiseTheme.getBoldFont() : HerbWiseTheme.getRegularFont());
			}
			else if (column == 2)
			{
				lbl.setHorizontalAlignment(SwingConstants.CENTER);
			}
			else
			{
				lbl.setHorizontalAlignment(SwingConstants.RIGHT);
			}

			// Colour rules per Section 5 & Section 6:
			// Grey out potions above the player's current Herblore level
			if (!unlocked && (column == 1 || column == 2))
			{
				lbl.setForeground(HerbWiseTheme.TEXT_LOCKED);
			}
			else if (column == 2 && unlocked)
			{
				lbl.setForeground(HerbWiseTheme.ACCENT_HERB_BRIGHT);
			}
			else if (column == 7 || column == 8 || column == 9)
			{
				// Profit/loss, Cost/XP, and Margin columns use semantic status colours
				PriceStatus status = r.getPriceStatus();
				if (status == PriceStatus.MISSING_DATA || !r.isHasCompletePriceData())
				{
					lbl.setForeground(HerbWiseTheme.COLOR_MISSING_GREY);
				}
				else if (status == PriceStatus.UNCERTAIN_OR_STALE)
				{
					lbl.setForeground(HerbWiseTheme.COLOR_STALE_YELLOW);
				}
				else if (r.getNetProfitPerPotion() >= 0L)
				{
					lbl.setForeground(HerbWiseTheme.COLOR_PROFIT_GREEN);
				}
				else
				{
					lbl.setForeground(HerbWiseTheme.COLOR_LOSS_RED);
				}
			}
			else
			{
				lbl.setForeground(unlocked ? HerbWiseTheme.TEXT_PRIMARY : HerbWiseTheme.TEXT_MUTED);
			}

			lbl.setToolTipText(buildRowTooltip(r));
			return lbl;
		}

		private String buildRowTooltip(PotionProfitResult r)
		{
			PotionRecipe rec = r.getRecipe();
			StringBuilder sb = new StringBuilder("<html><b>");
			sb.append(rec.getPotionName()).append("</b> (Lvl ").append(rec.getRequiredLevel())
				.append(" • ").append(rec.getXpGained()).append(" XP)<br/>");
			sb.append("Ingredients: ").append(rec.getFormattedIngredientList()).append("<br/>");
			if (r.isHasCompletePriceData())
			{
				sb.append("Cost: ").append(HerbWiseTheme.formatGp(r.getTotalIngredientCost()))
					.append(" | Sale: ").append(HerbWiseTheme.formatGp(r.getFinishedPotionSalePrice()))
					.append(" (Tax: ").append(HerbWiseTheme.formatGp(r.getGeTax())).append(")<br/>");
				sb.append("Net Profit/Loss: <b>").append(HerbWiseTheme.formatSignedGp(r.getNetProfitPerPotion())).append("</b>");
			}
			else
			{
				sb.append("Status: ").append(r.getStatusMessage());
			}
			if (!r.isAvailableAtPlayerLevel())
			{
				sb.append("<br/><span style='color:#f87171;'>Locked: Requires +")
					.append(r.getLevelsUntilUnlock()).append(" Herblore levels</span>");
			}
			sb.append("<br/><i>Click row to open Recipe Detail View</i></html>");
			return sb.toString();
		}
	}
}
