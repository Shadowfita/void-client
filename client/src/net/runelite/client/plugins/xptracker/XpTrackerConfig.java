package net.runelite.client.plugins.xptracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("xptracker")
public interface XpTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Shows the XP tracker panel.",
		hidden = true
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showXpPerHour",
		name = "XP/Hr",
		description = "Shows an estimated XP per hour total.",
		hidden = true
	)
	default boolean showXpPerHour()
	{
		return true;
	}

	@Range(
		min = 1,
		max = 12
	)
	@ConfigItem(
		keyName = "maxRows",
		name = "Rows",
		description = "Limits how many skill rows are shown.",
		hidden = true
	)
	default int maxRows()
	{
		return 6;
	}
}
