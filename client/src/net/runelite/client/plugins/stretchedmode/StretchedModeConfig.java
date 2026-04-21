package net.runelite.client.plugins.stretchedmode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("stretchedmode")
public interface StretchedModeConfig extends Config
{
	@ConfigItem(
		keyName = "increasedPerformance",
		name = "Increased Performance Mode",
		description = "Uses faster stretching."
	)
	default boolean increasedPerformance()
	{
		return false;
	}

	@ConfigItem(
		keyName = "integerScaling",
		name = "Integer Scaling",
		description = "Uses a whole-number scale factor."
	)
	default boolean integerScaling()
	{
		return false;
	}

	@ConfigItem(
		keyName = "keepAspectRatio",
		name = "Keep Aspect Ratio",
		description = "Keeps the game aspect ratio while stretching."
	)
	default boolean keepAspectRatio()
	{
		return true;
	}

	@Range(
		min = 25,
		max = 300
	)
	@ConfigItem(
		keyName = "scaling",
		name = "Resizable Scaling",
		description = "Scales the game view by this percent."
	)
	default int scaling()
	{
		return 100;
	}
}
