package net.runelite.client.plugins.itemcharges;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.compatibility.ClientCapability;
import net.runelite.client.compatibility.RequiresCapabilities;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@RequiresCapabilities({ClientCapability.VERIFIED_INVENTORY_CONTAINER, ClientCapability.VISIBLE_INVENTORY_SLOT_BOUNDS})
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
