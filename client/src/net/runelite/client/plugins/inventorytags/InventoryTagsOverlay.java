package net.runelite.client.plugins.inventorytags;

import com.GameClient;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.client.plugins.qol.QolInventoryLayout;
import net.runelite.client.plugins.qol.QolItemContainers;
import net.runelite.client.plugins.qol.QolPatterns;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

class InventoryTagsOverlay extends Overlay
{
	private final GameClient client;
	private final InventoryTagsConfig config;

	@Inject
	InventoryTagsOverlay(GameClient client, InventoryTagsPlugin plugin, InventoryTagsConfig config)
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
		GameClient.ItemContainerSnapshot inventory = QolItemContainers.inventory(client);
		if (inventory == null)
		{
			return null;
		}
		for (GameClient.ItemStackInfo item : inventory.getItems())
		{
			if (item.getSlot() < 0 || item.getSlot() >= QolInventoryLayout.SIZE)
			{
				continue;
			}
			Color color = colorFor(item.getName());
			if (color == null)
			{
				continue;
			}
			Rectangle slot = QolInventoryLayout.slotBounds(client, item.getSlot());
			graphics.setColor(color);
			graphics.fill(slot);
			if (config.outline())
			{
				graphics.setStroke(new BasicStroke(Math.max(1f, (float) QolInventoryLayout.scale(client))));
				graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(150, color.getAlpha())));
				graphics.draw(slot);
			}
		}
		return null;
	}

	private Color colorFor(String name)
	{
		if (QolPatterns.matches(name, config.groupOne())) return config.groupOneColor();
		if (QolPatterns.matches(name, config.groupTwo())) return config.groupTwoColor();
		if (QolPatterns.matches(name, config.groupThree())) return config.groupThreeColor();
		if (QolPatterns.matches(name, config.groupFour())) return config.groupFourColor();
		return null;
	}
}
