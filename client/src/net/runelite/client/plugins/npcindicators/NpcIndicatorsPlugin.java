package net.runelite.client.plugins.npcindicators;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "NPC Indicators",
	description = "Highlights selected NPCs by name or ID.",
	tags = {"npc", "tag", "highlight", "slayer", "indicator"},
	enabledByDefault = true,
	loadWhenOutdated = true
)
public class NpcIndicatorsPlugin extends Plugin
{
	@Inject private OverlayManager overlayManager;
	@Inject private NpcIndicatorsOverlay overlay;

	@Provides
	NpcIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NpcIndicatorsConfig.class);
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
