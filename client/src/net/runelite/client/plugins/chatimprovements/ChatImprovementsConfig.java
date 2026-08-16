package net.runelite.client.plugins.chatimprovements;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("chatimprovements")
public interface ChatImprovementsConfig extends Config
{
	@ConfigItem(keyName = "timestamps", name = "Timestamps", description = "Shows local timestamps in the persistent chat panel.")
	default boolean timestamps() { return true; }

	@Range(min = 25, max = 1000)
	@ConfigItem(keyName = "historyLimit", name = "History Limit", description = "Maximum number of messages retained across restarts.")
	default int historyLimit() { return 250; }

	@ConfigItem(keyName = "highlightWords", name = "Highlight Words", description = "Comma-separated words or wildcard patterns to highlight.")
	default String highlightWords() { return ""; }

	@ConfigItem(keyName = "notifyHighlights", name = "Notify Highlights", description = "Sends a desktop notification for highlighted chat messages.")
	default boolean notifyHighlights() { return true; }

	@ConfigItem(keyName = "highlightColor", name = "Highlight Color", description = "Text color for highlighted messages.")
	default Color highlightColor() { return new Color(255, 196, 0); }
}
