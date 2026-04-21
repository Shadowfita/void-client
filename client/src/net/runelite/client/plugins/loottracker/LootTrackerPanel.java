package net.runelite.client.plugins.loottracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class LootTrackerPanel extends PluginPanel
{
	private final JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
	private final JLabel summary = new JLabel("No drops tracked yet", SwingConstants.CENTER);

	LootTrackerPanel()
	{
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Loot Tracker");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		add(title);

		summary.setForeground(Color.LIGHT_GRAY);
		summary.setFont(FontManager.getRunescapeSmallFont());
		add(summary);

		list.setOpaque(false);
		add(list);
	}

	void rebuild(Collection<LootTrackerPlugin.LootEntry> entries, Collection<LootTrackerPlugin.LootEvent> events, int totalQuantity, int totalValue)
	{
		list.removeAll();
		summary.setText(totalQuantity <= 0 ? "No drops tracked yet" : String.format("%,d items - %,d gp", totalQuantity, totalValue));

		if (!events.isEmpty())
		{
			for (LootTrackerPlugin.LootEvent event : events)
			{
				list.add(eventRow(event));
			}
		}
		else
		{
			for (LootTrackerPlugin.LootEntry entry : entries)
			{
				list.add(itemRow(entry));
			}
		}

		revalidate();
		repaint();
	}

	private static JPanel eventRow(LootTrackerPlugin.LootEvent event)
	{
		JPanel group = new JPanel(new BorderLayout(0, 4));
		group.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		group.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel title = new JLabel(event.getSource());
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

		JLabel value = new JLabel(String.format("%,d gp", event.getValue()), SwingConstants.RIGHT);
		value.setForeground(new Color(255, 235, 135));
		value.setFont(FontManager.getRunescapeSmallFont());

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.add(title, BorderLayout.CENTER);
		header.add(value, BorderLayout.EAST);
		group.add(header, BorderLayout.NORTH);

		JPanel items = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
		items.setOpaque(false);
		for (LootTrackerPlugin.LootEntry item : event.getItems())
		{
			items.add(itemRow(item));
		}
		group.add(items, BorderLayout.CENTER);
		group.add(Box.createVerticalStrut(2), BorderLayout.SOUTH);
		return group;
	}

	private static JPanel itemRow(LootTrackerPlugin.LootEntry entry)
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
		row.setOpaque(false);
		row.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 32, 24));

		JLabel name = new JLabel(entry.getName());
		name.setForeground(Color.WHITE);
		name.setFont(FontManager.getRunescapeSmallFont());

		String right = String.format("%,d", entry.getQuantity());
		if (entry.getValue() > 0)
		{
			right += " / " + String.format("%,d", entry.getValue());
		}
		JLabel quantity = new JLabel(right, SwingConstants.RIGHT);
		quantity.setForeground(new Color(255, 235, 135));
		quantity.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));

		row.add(name, BorderLayout.CENTER);
		row.add(quantity, BorderLayout.EAST);
		return row;
	}
}
