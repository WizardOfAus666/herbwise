package com.herbwise;

import com.herbwise.services.HerbloreDataService;
import com.herbwise.services.PlayerSkillService;
import com.herbwise.services.PriceCacheService;
import com.herbwise.services.WikiPriceService;
import com.herbwise.services.WikiRecipeService;
import com.herbwise.ui.HerbWiseTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Standalone Swing preview launcher for inspecting the HerbWise sidebar UI outside a full RuneLite session.
 */
public class HerbWisePreviewLauncher
{
	public static void main(String[] args)
	{
		boolean smokeTest = false;
		if (args != null)
		{
			for (String a : args)
			{
				if ("--smoke-test".equalsIgnoreCase(a))
				{
					smokeTest = true;
				}
			}
		}
		final boolean autoExit = smokeTest;

		SwingUtilities.invokeLater(() -> {
			PriceCacheService cacheService = new PriceCacheService();
			WikiPriceService priceService = new WikiPriceService();
			WikiRecipeService recipeService = new WikiRecipeService();
			PlayerSkillService skillService = new PlayerSkillService();

			HerbloreDataService dataService = new HerbloreDataService(
				priceService,
				recipeService,
				cacheService,
				skillService,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
			);
			dataService.startUp();

			HerbWisePanel panel = new HerbWisePanel(dataService);

			JFrame frame = new JFrame("HerbWise — OSRS Herblore Profit & Training Assistant");
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setIconImage(HerbWiseTheme.createHeaderIcon());
			frame.getContentPane().setBackground(HerbWiseTheme.BG_MATTE_BLACK);
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(panel, BorderLayout.CENTER);
			frame.setSize(new Dimension(600, 860));
			frame.setMinimumSize(new Dimension(480, 620));
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

			if (autoExit)
			{
				javax.swing.Timer t = new javax.swing.Timer(800, e -> {
					frame.dispose();
					dataService.shutDown();
					System.exit(0);
				});
				t.setRepeats(false);
				t.start();
			}
		});
	}
}
