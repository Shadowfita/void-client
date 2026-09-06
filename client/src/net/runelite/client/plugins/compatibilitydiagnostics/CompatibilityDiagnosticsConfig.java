package net.runelite.client.plugins.compatibilitydiagnostics;

import net.runelite.client.config.*;

@ConfigGroup("compatibilitydiagnostics")
public interface CompatibilityDiagnosticsConfig extends Config
{
	@ConfigItem(keyName="showProjectionCanaries", name="Projection Canaries",
		description="Draws native/RuneLite projection agreement geometry.")
	default boolean showProjectionCanaries() { return true; }
	@ConfigItem(keyName="showItemSlotCanaries", name="Item Slot Canaries",
		description="Draws authoritative inventory/bank slot rectangles.")
	default boolean showItemSlotCanaries() { return false; }
	@ConfigItem(keyName="showCanaryLabels", name="Canary Labels",
		description="Labels projection and item-slot calibration geometry.")
	default boolean showCanaryLabels() { return true; }
}
