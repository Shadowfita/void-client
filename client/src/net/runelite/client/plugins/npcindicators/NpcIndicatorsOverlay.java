package net.runelite.client.plugins.npcindicators;

import com.GameClient;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.qol.QolPatterns;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class NpcIndicatorsOverlay extends Overlay
{
	private static final BasicStroke STROKE = new BasicStroke(2f);
	private final GameClient client;
	private final NpcIndicatorsConfig config;

	@Inject
	NpcIndicatorsOverlay(GameClient client, NpcIndicatorsPlugin plugin, NpcIndicatorsConfig config)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!client.hasLocalPlayer())
		{
			return null;
		}
		List<String> patterns = QolPatterns.split(config.npcNames());
		int playerX = client.getLocalPlayerSceneX();
		int playerY = client.getLocalPlayerSceneY();
		for (GameClient.NpcInfo npc : client.getNpcs())
		{
			if (npc.getPlane() != client.getPlane())
			{
				continue;
			}
			int sceneX = npc.getLocalX() >> Perspective.LOCAL_COORD_BITS;
			int sceneY = npc.getLocalY() >> Perspective.LOCAL_COORD_BITS;
			if (Math.max(Math.abs(sceneX - playerX), Math.abs(sceneY - playerY)) > config.drawDistance())
			{
				continue;
			}
			if (!config.highlightAll() && !QolPatterns.matches(npc.getName(), patterns) && !QolPatterns.matchesId(npc.getId(), config.npcNames()))
			{
				continue;
			}

			LocalPoint localPoint = new LocalPoint(npc.getLocalX(), npc.getLocalY());
			Color color = config.highlightColor();
			Polygon tile = Perspective.getCanvasTilePoly(client, localPoint);
			if (tile != null)
			{
				Color fill = config.fillTiles() ? new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(60, color.getAlpha())) : new Color(0, 0, 0, 0);
				OverlayUtil.renderPolygon(graphics, tile, color, fill, STROKE);
			}
			if (config.showNames())
			{
				String label = npc.getName();
				if (config.showCombatLevel() && npc.getCombatLevel() >= 0)
				{
					label += " (level-" + npc.getCombatLevel() + ")";
				}
				Point location = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, npc.getHeight() + 24);
				if (location != null)
				{
					OverlayUtil.renderTextLocation(graphics, location, label, color);
				}
			}
		}
		return null;
	}
}
