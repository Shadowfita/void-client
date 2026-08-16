package net.runelite.client.plugins.cluehelper;

import com.google.inject.Provides;
import java.awt.Color;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.qol.QolIcon;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Clue Helper (2011)",
	description = "Looks up classic Treasure Trail anagrams, ciphers, challenge answers and common search clues.",
	tags = {"clue", "treasure", "trail", "anagram", "cipher"},
	enabledByDefault = true,
	loadWhenOutdated = true
)
public class ClueHelperPlugin extends Plugin
{
	private static final String GROUP = "cluehelper";
	private static final String LAST_CLUE = "lastClue";

	@Inject private ClueHelperConfig config;
	@Inject private ConfigManager configManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private Notifier notifier;
	private ClueHelperPanel panel;
	private NavigationButton navButton;

	@Provides
	ClueHelperConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(ClueHelperConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = injector.getInstance(ClueHelperPanel.class);
		panel.setLookupListener(this::lookupPanelText);
		navButton = NavigationButton.builder()
			.tooltip("Clue Helper")
			.icon(QolIcon.letter("?", new Color(205, 175, 75)))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		if (config.rememberLastClue())
		{
			String saved = configManager.getConfiguration(GROUP, LAST_CLUE);
			if (saved != null && !saved.trim().isEmpty())
			{
				panel.setClueText(saved);
				lookup(saved, false);
			}
		}
	}

	@Override
	protected void shutDown()
	{
		if (config.rememberLastClue() && panel != null)
		{
			configManager.setConfiguration(GROUP, LAST_CLUE, panel.getClueText());
		}
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.detectChat())
		{
			return;
		}
		String message = event.getMessage();
		if (message == null)
		{
			return;
		}
		message = Text.sanitizeMultilineText(message).trim();
		if (message.length() < 4)
		{
			return;
		}
		ClueSolution solution = ClueDatabase.lookup(message);
		if (solution != null)
		{
			final String clue = message;
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.setClueText(clue);
					panel.showSolution(solution);
				}
			});
			remember(message);
			if (config.notifyRecognised())
			{
				notifier.notify("Clue recognised: " + solution.getTitle());
			}
		}
	}

	private void lookupPanelText()
	{
		if (panel != null)
		{
			lookup(panel.getClueText(), true);
		}
	}

	private void lookup(String clue, boolean showNoMatch)
	{
		ClueSolution solution = ClueDatabase.lookup(clue);
		if (solution != null)
		{
			panel.showSolution(solution);
			remember(clue);
		}
		else if (showNoMatch)
		{
			panel.showNoMatch(ClueDatabase.suggestions(clue, 3));
		}
	}

	private void remember(String clue)
	{
		if (config.rememberLastClue())
		{
			configManager.setConfiguration(GROUP, LAST_CLUE, clue == null ? "" : clue);
		}
	}
}
