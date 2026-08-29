package net.runelite.client.plugins.menuentryswapper;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Menu Entry Swapper",
	description = "Promotes configured context-menu actions to the default left-click action.",
	tags = {"menu", "left click", "swap", "bank", "pickpocket"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class MenuEntrySwapperPlugin extends Plugin
{
	@Inject private MenuEntrySwapperConfig config;
	@Provides MenuEntrySwapperConfig provideConfig(ConfigManager manager) { return manager.getConfig(MenuEntrySwapperConfig.class); }
	@Override protected void startUp() { updateState(); }
	@Override protected void shutDown() { MenuEntrySwapperState.reset(); }

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("menuentryswapper".equals(event.getGroup()))
		{
			updateState();
		}
	}

	private void updateState()
	{
		MenuEntrySwapperState.enabled = true;
		MenuEntrySwapperState.exactMatch = config.exactMatch();
		MenuEntrySwapperState.preferredOptions = config.preferredOptions();
	}
}
