package net.runelite.client.plugins.grounditems;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Ground Items",
	description = "Highlight ground items and show item value information.",
	tags = {"ground", "item", "items", "names", "prices", "highlight", "overlay"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class GroundItemNamesPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GroundItemNamesOverlay overlay;

	@Provides
	GroundItemNamesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GroundItemNamesConfig.class);
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
