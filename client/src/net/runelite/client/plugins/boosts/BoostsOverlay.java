package net.runelite.client.plugins.boosts;

import com.GameClient;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class BoostsOverlay extends OverlayPanel
{
	private final GameClient client;
	private final BoostsConfig config;

	@Inject
	BoostsOverlay(GameClient client, BoostsPlugin plugin, BoostsConfig config)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showBoosts() || !client.hasLocalPlayer())
		{
			return null;
		}
		int changed = 0;
		for (GameClient.SkillSnapshot snapshot : client.getSkillSnapshots())
		{
			int difference = snapshot.getBoostedLevel() - snapshot.getLevel();
			if (difference == 0)
			{
				continue;
			}
			if (changed++ == 0)
			{
				panelComponent.getChildren().add(TitleComponent.builder().text("Stat Changes").build());
			}
			String right = Integer.toString(snapshot.getBoostedLevel());
			if (config.showRelative())
			{
				right += difference > 0 ? " (+" + difference + ")" : " (" + difference + ")";
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left(snapshot.getSkill().getName())
				.right(right)
				.rightColor(difference > 0 ? new Color(85, 220, 100) : new Color(235, 80, 80))
				.build());
		}
		return changed == 0 ? null : super.render(graphics);
	}
}
