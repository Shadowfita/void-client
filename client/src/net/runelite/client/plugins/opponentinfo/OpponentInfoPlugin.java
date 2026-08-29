package net.runelite.client.plugins.opponentinfo;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Opponent Information",
	description = "Shows information about the player or NPC currently being fought.",
	tags = {"opponent", "combat", "npc", "player"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class OpponentInfoPlugin extends Plugin
{
	@Inject private OverlayManager overlayManager;
	@Inject private OpponentInfoOverlay overlay;
	@Provides OpponentInfoConfig provideConfig(ConfigManager manager) { return manager.getConfig(OpponentInfoConfig.class); }
	@Override protected void startUp() { overlayManager.add(overlay); }
	@Override protected void shutDown() { overlayManager.remove(overlay); }
}
