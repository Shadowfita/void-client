package net.runelite.client.plugins.entityhider;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Entity Hider",
	description = "Hides selected rendered entity categories and can reduce visual clutter.",
	tags = {"hide", "players", "npcs", "projectiles", "performance"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class EntityHiderPlugin extends Plugin
{
	@Inject private EntityHiderConfig config;
	@Provides EntityHiderConfig provideConfig(ConfigManager manager) { return manager.getConfig(EntityHiderConfig.class); }
	@Override protected void startUp() { updateState(); }
	@Override protected void shutDown() { EntityHiderState.reset(); }

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("entityhider".equals(event.getGroup()))
		{
			updateState();
		}
	}

	private void updateState()
	{
		EntityHiderState.hideOtherPlayers = config.hideOtherPlayers();
		EntityHiderState.hideNpcs = config.hideNpcs();
		EntityHiderState.hideProjectiles = config.hideProjectiles();
	}
}
