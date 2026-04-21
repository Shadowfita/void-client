package net.runelite.client.plugins.pathtracing;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Path Tracing",
	description = "Adds sampled soft shadows to the original DirectX dynamic shadow renderer.",
	tags = {"directx", "graphics", "lighting", "path", "tracing", "shadows"},
	enabledByDefault = true,
	loadInSafeMode = false
)
public class PathTracingPlugin extends Plugin
{
	@Inject
	private PathTracingConfig config;

	@Inject
	private PathTracingPostProcessOverlay overlay;

	@Inject
	private net.runelite.client.ui.overlay.OverlayManager overlayManager;

	@Provides
	PathTracingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PathTracingConfig.class);
	}

	@Override
	protected void startUp()
	{
		updateState();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		PathTracingState.setEnabled(false);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("pathtracing".equals(event.getGroup()))
		{
			updateState();
		}
	}

	private void updateState()
	{
		PathTracingState.configure(
			config.pathTracedShadows(),
			config.customLighting(),
			config.ssao(),
			config.antiAliasing(),
			config.framebufferEffects(),
			config.bloom(),
			config.vignette(),
			config.sharpen(),
			config.customSkybox(),
			config.skyboxStyle(),
			config.samples(),
			config.softness(),
			config.lightingIntensity(),
			config.sunHour(),
			config.sunIntensity(),
			config.framebufferScale(),
			config.captureFps(),
			config.bloomIntensity(),
			config.contrast(),
			config.saturation(),
			config.temperature()
		);
	}
}
