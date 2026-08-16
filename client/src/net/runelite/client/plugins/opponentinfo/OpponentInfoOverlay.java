package net.runelite.client.plugins.opponentinfo;

import com.GameClient;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class OpponentInfoOverlay extends OverlayPanel
{
	private final GameClient client;
	private final OpponentInfoConfig config;

	@Inject
	OpponentInfoOverlay(GameClient client, OpponentInfoPlugin plugin, OpponentInfoConfig config)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		GameClient.OpponentInfo opponent = client.getOpponentInfo();
		if (opponent == null)
		{
			return null;
		}
		String title = opponent.getName();
		if (config.showCombatLevel() && opponent.getCombatLevel() >= 0)
		{
			title += " (level-" + opponent.getCombatLevel() + ")";
		}
		panelComponent.getChildren().add(TitleComponent.builder().text(title).color(new Color(255, 210, 90)).build());
		panelComponent.getChildren().add(LineComponent.builder().left("Target").right(opponent.isNpc() ? "NPC" : "Player").build());
		if (config.showDistance())
		{
			int sceneX = opponent.getLocalX() >> Perspective.LOCAL_COORD_BITS;
			int sceneY = opponent.getLocalY() >> Perspective.LOCAL_COORD_BITS;
			int distance = Math.max(Math.abs(sceneX - client.getLocalPlayerSceneX()), Math.abs(sceneY - client.getLocalPlayerSceneY()));
			panelComponent.getChildren().add(LineComponent.builder().left("Distance").right(distance + " tiles").build());
		}
		return super.render(graphics);
	}
}
