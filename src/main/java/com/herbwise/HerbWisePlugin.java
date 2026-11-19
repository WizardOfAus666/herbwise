package com.herbwise;

import com.google.inject.Provides;
import com.herbwise.services.HerbloreDataService;
import com.herbwise.services.PlayerSkillService;
import com.herbwise.ui.HerbWiseTheme;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main RuneLite plugin entry point for HerbWise — OSRS Herblore Profit & Training Assistant.
 */
@PluginDescriptor(
	name = "HerbWise",
	description = "OSRS Herblore Profit & Training Assistant — real-time GE potion profit, XP/hr, budget & recipe calculator",
	tags = {
		"herblore",
		"potion",
		"profit",
		"calculator",
		"grand exchange",
		"training",
		"herbwise",
		"skilling"
	}
)
public class HerbWisePlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(HerbWisePlugin.class);

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private HerbWiseConfig config;

	@Inject
	private HerbloreDataService dataService;

	@Inject
	private PlayerSkillService playerSkillService;

	private HerbWisePanel panel;
	private NavigationButton navButton;

	@Provides
	HerbWiseConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HerbWiseConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("[HerbWise] Starting OSRS Herblore Profit & Training Assistant...");
		dataService.syncSettingsFromConfig();
		dataService.startUp();

		panel = new HerbWisePanel(dataService);
		BufferedImage icon = HerbWiseTheme.createToolbarIcon();

		navButton = NavigationButton.builder()
			.tooltip("HerbWise — Herblore Profit & Training Assistant")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();

		if (clientToolbar != null)
		{
			clientToolbar.addNavigation(navButton);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("[HerbWise] Shutting down HerbWise plugin...");
		if (clientToolbar != null && navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		if (dataService != null)
		{
			dataService.shutDown();
		}
		panel = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (playerSkillService != null)
		{
			playerSkillService.refreshFromClient();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event != null && event.getSkill() != net.runelite.api.Skill.HERBLORE)
		{
			return;
		}
		if (playerSkillService != null)
		{
			playerSkillService.refreshFromClient();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (playerSkillService != null)
		{
			playerSkillService.refreshFromClient();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event == null || !HerbWiseConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}
		if (dataService != null)
		{
			dataService.onExternalConfigChanged();
		}
	}

	public HerbWisePanel getPanel()
	{
		return panel;
	}

	public HerbloreDataService getDataService()
	{
		return dataService;
	}
}
