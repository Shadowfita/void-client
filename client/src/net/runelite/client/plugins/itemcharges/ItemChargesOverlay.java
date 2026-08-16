package net.runelite.client.plugins.itemcharges;

import com.GameClient;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.client.plugins.qol.QolInventoryLayout;
import net.runelite.client.plugins.qol.QolItemContainers;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class ItemChargesOverlay extends Overlay
{
	private static final Pattern PAREN_COUNT = Pattern.compile("\\((\\d{1,3})\\)\\s*$");
	private static final Pattern NAMED_COUNT = Pattern.compile("(?i)(\\d{1,4})\\s*(?:charges?|uses?)");
	private final GameClient client;
	private final ItemChargesConfig config;

	@Inject
	ItemChargesOverlay(GameClient client, ItemChargesPlugin plugin, ItemChargesConfig config)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.HIGHEST);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		GameClient.ItemContainerSnapshot inventory = QolItemContainers.inventory(client);
		if (inventory == null)
		{
			return null;
		}
		Font oldFont = graphics.getFont();
		graphics.setFont(oldFont.deriveFont(Font.BOLD, Math.max(10f, 10f * (float) QolInventoryLayout.scale(client))));
		for (GameClient.ItemStackInfo item : inventory.getItems())
		{
			String count = chargeText(item.getName());
			if (count == null || item.getSlot() < 0 || item.getSlot() >= QolInventoryLayout.SIZE)
			{
				continue;
			}
			Rectangle bounds = QolInventoryLayout.slotBounds(client, item.getSlot());
			int x = bounds.x + 2;
			int y = bounds.y + graphics.getFontMetrics().getAscent();
			OverlayUtil.renderTextLocation(graphics, new net.runelite.api.Point(x, y), count, config.textColor());
		}
		graphics.setFont(oldFont);
		return null;
	}

	private String chargeText(String name)
	{
		if (name == null)
		{
			return null;
		}
		if (config.showPotionDoses())
		{
			Matcher matcher = PAREN_COUNT.matcher(name);
			if (matcher.find())
			{
				return matcher.group(1);
			}
		}
		if (config.showNamedCharges())
		{
			Matcher matcher = NAMED_COUNT.matcher(name);
			if (matcher.find())
			{
				return matcher.group(1);
			}
		}
		return null;
	}
}
