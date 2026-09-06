package net.runelite.client.compatibility;

import com.GameClient;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ClientTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.*;

/** Central explicit capability contract for plugins running on this 634 deob. */
@Singleton
@Slf4j
public class ClientCapabilityService
{
	private final GameClient client;
	private final ProjectionCalibrationService projection;
	private final ItemSnapshotService items;
	private volatile Map<ClientCapability, CapabilitySnapshot> snapshots = emptySnapshots();
	private final Set<String> loggedBlocks = new HashSet<>();

	@Inject
	private ClientCapabilityService(GameClient client, ProjectionCalibrationService projection,
		ItemSnapshotService items)
	{ this.client = client; this.projection = projection; this.items = items; }

	@Subscribe public void onClientTick(ClientTick ignored) { refresh(); }

	private void refresh()
	{
		EnumMap<ClientCapability, CapabilitySnapshot> next = new EnumMap<>(ClientCapability.class);
		long now = System.nanoTime();
		ProjectionCalibrationSample sample = projection.getSample();
		put(next, ClientCapability.NATIVE_SCENE_PROJECTION, sample.getTileStatus(), sample.getTileReason(), now);
		put(next, ClientCapability.ACTOR_OVERHEAD_PROJECTION, sample.getActorStatus(), sample.getActorReason(), now);
		next.put(ClientCapability.STABLE_NPC_IDENTITY, evaluateNpcIdentity(now));
		putEvent(next, ClientCapability.DIRECT_NPC_EVENTS, BridgeEventChannel.NPC_LIFECYCLE, now);
		putEvent(next, ClientCapability.DIRECT_GROUND_ITEM_EVENTS, BridgeEventChannel.GROUND_ITEM_LIFECYCLE, now);
		putEvent(next, ClientCapability.DIRECT_SCENE_OBJECT_EVENTS, BridgeEventChannel.SCENE_OBJECT, now);
		putEvent(next, ClientCapability.DIRECT_STAT_EVENTS, BridgeEventChannel.STAT, now);
		putEvent(next, ClientCapability.DIRECT_ITEM_CONTAINER_EVENTS, BridgeEventChannel.ITEM_CONTAINER, now);

		boolean inventory = items.hasAuthoritativeContainer(ItemContainerRole.INVENTORY);
		boolean bank = items.hasAuthoritativeContainer(ItemContainerRole.BANK);
		put(next, ClientCapability.VERIFIED_INVENTORY_CONTAINER,
			inventory ? CapabilityStatus.AVAILABLE : CapabilityStatus.UNKNOWN,
			inventory ? "Exact native container ID 93 observed" : "Container ID 93 has not been observed", now);
		put(next, ClientCapability.VERIFIED_BANK_CONTAINER,
			bank ? CapabilityStatus.AVAILABLE : CapabilityStatus.UNKNOWN,
			bank ? "Exact native container ID 95 observed" : "Container ID 95 has not been observed", now);
		put(next, ClientCapability.AUTHORITATIVE_ITEM_CONTAINERS,
			inventory && bank ? CapabilityStatus.AVAILABLE : inventory ? CapabilityStatus.DEGRADED : CapabilityStatus.UNKNOWN,
			inventory && bank ? "Inventory and bank containers are exact"
				: inventory ? "Inventory is exact; bank has not yet been observed"
				: "No exact native item container has been observed", now);

		ItemSnapshotDiagnostics diagnostics = items.getDiagnostics();
		put(next, ClientCapability.VISIBLE_INVENTORY_SLOT_BOUNDS,
			items.hasAuthoritativeGeometry(ItemContainerRole.INVENTORY) ? CapabilityStatus.AVAILABLE : CapabilityStatus.UNKNOWN,
			diagnostics.getInventoryGeometry(), now);
		put(next, ClientCapability.VISIBLE_BANK_SLOT_BOUNDS,
			items.hasAuthoritativeGeometry(ItemContainerRole.BANK) ? CapabilityStatus.AVAILABLE : CapabilityStatus.UNKNOWN,
			diagnostics.getBankGeometry(), now);
		put(next, ClientCapability.NATIVE_DRAG_THRESHOLD, CapabilityStatus.UNAVAILABLE,
			"No verified 634 widget drag-threshold hook exists yet", now);
		put(next, ClientCapability.COMBAT_HEALTH_INFO, CapabilityStatus.UNAVAILABLE,
			"The native combat-info queue is not adapted yet", now);
		put(next, ClientCapability.INSTANCE_COORDINATES, CapabilityStatus.UNAVAILABLE,
			"Instance template chunks are not exposed by this deob", now);
		snapshots = Collections.unmodifiableMap(next);
	}

	private CapabilitySnapshot evaluateNpcIdentity(long now)
	{
		if (!client.hasLocalPlayer()) return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
			CapabilityStatus.UNKNOWN, "Awaiting a logged-in scene", now);
		List<GameClient.NpcInfo> values = client.getNpcs();
		if (values.isEmpty()) return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
			CapabilityStatus.UNKNOWN, "No NPC sample is currently visible", now);
		Set<Integer> indexes = new HashSet<>();
		for (GameClient.NpcInfo npc : values)
		{
			if (npc.getIndex() < 0) return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
				CapabilityStatus.UNAVAILABLE, "An NPC snapshot has no native index", now);
			if (!indexes.add(npc.getIndex())) return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
				CapabilityStatus.UNAVAILABLE, "Duplicate native NPC index " + npc.getIndex(), now);
		}
		return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
			CapabilityStatus.AVAILABLE, "All " + values.size() + " visible NPCs have unique native indices", now);
	}

	private static void putEvent(Map<ClientCapability, CapabilitySnapshot> target,
		ClientCapability capability, BridgeEventChannel channel, long now)
	{
		EventConformanceSnapshot event = EventConformance.snapshot(channel);
		CapabilityStatus status; String reason;
		if (event.getDirect() > 0 && event.getDelivered() > 0)
		{
			boolean clean = event.getDropped() == 0 && event.getOffClientThread() == 0
				&& event.getDuplicates() == 0 && event.getProduced() == event.getDelivered();
			status = clean ? CapabilityStatus.AVAILABLE : CapabilityStatus.DEGRADED;
			StringBuilder detail = new StringBuilder().append(event.getDirect())
				.append(" direct events; ").append(event.getDelivered()).append(" delivered");
			if (event.getDropped() > 0) detail.append(", ").append(event.getDropped()).append(" dropped");
			if (event.getOffClientThread() > 0) detail.append(", ").append(event.getOffClientThread()).append(" off-client-thread");
			if (event.getDuplicates() > 0) detail.append(", ").append(event.getDuplicates()).append(" probable duplicates");
			if (event.getProduced() != event.getDelivered() + event.getDropped()) detail.append(", accounting mismatch");
			reason = detail.toString();
		}
		else if (event.getPolled() > 0 && event.getDelivered() > 0)
		{ status = CapabilityStatus.DEGRADED; reason = "Only polling evidence exists (" + event.getPolled() + " events)"; }
		else if (event.getDropped() > 0)
		{ status = CapabilityStatus.UNAVAILABLE; reason = event.getDropped() + " bridge events were dropped: " + event.getLastFailure(); }
		else { status = CapabilityStatus.UNKNOWN; reason = "No runtime event has been observed yet"; }
		put(target, capability, status, reason, now);
	}

	private static void put(Map<ClientCapability, CapabilitySnapshot> target,
		ClientCapability capability, CapabilityStatus status, String reason, long now)
	{ target.put(capability, new CapabilitySnapshot(capability, status, reason, now)); }
	private static Map<ClientCapability, CapabilitySnapshot> emptySnapshots()
	{
		EnumMap<ClientCapability, CapabilitySnapshot> result = new EnumMap<>(ClientCapability.class);
		long now = System.nanoTime();
		for (ClientCapability capability : ClientCapability.values())
			result.put(capability, new CapabilitySnapshot(capability, CapabilityStatus.UNKNOWN,
				"Capability service has not sampled the client yet", now));
		return Collections.unmodifiableMap(result);
	}
	public CapabilitySnapshot get(ClientCapability capability)
	{
		CapabilitySnapshot value = snapshots.get(capability);
		return value == null ? new CapabilitySnapshot(capability, CapabilityStatus.UNKNOWN,
			"No capability snapshot exists", System.nanoTime()) : value;
	}
	public List<CapabilitySnapshot> snapshots()
	{
		List<CapabilitySnapshot> result = new ArrayList<>();
		for (ClientCapability capability : ClientCapability.values()) result.add(get(capability));
		return Collections.unmodifiableList(result);
	}
	public boolean canUse(Class<?> pluginClass)
	{
		RequiresCapabilities requirements = pluginClass.getAnnotation(RequiresCapabilities.class);
		if (requirements == null) return true;
		for (ClientCapability capability : requirements.value())
			if (!get(capability).getStatus().isUsable()) { noteBlocked(pluginClass, capability); return false; }
		return true;
	}
	private void noteBlocked(Class<?> pluginClass, ClientCapability capability)
	{
		String key = pluginClass.getName() + ':' + capability.name() + ':' + get(capability).getStatus();
		synchronized (loggedBlocks)
		{
			if (loggedBlocks.add(key)) log.info("Gating {} until {} is available: {}",
				pluginClass.getSimpleName(), capability.getDisplayName(), get(capability).getReason());
		}
	}
}
