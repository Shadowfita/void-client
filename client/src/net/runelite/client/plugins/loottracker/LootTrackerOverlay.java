package net.runelite.client.plugins.loottracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

class LootTrackerOverlay extends OverlayPanel
{
	private final LootTrackerPlugin plugin;
	private final LootTrackerConfig config;

	@Inject
	LootTrackerOverlay(LootTrackerPlugin plugin, LootTrackerConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		setPriority(OverlayPriority.LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Loot Tracker")
			.right(format(plugin.getTotalQuantity()))
			.leftColor(Color.WHITE)
			.rightColor(new Color(255, 235, 135))
			.build());

		int rows = 0;
		for (LootTrackerPlugin.LootEntry entry : plugin.getLoot())
		{
			if (rows++ >= config.maxRows())
			{
				break;
			}

			panelComponent.getChildren().add(LineComponent.builder()
				.left(entry.getName())
				.right(format(entry.getQuantity()))
				.build());
		}

		return super.render(graphics);
	}

	private static String format(int value)
	{
		return String.format("%,d", value);
	}
}
