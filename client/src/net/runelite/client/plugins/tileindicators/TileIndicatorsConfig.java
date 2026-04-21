package net.runelite.client.plugins.tileindicators;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("tileindicators")
public interface TileIndicatorsConfig extends Config
{
	@ConfigItem(
		keyName = "highlightTrueTile",
		name = "True Tile",
		description = "Highlights the tile the local player occupies."
	)
	default boolean highlightTrueTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "labelTrueTile",
		name = "True Tile Label",
		description = "Shows a small tile label on the local player's true tile."
	)
	default boolean labelTrueTile()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "trueTileColor",
		name = "True Tile Color",
		description = "Configures the local player tile color."
	)
	default Color trueTileColor()
	{
		return new Color(0, 184, 212, 120);
	}

	@ConfigItem(
		keyName = "tileFill",
		name = "Fill",
		description = "Fills highlighted tile polygons."
	)
	default boolean tileFill()
	{
		return true;
	}
}
