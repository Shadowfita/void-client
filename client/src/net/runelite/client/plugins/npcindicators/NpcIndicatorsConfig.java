package net.runelite.client.plugins.npcindicators;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("npcindicators")
public interface NpcIndicatorsConfig extends Config
{
	@ConfigItem(
		keyName = "npcNames",
		name = "NPCs to Highlight",
		description = "Comma-separated NPC names, partial names, wildcard patterns, or numeric IDs."
	)
	default String npcNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "highlightAll",
		name = "Highlight All",
		description = "Highlights all nearby NPCs."
	)
	default boolean highlightAll()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showNames",
		name = "Show Names",
		description = "Shows the NPC name above highlighted NPCs."
	)
	default boolean showNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCombatLevel",
		name = "Show Combat Level",
		description = "Appends the NPC combat level where available."
	)
	default boolean showCombatLevel()
	{
		return true;
	}

	@Range(min = 1, max = 64)
	@ConfigItem(
		keyName = "drawDistance",
		name = "Draw Distance",
		description = "Maximum scene-tile distance for highlighted NPCs."
	)
	default int drawDistance()
	{
		return 24;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight Color",
		description = "Configures the NPC highlight color."
	)
	default Color highlightColor()
	{
		return new Color(0, 220, 255, 180);
	}

	@ConfigItem(
		keyName = "fillTiles",
		name = "Fill Tiles",
		description = "Fills highlighted NPC tiles."
	)
	default boolean fillTiles()
	{
		return true;
	}
}
