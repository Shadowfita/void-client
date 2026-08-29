package net.runelite.client.plugins.playerindicators;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Player Indicators",
    description = "Shows nearby player names and optional occupied-tile highlights using the native 634 camera projection.",
    tags = {"player", "names", "tiles", "indicator"},
    enabledByDefault = false,
    loadWhenOutdated = true
)
public class PlayerIndicatorsPlugin extends Plugin
{
    @Inject private OverlayManager overlayManager;
    @Inject private PlayerIndicatorsOverlay overlay;

    @Provides PlayerIndicatorsConfig provideConfig(ConfigManager manager) { return manager.getConfig(PlayerIndicatorsConfig.class); }
    @Override protected void startUp() { overlayManager.add(overlay); }
    @Override protected void shutDown() { overlayManager.remove(overlay); }
}
