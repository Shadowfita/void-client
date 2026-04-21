package net.runelite.client.plugins.pathtracing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("pathtracing")
public interface PathTracingConfig extends Config
{
	@ConfigItem(
		keyName = "pathTracedShadows",
		name = "Path traced shadows",
		description = "Uses a simple sampled soft-shadow pass for dynamic actor shadows."
	)
	default boolean pathTracedShadows()
	{
		return true;
	}

	@ConfigItem(
		keyName = "customLighting",
		name = "Custom lighting",
		description = "Uses the plugin lighting pass."
	)
	default boolean customLighting()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ssao",
		name = "SSAO",
		description = "Adds a light screen-space ambient occlusion pass to the plugin lighting mode."
	)
	default boolean ssao()
	{
		return true;
	}

	@ConfigItem(
		keyName = "antiAliasing",
		name = "Antialiasing",
		description = "Enables antialiasing hints and a subtle edge smoothing pass for the plugin lighting mode."
	)
	default boolean antiAliasing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "framebufferEffects",
		name = "Framebuffer effects",
		description = "Captures the DirectX backbuffer and uses it for plugin post-processing."
	)
	default boolean framebufferEffects()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bloom",
		name = "Bloom",
		description = "Adds a lightweight bloom pass from bright areas of the captured DirectX frame."
	)
	default boolean bloom()
	{
		return true;
	}

	@ConfigItem(
		keyName = "vignette",
		name = "Vignette",
		description = "Darkens the outside of the frame in the custom lighting pass."
	)
	default boolean vignette()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sharpen",
		name = "Sharpen",
		description = "Adds a subtle local contrast pass from the captured DirectX frame."
	)
	default boolean sharpen()
	{
		return false;
	}

	@ConfigItem(
		keyName = "customSkybox",
		name = "Custom skybox",
		description = "Draws a plugin skybox while custom lighting is active."
	)
	default boolean customSkybox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skyboxStyle",
		name = "Skybox",
		description = "Selects the custom skybox color profile."
	)
	default PathTracingSkyboxStyle skyboxStyle()
	{
		return PathTracingSkyboxStyle.CLEAR;
	}

	@Range(
		min = 4,
		max = 16
	)
	@ConfigItem(
		keyName = "samples",
		name = "Samples",
		description = "Controls the number of shadow samples used for the soft penumbra."
	)
	default int samples()
	{
		return 8;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		keyName = "softness",
		name = "Softness",
		description = "Controls how far the sampled shadow penumbra spreads."
	)
	default int softness()
	{
		return 55;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		keyName = "lightingIntensity",
		name = "Lighting intensity",
		description = "Controls the strength of the custom lighting and SSAO pass."
	)
	default int lightingIntensity()
	{
		return 45;
	}

	@Range(
		min = 0,
		max = 23
	)
	@ConfigItem(
		keyName = "sunHour",
		name = "Sun hour",
		description = "Controls the custom sun position. 14 is 2pm."
	)
	default int sunHour()
	{
		return 14;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		keyName = "sunIntensity",
		name = "Sun intensity",
		description = "Controls the brightness of the custom sun and sky lighting."
	)
	default int sunIntensity()
	{
		return 65;
	}

	@Range(
		min = 25,
		max = 100
	)
	@ConfigItem(
		keyName = "framebufferScale",
		name = "Framebuffer scale",
		description = "Controls the captured frame size as a percent of the game viewport."
	)
	default int framebufferScale()
	{
		return 50;
	}

	@Range(
		min = 5,
		max = 60
	)
	@ConfigItem(
		keyName = "captureFps",
		name = "Capture FPS",
		description = "Limits DirectX framebuffer readback frequency."
	)
	default int captureFps()
	{
		return 20;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		keyName = "bloomIntensity",
		name = "Bloom intensity",
		description = "Controls the strength of the framebuffer bloom pass."
	)
	default int bloomIntensity()
	{
		return 28;
	}

	@Range(
		min = 50,
		max = 150
	)
	@ConfigItem(
		keyName = "contrast",
		name = "Contrast",
		description = "Controls plugin post-process contrast."
	)
	default int contrast()
	{
		return 108;
	}

	@Range(
		min = 50,
		max = 150
	)
	@ConfigItem(
		keyName = "saturation",
		name = "Saturation",
		description = "Controls plugin post-process saturation."
	)
	default int saturation()
	{
		return 112;
	}

	@Range(
		min = -100,
		max = 100
	)
	@ConfigItem(
		keyName = "temperature",
		name = "Temperature",
		description = "Warms or cools the plugin lighting composite."
	)
	default int temperature()
	{
		return 8;
	}
}
