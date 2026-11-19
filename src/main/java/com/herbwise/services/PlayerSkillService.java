package com.herbwise.services;

import com.herbwise.HerbWiseConfig.HerbloreBoostMode;
import com.herbwise.data.HerbloreXpTable;
import com.herbwise.models.PlayerHerbloreData;
import com.herbwise.models.PotionRecipe;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.client.util.Text;

/**
 * Integrates with RuneLite's {@link Client} skill API to automatically retrieve the player's
 * current Herblore level, boosted level, experience, and XP remaining to the next level.
 * Supports manual level/XP inspection only when used outside an active game session.
 */
@Singleton
public class PlayerSkillService
{
	private final Client client;
	private final WikiRecipeService wikiRecipeService;
	private final List<Consumer<PlayerHerbloreData>> listeners = new CopyOnWriteArrayList<>();

	private volatile int fallbackManualLevel = 75;
	private volatile long fallbackManualXp = HerbloreXpTable.getXpForLevel(75);
	private volatile HerbloreBoostMode boostMode = HerbloreBoostMode.NONE;
	private volatile PlayerHerbloreData currentSnapshot;

	@Inject
	public PlayerSkillService(Client client, WikiRecipeService wikiRecipeService)
	{
		this.client = client;
		this.wikiRecipeService = wikiRecipeService != null ? wikiRecipeService : new WikiRecipeService();
		this.currentSnapshot = buildSnapshot(false, "Offline Player", fallbackManualLevel, fallbackManualLevel, fallbackManualXp, true);
	}

	public PlayerSkillService()
	{
		this(null, new WikiRecipeService());
	}

	/**
	 * Reads the player's live Herblore skill data from the RuneLite {@link Client}.
	 * Must be called on the client thread when logged in.
	 */
	public synchronized PlayerHerbloreData refreshFromClient()
	{
		PlayerHerbloreData previous = this.currentSnapshot;
		if (client != null && client.getGameState() == GameState.LOGGED_IN)
		{
			try
			{
				int realLevel = Math.max(1, client.getRealSkillLevel(Skill.HERBLORE));
				int boostedLevel = Math.max(1, client.getBoostedSkillLevel(Skill.HERBLORE));
				long xp = Math.max(0L, client.getSkillExperience(Skill.HERBLORE));
				if (xp == 0L && realLevel > 1)
				{
					xp = HerbloreXpTable.getXpForLevel(realLevel);
				}

				String playerName = "Logged-In Player";
				Player local = client.getLocalPlayer();
				if (local != null && local.getName() != null && !local.getName().trim().isEmpty())
				{
					playerName = Text.sanitize(local.getName());
				}

				PlayerHerbloreData next = buildSnapshot(true, playerName, realLevel, boostedLevel, xp, false);
				currentSnapshot = next;
				if (hasMeaningfulChange(previous, next))
				{
					notifyListeners(next);
				}
				return currentSnapshot;
			}
			catch (Throwable ignored)
			{
				// Fallback if client state is transitioning
			}
		}

		PlayerHerbloreData next = buildSnapshot(
			false,
			"Preview / Offline Session",
			fallbackManualLevel,
			fallbackManualLevel,
			fallbackManualXp,
			true
		);
		currentSnapshot = next;
		if (hasMeaningfulChange(previous, next))
		{
			notifyListeners(next);
		}
		return currentSnapshot;
	}

	private static boolean hasMeaningfulChange(PlayerHerbloreData prev, PlayerHerbloreData next)
	{
		if (prev == null || next == null)
		{
			return true;
		}
		return prev.isLoggedIn() != next.isLoggedIn()
			|| prev.getCurrentLevel() != next.getCurrentLevel()
			|| prev.getBoostedLevel() != next.getBoostedLevel()
			|| prev.getCurrentXp() != next.getCurrentXp()
			|| prev.getAppliedBoostLevels() != next.getAppliedBoostLevels()
			|| prev.isManualOverride() != next.isManualOverride();
	}

	/**
	 * Updates the offline/preview manual Herblore level (used only when not logged into an active client session).
	 */
	public synchronized PlayerHerbloreData setOfflineManualLevel(int level)
	{
		int safeLevel = Math.max(1, Math.min(99, level));
		this.fallbackManualLevel = safeLevel;
		this.fallbackManualXp = HerbloreXpTable.getXpForLevel(safeLevel);
		return refreshFromClient();
	}

	/**
	 * Updates the offline/preview manual Herblore XP (used only when not logged into an active client session).
	 */
	public synchronized PlayerHerbloreData setOfflineManualXp(long xp)
	{
		long safeXp = Math.max(0L, Math.min(HerbloreXpTable.MAX_XP, xp));
		this.fallbackManualXp = safeXp;
		this.fallbackManualLevel = HerbloreXpTable.getLevelForXp(safeXp);
		return refreshFromClient();
	}

	public synchronized void setBoostMode(HerbloreBoostMode mode)
	{
		this.boostMode = mode != null ? mode : HerbloreBoostMode.NONE;
		refreshFromClient();
	}

	public HerbloreBoostMode getBoostMode()
	{
		return boostMode;
	}

	public PlayerHerbloreData getCurrentHerbloreData()
	{
		return currentSnapshot;
	}

	public void addListener(Consumer<PlayerHerbloreData> listener)
	{
		if (listener != null)
		{
			listeners.add(listener);
		}
	}

	private PlayerHerbloreData buildSnapshot(
		boolean loggedIn,
		String playerName,
		int realLevel,
		int clientBoostedLevel,
		long currentXp,
		boolean manualOverride)
	{
		int safeLevel = Math.max(1, Math.min(99, realLevel));
		int nextLevel = Math.min(99, safeLevel + 1);
		long xpForNext = HerbloreXpTable.getXpForLevel(nextLevel);
		long xpRemaining = HerbloreXpTable.getXpRemainingToNextLevel(currentXp);
		double progressPct = HerbloreXpTable.getProgressToNextLevelPercent(currentXp);

		int boostLevels = boostMode != null ? boostMode.getBoostLevels() : 0;
		int effectiveLevel = Math.max(safeLevel + boostLevels, clientBoostedLevel);

		List<PotionRecipe> primaryCompleted = wikiRecipeService.getVerifiedCompletedRecipes(false);
		int unlockable = 0;
		for (PotionRecipe r : primaryCompleted)
		{
			if (r.getRequiredLevel() <= effectiveLevel)
			{
				unlockable++;
			}
		}

		return new PlayerHerbloreData(
			loggedIn,
			playerName,
			safeLevel,
			clientBoostedLevel,
			currentXp,
			nextLevel,
			xpForNext,
			xpRemaining,
			progressPct,
			unlockable,
			primaryCompleted.size(),
			manualOverride,
			boostLevels,
			boostMode != null ? boostMode.toString() : "No Boost (+0)"
		);
	}

	private void notifyListeners(PlayerHerbloreData data)
	{
		for (Consumer<PlayerHerbloreData> listener : listeners)
		{
			try
			{
				listener.accept(data);
			}
			catch (Exception ignored)
			{
			}
		}
	}
}
