package net.runelite.client.plugins.xptracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

class XpTrackerOverlay extends OverlayPanel
{
	private static final int MAX_ROWS = 6;

	private final XpTrackerPlugin plugin;

	@Inject
	XpTrackerOverlay(XpTrackerPlugin plugin)
	{
		super(plugin);
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_RIGHT);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		setPriority(OverlayPriority.LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		String total = format(plugin.getTotalGained());
		total += " / " + format(plugin.getXpPerHour()) + " xp / hr";

		panelComponent.getChildren().add(LineComponent.builder()
			.left("XP Tracker")
			.right(total)
			.leftColor(Color.WHITE)
			.rightColor(new Color(135, 220, 255))
			.build());

		int rows = 0;
		for (XpTrackerPlugin.XpState state : plugin.getSkills())
		{
			if (state.getGained() <= 0 || rows++ >= MAX_ROWS)
			{
				continue;
			}

			panelComponent.getChildren().add(LineComponent.builder()
				.left(state.getSkill().getName())
				.right(format(state.getGained()))
				.build());
		}

		return super.render(graphics);
	}

	private static String format(int value)
	{
		return String.format("%,d", value);
	}
}
