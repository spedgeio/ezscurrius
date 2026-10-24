package com.ezscurrius;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioSystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FoodPileTest
{
	@Test
	public void readyWhenCooldownReachesZero()
	{
		assertTrue(EzScurriusPlugin.isFoodPileReady(1, 0));
		assertTrue(EzScurriusPlugin.isFoodPileReady(6, 0));
	}

	@Test
	public void notReadyWhileCountingDown()
	{
		assertFalse(EzScurriusPlugin.isFoodPileReady(6, 5));
	}

	@Test
	public void notReadyWhenCooldownIsFirstSeenAfterLogin()
	{
		assertFalse(EzScurriusPlugin.isFoodPileReady(-1, 0));
		assertFalse(EzScurriusPlugin.isFoodPileReady(0, 0));
	}

	@Test
	public void fullVolumeHasNoGain()
	{
		assertEquals(0f, EzScurriusPlugin.gain(100), 0.001f);
	}

	@Test
	public void halfVolumeIsAboutSixDecibelsQuieter()
	{
		assertEquals(-6.02f, EzScurriusPlugin.gain(50), 0.01f);
	}

	@Test
	public void onePercentIsFortyDecibelsQuieter()
	{
		assertEquals(-40f, EzScurriusPlugin.gain(1), 0.001f);
	}

	@Test
	public void choeseSoundIsPlayable() throws Exception
	{
		try (InputStream sound = EzScurriusPlugin.class.getResourceAsStream("choese.wav"))
		{
			assertNotNull(sound);
			assertNotNull(AudioSystem.getAudioInputStream(new BufferedInputStream(sound)));
		}
	}
}
