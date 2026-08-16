package net.runelite.client.plugins.qol;

import com.GameClient;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class QolItemContainers
{
	private static final int INVENTORY_CONTAINER = 93;
	private static final int BANK_CONTAINER = 95;

	private QolItemContainers()
	{
	}

	public static GameClient.ItemContainerSnapshot inventory(GameClient client)
	{
		GameClient.ItemContainerSnapshot best = null;
		for (GameClient.ItemContainerSnapshot container : client.getItemContainers())
		{
			int lowId = (int) (container.getId() & 0xffffL);
			if (lowId == INVENTORY_CONTAINER)
			{
				return container;
			}
			if (container.getCapacity() == 28 && (best == null || container.getOccupiedSlots() > best.getOccupiedSlots()))
			{
				best = container;
			}
		}
		return best;
	}

	public static GameClient.ItemContainerSnapshot bank(GameClient client)
	{
		GameClient.ItemContainerSnapshot best = null;
		for (GameClient.ItemContainerSnapshot container : client.getItemContainers())
		{
			int lowId = (int) (container.getId() & 0xffffL);
			if (lowId == BANK_CONTAINER)
			{
				return container;
			}
			if (container.getCapacity() > 28 && (best == null || container.getCapacity() > best.getCapacity()))
			{
				best = container;
			}
		}
		return best;
	}

	public static List<GameClient.ItemStackInfo> items(GameClient.ItemContainerSnapshot container)
	{
		if (container == null)
		{
			return Collections.emptyList();
		}
		return container.getItems();
	}

	public static List<GameClient.ItemStackInfo> sortedBySlot(GameClient.ItemContainerSnapshot container)
	{
		List<GameClient.ItemStackInfo> items = new java.util.ArrayList<>(items(container));
		items.sort(Comparator.comparingInt(GameClient.ItemStackInfo::getSlot));
		return items;
	}
}
