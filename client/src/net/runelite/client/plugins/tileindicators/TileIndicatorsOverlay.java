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
		if (!client.hasLocalPlayer())
		{
			return null;
		}

		int trueX = client.getLocalPlayerSceneX();
		int trueY = client.getLocalPlayerSceneY();
		if (config.highlightTrueTile())
		{
			renderTile(graphics, trueX, trueY, Math.max(1, client.getLocalPlayerSize()), config.trueTileColor(), config.labelTrueTile() ? "True tile" : null);
		}

		int destinationX = client.getLocalPlayerDestinationSceneX();
		int destinationY = client.getLocalPlayerDestinationSceneY();
		if (config.highlightDestinationTile() && (destinationX != trueX || destinationY != trueY))
		{
			renderTile(graphics, destinationX, destinationY, 1, config.destinationTileColor(), config.labelTrueTile() ? "Destination" : null);
		}

		for (String token : config.markedTiles().split("[,;\\n]"))
		{
			String[] coordinates = token.trim().split(":");
			if (coordinates.length != 2)
			{
				continue;
			}
			try
			{
				renderTile(graphics, Integer.parseInt(coordinates[0].trim()), Integer.parseInt(coordinates[1].trim()), 1, config.markedTileColor(), null);
			}
			catch (NumberFormatException ignored)
			{
			}
		}
		return null;
	}

	private void renderTile(Graphics2D graphics, int sceneX, int sceneY, int size, Color color, String label)
	{
		if (sceneX < 0 || sceneY < 0 || sceneX >= 104 || sceneY >= 104)
		{
			return;
		}
		LocalPoint localPoint = LocalPoint.fromScene(sceneX, sceneY);
		Polygon polygon = Perspective.getCanvasTileAreaPoly(client, localPoint, size);
		if (polygon == null)
		{
			return;
		}
		Color fill = config.tileFill() ? new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(80, color.getAlpha())) : new Color(0, 0, 0, 0);
		OverlayUtil.renderPolygon(graphics, polygon, color, fill, STROKE);
		if (label != null)
		{
			net.runelite.api.Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 0);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLocation, label, color);
			}
		}
	}
}
