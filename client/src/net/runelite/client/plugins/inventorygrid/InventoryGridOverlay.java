package net.runelite.client.plugins.inventorygrid;

import com.GameClient;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.game.ItemContainerRole;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.util.AsyncBufferedImage;

class InventoryGridOverlay extends Overlay implements MouseListener
{
	private static final int INVENTORY_SIZE = 28;
	private static final int HOVER_DISTANCE = 5;
	private final InventoryGridConfig config;
	private final GameClient client;
	private final ItemManager itemManager;
	private final List<GameClient.VisibleItemSlot> lastItems = new ArrayList<>();
	private Point mousePoint, initialMousePoint;
	private GameClient.VisibleItemSlot draggedItem;
	private boolean mouseDown, hoverActive;

	@Inject
	InventoryGridOverlay(InventoryGridPlugin plugin, InventoryGridConfig config,
		GameClient client, ItemManager itemManager)
	{
		super(plugin); this.config = config; this.client = client; this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.HIGH);
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		List<GameClient.VisibleItemSlot> items = currentItems();
		if (items.size() == INVENTORY_SIZE) { lastItems.clear(); lastItems.addAll(items); }
		if (!mouseDown || mousePoint == null || initialMousePoint == null || draggedItem == null) return null;
		if (!hoverActive && initialMousePoint.distance(mousePoint) < HOVER_DISTANCE) return null;
		hoverActive = true;
		if (items.size() != INVENTORY_SIZE) items = new ArrayList<>(lastItems);
		if (items.size() != INVENTORY_SIZE) return null;
		Rectangle initial = draggedItem.getBounds();
		if (initial.width <= 0 || initial.height <= 0) return null;
		for (GameClient.VisibleItemSlot target : items)
		{
			Rectangle bounds = target.getBounds();
			boolean inside = bounds.contains(mousePoint);
			if (config.showItem() && inside) { drawItem(graphics, bounds, draggedItem); drawItem(graphics, initial, target); }
			if (config.showHighlight() && inside) { graphics.setColor(config.highlightColor()); graphics.fill(bounds); }
			else if (config.showGrid()) { graphics.setColor(config.gridColor()); graphics.fill(bounds); }
		}
		return null;
	}

	private List<GameClient.VisibleItemSlot> currentItems()
	{
		List<GameClient.VisibleItemSlot> result = new ArrayList<>();
		for (GameClient.VisibleItemSlot slot : client.getVisibleItemSlots())
			if (slot.isAuthoritative() && slot.getRole() == ItemContainerRole.INVENTORY
				&& slot.getChildIndex() >= 0 && slot.getChildIndex() < INVENTORY_SIZE) result.add(slot);
		result.sort(Comparator.comparingInt(GameClient.VisibleItemSlot::getChildIndex));
		return result;
	}

	private static GameClient.VisibleItemSlot findAt(List<GameClient.VisibleItemSlot> items, Point point)
	{
		if (point == null) return null;
		for (GameClient.VisibleItemSlot item : items)
			if (item.isOccupied() && item.getBounds().contains(point)) return item;
		return null;
	}

	@Override public MouseEvent mouseClicked(MouseEvent event) { return event; }
	@Override public MouseEvent mousePressed(MouseEvent event)
	{
		mousePoint = event.getPoint(); initialMousePoint = event.getPoint(); hoverActive = false;
		List<GameClient.VisibleItemSlot> items = currentItems();
		if (items.size() == INVENTORY_SIZE) { lastItems.clear(); lastItems.addAll(items); }
		draggedItem = findAt(lastItems, mousePoint); mouseDown = draggedItem != null;
		if (!mouseDown) initialMousePoint = null;
		return event;
	}
	@Override public MouseEvent mouseReleased(MouseEvent event) { resetDrag(); return event; }
	@Override public MouseEvent mouseEntered(MouseEvent event) { return event; }
	@Override public MouseEvent mouseExited(MouseEvent event) { resetDrag(); return event; }
	@Override public MouseEvent mouseDragged(MouseEvent event) { mousePoint = event.getPoint(); return event; }
	@Override public MouseEvent mouseMoved(MouseEvent event) { mousePoint = event.getPoint(); return event; }
	void resetDrag() { mouseDown = false; hoverActive = false; mousePoint = null; initialMousePoint = null; draggedItem = null; }

	private void drawItem(Graphics2D graphics, Rectangle bounds, GameClient.VisibleItemSlot item)
	{
		if (bounds == null || item == null || !item.isOccupied()) return;
		AsyncBufferedImage image;
		try { image = itemManager.getImage(item.getItemId(), Math.max(1, item.getQuantity()), false); }
		catch (RuntimeException ex) { return; }
		if (image == null) return;
		Composite previous = graphics.getComposite();
		graphics.setComposite(AlphaComposite.SrcOver.derive(0.3f));
		graphics.drawImage((BufferedImage) image, bounds.x, bounds.y, bounds.width, bounds.height, null);
		graphics.setComposite(previous);
	}
}
