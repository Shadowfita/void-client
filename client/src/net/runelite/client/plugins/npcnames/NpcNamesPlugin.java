package net.runelite.client.plugins.npcnames;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "NPC Names",
	description = "Shows names above NPCs.",
	tags = {"npc", "names", "overlay"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class NpcNamesPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NpcNamesOverlay overlay;

	@Provides
	NpcNamesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcNamesConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}
}
