package net.runelite.client.plugins.banktags;

import com.GameClient;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.events.ClientTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.qol.QolIcon;
import net.runelite.client.plugins.qol.QolItemContainers;
import net.runelite.client.plugins.qol.QolPatterns;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
	name = "Bank Tags",
	description = "Provides a searchable, tagged sidebar view of the current 634 bank container.",
	tags = {"bank", "tags", "search", "items"},
	enabledByDefault = false,
	loadWhenOutdated = true
)
public class BankTagsPlugin extends Plugin
{
	@Inject private GameClient client;
	@Inject private ClientToolbar clientToolbar;
	@Inject private BankTagsConfig config;
	private BankTagsPanel panel;
	private NavigationButton navButton;
	private long lastRefresh;
	private int lastFingerprint;

	@Provides BankTagsConfig provideConfig(ConfigManager manager) { return manager.getConfig(BankTagsConfig.class); }

	@Override
	protected void startUp()
	{
		panel = injector.getInstance(BankTagsPanel.class);
		panel.setSearchListener(this::forceRefresh);
		BufferedImage icon = QolIcon.letter("B", new Color(220, 138, 0));
		navButton = NavigationButton.builder().tooltip("Bank Tags").icon(icon).priority(6).panel(panel).build();
		clientToolbar.addNavigation(navButton);
		forceRefresh();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		lastFingerprint = 0;
		lastRefresh = 0L;
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		if (panel == null || System.currentTimeMillis() - lastRefresh < 500L)
		{
			return;
		}
		refresh(false);
	}

	private void forceRefresh()
	{
		lastFingerprint = Integer.MIN_VALUE;
		refresh(true);
	}

	private void refresh(boolean forced)
	{
		lastRefresh = System.currentTimeMillis();
		GameClient.ItemContainerSnapshot bank = QolItemContainers.bank(client);
		int fingerprint = bank == null ? 0 : fingerprint(bank);
		if (!forced && fingerprint == lastFingerprint)
		{
			return;
		}
		lastFingerprint = fingerprint;
		String query = panel == null ? "" : panel.getSearchText();
		Map<String, List<GameClient.ItemStackInfo>> groups = group(bank, query);
		int itemCount = bank == null ? 0 : bank.getOccupiedSlots();
		int totalValue = bank == null ? 0 : bank.getTotalValue();
		if (panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.rebuild(groups, itemCount, totalValue, config.showValues()));
		}
	}

	private Map<String, List<GameClient.ItemStackInfo>> group(GameClient.ItemContainerSnapshot bank, String query)
	{
		Map<String, List<GameClient.ItemStackInfo>> groups = new LinkedHashMap<>();
		Map<String, String> definitions = definitions();
		for (String tag : definitions.keySet())
		{
			groups.put(tag, new ArrayList<>());
		}
		if (config.showUntagged())
		{
			groups.put("Untagged", new ArrayList<>());
		}
		if (bank == null)
		{
			return groups;
		}
		for (GameClient.ItemStackInfo item : bank.getItems())
		{
			if (query != null && !query.trim().isEmpty() && !QolPatterns.matches(item.getName(), query))
			{
				continue;
			}
			boolean tagged = false;
			for (Map.Entry<String, String> definition : definitions.entrySet())
			{
				if (QolPatterns.matches(item.getName(), definition.getValue()) || QolPatterns.matchesId(item.getId(), definition.getValue()))
				{
					groups.get(definition.getKey()).add(item);
					tagged = true;
				}
			}
			if (!tagged && config.showUntagged())
			{
				groups.get("Untagged").add(item);
			}
		}
		groups.values().forEach(items -> items.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName())));
		return groups;
	}

	private Map<String, String> definitions()
	{
		Map<String, String> tags = new LinkedHashMap<>();
		for (String line : config.tagDefinitions().split("[\\r\\n]+"))
		{
			int separator = line.indexOf(':');
			if (separator <= 0 || separator >= line.length() - 1)
			{
				continue;
			}
			String name = line.substring(0, separator).trim();
			String patterns = line.substring(separator + 1).trim();
			if (!name.isEmpty() && !patterns.isEmpty())
			{
				tags.put(name, patterns);
			}
		}
		return tags;
	}

	private static int fingerprint(GameClient.ItemContainerSnapshot bank)
	{
		int hash = bank.getCapacity();
		for (GameClient.ItemStackInfo item : bank.getItems())
		{
			hash = 31 * hash + item.getId();
			hash = 31 * hash + item.getQuantity();
			hash = 31 * hash + item.getSlot();
		}
		return hash;
	}
}
