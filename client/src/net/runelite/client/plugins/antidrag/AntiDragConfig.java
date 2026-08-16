package net.runelite.client.plugins.antidrag;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("antidrag")
public interface AntiDragConfig extends Config
{
	@Range(min = 0, max = 1000)
	@ConfigItem(keyName = "dragDelay", name = "Drag Delay (ms)", description = "Suppresses inventory drag motion until this delay has elapsed.")
	default int dragDelay() { return 120; }

	@Range(min = 0, max = 32)
	@ConfigItem(keyName = "dragDistance", name = "Drag Distance", description = "Allows dragging immediately after moving this many pixels. Set to 0 to use delay only.")
	default int dragDistance() { return 8; }
}
