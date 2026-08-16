package net.runelite.client.plugins.grounditems;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("grounditems")
public interface GroundItemNamesConfig extends Config
{
	@ConfigItem(
		keyName = "showNames",
		name = "Show Names",
		description = "Shows item names above ground item piles."
	)
	default boolean showNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showQuantities",
		name = "Quantities",
		description = "Shows stack quantities next to ground item names."
	)
	default boolean showQuantities()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrices",
		name = "Show Prices",
		description = "Shows item stack value next to ground item names."
	)
	default boolean showPrices()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightedItems",
		name = "Highlighted Items",
		description = "Comma-separated item names to highlight. Supports * wildcards."
	)
	default String highlightedItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hiddenItems",
		name = "Hidden Items",
		description = "Comma-separated item names to hide. Supports * wildcards."
	)
	default String hiddenItems()
	{
		return "Bones, Ashes";
	}

	@ConfigItem(
		keyName = "showHighlightedOnly",
		name = "Highlighted Only",
		description = "Only shows items in the highlighted item list."
	)
	default boolean showHighlightedOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightTiles",
		name = "Highlight Tiles",
		description = "Highlights tiles containing visible ground items."
	)
	default boolean highlightTiles()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 10000000
	)
	@ConfigItem(
		keyName = "hideUnderValue",
		name = "Hide Under Value",
		description = "Hides non-highlighted items under this stack value."
	)
	default int hideUnderValue()
	{
		return 0;
	}

	@Range(
		min = 1,
		max = 8
	)
	@ConfigItem(
		keyName = "maxItemsPerTile",
		name = "Items Per Tile",
		description = "Limits how many item names are drawn per tile."
	)
	default int maxItemsPerTile()
	{
		return 3;
	}

	@Range(
		min = 1,
		max = 64
	)
	@ConfigItem(
		keyName = "drawDistance",
		name = "Draw Distance",
		description = "Limits ground item labels by distance from the local player."
	)
	default int drawDistance()
	{
		return 24;
	}

	@Alpha
	@ConfigItem(
		keyName = "textColor",
		name = "Text Color",
		description = "Configures ground item name color."
	)
	default Color textColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightedColor",
		name = "Highlighted Color",
		description = "Configures highlighted item color."
	)
	default Color highlightedColor()
	{
		return new Color(170, 80, 255, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenColor",
		name = "Hidden Color",
		description = "Configures hidden item color."
	)
	default Color hiddenColor()
	{
		return new Color(95, 95, 95, 180);
	}

	@Alpha
	@ConfigItem(
		keyName = "lowValueColor",
		name = "Low Value Color",
		description = "Configures low-value item color."
	)
	default Color lowValueColor()
	{
		return new Color(100, 160, 255, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "mediumValueColor",
		name = "Medium Value Color",
		description = "Configures medium-value item color."
	)
	default Color mediumValueColor()
	{
		return new Color(90, 220, 100, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "highValueColor",
		name = "High Value Color",
		description = "Configures high-value item color."
	)
	default Color highValueColor()
	{
		return new Color(255, 150, 45, 255);
	}

	@Range(
		min = 0,
		max = 10000000
	)
	@ConfigItem(
		keyName = "lowValuePrice",
		name = "Low Value Price",
		description = "Stack value where low-value coloring starts."
	)
	default int lowValuePrice()
	{
		return 20000;
	}

	@Range(
		min = 0,
		max = 10000000
	)
	@ConfigItem(
		keyName = "mediumValuePrice",
		name = "Medium Value Price",
		description = "Stack value where medium-value coloring starts."
	)
	default int mediumValuePrice()
	{
		return 100000;
	}

	@Range(
		min = 0,
		max = 10000000
	)
	@ConfigItem(
		keyName = "highValuePrice",
		name = "High Value Price",
		description = "Stack value where high-value coloring starts."
	)
	default int highValuePrice()
	{
		return 1000000;
	}
}
