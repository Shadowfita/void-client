package net.runelite.client.plugins.banktags;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("banktags")
public interface BankTagsConfig extends Config
{
	@ConfigItem(
		keyName = "tagDefinitions",
		name = "Tag Definitions",
		description = "One tag per line in the form Tag name: item pattern, item pattern. Wildcards are supported."
	)
	default String tagDefinitions()
	{
		return "Combat: *sword*, *shield*, *armour*, *plate*, *helm*\nFood: shark, rocktail, manta ray, monkfish\nPotions: *potion*, *brew*, *restore*\nTeleports: *teleport*, *tablet*, *rune*";
	}

	@ConfigItem(keyName = "showValues", name = "Show Values", description = "Shows estimated stack values in the tagged bank browser.")
	default boolean showValues() { return true; }

	@ConfigItem(keyName = "showUntagged", name = "Show Untagged", description = "Adds an Untagged group for items that do not match a configured tag.")
	default boolean showUntagged() { return false; }
}
