package net.runelite.client.game;

import com.GameClient;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Correlates native dynamic widget children to one exact native container. */
public final class ItemSnapshotResolver
{
	private ItemSnapshotResolver() {}

	public static Resolution resolve(ItemContainerRole role,
		GameClient.ItemContainerSnapshot container, WidgetItemSnapshotStore.FrameSnapshot frame)
	{
		if (role == null || role == ItemContainerRole.UNKNOWN)
			return Resolution.unavailable(role, "No authoritative role requested");
		if (container == null || !container.isAuthoritative() || container.getRole() != role)
			return Resolution.unavailable(role, "Exact native container has not been observed");
		if (frame == null || !frame.isComplete())
			return Resolution.unavailable(role, "The latest native widget frame is incomplete");
		if (frame.getItems().isEmpty())
			return Resolution.unavailable(role, "No visible native widget items were captured");

		Map<Integer, Map<Integer, WidgetItemSnapshotStore.RawWidgetItem>> groups = new LinkedHashMap<>();
		for (WidgetItemSnapshotStore.RawWidgetItem raw : frame.getItems())
		{
			if (raw.getChildIndex() < 0 || raw.getBounds().width <= 0 || raw.getBounds().height <= 0) continue;
			groups.computeIfAbsent(raw.getPackedWidgetId(), ignored -> new LinkedHashMap<>())
				.put(raw.getChildIndex(), raw);
		}

		List<Candidate> candidates = new ArrayList<>();
		for (Map.Entry<Integer, Map<Integer, WidgetItemSnapshotStore.RawWidgetItem>> entry : groups.entrySet())
		{
			Candidate candidate = score(role, container, entry.getKey(), entry.getValue());
			if (candidate.acceptable) candidates.add(candidate);
		}
		if (candidates.isEmpty())
			return Resolution.unavailable(role, "No visible widget group exactly matched container " + container.getId());

		candidates.sort(Comparator.comparingInt(Candidate::rank).reversed());
		Candidate best = candidates.get(0);
		if (candidates.size() > 1 && candidates.get(1).rank() == best.rank())
			return Resolution.unavailable(role, "Multiple native widget groups matched equally; refusing ambiguous geometry");

		List<GameClient.VisibleItemSlot> slots = new ArrayList<>();
		List<Integer> indexes = new ArrayList<>(best.widgets.keySet());
		Collections.sort(indexes);
		for (Integer index : indexes)
		{
			WidgetItemSnapshotStore.RawWidgetItem raw = best.widgets.get(index);
			GameClient.ItemStackInfo expected = container.getItemAt(index);
			int itemId = expected == null ? -1 : expected.getId();
			int quantity = expected == null ? 0 : expected.getQuantity();
			Rectangle bounds = raw.getBounds().intersection(raw.getClipBounds());
			if (bounds.width <= 0 || bounds.height <= 0) continue;
			slots.add(new GameClient.VisibleItemSlot(raw.getPackedWidgetId(), index,
				container.getId(), role, itemId, quantity, bounds, raw.getClipBounds(),
				frame.getGeneration(), true));
		}
		return new Resolution(role, true, "Matched widget " + best.widgetId + " with "
			+ best.coverage + " visible slots and " + best.itemMatches + " occupied-item matches",
			best.widgetId, slots);
	}

	private static Candidate score(ItemContainerRole role, GameClient.ItemContainerSnapshot container,
		int widgetId, Map<Integer, WidgetItemSnapshotStore.RawWidgetItem> widgets)
	{
		int maxSlots = role == ItemContainerRole.INVENTORY ? 28 : container.getCapacity();
		int coverage = 0, idMismatches = 0, quantityMismatches = 0, itemMatches = 0, emptyMatches = 0;
		for (Map.Entry<Integer, WidgetItemSnapshotStore.RawWidgetItem> entry : widgets.entrySet())
		{
			int slot = entry.getKey();
			if (slot < 0 || slot >= maxSlots) continue;
			coverage++;
			WidgetItemSnapshotStore.RawWidgetItem raw = entry.getValue();
			GameClient.ItemStackInfo expected = container.getItemAt(slot);
			int expectedId = expected == null ? -1 : expected.getId();
			if (raw.getItemId() != expectedId) { idMismatches++; continue; }
			if (expectedId < 0) emptyMatches++;
			else
			{
				itemMatches++;
				if (raw.getQuantity() > 0 && raw.getQuantity() != expected.getQuantity()) quantityMismatches++;
			}
		}
		boolean acceptable;
		if (role == ItemContainerRole.INVENTORY) acceptable = coverage == 28 && idMismatches == 0;
		else
		{
			int minimumCoverage = Math.min(8, Math.max(1, container.getCapacity()));
			int requiredItemMatches = Math.min(3, Math.max(1, container.getOccupiedSlots()));
			acceptable = coverage >= minimumCoverage && idMismatches == 0
				&& (container.getOccupiedSlots() == 0 || itemMatches >= requiredItemMatches);
		}
		return new Candidate(widgetId, widgets, coverage, itemMatches, emptyMatches,
			idMismatches, quantityMismatches, acceptable);
	}

	private static final class Candidate
	{
		private final int widgetId, coverage, itemMatches, emptyMatches, idMismatches, quantityMismatches;
		private final Map<Integer, WidgetItemSnapshotStore.RawWidgetItem> widgets;
		private final boolean acceptable;
		private Candidate(int widgetId, Map<Integer, WidgetItemSnapshotStore.RawWidgetItem> widgets,
			int coverage, int itemMatches, int emptyMatches, int idMismatches,
			int quantityMismatches, boolean acceptable)
		{
			this.widgetId = widgetId; this.widgets = widgets; this.coverage = coverage;
			this.itemMatches = itemMatches; this.emptyMatches = emptyMatches;
			this.idMismatches = idMismatches; this.quantityMismatches = quantityMismatches;
			this.acceptable = acceptable;
		}
		private int rank()
		{
			return coverage * 1000 + itemMatches * 100 + emptyMatches * 2
				- idMismatches * 10000 - quantityMismatches;
		}
	}

	public static final class Resolution
	{
		private final ItemContainerRole role;
		private final boolean available;
		private final String reason;
		private final int packedWidgetId;
		private final List<GameClient.VisibleItemSlot> slots;
		private Resolution(ItemContainerRole role, boolean available, String reason,
			int packedWidgetId, List<GameClient.VisibleItemSlot> slots)
		{
			this.role = role == null ? ItemContainerRole.UNKNOWN : role;
			this.available = available; this.reason = reason; this.packedWidgetId = packedWidgetId;
			this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
		}
		private static Resolution unavailable(ItemContainerRole role, String reason)
		{
			return new Resolution(role, false, reason, -1, Collections.<GameClient.VisibleItemSlot>emptyList());
		}
		public ItemContainerRole getRole() { return role; }
		public boolean isAvailable() { return available; }
		public String getReason() { return reason; }
		public int getPackedWidgetId() { return packedWidgetId; }
		public List<GameClient.VisibleItemSlot> getSlots() { return slots; }
	}
}
