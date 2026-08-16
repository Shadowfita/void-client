package net.runelite.client.plugins.boosts;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("boosts")
public interface BoostsConfig extends Config
{
	@ConfigItem(keyName = "showBoosts", name = "Show Boosts", description = "Shows skills whose current level differs from the base level.")
	default boolean showBoosts() { return true; }

	@ConfigItem(keyName = "showRelative", name = "Show Relative Change", description = "Shows the amount gained or drained alongside the current level.")
	default boolean showRelative() { return true; }
}
