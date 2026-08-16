package net.runelite.client.plugins.loottracker;

import com.GameClient;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Value;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.GameStateBridge;
import net.runelite.client.game.ItemPriceProvider;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Loot Tracker",
	description = "Tracks newly appearing ground item loot.",
	tags = {"loot", "drops", "items", "tracker"},
	enabledByDefault = true,
	loadWhenOutdated = true
)
public class LootTrackerPlugin extends Plugin
{
	@Inject
	private GameClient client;

	@Inject
	private LootTrackerConfig config;

	@Inject
	private LootTrackerOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private GameStateBridge gameStateBridge;

	@Inject
	private ItemPriceProvider itemPriceProvider;

	private LootTrackerPanel panel;
	private NavigationButton navButton;
	private final Map<Integer, LootEntry> loot = new HashMap<>();
	private final List<LootEvent> events = new ArrayList<>();

	@Provides
	LootTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LootTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		reset();
		panel = injector.getInstance(LootTrackerPanel.class);
		navButton = NavigationButton.builder()
			.tooltip("Loot Tracker")
			.icon(icon(new Color(255, 190, 60)))
			.priority(4)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		reset();
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		if (!config.trackGroundItems() || !(event.getItem() instanceof GameClient.GroundItemInfo))
		{
			return;
		}

		List<GameClient.GroundItemInfo> items = new ArrayList<>();
		items.add((GameClient.GroundItemInfo) event.getItem());
		recordEvent(items);
	}

	Collection<LootEntry> getLoot()
	{
		if (loot.isEmpty())
		{
			return Collections.emptyList();
		}

		List<LootEntry> entries = new ArrayList<>(loot.values());
		entries.sort((left, right) -> Integer.compare(right.getQuantity(), left.getQuantity()));
		return entries;
	}

	int getTotalQuantity()
	{
		int total = 0;
		for (LootEntry entry : loot.values())
		{
			total += entry.getQuantity();
		}
		return total;
	}

	Collection<LootEvent> getEvents()
	{
		if (events.isEmpty())
		{
			return Collections.emptyList();
		}

		List<LootEvent> copy = new ArrayList<>(events);
		Collections.reverse(copy);
		return copy;
	}

	int getTotalValue()
	{
		int total = 0;
		for (LootEntry entry : loot.values())
		{
			total += entry.getValue();
		}
		return total;
	}

	private void recordEvent(List<GameClient.GroundItemInfo> items)
	{
		String source = sourceFor(items);
		Map<Integer, LootEntry> eventItems = new LinkedHashMap<>();
		for (GameClient.GroundItemInfo item : items)
		{
			LootEntry entry = record(item);
			LootEntry existing = eventItems.get(item.getId());
			if (existing == null)
			{
				eventItems.put(item.getId(), new LootEntry(entry.getName(), Math.max(1, item.getQuantity()), 1, itemPriceProvider.getPrice(item)));
			}
			else
			{
				eventItems.put(item.getId(), new LootEntry(existing.getName(), existing.getQuantity() + Math.max(1, item.getQuantity()), existing.getStacks() + 1, existing.getPrice()));
			}
		}

		events.add(new LootEvent(source, System.currentTimeMillis(), new ArrayList<>(eventItems.values())));
		while (events.size() > 64)
		{
			events.remove(0);
		}
		refreshPanel();
	}

	private LootEntry record(GameClient.GroundItemInfo item)
	{
		int quantity = Math.max(1, item.getQuantity());
		LootEntry entry = loot.get(item.getId());
		if (entry == null)
		{
			entry = new LootEntry(item.getName(), quantity, 1, itemPriceProvider.getPrice(item));
			loot.put(item.getId(), entry);
			return entry;
		}

		entry = new LootEntry(entry.getName(), entry.getQuantity() + quantity, entry.getStacks() + 1, entry.getPrice());
		loot.put(item.getId(), entry);
		return entry;
	}

	private void reset()
	{
		loot.clear();
		events.clear();
		refreshPanel();
	}

	private String sourceFor(List<GameClient.GroundItemInfo> items)
	{
		if (items.isEmpty())
		{
			return "Unknown";
		}

		GameClient.GroundItemInfo first = items.get(0);
		GameClient.NpcInfo nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		GameStateBridge.RecentNpc recentNpc = gameStateBridge.findRecentNpcSource(first);
		if (recentNpc != null)
		{
			return recentNpc.getName();
		}

		for (GameClient.NpcInfo npc : client.getNpcs())
		{
			if (npc.getPlane() != first.getPlane())
			{
				continue;
			}

			int distance = Math.abs(npc.getLocalX() - first.getLocalX()) + Math.abs(npc.getLocalY() - first.getLocalY());
			if (distance < nearestDistance)
			{
				nearest = npc;
				nearestDistance = distance;
			}
		}

		return nearest != null && nearestDistance < 1024 ? nearest.getName() : "Ground items";
	}

	private void refreshPanel()
	{
		if (panel == null)
		{
			return;
		}

		Collection<LootEntry> entries = getLoot();
		int totalQuantity = getTotalQuantity();
		int totalValue = getTotalValue();
		Collection<LootEvent> eventEntries = getEvents();
		SwingUtilities.invokeLater(() -> panel.rebuild(entries, eventEntries, totalQuantity, totalValue));
	}

	private static BufferedImage icon(Color color)
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(0, 0, 0, 80));
		graphics.fillOval(2, 8, 12, 6);
		graphics.setColor(color);
		graphics.fillOval(4, 4, 8, 8);
		graphics.dispose();
		return image;
	}

	@Value
	static class LootEntry
	{
		String name;
		int quantity;
		int stacks;
		int price;

		int getValue()
		{
			return Math.max(0, price) * Math.max(1, quantity);
		}
	}

	@Value
	static class LootEvent
	{
		String source;
		long time;
		List<LootEntry> items;

		int getValue()
		{
			int value = 0;
			for (LootEntry item : items)
			{
				value += item.getValue();
			}
			return value;
		}
	}
}
