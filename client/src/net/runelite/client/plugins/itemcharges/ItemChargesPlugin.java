package net.runelite.client.plugins.itemcharges;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Item Charges",
	description = "Displays potion doses and charge counts encoded in inventory item names.",
	tags = {"charges", "doses", "inventory", "teleport"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class ItemChargesPlugin extends Plugin
{
	@Inject private OverlayManager overlayManager;
	@Inject private ItemChargesOverlay overlay;
	@Provides ItemChargesConfig provideConfig(ConfigManager manager) { return manager.getConfig(ItemChargesConfig.class); }
	@Override protected void startUp() { overlayManager.add(overlay); }
	@Override protected void shutDown() { overlayManager.remove(overlay); }
}
