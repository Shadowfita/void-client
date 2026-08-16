package net.runelite.client.plugins.inventorytags;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("inventorytags")
public interface InventoryTagsConfig extends Config
{
	@ConfigItem(keyName = "groupOne", name = "Group 1 Items", description = "Comma-separated names or wildcard patterns, for example shark, *brew*.")
	default String groupOne() { return "shark, rocktail, manta ray, monkfish"; }
	@Alpha @ConfigItem(keyName = "groupOneColor", name = "Group 1 Color", description = "Color for group 1.")
	default Color groupOneColor() { return new Color(60, 200, 90, 100); }

	@ConfigItem(keyName = "groupTwo", name = "Group 2 Items", description = "Comma-separated names or wildcard patterns.")
	default String groupTwo() { return "*potion*, *brew*, *restore*"; }
	@Alpha @ConfigItem(keyName = "groupTwoColor", name = "Group 2 Color", description = "Color for group 2.")
	default Color groupTwoColor() { return new Color(80, 150, 255, 100); }

	@ConfigItem(keyName = "groupThree", name = "Group 3 Items", description = "Comma-separated names or wildcard patterns.")
	default String groupThree() { return "*teleport*, *tablet*, *rune*"; }
	@Alpha @ConfigItem(keyName = "groupThreeColor", name = "Group 3 Color", description = "Color for group 3.")
	default Color groupThreeColor() { return new Color(190, 110, 255, 100); }

	@ConfigItem(keyName = "groupFour", name = "Group 4 Items", description = "Comma-separated names or wildcard patterns.")
	default String groupFour() { return ""; }
	@Alpha @ConfigItem(keyName = "groupFourColor", name = "Group 4 Color", description = "Color for group 4.")
	default Color groupFourColor() { return new Color(255, 175, 50, 100); }

	@ConfigItem(keyName = "outline", name = "Outline Slots", description = "Draws a solid outline around tagged inventory slots.")
	default boolean outline() { return true; }
}
