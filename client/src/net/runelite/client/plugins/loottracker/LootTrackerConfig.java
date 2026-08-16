package net.runelite.client.plugins.loottracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("loottracker")
public interface LootTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Shows the loot tracker panel."
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackGroundItems",
		name = "Ground Items",
		description = "Tracks new ground item piles that appear after startup."
	)
	default boolean trackGroundItems()
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
		description = "Limits how many loot rows are shown."
	)
	default int maxRows()
	{
		return 6;
	}
}
