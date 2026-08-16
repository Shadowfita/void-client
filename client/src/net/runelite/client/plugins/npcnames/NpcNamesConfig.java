package net.runelite.client.plugins.npcnames;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("npcnames")
public interface NpcNamesConfig extends Config
{
	@ConfigItem(
		keyName = "showNames",
		name = "Show Names",
		description = "Shows names above NPCs."
	)
	default boolean showNames()
	{
		return true;
	}

	@Range(
		min = 1,
		max = 64
	)
	@ConfigItem(
		keyName = "drawDistance",
		name = "Draw Distance",
		description = "Limits NPC labels by distance from the local player."
	)
	default int drawDistance()
	{
		return 20;
	}

	@Alpha
	@ConfigItem(
		keyName = "textColor",
		name = "Text Color",
		description = "Configures NPC name color."
	)
	default Color textColor()
	{
		return new Color(190, 230, 255, 255);
	}
}
