package com.ezscurrius;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class EzScurriusPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EzScurriusPlugin.class);
		RuneLite.main(args);
	}
}
