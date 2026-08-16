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
		description = "Highlights the tile the local player currently occupies."
	)
	default boolean highlightTrueTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightDestinationTile",
		name = "Destination Tile",
		description = "Highlights the final tile in the local player's movement queue."
	)
	default boolean highlightDestinationTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "markedTiles",
		name = "Marked Scene Tiles",
		description = "Scene-local tiles to mark as x:y pairs, separated by commas (for example 50:50, 51:50)."
	)
	default String markedTiles()
	{
		return "";
	}

	@ConfigItem(
		keyName = "labelTrueTile",
		name = "Tile Labels",
		description = "Labels true and destination tiles."
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
		return new Color(0, 184, 212, 160);
	}

	@Alpha
	@ConfigItem(
		keyName = "destinationTileColor",
		name = "Destination Color",
		description = "Configures the destination tile color."
	)
	default Color destinationTileColor()
	{
		return new Color(255, 196, 0, 160);
	}

	@Alpha
	@ConfigItem(
		keyName = "markedTileColor",
		name = "Marked Tile Color",
		description = "Configures manually marked tile color."
	)
	default Color markedTileColor()
	{
		return new Color(170, 100, 255, 160);
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
