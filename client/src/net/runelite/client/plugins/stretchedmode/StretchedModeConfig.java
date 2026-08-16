package net.runelite.client.plugins.stretchedmode;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("stretchedmode")
public interface StretchedModeConfig extends Config
{
	@Range(
		min = 1,
		max = 3
	)
	@ConfigItem(
		keyName = "scalingFactor",
		name = "Interface Scale",
		description = "Scales the RuneScape HUD and interfaces while keeping the 3D scene at native resolution. Decimal values such as 1.3x and 1.5x are supported."
	)
	default double scaling()
	{
		return 1.0;
	}

	@ConfigItem(
		keyName = "legacyFullCanvasStretch",
		name = "Legacy Full-Canvas Stretch",
		description = "Uses the old Stretched Mode behaviour, scaling the whole game including the 3D scene. Use this as a fallback for software rendering."
	)
	default boolean legacyFullCanvasStretch()
	{
		return false;
	}

	@ConfigItem(
		keyName = "increasedPerformance",
		name = "Legacy Fast Scaling",
		description = "Legacy full-canvas mode only. Uses nearest-neighbour scaling instead of bilinear filtering."
	)
	default boolean increasedPerformance()
	{
		return false;
	}

	@ConfigItem(
		keyName = "integerScaling",
		name = "Legacy Integer Scaling",
		description = "Legacy full-canvas mode only. Restricts the physical game canvas to whole-number scale multiples."
	)
	default boolean integerScaling()
	{
		return false;
	}

	@ConfigItem(
		keyName = "keepAspectRatio",
		name = "Legacy Keep Aspect Ratio",
		description = "Legacy full-canvas mode only. Keeps the game aspect ratio while stretching."
	)
	default boolean keepAspectRatio()
	{
		return true;
	}
}
