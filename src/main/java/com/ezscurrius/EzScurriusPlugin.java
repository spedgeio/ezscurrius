package com.ezscurrius;

import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Model;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@PluginDescriptor(
	name = "EzScurrius",
	description = "Recolours the shadows of Scurrius' falling debris to make them easier to see",
	tags = {"scurrius", "rat", "boss", "pvm", "debris", "colour", "color", "accessibility", "colorblind"}
)
public class EzScurriusPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private EzScurriusConfig config;

	// Original colour arrays, restored once no debris is left
	private final Map<int[], int[]> originalColours = new IdentityHashMap<>();
	private final Set<GraphicsObject> describedDebris = Collections.newSetFromMap(new IdentityHashMap<>());
	private Set<Integer> onlyColours = Collections.emptySet();

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
