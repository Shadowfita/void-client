package net.runelite.client.plugins.objectindicators;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("objectindicators")
public interface ObjectIndicatorsConfig extends Config
{
	@ConfigItem(
		keyName = "objects",
		name = "Objects to Highlight",
		description = "Comma-separated object names, partial names, wildcard patterns, or numeric IDs."
	)
	default String objects()
	{
		return "bank booth, furnace, altar";
	}

	@ConfigItem(
		keyName = "highlightAll",
		name = "Highlight All",
		description = "Highlights every object observed by the 634 scene event bridge."
	)
	default boolean highlightAll()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showNames",
		name = "Show Names",
		description = "Shows object names above highlighted tiles."
	)
	default boolean showNames()
	{
		return true;
	}

	@Range(min = 1, max = 64)
	@ConfigItem(
		keyName = "drawDistance",
		name = "Draw Distance",
		description = "Maximum scene-tile distance for object highlights."
	)
	default int drawDistance()
	{
		return 32;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight Color",
		description = "Configures the object highlight color."
	)
	default Color highlightColor()
	{
		return new Color(255, 165, 0, 180);
	}
}
