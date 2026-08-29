package net.runelite.client.plugins.tileindicators;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Tile Indicators",
	description = "Highlights useful scene tiles.",
	tags = {"tile", "true", "indicator", "overlay"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class TileIndicatorsPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TileIndicatorsOverlay overlay;

	@Provides
	TileIndicatorsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TileIndicatorsConfig.class);
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
