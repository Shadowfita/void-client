package net.runelite.client.plugins.objectindicators;

import com.google.inject.Provides;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.GameEventBridgeHooks;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Object Indicators",
	description = "Highlights selected scene objects by name or ID.",
	tags = {"object", "marker", "highlight", "bank", "indicator"},
	enabledByDefault = true,
	loadWhenOutdated = true
)
public class ObjectIndicatorsPlugin extends Plugin
{
	@Inject private OverlayManager overlayManager;
	@Inject private ObjectIndicatorsOverlay overlay;
	private final Map<String, GameEventBridgeHooks.TileObjectInfo> objects = new LinkedHashMap<>();

	@Provides
	ObjectIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ObjectIndicatorsConfig.class);
	}

	@Override
	protected void startUp()
	{
		objects.clear();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		objects.clear();
	}

	Collection<GameEventBridgeHooks.TileObjectInfo> getObjects()
	{
		return objects.values();
	}

	@Subscribe public void onGameObjectSpawned(GameObjectSpawned e) { add(e.getGameObject()); }
	@Subscribe public void onGroundObjectSpawned(GroundObjectSpawned e) { add(e.getGroundObject()); }
	@Subscribe public void onWallObjectSpawned(WallObjectSpawned e) { add(e.getWallObject()); }
	@Subscribe public void onDecorativeObjectSpawned(DecorativeObjectSpawned e) { add(e.getDecorativeObject()); }
	@Subscribe public void onGameObjectDespawned(GameObjectDespawned e) { remove(e.getGameObject()); }
	@Subscribe public void onGroundObjectDespawned(GroundObjectDespawned e) { remove(e.getGroundObject()); }
	@Subscribe public void onWallObjectDespawned(WallObjectDespawned e) { remove(e.getWallObject()); }
	@Subscribe public void onDecorativeObjectDespawned(DecorativeObjectDespawned e) { remove(e.getDecorativeObject()); }

	private void add(Object value)
	{
		if (value instanceof GameEventBridgeHooks.TileObjectInfo)
		{
			GameEventBridgeHooks.TileObjectInfo object = (GameEventBridgeHooks.TileObjectInfo) value;
			objects.put(key(object), object);
		}
	}

	private void remove(Object value)
	{
		if (value instanceof GameEventBridgeHooks.TileObjectInfo)
		{
			objects.remove(key((GameEventBridgeHooks.TileObjectInfo) value));
		}
	}

	private static String key(GameEventBridgeHooks.TileObjectInfo object)
	{
		return object.getPlane() + ":" + object.getLocalX() + ":" + object.getLocalY() + ":" + object.getLayer() + ":" + object.getId();
	}
}
