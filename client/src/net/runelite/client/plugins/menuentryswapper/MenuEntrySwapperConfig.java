package net.runelite.client.plugins.menuentryswapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("menuentryswapper")
public interface MenuEntrySwapperConfig extends Config
{
	@ConfigItem(
		keyName = "preferredOptions",
		name = "Preferred Left-click Options",
		description = "Priority-ordered comma-separated menu options. The first matching option becomes left-click. Use option@target to constrain the target."
	)
	default String preferredOptions()
	{
		return "Bank, Exchange, Trade, Pickpocket, Bury, Wear, Wield";
	}

	@ConfigItem(keyName = "exactMatch", name = "Exact Option Match", description = "Requires exact menu-option text instead of allowing partial option matches.")
	default boolean exactMatch() { return true; }
}
