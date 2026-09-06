package net.runelite.client.plugins.npcnames;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.compatibility.ClientCapability;
import net.runelite.client.compatibility.RequiresCapabilities;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@RequiresCapabilities({ClientCapability.NATIVE_SCENE_PROJECTION, ClientCapability.ACTOR_OVERHEAD_PROJECTION, ClientCapability.STABLE_NPC_IDENTITY})
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
