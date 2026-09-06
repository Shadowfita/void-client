package net.runelite.client.plugins.qol;

import com.GameClient;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.runelite.client.game.ItemContainerRole;

public final class QolItemContainers
{
	private QolItemContainers() {}
	public static GameClient.ItemContainerSnapshot inventory(GameClient client)
	{
		return client == null ? null : client.getItemContainer(ItemContainerRole.INVENTORY);
	}
	public static GameClient.ItemContainerSnapshot bank(GameClient client)
	{
		return client == null ? null : client.getItemContainer(ItemContainerRole.BANK);
	}
	public static List<GameClient.ItemStackInfo> items(GameClient.ItemContainerSnapshot container)
	{
		return container == null ? Collections.<GameClient.ItemStackInfo>emptyList() : container.getItems();
	}
	public static List<GameClient.ItemStackInfo> sortedBySlot(GameClient.ItemContainerSnapshot container)
	{
		List<GameClient.ItemStackInfo> items = new java.util.ArrayList<>(items(container));
		items.sort(Comparator.comparingInt(GameClient.ItemStackInfo::getSlot));
		return items;
	}
}
