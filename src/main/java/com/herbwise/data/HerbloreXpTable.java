package com.herbwise.data;

/**
 * Official Old School RuneScape experience table and level progression helper methods.
 * Uses the exact OSRS experience curve formula without approximation.
 */
public final class HerbloreXpTable
{
	public static final int MIN_LEVEL = 1;
	public static final int MAX_SKILL_LEVEL = 99;
	public static final int MAX_VIRTUAL_LEVEL = 126;
	public static final long MAX_XP = 200_000_000L;

	public static final int[] LEVEL_PRESETS = new int[]{70, 75, 80, 85, 90, 95, 99};

	/**
	 * Exact OSRS XP required for levels 1..99 (index 0 = level 1, index 98 = level 99).
	 */
	private static final long[] XP_FOR_LEVEL = new long[MAX_VIRTUAL_LEVEL + 1];

	static
	{
		XP_FOR_LEVEL[0] = 0L;
		XP_FOR_LEVEL[1] = 0L;
		long points = 0L;
		for (int lvl = 1; lvl < MAX_VIRTUAL_LEVEL; lvl++)
		{
			points += (long) Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
			XP_FOR_LEVEL[lvl + 1] = points / 4L;
		}
	}

	private HerbloreXpTable()
	{
	}

	/**
	 * Returns the exact minimum XP required to reach the specified level (1..126).
	 */
	public static long getXpForLevel(int level)
	{
		if (level <= 1)
		{
			return 0L;
		}
		if (level > MAX_VIRTUAL_LEVEL)
		{
			return XP_FOR_LEVEL[MAX_VIRTUAL_LEVEL];
		}
		return XP_FOR_LEVEL[level];
	}

	/**
	 * Returns the player's standard OSRS skill level (1..99) for a given XP amount.
	 */
	public static int getLevelForXp(long xp)
	{
		if (xp <= 0L)
		{
			return 1;
		}
		for (int level = MAX_SKILL_LEVEL; level >= 1; level--)
		{
			if (xp >= XP_FOR_LEVEL[level])
			{
				return level;
			}
		}
		return 1;
	}

	/**
	 * Returns the virtual level (1..126) for a given XP amount.
	 */
	public static int getVirtualLevelForXp(long xp)
	{
		if (xp <= 0L)
		{
			return 1;
		}
		for (int level = MAX_VIRTUAL_LEVEL; level >= 1; level--)
		{
			if (xp >= XP_FOR_LEVEL[level])
			{
				return level;
			}
		}
		return 1;
	}

	/**
	 * Calculates exact XP needed to reach {@code targetLevel} from {@code currentXp}.
	 * Returns 0 if {@code currentXp} already meets or exceeds {@code targetLevel}.
	 */
	public static long getXpRemainingToLevel(long currentXp, int targetLevel)
	{
		long safeCurrent = Math.max(0L, currentXp);
		long targetXp = getXpForLevel(targetLevel);
		return Math.max(0L, targetXp - safeCurrent);
	}

	/**
	 * Calculates exact XP remaining to reach the next Herblore level (capped at 99).
	 */
	public static long getXpRemainingToNextLevel(long currentXp)
	{
		int currentLevel = getLevelForXp(currentXp);
		if (currentLevel >= MAX_SKILL_LEVEL)
		{
			return 0L;
		}
		return getXpRemainingToLevel(currentXp, currentLevel + 1);
	}

	/**
	 * Calculates the percentage progress (0.0 to 100.0) between the player's current level and next level.
	 */
	public static double getProgressToNextLevelPercent(long currentXp)
	{
		int currentLevel = getLevelForXp(currentXp);
		if (currentLevel >= MAX_SKILL_LEVEL)
		{
			return 100.0;
		}
		long startXp = getXpForLevel(currentLevel);
		long endXp = getXpForLevel(currentLevel + 1);
		long span = endXp - startXp;
		if (span <= 0L)
		{
			return 100.0;
		}
		double progress = ((double) (currentXp - startXp) / (double) span) * 100.0;
		return Math.max(0.0, Math.min(100.0, progress));
	}
}
