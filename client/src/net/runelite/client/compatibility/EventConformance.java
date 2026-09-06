package net.runelite.client.compatibility;

import com.GameClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.runelite.api.events.*;

/**
 * Production/delivery telemetry for the 634-to-RuneLite event bridge. It has no
 * Guice dependency, so native packet hooks can report evidence during startup.
 */
public final class EventConformance
{
	private static final long DUPLICATE_WINDOW_NANOS = 100_000_000L;
	private static final Map<BridgeEventChannel, MutableStats> STATS =
		new EnumMap<>(BridgeEventChannel.class);
	private static final AtomicLong SEQUENCE = new AtomicLong();

	static
	{
		for (BridgeEventChannel channel : BridgeEventChannel.values())
		{
			STATS.put(channel, new MutableStats(channel));
		}
	}

	private EventConformance() {}

	public static DeliveryToken produced(Object event, EventOrigin origin)
	{
		MutableStats stats = STATS.get(channelOf(event));
		String fingerprint = fingerprint(event);
		boolean clientThread = isClientThread();
		// First-use class loading must not consume the duplicate timing window.
		long now = System.nanoTime();
		DeliveryToken token = new DeliveryToken(SEQUENCE.incrementAndGet(), stats);
		stats.produced(event, origin, fingerprint, now, clientThread);
		return token;
	}

	public static void delivered(DeliveryToken token)
	{
		if (token != null) token.stats.delivered();
	}

	public static void dropped(DeliveryToken token, Throwable failure)
	{
		if (token != null) token.stats.dropped(failure);
	}

	public static EventConformanceSnapshot snapshot(BridgeEventChannel channel)
	{
		return STATS.get(channel).snapshot();
	}

	public static List<EventConformanceSnapshot> snapshots()
	{
		List<EventConformanceSnapshot> result = new ArrayList<>();
		for (BridgeEventChannel channel : BridgeEventChannel.values())
		{
			result.add(snapshot(channel));
		}
		return Collections.unmodifiableList(result);
	}

	static void resetForTests()
	{
		for (MutableStats stats : STATS.values()) stats.reset();
		SEQUENCE.set(0L);
	}

	private static boolean isClientThread()
	{
		try
		{
			GameClient client = GameClient.getClient();
			return client != null && client.isClientThread();
		}
		catch (Throwable ignored)
		{
			return false;
		}
	}

	private static BridgeEventChannel channelOf(Object event)
	{
		if (event instanceof NpcSpawned || event instanceof NpcDespawned || event instanceof NpcChanged)
			return BridgeEventChannel.NPC_LIFECYCLE;
		if (event instanceof ItemSpawned || event instanceof ItemDespawned || event instanceof ItemQuantityChanged)
			return BridgeEventChannel.GROUND_ITEM_LIFECYCLE;
		if (event instanceof StatChanged) return BridgeEventChannel.STAT;
		if (event instanceof ItemContainerChanged) return BridgeEventChannel.ITEM_CONTAINER;
		if (event instanceof GameObjectSpawned || event instanceof GameObjectChanged || event instanceof GameObjectDespawned
			|| event instanceof GroundObjectSpawned || event instanceof GroundObjectChanged || event instanceof GroundObjectDespawned
			|| event instanceof WallObjectSpawned || event instanceof WallObjectChanged || event instanceof WallObjectDespawned
			|| event instanceof DecorativeObjectSpawned || event instanceof DecorativeObjectChanged || event instanceof DecorativeObjectDespawned)
			return BridgeEventChannel.SCENE_OBJECT;
		if (event instanceof ChatMessage) return BridgeEventChannel.CHAT;
		if (event instanceof GameStateChanged) return BridgeEventChannel.GAME_STATE;
		if (event instanceof WidgetLoaded || event instanceof WidgetClosed || event instanceof WidgetDrag)
			return BridgeEventChannel.WIDGET;
		if (event instanceof InteractingChanged || event instanceof ActorDeath)
			return BridgeEventChannel.INTERACTION;
		return BridgeEventChannel.OTHER;
	}

	private static String fingerprint(Object event)
	{
		if (event == null) return "null";
		if (event instanceof ChatMessage)
		{
			ChatMessage value = (ChatMessage) event;
			return "chat|" + value.getTimestamp() + '|' + safe(value.getType()) + '|'
				+ safe(value.getName()) + '|' + safe(value.getSender()) + '|' + safe(value.getMessage());
		}
		if (event instanceof StatChanged)
		{
			StatChanged value = (StatChanged) event;
			return "stat|" + value.getSkill() + '|' + value.getXp() + '|'
				+ value.getLevel() + '|' + value.getBoostedLevel();
		}
		if (event instanceof ItemContainerChanged)
			return "container|" + ((ItemContainerChanged) event).getContainerKey();
		if (event instanceof ItemSpawned)
		{
			ItemSpawned value = (ItemSpawned) event;
			return "item+|" + safe(value.getTile()) + '|' + itemFingerprint(value.getItem());
		}
		if (event instanceof ItemDespawned)
		{
			ItemDespawned value = (ItemDespawned) event;
			return "item-|" + safe(value.getTile()) + '|' + itemFingerprint(value.getItem());
		}
		if (event instanceof ItemQuantityChanged)
		{
			ItemQuantityChanged value = (ItemQuantityChanged) event;
			return "itemq|" + safe(value.getTile()) + '|' + itemFingerprint(value.getItem())
				+ '|' + value.getOldQuantity() + '|' + value.getNewQuantity();
		}
		if (event instanceof NpcSpawned) return "npc+|" + npcFingerprint(((NpcSpawned) event).getNpc());
		if (event instanceof NpcDespawned) return "npc-|" + npcFingerprint(((NpcDespawned) event).getNpc());
		return event.getClass().getName() + '|' + safe(event);
	}

	private static String itemFingerprint(Object item)
	{
		if (item instanceof GameClient.GroundItemInfo)
		{
			GameClient.GroundItemInfo value = (GameClient.GroundItemInfo) item;
			return value.getId() + ":" + value.getQuantity() + ":" + value.getPlane()
				+ ":" + value.getLocalX() + ":" + value.getLocalY();
		}
		return safe(item);
	}

	private static String npcFingerprint(Object npc)
	{
		if (npc instanceof GameClient.NpcInfo)
		{
			GameClient.NpcInfo value = (GameClient.NpcInfo) npc;
			return value.getIndex() + ":" + value.getId() + ":" + value.getPlane();
		}
		return safe(npc);
	}

	private static String safe(Object value) { return value == null ? "" : String.valueOf(value); }

	public static final class DeliveryToken
	{
		private final long sequence;
		private final MutableStats stats;
		private DeliveryToken(long sequence, MutableStats stats)
		{
			this.sequence = sequence;
			this.stats = stats;
		}
		public long getSequence() { return sequence; }
	}

	private static final class MutableStats
	{
		private final BridgeEventChannel channel;
		private long produced, delivered, dropped, direct, polled, offClientThread, duplicates;
		private String lastEventClass = "", lastThread = "", lastFailure = "", lastFingerprint = "";
		private long lastEventNanos;

		private MutableStats(BridgeEventChannel channel) { this.channel = channel; }

		private synchronized void produced(Object event, EventOrigin origin, String fingerprint,
			long now, boolean clientThread)
		{
			produced++;
			if (origin == EventOrigin.DIRECT) direct++;
			else if (origin == EventOrigin.POLLED) polled++;
			if (channel != BridgeEventChannel.OTHER && !clientThread) offClientThread++;
			if (!fingerprint.isEmpty() && fingerprint.equals(lastFingerprint)
				&& now - lastEventNanos <= DUPLICATE_WINDOW_NANOS) duplicates++;
			lastFingerprint = fingerprint;
			lastEventNanos = now;
			lastEventClass = event == null ? "null" : event.getClass().getSimpleName();
			lastThread = Thread.currentThread().getName() + (clientThread ? " [client]" : " [non-client]");
		}

		private synchronized void delivered() { delivered++; }
		private synchronized void dropped(Throwable failure)
		{
			dropped++;
			lastFailure = failure == null ? "delivery unavailable"
				: failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
		}
		private synchronized EventConformanceSnapshot snapshot()
		{
			return new EventConformanceSnapshot(channel, produced, delivered, dropped, direct, polled,
				offClientThread, duplicates, lastEventClass, lastThread, lastFailure, lastEventNanos);
		}
		private synchronized void reset()
		{
			produced = delivered = dropped = direct = polled = offClientThread = duplicates = 0L;
			lastEventClass = lastThread = lastFailure = lastFingerprint = "";
			lastEventNanos = 0L;
		}
	}
}
