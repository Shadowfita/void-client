package net.runelite.client.plugins.cluehelper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("cluehelper")
public interface ClueHelperConfig extends Config
{
	@ConfigItem(
		keyName = "detectChat",
		name = "Detect clue text in chat",
		description = "Automatically look up recognised clue text received through the client chat event bridge."
	)
	default boolean detectChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyRecognised",
		name = "Notify when recognised",
		description = "Show a desktop notification when a supported clue is recognised."
	)
	default boolean notifyRecognised()
	{
		return false;
	}

	@ConfigItem(
		keyName = "rememberLastClue",
		name = "Remember last clue",
		description = "Restore the most recently looked-up clue when the client starts."
	)
	default boolean rememberLastClue()
	{
		return true;
	}
}
