package net.runelite.client.plugins.qol;

import com.GameClient;
import java.awt.Point;
import java.awt.Rectangle;

public final class QolInventoryLayout
{
	public static final int SIZE = 28;
	public static final int COLUMNS = 4;
	private static final int BASE_SLOT_SIZE = 32;
	private static final int BASE_SLOT_X_STEP = 42;
	private static final int BASE_SLOT_Y_STEP = 36;
	private static final int BASE_RIGHT_INSET = 202;
	private static final int BASE_BOTTOM_INSET = 290;

	private QolInventoryLayout()
	{
	}

	public static double scale(GameClient client)
	{
		return Math.max(0.5, Math.min(4.0, client.getInterfaceScalingFactor() / 100.0));
	}

	public static Rectangle inventoryBounds(GameClient client)
	{
		double scale = scale(client);
		int width = Math.max(1, client.getCanvasWidth());
		int height = Math.max(1, client.getCanvasHeight());
		int slotSize = scaled(BASE_SLOT_SIZE, scale);
		int xStep = scaled(BASE_SLOT_X_STEP, scale);
		int yStep = scaled(BASE_SLOT_Y_STEP, scale);
		return new Rectangle(
			Math.max(0, width - scaled(BASE_RIGHT_INSET, scale)),
			Math.max(0, height - scaled(BASE_BOTTOM_INSET, scale)),
			(COLUMNS - 1) * xStep + slotSize,
			((SIZE - 1) / COLUMNS) * yStep + slotSize);
	}

	public static Rectangle slotBounds(GameClient client, int slot)
	{
		double scale = scale(client);
		Rectangle inventory = inventoryBounds(client);
		int column = slot % COLUMNS;
		int row = slot / COLUMNS;
		int slotSize = scaled(BASE_SLOT_SIZE, scale);
		return new Rectangle(
			inventory.x + column * scaled(BASE_SLOT_X_STEP, scale),
			inventory.y + row * scaled(BASE_SLOT_Y_STEP, scale),
			slotSize,
			slotSize);
	}

	public static int slotAt(GameClient client, Point point)
	{
		if (point == null || !inventoryBounds(client).contains(point))
		{
			return -1;
		}
		for (int slot = 0; slot < SIZE; slot++)
		{
			if (slotBounds(client, slot).contains(point))
			{
				return slot;
			}
		}
		return -1;
	}

	private static int scaled(int value, double scale)
	{
		return Math.max(1, (int) Math.round(value * scale));
	}
}
