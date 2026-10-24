package com.ezscurrius;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@PluginDescriptor(
	name = "EzScurrius",
	description = "Recolours the shadows of Scurrius' falling debris to make them easier to see, and plays a sound when the food pile is ready to eat",
	tags = {"scurrius", "rat", "boss", "pvm", "debris", "colour", "color", "accessibility", "colorblind", "food", "pile", "cheese", "sound"}
)
@Slf4j
public class EzScurriusPlugin extends Plugin
{
	private static final int UNKNOWN_DELAY = -1;

	@Inject
	private Client client;

	@Inject
	private EzScurriusConfig config;

	@Inject
	private AudioPlayer audioPlayer;

	// Original colour arrays, restored once no debris is left
	private final Map<int[], int[]> originalColours = new IdentityHashMap<>();
	private final Set<GraphicsObject> describedDebris = Collections.newSetFromMap(new IdentityHashMap<>());
	private Set<Integer> onlyColours = Collections.emptySet();
	// Food pile cooldown, unknown until the game sends it after logging in
	private int foodPileDelay = UNKNOWN_DELAY;

	@Override
	protected void startUp()
	{
		onlyColours = DebrisRecolour.parseColours(config.onlyColours());
	}

	@Override
	protected void shutDown()
	{
		restoreColours();
		describedDebris.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("ezscurrius"))
		{
			restoreColours();
			onlyColours = DebrisRecolour.parseColours(config.onlyColours());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			foodPileDelay = UNKNOWN_DELAY;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() != VarbitID.RAT_BOSS_FOOD_PILE_PLAYER_EAT_DELAY)
		{
			return;
		}
		if (config.choeseSound() && config.choeseVolume() > 0 && isFoodPileReady(foodPileDelay, event.getValue()))
		{
			playChoese();
		}
		foodPileDelay = event.getValue();
	}

	// Ready once a running cooldown reaches zero
	static boolean isFoodPileReady(int previousDelay, int delay)
	{
		return previousDelay > 0 && delay == 0;
	}

	// Converts a volume percentage to decibels of gain; 100% plays the sound as it is
	static float gain(int volumePercent)
	{
		return (float) (20 * Math.log10(volumePercent / 100.0));
	}

	private void playChoese()
	{
		try
		{
			audioPlayer.play(EzScurriusPlugin.class, "choese.wav", gain(config.choeseVolume()));
		}
		catch (IOException | UnsupportedAudioFileException | LineUnavailableException e)
		{
			log.warn("Unable to play choese sound", e);
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (config.debug() && isScurriusNearby())
		{
			GraphicsObject graphic = event.getGraphicsObject();
			LocalPoint location = graphic.getLocation();
			debug("Graphic " + graphic.getId() + " at scene " + location.getSceneX() + ", " + location.getSceneY());
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		List<GraphicsObject> debris = new ArrayList<>();
		for (GraphicsObject graphic : client.getTopLevelWorldView().getGraphicsObjects())
		{
			if (graphic.getId() == SpotanimID.VFX_RAT_BOSS_FALLING_DEBRIS_01)
			{
				debris.add(graphic);
			}
		}
		if (debris.isEmpty())
		{
			restoreColours();
			describedDebris.clear();
			return;
		}

		int hsl = DebrisRecolour.toHsl(config.debrisColour());
		for (GraphicsObject graphic : debris)
		{
			Model model = graphic.getModel();
			if (model == null)
			{
				continue;
			}
			if (config.debug() && describedDebris.add(graphic))
			{
				describe(graphic, model);
			}
			if (config.recolourDebris())
			{
				remember(model.getFaceColors1());
				remember(model.getFaceColors2());
				remember(model.getFaceColors3());
				DebrisRecolour.recolourFaces(model.getFaceColors1(), model.getFaceColors2(), model.getFaceColors3(),
					model.getUnlitFaceColors(), onlyColours, hsl);
			}
		}
	}

	private void remember(int[] colours)
	{
		originalColours.computeIfAbsent(colours, int[]::clone);
	}

	private void restoreColours()
	{
		originalColours.forEach((colours, original) -> System.arraycopy(original, 0, colours, 0, colours.length));
		originalColours.clear();
	}

	// Reports the model's original colours and whether the same model is reused between calls
	private void describe(GraphicsObject graphic, Model model)
	{
		Map<Integer, Integer> colourCounts = new TreeMap<>();
		short[] unlit = model.getUnlitFaceColors();
		for (int face = 0; unlit != null && face < unlit.length; face++)
		{
			colourCounts.merge(unlit[face] & 0xffff, 1, Integer::sum);
		}
		Map<Integer, Integer> transparencyCounts = new TreeMap<>();
		byte[] transparencies = model.getFaceTransparencies();
		for (int face = 0; transparencies != null && face < transparencies.length; face++)
		{
			transparencyCounts.merge(transparencies[face] & 0xff, 1, Integer::sum);
		}
		debug("Debris " + graphic.getId() + ": " + model.getFaceCount() + " faces, reused "
			+ (graphic.getModel() == model) + ", colours " + colourCounts + ", transparencies " + transparencyCounts);
	}

	private boolean isScurriusNearby()
	{
		WorldView worldView = client.getTopLevelWorldView();
		return worldView.npcs().stream()
			.anyMatch(npc -> npc.getId() == NpcID.RAT_BOSS_NORMAL || npc.getId() == NpcID.RAT_BOSS_INSTANCE);
	}

	private void debug(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[EzScurrius] " + message, null);
	}

	@Provides
	EzScurriusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EzScurriusConfig.class);
	}
}
