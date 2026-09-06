package net.runelite.client.plugins.qol;

import com.GameClient;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import net.runelite.client.game.ItemContainerRole;

/** Authoritative inventory geometry backed by the native widget-frame snapshot. */
public final class QolInventoryLayout
{
	public static final int SIZE = 28;
	public static final int COLUMNS = 4;
	private QolInventoryLayout() {}
	public static double scale(GameClient client)
	{
		return Math.max(0.5, Math.min(4.0, client.getInterfaceScalingFactor() / 100.0));
	}
	public static List<GameClient.VisibleItemSlot> slots(GameClient client)
	{
		java.util.ArrayList<GameClient.VisibleItemSlot> result = new java.util.ArrayList<>();
		if (client == null) return java.util.Collections.emptyList();
		for (GameClient.VisibleItemSlot slot : client.getVisibleItemSlots())
			if (slot.isAuthoritative() && slot.getRole() == ItemContainerRole.INVENTORY) result.add(slot);
		result.sort((left, right) -> Integer.compare(left.getChildIndex(), right.getChildIndex()));
		return java.util.Collections.unmodifiableList(result);
	}
	public static Rectangle slotBounds(GameClient client, int index)
	{
		if (index < 0 || index >= SIZE) return null;
		for (GameClient.VisibleItemSlot slot : slots(client))
			if (slot.getChildIndex() == index) return slot.getBounds();
		return null;
	}
	public static int slotAt(GameClient client, Point point)
	{
		if (point == null) return -1;
		for (GameClient.VisibleItemSlot slot : slots(client))
			if (slot.getBounds().contains(point)) return slot.getChildIndex();
		return -1;
	}
}
