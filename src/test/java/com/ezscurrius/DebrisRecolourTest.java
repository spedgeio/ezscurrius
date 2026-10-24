package com.ezscurrius;

import org.junit.Test;

import java.awt.Color;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DebrisRecolourTest
{
	private static final int NEW = 999;

	@Test
	public void recoloursEveryVisibleFaceWhenNoColoursAreGiven()
	{
		int[] colours1 = {1, 2, 3};
		int[] colours2 = {1, 2, 3};
		int[] colours3 = {1, -1, -2};

		DebrisRecolour.recolourFaces(colours1, colours2, colours3, new short[]{10, 20, 30}, Collections.emptySet(), NEW);

		assertArrayEquals(new int[]{NEW, NEW, 3}, colours1);
		assertArrayEquals(new int[]{NEW, 2, 3}, colours2);
		assertArrayEquals(new int[]{NEW, -1, -2}, colours3);
	}

	@Test
	public void recoloursOnlyFacesWithTheGivenOriginalColours()
	{
		int[] colours1 = {1, 2};
		int[] colours2 = {1, 2};
		int[] colours3 = {1, 2};

		DebrisRecolour.recolourFaces(colours1, colours2, colours3, new short[]{10, 20}, Set.of(20), NEW);

		assertArrayEquals(new int[]{1, NEW}, colours1);
		assertArrayEquals(new int[]{1, NEW}, colours3);
	}

	@Test
	public void originalColoursAboveShortRangeStillMatch()
	{
		int[] colours1 = {1};

		DebrisRecolour.recolourFaces(colours1, new int[]{1}, new int[]{1}, new short[]{(short) 40000}, Set.of(40000), NEW);

		assertEquals(NEW, colours1[0]);
	}

	@Test
	public void parsesCommaSeparatedColoursAndSkipsJunk()
	{
		assertEquals(Set.of(123, 456), DebrisRecolour.parseColours(" 123, abc,456 ,"));
		assertTrue(DebrisRecolour.parseColours("").isEmpty());
	}

	@Test
	public void hslIsUnsigned()
	{
		assertTrue(DebrisRecolour.toHsl(Color.MAGENTA) >= 0);
	}
}
