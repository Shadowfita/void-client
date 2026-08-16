package net.runelite.client.plugins.grounditems;

import com.GameClient;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.ItemPriceProvider;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class GroundItemNamesOverlay extends Overlay
{
	private static final BasicStroke TILE_STROKE = new BasicStroke(1.5f);

	private final GameClient client;
	private final GroundItemNamesConfig config;
	private final ItemPriceProvider itemPriceProvider;

	@Inject
	GroundItemNamesOverlay(GameClient client, GroundItemNamesPlugin plugin, GroundItemNamesConfig config, ItemPriceProvider itemPriceProvider)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		this.itemPriceProvider = itemPriceProvider;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showNames() || !client.hasLocalPlayer())
		{
			return null;
		}

		int localSceneX = client.getLocalPlayerSceneX();
		int localSceneY = client.getLocalPlayerSceneY();
		int maxDistance = config.drawDistance();
		Map<String, Integer> drawnPerTile = new HashMap<>();
		List<String> highlighted = itemPatterns(config.highlightedItems());
		List<String> hidden = itemPatterns(config.hiddenItems());

		for (GameClient.GroundItemInfo item : client.getGroundItems())
		{
			if (item.getPlane() != client.getPlane())
			{
				continue;
			}

			int sceneX = item.getLocalX() >> Perspective.LOCAL_COORD_BITS;
			int sceneY = item.getLocalY() >> Perspective.LOCAL_COORD_BITS;
			if (Math.max(Math.abs(sceneX - localSceneX), Math.abs(sceneY - localSceneY)) > maxDistance)
			{
				continue;
			}

			Color color = colorFor(item, highlighted, hidden);
			if (color == null)
			{
				continue;
			}

			String key = sceneX + ":" + sceneY;
			int offset = drawnPerTile.getOrDefault(key, 0);
			if (offset >= config.maxItemsPerTile())
			{
				continue;
			}
			drawnPerTile.put(key, offset + 1);

			LocalPoint localPoint = new LocalPoint(item.getLocalX(), item.getLocalY());
			if (config.highlightTiles() && offset == 0)
			{
				Polygon tile = Perspective.getCanvasTilePoly(client, localPoint);
				if (tile != null)
				{
					OverlayUtil.renderPolygon(graphics, tile, color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 35), TILE_STROKE);
				}
			}

			String text = item.getName();
			if (config.showQuantities() && item.getQuantity() > 1)
			{
				text += " x" + item.getQuantity();
			}
			int stackValue = stackValue(item);
			if (config.showPrices() && stackValue > 0)
			{
				text += " (" + format(stackValue) + " gp)";
			}

			Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, text, 20 + offset * 14);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLocation, text, color);
			}
		}

		return null;
	}

	private Color colorFor(GameClient.GroundItemInfo item, List<String> highlighted, List<String> hidden)
	{
		String name = item.getName();
		boolean isHighlighted = matches(name, highlighted);
		boolean isHidden = matches(name, hidden);

		if (isHidden && !isHighlighted)
		{
			return null;
		}
		if (config.showHighlightedOnly() && !isHighlighted)
		{
			return null;
		}
		int value = stackValue(item);
		if (!isHighlighted && value < config.hideUnderValue())
		{
			return null;
		}
		if (isHighlighted)
		{
			return config.highlightedColor();
		}

		if (value >= config.highValuePrice())
		{
			return config.highValueColor();
		}
		if (value >= config.mediumValuePrice())
		{
			return config.mediumValueColor();
		}
		if (value >= config.lowValuePrice())
		{
			return config.lowValueColor();
		}
		return config.textColor();
	}

	private int stackValue(GameClient.GroundItemInfo item)
	{
		return Math.max(1, item.getQuantity()) * itemPriceProvider.getPrice(item);
	}

	private static List<String> itemPatterns(String input)
	{
		List<String> patterns = new ArrayList<>();
		if (input == null || input.trim().isEmpty())
		{
			return patterns;
		}

		for (String token : input.split(","))
		{
			String pattern = token.trim().toLowerCase();
			if (!pattern.isEmpty())
			{
				patterns.add(pattern);
			}
		}
		return patterns;
	}

	private static boolean matches(String name, List<String> patterns)
	{
		String lower = name == null ? "" : name.toLowerCase();
		for (String pattern : patterns)
		{
			if (pattern.indexOf('*') >= 0)
			{
				String regex = pattern.replace(".", "\\.").replace("*", ".*");
				if (lower.matches(regex))
				{
					return true;
				}
			}
			else if (lower.equals(pattern))
			{
				return true;
			}
		}
		return false;
	}

	private static String format(int value)
	{
		return String.format("%,d", value);
	}
}
