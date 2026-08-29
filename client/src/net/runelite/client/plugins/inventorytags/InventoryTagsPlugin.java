package net.runelite.client.plugins.inventorytags;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Inventory Tags",
	description = "Color-codes inventory items using configurable name patterns.",
	tags = {"inventory", "tag", "gear", "food", "items"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class InventoryTagsPlugin extends Plugin
{
	@Inject private OverlayManager overlayManager;
	@Inject private InventoryTagsOverlay overlay;
	@Provides InventoryTagsConfig provideConfig(ConfigManager manager) { return manager.getConfig(InventoryTagsConfig.class); }
	@Override protected void startUp() { overlayManager.add(overlay); }
	@Override protected void shutDown() { overlayManager.remove(overlay); }
}
