package com.ezscurrius;

import net.runelite.api.JagexColor;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

class DebrisRecolour
{
	// Third face colour values with special meanings
	private static final int FLAT_SHADED = -1;
	private static final int HIDDEN = -2;

	static int toHsl(Color colour)
	{
		return JagexColor.rgbToHSL(colour.getRGB(), 1.0) & 0xffff;
	}

	// Parses "123, 456" into colours; anything that isn't a number is skipped
	static Set<Integer> parseColours(String text)
	{
		Set<Integer> colours = new HashSet<>();
		for (String part : text.split(","))
		{
			try
			{
				colours.add(Integer.parseInt(part.trim()));
			}
			catch (NumberFormatException ignored)
			{
			}
		}
		return colours;
	}

	// Recolours every visible face, or only those whose original colour is in onlyColours when it isn't empty
	static void recolourFaces(int[] colours1, int[] colours2, int[] colours3, short[] unlitColours,
		Set<Integer> onlyColours, int hsl)
	{
		for (int face = 0; face < colours1.length; face++)
		{
			if (colours3[face] == HIDDEN
				|| !onlyColours.isEmpty() && (unlitColours == null || !onlyColours.contains(unlitColours[face] & 0xffff)))
			{
				continue;
			}
			colours1[face] = hsl;
			if (colours3[face] != FLAT_SHADED)
			{
				colours2[face] = hsl;
				colours3[face] = hsl;
			}
		}
	}
}
