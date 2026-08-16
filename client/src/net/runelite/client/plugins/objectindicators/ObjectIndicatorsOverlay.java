package net.runelite.client.plugins.objectindicators;

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
import net.runelite.client.game.GameEventBridgeHooks;
import net.runelite.client.plugins.qol.QolPatterns;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class ObjectIndicatorsOverlay extends Overlay
{
	private static final BasicStroke STROKE = new BasicStroke(2f);
	private final GameClient client;
	private final ObjectIndicatorsPlugin plugin;
	private final ObjectIndicatorsConfig config;

	@Inject
	ObjectIndicatorsOverlay(GameClient client, ObjectIndicatorsPlugin plugin, ObjectIndicatorsConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
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
		List<String> patterns = QolPatterns.split(config.objects());
		int playerX = client.getLocalPlayerSceneX();
		int playerY = client.getLocalPlayerSceneY();
		for (GameEventBridgeHooks.TileObjectInfo object : plugin.getObjects())
		{
			if (object.getPlane() != client.getPlane())
			{
				continue;
			}
			int sceneX = object.getLocalX() < 104 ? object.getLocalX() : object.getLocalX() >> Perspective.LOCAL_COORD_BITS;
			int sceneY = object.getLocalY() < 104 ? object.getLocalY() : object.getLocalY() >> Perspective.LOCAL_COORD_BITS;
			if (Math.max(Math.abs(sceneX - playerX), Math.abs(sceneY - playerY)) > config.drawDistance())
			{
				continue;
			}
			String name = client.getObjectName(object.getId());
			if (!config.highlightAll() && !QolPatterns.matches(name, patterns) && !QolPatterns.matchesId(object.getId(), config.objects()))
			{
				continue;
			}

			LocalPoint local = LocalPoint.fromScene(sceneX, sceneY);
			Polygon tile = Perspective.getCanvasTilePoly(client, local);
			Color color = config.highlightColor();
			if (tile != null)
			{
				OverlayUtil.renderPolygon(graphics, tile, color, new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(55, color.getAlpha())), STROKE);
			}
			if (config.showNames())
			{
				Point point = Perspective.getCanvasTextLocation(client, graphics, local, name, 12);
				if (point != null)
				{
					OverlayUtil.renderTextLocation(graphics, point, name, color);
				}
			}
		}
		return null;
	}
}
