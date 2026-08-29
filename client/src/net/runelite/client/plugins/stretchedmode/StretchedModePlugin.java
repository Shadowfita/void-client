package net.runelite.client.plugins.stretchedmode;

import com.GameClient;
import com.google.inject.Provides;
import javax.inject.Inject;
import com.google.inject.name.Named;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Interface Scaling - Beta",
	description = "Scales the RuneScape HUD independently from the native-resolution 3D scene.",
	tags = {"resize", "ui", "interface", "hud", "stretch", "scaling", "fixed"},
	enabledByDefault = true,
	loadInSafeMode = false,
	loadWhenOutdated = true
)
public class StretchedModePlugin extends Plugin
{
	@Inject
	private GameClient client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private StretchedModeConfig config;

	@Inject
	@Named("safeMode")
	private boolean safeMode;

	@Provides
	StretchedModeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StretchedModeConfig.class);
	}

	@Override
	protected void startUp()
	{
		updateConfig();
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(() ->
		{
			client.setInterfaceSupersamplingEnabled(false);
			client.setInterfaceScalingFactor(100);
			client.setScalingFactor(100);
			client.setStretchedEnabled(false);
			client.invalidateStretching(true);
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"stretchedmode".equals(event.getGroup()))
		{
			return;
		}

		updateConfig();
	}

	private void updateConfig()
	{
		if (safeMode)
		{
			clientThread.invoke(() ->
			{
				client.setInterfaceSupersamplingEnabled(false);
				client.setInterfaceScalingFactor(100);
				client.setScalingFactor(100);
				client.setStretchedEnabled(false);
				client.invalidateStretching(true);
			});
			return;
		}

		final int factor = (int) Math.round(config.scaling() * 100.0);
		clientThread.invoke(() ->
		{
			client.setInterfaceSupersamplingEnabled(config.fractionalSupersampling());
			if (config.legacyFullCanvasStretch())
			{
				client.setInterfaceScalingFactor(100);
				client.setStretchedFast(config.increasedPerformance());
				client.setStretchedIntegerScaling(config.integerScaling());
				client.setStretchedKeepAspectRatio(config.keepAspectRatio());
				client.setScalingFactor(factor);
				client.setStretchedEnabled(true);
				client.invalidateStretching(true);
				return;
			}

			client.setStretchedEnabled(false);
			client.setScalingFactor(100);
			client.setInterfaceScalingFactor(factor);
			client.invalidateStretching(false);
		});
	}
}
