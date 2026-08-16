package net.runelite.client.plugins.opponentinfo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("opponentinfo")
public interface OpponentInfoConfig extends Config
{
	@ConfigItem(keyName = "showCombatLevel", name = "Combat Level", description = "Shows the opponent combat level when available.")
	default boolean showCombatLevel() { return true; }

	@ConfigItem(keyName = "showDistance", name = "Distance", description = "Shows the approximate scene-tile distance to the opponent.")
	default boolean showDistance() { return true; }
}
