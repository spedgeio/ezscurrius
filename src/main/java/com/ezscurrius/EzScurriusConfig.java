package com.ezscurrius;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

import java.awt.Color;

@ConfigGroup("ezscurrius")
public interface EzScurriusConfig extends Config
{
	@ConfigItem(
		keyName = "recolourDebris",
		name = "Recolour Debris",
		description = "Recolour the falling debris so its shadow is easier to see.",
		position = 1
	)
	default boolean recolourDebris()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debrisColour",
		name = "Debris Colour",
		description = "Colour of the falling debris.",
		position = 2
	)
	default Color debrisColour()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(
		keyName = "onlyColours",
		name = "Only Original Colours",
		description = "Comma-separated original face colours to recolour, as shown by debug messages. Leave blank to recolour every face.",
		position = 3
	)
	default String onlyColours()
	{
		return "";
	}

	@ConfigItem(
		keyName = "choeseSound",
		name = "Choese Sound",
		description = "Play a sound when the food pile is ready to eat again.",
		position = 4
	)
	default boolean choeseSound()
	{
		return true;
	}

	@Range(max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "choeseVolume",
		name = "Choese Volume",
		description = "Volume of the choese sound, when it's turned on.",
		position = 5
	)
	default int choeseVolume()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "debug",
		name = "Debug Messages",
		description = "Describe new graphics and the debris model in chat while Scurrius is nearby.",
		position = 6
	)
	default boolean debug()
	{
		return false;
	}
}
