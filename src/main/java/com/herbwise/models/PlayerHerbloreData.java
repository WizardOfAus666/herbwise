package com.herbwise.models;

/**
 * Snapshot of the player's current Herblore skill state retrieved from RuneLite's Client API
 * (or manual override when outside an active game session).
 */
public class PlayerHerbloreData
{
	private final boolean loggedIn;
	private final String playerName;
	private final int currentLevel;
	private final int boostedLevel;
	private final long currentXp;
	private final int nextLevel;
	private final long xpForNextLevel;
	private final long xpRemainingToNextLevel;
	private final double levelProgressPercent;
	private final int unlockablePotionCount;
	private final int totalVerifiedPotionCount;
	private final boolean manualOverride;
	private final int appliedBoostLevels;
	private final String boostSourceDescription;

	public PlayerHerbloreData(
		boolean loggedIn,
		String playerName,
		int currentLevel,
		int boostedLevel,
		long currentXp,
		int nextLevel,
		long xpForNextLevel,
		long xpRemainingToNextLevel,
		double levelProgressPercent,
		int unlockablePotionCount,
		int totalVerifiedPotionCount,
		boolean manualOverride,
		int appliedBoostLevels,
		String boostSourceDescription)
	{
		this.loggedIn = loggedIn;
		this.playerName = playerName != null ? playerName : "Adventurer";
		this.currentLevel = Math.max(1, Math.min(99, currentLevel));
		this.boostedLevel = Math.max(1, boostedLevel);
		this.currentXp = Math.max(0L, currentXp);
		this.nextLevel = Math.max(1, Math.min(99, nextLevel));
		this.xpForNextLevel = Math.max(0L, xpForNextLevel);
		this.xpRemainingToNextLevel = Math.max(0L, xpRemainingToNextLevel);
		this.levelProgressPercent = Math.max(0.0, Math.min(100.0, levelProgressPercent));
		this.unlockablePotionCount = Math.max(0, unlockablePotionCount);
		this.totalVerifiedPotionCount = Math.max(0, totalVerifiedPotionCount);
		this.manualOverride = manualOverride;
		this.appliedBoostLevels = Math.max(0, appliedBoostLevels);
		this.boostSourceDescription = boostSourceDescription != null ? boostSourceDescription : "None";
	}

	public boolean isLoggedIn() { return loggedIn; }
	public String getPlayerName() { return playerName; }
	public int getCurrentLevel() { return currentLevel; }
	public int getBoostedLevel() { return boostedLevel; }
	public long getCurrentXp() { return currentXp; }
	public int getNextLevel() { return nextLevel; }
	public long getXpForNextLevel() { return xpForNextLevel; }
	public long getXpRemainingToNextLevel() { return xpRemainingToNextLevel; }
	public double getLevelProgressPercent() { return levelProgressPercent; }
	public int getUnlockablePotionCount() { return unlockablePotionCount; }
	public int getTotalVerifiedPotionCount() { return totalVerifiedPotionCount; }
	public boolean isManualOverride() { return manualOverride; }
	public int getAppliedBoostLevels() { return appliedBoostLevels; }
	public String getBoostSourceDescription() { return boostSourceDescription; }

	/**
	 * Returns the effective Herblore level including any supported boost (e.g. Botanical pie +4).
	 */
	public int getEffectiveHerbloreLevel()
	{
		return Math.max(currentLevel + appliedBoostLevels, boostedLevel);
	}
}
