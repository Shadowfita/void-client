package net.runelite.client.plugins.chatimprovements;

import com.google.inject.Provides;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Value;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.qol.QolIcon;
import net.runelite.client.plugins.qol.QolPatterns;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Chat Improvements",
	description = "Adds searchable persistent chat history, timestamps, keyword highlights and notifications.",
	tags = {"chat", "history", "timestamps", "highlight", "notification"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class ChatImprovementsPlugin extends Plugin
{
	private static final String GROUP = "chatimprovements";
	private static final String HISTORY_KEY = "savedHistory";
	@Inject private ChatImprovementsConfig config;
	@Inject private ConfigManager configManager;
	@Inject private ClientToolbar clientToolbar;
	@Inject private Notifier notifier;
	private final List<ChatRecord> records = new ArrayList<>();
	private ChatImprovementsPanel panel;
	private NavigationButton navButton;

	@Provides ChatImprovementsConfig provideConfig(ConfigManager manager) { return manager.getConfig(ChatImprovementsConfig.class); }

	@Override
	protected void startUp()
	{
		loadHistory();
		panel = injector.getInstance(ChatImprovementsPanel.class);
		panel.setFilterListener(this::refreshPanel);
		navButton = NavigationButton.builder()
			.tooltip("Chat History")
			.icon(QolIcon.letter("C", new Color(80, 190, 255)))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		saveHistory();
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		records.clear();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String message = clean(event.getMessage());
		if (message.isEmpty())
		{
			return;
		}
		String name = clean(event.getName());
		if (name.isEmpty())
		{
			name = clean(event.getSender());
		}
		boolean highlighted = QolPatterns.matches((name + " " + message).trim(), config.highlightWords());
		ChatRecord record = new ChatRecord(System.currentTimeMillis(), name, message, highlighted);
		records.add(record);
		trim();
		if (highlighted && config.notifyHighlights())
		{
			notifier.notify((name.isEmpty() ? "Chat" : name) + ": " + message);
		}
		saveHistory();
		refreshPanel();
	}

	Collection<ChatRecord> filteredRecords()
	{
		String filter = panel == null ? "" : panel.getFilterText().trim();
		if (filter.isEmpty())
		{
			return new ArrayList<>(records);
		}
		List<ChatRecord> filtered = new ArrayList<>();
		for (ChatRecord record : records)
		{
			if (QolPatterns.matches(record.getName() + " " + record.getMessage(), filter))
			{
				filtered.add(record);
			}
		}
		return filtered;
	}

	private void refreshPanel()
	{
		if (panel != null)
		{
			Collection<ChatRecord> view = filteredRecords();
			SwingUtilities.invokeLater(() -> panel.rebuild(view, config.timestamps(), config.highlightColor()));
		}
	}

	private void trim()
	{
		int limit = Math.max(25, config.historyLimit());
		while (records.size() > limit)
		{
			records.remove(0);
		}
	}

	private void saveHistory()
	{
		StringBuilder out = new StringBuilder();
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		for (ChatRecord record : records)
		{
			if (out.length() > 0) out.append(';');
			out.append(record.getTimestamp()).append('|')
				.append(encoder.encodeToString(record.getName().getBytes(StandardCharsets.UTF_8))).append('|')
				.append(encoder.encodeToString(record.getMessage().getBytes(StandardCharsets.UTF_8)));
		}
		configManager.setConfiguration(GROUP, HISTORY_KEY, out.toString());
	}

	private void loadHistory()
	{
		records.clear();
		String saved = configManager.getConfiguration(GROUP, HISTORY_KEY);
		if (saved == null || saved.isEmpty()) return;
		Base64.Decoder decoder = Base64.getUrlDecoder();
		for (String entry : saved.split(";"))
		{
			String[] fields = entry.split("\\|", 3);
			if (fields.length != 3) continue;
			try
			{
				long timestamp = Long.parseLong(fields[0]);
				String name = new String(decoder.decode(fields[1]), StandardCharsets.UTF_8);
				String message = new String(decoder.decode(fields[2]), StandardCharsets.UTF_8);
				boolean highlighted = QolPatterns.matches(name + " " + message, config.highlightWords());
				records.add(new ChatRecord(timestamp, name, message, highlighted));
			}
			catch (IllegalArgumentException ignored)
			{
			}
		}
		trim();
	}

	private static String clean(String value)
	{
		return value == null ? "" : Text.sanitizeMultilineText(value).trim();
	}

	@Value
	static class ChatRecord
	{
		long timestamp;
		String name;
		String message;
		boolean highlighted;
	}
}
