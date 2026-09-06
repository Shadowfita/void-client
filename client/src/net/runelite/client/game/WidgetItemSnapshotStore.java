package net.runelite.client.game;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Native interface-render bridge. A complete immutable frame is published only
 * after the entire root traversal; failed traversals publish an incomplete frame.
 */
public final class WidgetItemSnapshotStore
{
	private static final Object LOCK = new Object();
	private static final Map<Long, RawWidgetItem> BUILDING = new LinkedHashMap<>();
	private static volatile FrameSnapshot published = FrameSnapshot.empty();
	private static boolean frameOpen;
	private static long buildingGeneration;
	private static int buildingRootInterface = -1;

	private WidgetItemSnapshotStore() {}

	public static void beginFrame(long generation, int rootInterface)
	{
		synchronized (LOCK)
		{
			BUILDING.clear();
			buildingGeneration = generation;
			buildingRootInterface = rootInterface;
			frameOpen = true;
		}
	}

	public static void capture(int packedWidgetId, int childIndex, int widgetType,
		int itemId, int quantity, Rectangle bounds, Rectangle clipBounds)
	{
		if (childIndex < 0 || bounds == null || clipBounds == null
			|| bounds.width <= 0 || bounds.height <= 0 || !bounds.intersects(clipBounds)) return;
		synchronized (LOCK)
		{
			if (!frameOpen) return;
			long key = (((long) packedWidgetId) << 32) ^ (childIndex & 0xffffffffL);
			BUILDING.put(key, new RawWidgetItem(packedWidgetId, childIndex, widgetType,
				itemId, quantity, bounds, clipBounds, buildingGeneration));
		}
	}

	public static void endFrame() { publishFrame(true); }
	public static void abortFrame() { publishFrame(false); }

	private static void publishFrame(boolean complete)
	{
		synchronized (LOCK)
		{
			if (!frameOpen) return;
			published = new FrameSnapshot(buildingGeneration, buildingRootInterface,
				System.nanoTime(), complete, complete ? new ArrayList<>(BUILDING.values())
					: Collections.<RawWidgetItem>emptyList());
			BUILDING.clear();
			frameOpen = false;
		}
	}

	public static FrameSnapshot snapshot() { return published; }

	public static final class RawWidgetItem
	{
		private final int packedWidgetId, childIndex, widgetType, itemId, quantity;
		private final Rectangle bounds, clipBounds;
		private final long generation;

		public RawWidgetItem(int packedWidgetId, int childIndex, int widgetType,
			int itemId, int quantity, Rectangle bounds, Rectangle clipBounds, long generation)
		{
			this.packedWidgetId = packedWidgetId;
			this.childIndex = childIndex;
			this.widgetType = widgetType;
			this.itemId = itemId;
			this.quantity = quantity;
			this.bounds = new Rectangle(bounds);
			this.clipBounds = new Rectangle(clipBounds);
			this.generation = generation;
		}

		public int getPackedWidgetId() { return packedWidgetId; }
		public int getChildIndex() { return childIndex; }
		public int getWidgetType() { return widgetType; }
		public int getItemId() { return itemId; }
		public int getQuantity() { return quantity; }
		public Rectangle getBounds() { return new Rectangle(bounds); }
		public Rectangle getClipBounds() { return new Rectangle(clipBounds); }
		public long getGeneration() { return generation; }
	}

	public static final class FrameSnapshot
	{
		private final long generation, publishedAtNanos;
		private final int rootInterface;
		private final boolean complete;
		private final List<RawWidgetItem> items;

		private FrameSnapshot(long generation, int rootInterface, long publishedAtNanos,
			boolean complete, List<RawWidgetItem> items)
		{
			this.generation = generation;
			this.rootInterface = rootInterface;
			this.publishedAtNanos = publishedAtNanos;
			this.complete = complete;
			this.items = Collections.unmodifiableList(new ArrayList<>(items));
		}

		private static FrameSnapshot empty()
		{
			return new FrameSnapshot(-1L, -1, 0L, false, Collections.<RawWidgetItem>emptyList());
		}

		public long getGeneration() { return generation; }
		public int getRootInterface() { return rootInterface; }
		public long getPublishedAtNanos() { return publishedAtNanos; }
		public boolean isComplete() { return complete; }
		public List<RawWidgetItem> getItems() { return items; }
	}
}
