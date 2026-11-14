package com.herbwise.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import net.runelite.client.ui.FontManager;

/**
 * Design system and UI component factory for HerbWise.
 * Implements a dark matte-black and deep charcoal aesthetic with subtle Herblore emerald accents.
 */
public final class HerbWiseTheme
{
	// Core Surfaces (Matte Black & Deep Charcoal)
	public static final Color BG_MATTE_BLACK = new Color(0x0D, 0x10, 0x14);
	public static final Color BG_DEEP_CHARCOAL = new Color(0x14, 0x19, 0x20);
	public static final Color BG_CARD = new Color(0x1A, 0x21, 0x2B);
	public static final Color BG_CARD_ELEVATED = new Color(0x21, 0x2A, 0x36);
	public static final Color BG_CARD_HOVER = new Color(0x27, 0x32, 0x40);
	public static final Color BG_INPUT = new Color(0x10, 0x14, 0x1A);

	// Borders
	public static final Color BORDER_SUBTLE = new Color(0x26, 0x30, 0x3D);
	public static final Color BORDER_DEFAULT = new Color(0x31, 0x3E, 0x4F);
	public static final Color BORDER_HERB_GREEN = new Color(0x16, 0x65, 0x34);
	public static final Color BORDER_HERB_BRIGHT = new Color(0x22, 0xC5, 0x5E);

	// Herblore Emerald Accents
	public static final Color ACCENT_HERB = new Color(0x22, 0xC5, 0x5E);
	public static final Color ACCENT_HERB_BRIGHT = new Color(0x4A, 0xDE, 0x80);
	public static final Color ACCENT_HERB_DARK = new Color(0x14, 0x53, 0x2D);
	public static final Color ACCENT_GOLD = new Color(0xF5, 0x9E, 0x0B);

	// Semantic Profit / Loss / Status Colours (Section 5 specification)
	public static final Color COLOR_PROFIT_GREEN = new Color(0x34, 0xD3, 0x99);
	public static final Color COLOR_LOSS_RED = new Color(0xF8, 0x71, 0x71);
	public static final Color COLOR_STALE_YELLOW = new Color(0xFB, 0xBF, 0x24);
	public static final Color COLOR_MISSING_GREY = new Color(0x94, 0xA3, 0xB8);
	public static final Color COLOR_INFO_CYAN = new Color(0x38, 0xBD, 0xF8);

	// Typography Colours
	public static final Color TEXT_PRIMARY = new Color(0xF1, 0xF5, 0xF9);
	public static final Color TEXT_SECONDARY = new Color(0xCB, 0xD5, 0xE1);
	public static final Color TEXT_MUTED = new Color(0x88, 0x96, 0xA8);
	public static final Color TEXT_LOCKED = new Color(0x64, 0x74, 0x8B);

	// Standard Borders
	public static final Border CARD_BORDER = BorderFactory.createCompoundBorder(
		BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
		BorderFactory.createEmptyBorder(7, 8, 7, 8)
	);

	public static final Border CARD_BORDER_ACCENT = BorderFactory.createCompoundBorder(
		BorderFactory.createLineBorder(BORDER_HERB_GREEN, 1),
		BorderFactory.createEmptyBorder(7, 8, 7, 8)
	);

	public static final Border INPUT_BORDER = BorderFactory.createCompoundBorder(
		BorderFactory.createLineBorder(BORDER_DEFAULT, 1),
		BorderFactory.createEmptyBorder(3, 6, 3, 6)
	);

	private static final NumberFormat INT_FMT = NumberFormat.getIntegerInstance(Locale.US);

	private HerbWiseTheme()
	{
	}

	public static Font getTitleFont()
	{
		try
		{
			return FontManager.getDefaultBoldFont().deriveFont(14.5f);
		}
		catch (Throwable t)
		{
			return new Font("SansSerif", Font.BOLD, 14);
		}
	}

	public static Font getSectionFont()
	{
		try
		{
			return FontManager.getDefaultBoldFont().deriveFont(12.0f);
		}
		catch (Throwable t)
		{
			return new Font("SansSerif", Font.BOLD, 12);
		}
	}

	public static Font getBoldFont()
	{
		try
		{
			return FontManager.getDefaultBoldFont().deriveFont(11.0f);
		}
		catch (Throwable t)
		{
			return new Font("SansSerif", Font.BOLD, 11);
		}
	}

	public static Font getRegularFont()
	{
		try
		{
			return FontManager.getDefaultFont().deriveFont(11.0f);
		}
		catch (Throwable t)
		{
			return new Font("SansSerif", Font.PLAIN, 11);
		}
	}

	public static Font getSmallFont()
	{
		try
		{
			return FontManager.getDefaultFont().deriveFont(10.0f);
		}
		catch (Throwable t)
		{
			return new Font("SansSerif", Font.PLAIN, 10);
		}
	}

	public static Font getBadgeFont()
	{
		try
		{
			return FontManager.getDefaultBoldFont().deriveFont(9.5f);
		}
		catch (Throwable t)
		{
			return new Font("SansSerif", Font.BOLD, 9);
		}
	}

	public static String formatGp(long gp)
	{
		return INT_FMT.format(gp) + " GP";
	}

	public static String formatSignedGp(long gp)
	{
		if (gp > 0L)
		{
			return "+" + INT_FMT.format(gp) + " GP";
		}
		return INT_FMT.format(gp) + " GP";
	}

	public static String formatCompactGp(long gp)
	{
		long abs = Math.abs(gp);
		String sign = gp < 0 ? "-" : (gp > 0 ? "+" : "");
		if (abs >= 1_000_000_000L)
		{
			return String.format(Locale.US, "%s%.2fB", sign, gp / 1_000_000_000.0);
		}
		if (abs >= 1_000_000L)
		{
			return String.format(Locale.US, "%s%.2fM", sign, Math.abs(gp) / 1_000_000.0);
		}
		if (abs >= 100_000L)
		{
			return String.format(Locale.US, "%s%.1fK", sign, Math.abs(gp) / 1_000.0);
		}
		return INT_FMT.format(gp);
	}

	public static String formatNumber(long num)
	{
		return INT_FMT.format(num);
	}

	public static JPanel createCardPanel()
	{
		JPanel card = new JPanel();
		card.setBackground(BG_CARD);
		card.setBorder(CARD_BORDER);
		return card;
	}

	public static JTextField createStyledTextField(String initialValue)
	{
		JTextField tf = new JTextField(initialValue);
		tf.setBackground(BG_INPUT);
		tf.setForeground(TEXT_PRIMARY);
		tf.setCaretColor(ACCENT_HERB_BRIGHT);
		tf.setFont(getRegularFont());
		tf.setBorder(INPUT_BORDER);
		return tf;
	}

	public static <T> void styleComboBox(JComboBox<T> combo)
	{
		combo.setBackground(BG_INPUT);
		combo.setForeground(TEXT_PRIMARY);
		combo.setFont(getSmallFont());
		combo.setBorder(BorderFactory.createLineBorder(BORDER_DEFAULT, 1));
	}

	public static JScrollPane createStyledScrollPane(java.awt.Component view)
	{
		java.awt.Component actualView = view;
		if (view instanceof JPanel && !(view instanceof javax.swing.Scrollable))
		{
			actualView = new ScrollableWidthPanel((JPanel) view);
		}
		JScrollPane sp = new JScrollPane(actualView);
		sp.setBackground(BG_MATTE_BLACK);
		sp.getViewport().setBackground(BG_MATTE_BLACK);
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sp.getVerticalScrollBar().setUnitIncrement(16);
		sp.getHorizontalScrollBar().setUnitIncrement(16);
		styleScrollBar(sp.getVerticalScrollBar());
		styleScrollBar(sp.getHorizontalScrollBar());
		return sp;
	}

	/**
	 * Wrapper panel that implements {@link javax.swing.Scrollable} with {@code getScrollableTracksViewportWidth() = true}
	 * so vertical scroll panes never show a horizontal scrollbar and always fit 100% of the panel width.
	 */
	public static class ScrollableWidthPanel extends JPanel implements javax.swing.Scrollable
	{
		public ScrollableWidthPanel(JPanel inner)
		{
			super(new java.awt.BorderLayout());
			setBackground(BG_MATTE_BLACK);
			setOpaque(false);
			add(inner, java.awt.BorderLayout.NORTH);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
		{
			return 64;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

	public static void styleScrollBar(javax.swing.JScrollBar bar)
	{
		if (bar == null) return;
		bar.setPreferredSize(new Dimension(7, 7));
		bar.setBackground(BG_DEEP_CHARCOAL);
		bar.setUI(new BasicScrollBarUI()
		{
			@Override
			protected void configureScrollBarColors()
			{
				this.thumbColor = BORDER_DEFAULT;
				this.trackColor = BG_DEEP_CHARCOAL;
			}

			@Override
			protected JButton createDecreaseButton(int orientation)
			{
				return createZeroButton();
			}

			@Override
			protected JButton createIncreaseButton(int orientation)
			{
				return createZeroButton();
			}

			private JButton createZeroButton()
			{
				JButton b = new JButton();
				b.setPreferredSize(new Dimension(0, 0));
				b.setMinimumSize(new Dimension(0, 0));
				b.setMaximumSize(new Dimension(0, 0));
				return b;
			}
		});
	}

	public static JLabel createSectionHeader(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(getSectionFont());
		lbl.setForeground(ACCENT_HERB_BRIGHT);
		return lbl;
	}

	/**
	 * Recursively ensures that all direct children inside any {@link javax.swing.BoxLayout} container
	 * share {@link java.awt.Component#LEFT_ALIGNMENT} so that mixed 0.0f / 0.5f alignments never
	 * halve child container widths in Swing.
	 */
	public static void normalizeBoxAlignment(java.awt.Container root)
	{
		if (root == null)
		{
			return;
		}
		boolean isBox = root.getLayout() instanceof javax.swing.BoxLayout;
		for (java.awt.Component c : root.getComponents())
		{
			if (isBox && c instanceof javax.swing.JComponent)
			{
				((javax.swing.JComponent) c).setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
			}
			if (c instanceof java.awt.Container)
			{
				normalizeBoxAlignment((java.awt.Container) c);
			}
		}
	}

	/**
	 * Custom matte charcoal button with subtle Herblore green active/hover state.
	 */
	public static class HerbButton extends JButton
	{
		private boolean active = false;
		private boolean hovered = false;
		private final boolean primary;

		public HerbButton(String text)
		{
			this(text, false);
		}

		public HerbButton(String text, boolean primary)
		{
			super(text);
			this.primary = primary;
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setOpaque(false);
			setFont(getBadgeFont());
			setForeground(TEXT_PRIMARY);
			setMargin(new Insets(3, 6, 3, 6));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					hovered = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hovered = false;
					repaint();
				}
			});
		}

		public void setActive(boolean active)
		{
			this.active = active;
			setForeground(active || primary ? TEXT_PRIMARY : TEXT_SECONDARY);
			repaint();
		}

		public boolean isActive()
		{
			return active;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int w = getWidth();
			int h = getHeight();

			if (active || primary)
			{
				Color top = hovered ? new Color(0x1E, 0x80, 0x45) : new Color(0x16, 0x65, 0x34);
				Color bot = hovered ? new Color(0x15, 0x5E, 0x32) : new Color(0x11, 0x42, 0x24);
				g2.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
				g2.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);
				g2.setColor(ACCENT_HERB);
				g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
			}
			else
			{
				g2.setColor(hovered ? BG_CARD_HOVER : BG_CARD);
				g2.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);
				g2.setColor(hovered ? BORDER_DEFAULT : BORDER_SUBTLE);
				g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
			}

			g2.dispose();
			super.paintComponent(g);
		}
	}

	/**
	 * Creates the pixel-crisp 16x16 RuneLite client sidebar toolbar icon (OSRS Potion Vial)
	 * with a transparent background so RuneLite renders it 1:1 without resampling blur.
	 */
	public static BufferedImage createToolbarIcon()
	{
		String[] rows = new String[]{
			"......OOOO......",
			".....OCCccO.....",
			"....OGGGgggO....",
			".....OGgggO.....",
			".....OGgggO.....",
			"....OGHggggO....",
			"...OGHHMMMMgO...",
			"..OGHHMMMMLLO...",
			".OGHHLLLYLLDDO..",
			".OGHLLLYYYLDDO..",
			".OGHLLLLYLIDKO..",
			".OGLLLLLLLDDKO..",
			"..OGLLLLLDDKO...",
			"...OGDDDDKKO....",
			"....OOOOOOO.....",
			"................"
		};
		return renderPixelSprite(rows);
	}

	/**
	 * Creates the pixel-crisp 24x24 header & window icon (OSRS Herblore Potion Vial).
	 */
	public static BufferedImage createHeaderIcon()
	{
		String[] rows = new String[]{
			".........OOOOOO.........",
			"........OCCCCccO........",
			"........OCCccccO........",
			".......OGGGGGgggO.......",
			"........OGGggggO........",
			"........OGGggggO........",
			".......OGHHgggggO.......",
			"......OGHHgggggggO......",
			".....OGHHMMMMMMMggO.....",
			"....OGHHMMMMMMMLLggO....",
			"...OGHHHLLLLLLLLLDDO....",
			"..OGHHHLLLLYYLLLLDDKO...",
			"..OGHHLLLLYYYYLLLDDKO...",
			"..OGHHLLLYYYYYYLLDDKO...",
			"..OGHHLLLLYYYYLLLDKKO...",
			"..OGHLLLLLLYYLLLDDKKO...",
			"..OGHLLLLLLLLLLDDDKKO...",
			"...OGHLLLLLLLLDDDKKO....",
			"...OGGLLLLLLDDDDKKKO....",
			"....OGGDDDDDDDKKKKO.....",
			".....OGGDDDKKKKKKO......",
			"......OOGDKKKKKOO.......",
			"........OOOOOOO.........",
			"........................"
		};
		return renderPixelSprite(rows);
	}

	/**
	 * Creates the 48x72 PNG icon required by the RuneLite Plugin Hub repository root (icon.png).
	 * Scales the 24x24 pixel-art OSRS Herblore Potion Vial by 2x (48x48) centered vertically in 48x72.
	 */
	public static BufferedImage createPluginHubIcon()
	{
		BufferedImage src24 = createHeaderIcon();
		BufferedImage hubImg = new BufferedImage(48, 72, BufferedImage.TYPE_INT_ARGB);
		int offsetY = (72 - 48) / 2;
		for (int y = 0; y < 24; y++)
		{
			for (int x = 0; x < 24; x++)
			{
				int argb = src24.getRGB(x, y);
				if (argb != 0)
				{
					int dx = x * 2;
					int dy = offsetY + (y * 2);
					hubImg.setRGB(dx, dy, argb);
					hubImg.setRGB(dx + 1, dy, argb);
					hubImg.setRGB(dx, dy + 1, argb);
					hubImg.setRGB(dx + 1, dy + 1, argb);
				}
			}
		}
		return hubImg;
	}

	private static BufferedImage renderPixelSprite(String[] rows)
	{
		int h = rows.length;
		int w = rows[0].length();
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++)
		{
			String row = rows[y];
			for (int x = 0; x < w && x < row.length(); x++)
			{
				char ch = row.charAt(x);
				int argb = mapSpriteCharToArgb(ch);
				if (argb != 0)
				{
					img.setRGB(x, y, argb);
				}
			}
		}
		return img;
	}

	private static int mapSpriteCharToArgb(char ch)
	{
		switch (ch)
		{
			case 'O': return 0xFF0B0F14; // Crisp 1px dark outline
			case 'C': return 0xFFE0A94E; // Cork stopper highlight
			case 'c': return 0xFFA36E22; // Cork stopper shadow
			case 'G': return 0xFFE2E8F0; // Glass rim highlight
			case 'g': return 0xFF94A3B8; // Glass neck midtone
			case 'H': return 0xFFECFDF5; // Specular white-mint reflection
			case 'M': return 0xFF4ADE80; // Bright glowing emerald meniscus
			case 'L': return 0xFF22C55E; // Vibrant Herblore emerald liquid
			case 'I':
			case 'D': return 0xFF15803D; // Deep emerald shading
			case 'K': return 0xFF14532D; // Darkest emerald base shadow
			case 'Y': return 0xFFFBBF24; // Golden OSRS emblem accent
			default:  return 0x00000000; // Transparent
		}
	}
}
