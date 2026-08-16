package net.runelite.client.plugins.boosts;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Boosts Information",
	description = "Shows boosted and drained skill levels.",
	tags = {"boost", "stats", "levels", "potions"},
	enabledByDefault = true,
	loadWhenOutdated = true
)
public class BoostsPlugin extends Plugin
{
	@Inject private OverlayManager overlayManager;
	@Inject private BoostsOverlay overlay;

	@Provides BoostsConfig provideConfig(ConfigManager manager) { return manager.getConfig(BoostsConfig.class); }
	@Override protected void startUp() { overlayManager.add(overlay); }
	@Override protected void shutDown() { overlayManager.remove(overlay); }
}
