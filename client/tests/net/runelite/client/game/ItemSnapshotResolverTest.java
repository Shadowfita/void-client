package net.runelite.client.game;

import com.GameClient;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemSnapshotResolverTest
{
	private static GameClient.ItemContainerSnapshot inventory(int firstItem)
	{
		List<GameClient.ItemStackInfo> items = new ArrayList<>();
		if (firstItem >= 0) items.add(new GameClient.ItemStackInfo(firstItem, 1, "Item", 1, 0));
		return new GameClient.ItemContainerSnapshot(93L, items, 28,
			ItemContainerRole.INVENTORY, true, 1L);
	}

	private static WidgetItemSnapshotStore.FrameSnapshot frame(int widget, int firstItem)
	{
		WidgetItemSnapshotStore.beginFrame(1L, 746);
		for (int i = 0; i < 28; i++)
		{
			int item = i == 0 ? firstItem : -1;
			Rectangle bounds = new Rectangle(500 + (i % 4) * 42, 200 + (i / 4) * 36, 32, 32);
			WidgetItemSnapshotStore.capture(widget, i, 5, item, item < 0 ? 0 : 1,
				bounds, new Rectangle(0,0,800,600));
		}
		WidgetItemSnapshotStore.endFrame();
		return WidgetItemSnapshotStore.snapshot();
	}

	@Test public void resolvesExactInventoryGeometry()
	{
		ItemSnapshotResolver.Resolution value = ItemSnapshotResolver.resolve(
			ItemContainerRole.INVENTORY, inventory(4151), frame(14942208, 4151));
		assertTrue(value.getReason(), value.isAvailable());
		assertEquals(28, value.getSlots().size());
		assertEquals(14942208, value.getPackedWidgetId());
	}

	@Test public void rejectsAnyItemIdentityMismatch()
	{
		ItemSnapshotResolver.Resolution value = ItemSnapshotResolver.resolve(
			ItemContainerRole.INVENTORY, inventory(4151), frame(14942208, 995));
		assertFalse(value.isAvailable());
	}

	@Test public void rejectsEquallyMatchingWidgetGroups()
	{
		WidgetItemSnapshotStore.beginFrame(2L, 746);
		for (int widget : new int[]{100,200})
			for (int i = 0; i < 28; i++)
				WidgetItemSnapshotStore.capture(widget, i, 5, i == 0 ? 4151 : -1,
					i == 0 ? 1 : 0, new Rectangle(i*2, i*2, 32,32), new Rectangle(0,0,800,600));
		WidgetItemSnapshotStore.endFrame();
		ItemSnapshotResolver.Resolution value = ItemSnapshotResolver.resolve(
			ItemContainerRole.INVENTORY, inventory(4151), WidgetItemSnapshotStore.snapshot());
		assertFalse(value.isAvailable());
		assertTrue(value.getReason().contains("Multiple native widget groups"));
	}

	@Test public void rejectsAnAbortedWidgetFrame()
	{
		WidgetItemSnapshotStore.beginFrame(3L, 746);
		WidgetItemSnapshotStore.capture(100, 0, 5, 4151, 1,
			new Rectangle(0,0,32,32), new Rectangle(0,0,800,600));
		WidgetItemSnapshotStore.abortFrame();
		ItemSnapshotResolver.Resolution value = ItemSnapshotResolver.resolve(
			ItemContainerRole.INVENTORY, inventory(4151), WidgetItemSnapshotStore.snapshot());
		assertFalse(value.isAvailable());
		assertTrue(value.getReason().contains("incomplete"));
	}
}
