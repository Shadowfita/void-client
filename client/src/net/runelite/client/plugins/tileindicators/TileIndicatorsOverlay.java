package net.runelite.client.plugins.tileindicators;

import com.GameClient;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class TileIndicatorsOverlay extends Overlay
{
	private static final BasicStroke STROKE = new BasicStroke(2);

	private final GameClient client;
	private final TileIndicatorsConfig config;

	@Inject
	TileIndicatorsOverlay(GameClient client, TileIndicatorsPlugin plugin, TileIndicatorsConfig config)
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
		if (!config.highlightTrueTile() || !client.hasLocalPlayer())
		{
			return null;
		}

		int sceneX = client.getLocalPlayerSceneX();
		int sceneY = client.getLocalPlayerSceneY();
		if (sceneX < 0 || sceneY < 0)
		{
			return null;
		}

		LocalPoint localPoint = LocalPoint.fromScene(sceneX, sceneY);
		Polygon polygon = Perspective.getCanvasTileAreaPoly(client, localPoint, Math.max(1, client.getLocalPlayerSize()));
		if (polygon != null)
		{
			Color color = config.trueTileColor();
			Color fill = config.tileFill() ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 35) : new Color(0, 0, 0, 0);
			OverlayUtil.renderPolygon(graphics, polygon, color, fill, STROKE);
			if (config.labelTrueTile())
			{
				net.runelite.api.Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, "True tile", 0);
				if (textLocation != null)
				{
					OverlayUtil.renderTextLocation(graphics, textLocation, "True tile", color);
				}
			}
		}

		return null;
	}
}
