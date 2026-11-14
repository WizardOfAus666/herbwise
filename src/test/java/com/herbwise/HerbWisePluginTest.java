package com.herbwise;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Developer launcher that boots the official RuneLite client with {@link HerbWisePlugin} loaded.
 */
public class HerbWisePluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HerbWisePlugin.class);
		RuneLite.main(args);
	}
}
