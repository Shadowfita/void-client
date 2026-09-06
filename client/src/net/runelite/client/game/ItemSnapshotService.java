package net.runelite.client.game;

import com.GameClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.events.ClientTick;
import net.runelite.client.eventbus.Subscribe;

/** Publishes immutable, client-thread-captured item container and widget snapshots. */
@Singleton
public class ItemSnapshotService
{
	private final GameClient client;
	private volatile Snapshot snapshot = Snapshot.empty();

	@Inject
	private ItemSnapshotService(GameClient client) { this.client = client; }

	@Subscribe
	public void onClientTick(ClientTick ignored) { refresh(); }

	private void refresh()
	{
		List<GameClient.ItemContainerSnapshot> containers =
			Collections.unmodifiableList(new ArrayList<>(client.getItemContainers()));
		Map<ItemContainerRole, GameClient.ItemContainerSnapshot> exact = new EnumMap<>(ItemContainerRole.class);
		Map<ItemContainerRole, Boolean> duplicate = new EnumMap<>(ItemContainerRole.class);
		for (GameClient.ItemContainerSnapshot container : containers)
		{
			ItemContainerRole role = container.getRole();
			if (role == ItemContainerRole.UNKNOWN || !container.isAuthoritative()) continue;
			if (exact.containsKey(role)) { duplicate.put(role, true); exact.remove(role); }
			else if (!Boolean.TRUE.equals(duplicate.get(role))) exact.put(role, container);
		}

		WidgetItemSnapshotStore.FrameSnapshot frame = WidgetItemSnapshotStore.snapshot();
		ItemSnapshotResolver.Resolution inventory = ItemSnapshotResolver.resolve(
			ItemContainerRole.INVENTORY, exact.get(ItemContainerRole.INVENTORY), frame);
		ItemSnapshotResolver.Resolution bank = ItemSnapshotResolver.resolve(
			ItemContainerRole.BANK, exact.get(ItemContainerRole.BANK), frame);
		List<GameClient.VisibleItemSlot> slots = new ArrayList<>();
		slots.addAll(inventory.getSlots());
		slots.addAll(bank.getSlots());
		ItemSnapshotDiagnostics diagnostics = new ItemSnapshotDiagnostics(frame.getGeneration(),
			frame.getRootInterface(), frame.getItems().size(),
			describe(exact.get(ItemContainerRole.INVENTORY), duplicate.get(ItemContainerRole.INVENTORY)),
			describe(exact.get(ItemContainerRole.BANK), duplicate.get(ItemContainerRole.BANK)),
			inventory.getReason(), bank.getReason());
		snapshot = new Snapshot(containers, exact, slots, inventory, bank, diagnostics);
	}

	private static String describe(GameClient.ItemContainerSnapshot value, Boolean duplicate)
	{
		if (Boolean.TRUE.equals(duplicate)) return "Ambiguous: multiple exact native containers";
		if (value == null) return "Not observed";
		return "id=" + value.getId() + ", capacity=" + value.getCapacity()
			+ ", occupied=" + value.getOccupiedSlots();
	}

	public List<GameClient.ItemContainerSnapshot> getContainers() { return snapshot.containers; }
	public GameClient.ItemContainerSnapshot getContainer(ItemContainerRole role) { return snapshot.exact.get(role); }
	public List<GameClient.VisibleItemSlot> getVisibleItemSlots() { return snapshot.slots; }
	public long getGeneration() { return snapshot.diagnostics.getWidgetGeneration(); }
	public ItemSnapshotDiagnostics getDiagnostics() { return snapshot.diagnostics; }
	public boolean hasAuthoritativeContainer(ItemContainerRole role) { return getContainer(role) != null; }
	public boolean hasAuthoritativeGeometry(ItemContainerRole role)
	{
		return role == ItemContainerRole.INVENTORY ? snapshot.inventory.isAvailable()
			: role == ItemContainerRole.BANK && snapshot.bank.isAvailable();
	}

	private static final class Snapshot
	{
		private final List<GameClient.ItemContainerSnapshot> containers;
		private final Map<ItemContainerRole, GameClient.ItemContainerSnapshot> exact;
		private final List<GameClient.VisibleItemSlot> slots;
		private final ItemSnapshotResolver.Resolution inventory, bank;
		private final ItemSnapshotDiagnostics diagnostics;
		private Snapshot(List<GameClient.ItemContainerSnapshot> containers,
			Map<ItemContainerRole, GameClient.ItemContainerSnapshot> exact,
			List<GameClient.VisibleItemSlot> slots, ItemSnapshotResolver.Resolution inventory,
			ItemSnapshotResolver.Resolution bank, ItemSnapshotDiagnostics diagnostics)
		{
			this.containers = containers;
			EnumMap<ItemContainerRole, GameClient.ItemContainerSnapshot> copy = new EnumMap<>(ItemContainerRole.class);
			copy.putAll(exact);
			this.exact = Collections.unmodifiableMap(copy);
			this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
			this.inventory = inventory; this.bank = bank; this.diagnostics = diagnostics;
		}
		private static Snapshot empty()
		{
			ItemSnapshotResolver.Resolution inventory = ItemSnapshotResolver.resolve(
				ItemContainerRole.INVENTORY, null, WidgetItemSnapshotStore.snapshot());
			ItemSnapshotResolver.Resolution bank = ItemSnapshotResolver.resolve(
				ItemContainerRole.BANK, null, WidgetItemSnapshotStore.snapshot());
			return new Snapshot(Collections.<GameClient.ItemContainerSnapshot>emptyList(),
				Collections.<ItemContainerRole, GameClient.ItemContainerSnapshot>emptyMap(),
				Collections.<GameClient.VisibleItemSlot>emptyList(), inventory, bank,
				new ItemSnapshotDiagnostics(-1L, -1, 0, "Not observed", "Not observed",
					inventory.getReason(), bank.getReason()));
		}
	}
}
