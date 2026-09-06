from pathlib import Path
import re


def write(path, content):
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content)


def replace_once(path, old, new, label):
    target = Path(path)
    text = target.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    target.write_text(text.replace(old, new, 1))


def regex_once(path, pattern, replacement, label):
    target = Path(path)
    text = target.read_text()
    text, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected one regex match, found {count}")
    target.write_text(text)


write('client/src/net/runelite/client/compatibility/BridgeEventChannel.java', '''package net.runelite.client.compatibility;

public enum BridgeEventChannel
{
\tNPC_LIFECYCLE("NPC lifecycle"),
\tGROUND_ITEM_LIFECYCLE("Ground-item lifecycle"),
\tSTAT("Skill/stat updates"),
\tITEM_CONTAINER("Item-container updates"),
\tSCENE_OBJECT("Scene-object lifecycle"),
\tCHAT("Chat messages"),
\tGAME_STATE("Game-state transitions"),
\tWIDGET("Widget lifecycle"),
\tINTERACTION("Actor interaction/combat"),
\tOTHER("Other bridge events");

\tprivate final String displayName;

\tBridgeEventChannel(String displayName)
\t{
\t\tthis.displayName = displayName;
\t}

\tpublic String getDisplayName()
\t{
\t\treturn displayName;
\t}
}
''')

write('client/src/net/runelite/client/compatibility/EventOrigin.java', '''package net.runelite.client.compatibility;

public enum EventOrigin
{
\tDIRECT,
\tPOLLED,
\tINTERNAL
}
''')

write('client/src/net/runelite/client/compatibility/EventConformanceSnapshot.java', '''package net.runelite.client.compatibility;

public final class EventConformanceSnapshot
{
\tprivate final BridgeEventChannel channel;
\tprivate final long produced;
\tprivate final long delivered;
\tprivate final long dropped;
\tprivate final long direct;
\tprivate final long polled;
\tprivate final long offClientThread;
\tprivate final long duplicates;
\tprivate final String lastEventClass;
\tprivate final String lastThread;
\tprivate final String lastFailure;
\tprivate final long lastEventNanos;

\tEventConformanceSnapshot(BridgeEventChannel channel, long produced, long delivered,
\t\tlong dropped, long direct, long polled, long offClientThread, long duplicates,
\t\tString lastEventClass, String lastThread, String lastFailure, long lastEventNanos)
\t{
\t\tthis.channel = channel;
\t\tthis.produced = produced;
\t\tthis.delivered = delivered;
\t\tthis.dropped = dropped;
\t\tthis.direct = direct;
\t\tthis.polled = polled;
\t\tthis.offClientThread = offClientThread;
\t\tthis.duplicates = duplicates;
\t\tthis.lastEventClass = lastEventClass;
\t\tthis.lastThread = lastThread;
\t\tthis.lastFailure = lastFailure;
\t\tthis.lastEventNanos = lastEventNanos;
\t}

\tpublic BridgeEventChannel getChannel() { return channel; }
\tpublic long getProduced() { return produced; }
\tpublic long getDelivered() { return delivered; }
\tpublic long getDropped() { return dropped; }
\tpublic long getDirect() { return direct; }
\tpublic long getPolled() { return polled; }
\tpublic long getOffClientThread() { return offClientThread; }
\tpublic long getDuplicates() { return duplicates; }
\tpublic String getLastEventClass() { return lastEventClass; }
\tpublic String getLastThread() { return lastThread; }
\tpublic String getLastFailure() { return lastFailure; }
\tpublic long getLastEventNanos() { return lastEventNanos; }
}
''')

write('client/src/net/runelite/client/compatibility/EventConformance.java', '''package net.runelite.client.compatibility;

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
\tprivate static final long DUPLICATE_WINDOW_NANOS = 100_000_000L;
\tprivate static final Map<BridgeEventChannel, MutableStats> STATS =
\t\tnew EnumMap<>(BridgeEventChannel.class);
\tprivate static final AtomicLong SEQUENCE = new AtomicLong();

\tstatic
\t{
\t\tfor (BridgeEventChannel channel : BridgeEventChannel.values())
\t\t{
\t\t\tSTATS.put(channel, new MutableStats(channel));
\t\t}
\t}

\tprivate EventConformance() {}

\tpublic static DeliveryToken produced(Object event, EventOrigin origin)
\t{
\t\tMutableStats stats = STATS.get(channelOf(event));
\t\tString fingerprint = fingerprint(event);
\t\tboolean clientThread = isClientThread();
\t\t// First-use class loading must not consume the duplicate timing window.
\t\tlong now = System.nanoTime();
\t\tDeliveryToken token = new DeliveryToken(SEQUENCE.incrementAndGet(), stats);
\t\tstats.produced(event, origin, fingerprint, now, clientThread);
\t\treturn token;
\t}

\tpublic static void delivered(DeliveryToken token)
\t{
\t\tif (token != null) token.stats.delivered();
\t}

\tpublic static void dropped(DeliveryToken token, Throwable failure)
\t{
\t\tif (token != null) token.stats.dropped(failure);
\t}

\tpublic static EventConformanceSnapshot snapshot(BridgeEventChannel channel)
\t{
\t\treturn STATS.get(channel).snapshot();
\t}

\tpublic static List<EventConformanceSnapshot> snapshots()
\t{
\t\tList<EventConformanceSnapshot> result = new ArrayList<>();
\t\tfor (BridgeEventChannel channel : BridgeEventChannel.values())
\t\t{
\t\t\tresult.add(snapshot(channel));
\t\t}
\t\treturn Collections.unmodifiableList(result);
\t}

\tstatic void resetForTests()
\t{
\t\tfor (MutableStats stats : STATS.values()) stats.reset();
\t\tSEQUENCE.set(0L);
\t}

\tprivate static boolean isClientThread()
\t{
\t\ttry
\t\t{
\t\t\tGameClient client = GameClient.getClient();
\t\t\treturn client != null && client.isClientThread();
\t\t}
\t\tcatch (Throwable ignored)
\t\t{
\t\t\treturn false;
\t\t}
\t}

\tprivate static BridgeEventChannel channelOf(Object event)
\t{
\t\tif (event instanceof NpcSpawned || event instanceof NpcDespawned || event instanceof NpcChanged)
\t\t\treturn BridgeEventChannel.NPC_LIFECYCLE;
\t\tif (event instanceof ItemSpawned || event instanceof ItemDespawned || event instanceof ItemQuantityChanged)
\t\t\treturn BridgeEventChannel.GROUND_ITEM_LIFECYCLE;
\t\tif (event instanceof StatChanged) return BridgeEventChannel.STAT;
\t\tif (event instanceof ItemContainerChanged) return BridgeEventChannel.ITEM_CONTAINER;
\t\tif (event instanceof GameObjectSpawned || event instanceof GameObjectChanged || event instanceof GameObjectDespawned
\t\t\t|| event instanceof GroundObjectSpawned || event instanceof GroundObjectChanged || event instanceof GroundObjectDespawned
\t\t\t|| event instanceof WallObjectSpawned || event instanceof WallObjectChanged || event instanceof WallObjectDespawned
\t\t\t|| event instanceof DecorativeObjectSpawned || event instanceof DecorativeObjectChanged || event instanceof DecorativeObjectDespawned)
\t\t\treturn BridgeEventChannel.SCENE_OBJECT;
\t\tif (event instanceof ChatMessage) return BridgeEventChannel.CHAT;
\t\tif (event instanceof GameStateChanged) return BridgeEventChannel.GAME_STATE;
\t\tif (event instanceof WidgetLoaded || event instanceof WidgetClosed || event instanceof WidgetDrag)
\t\t\treturn BridgeEventChannel.WIDGET;
\t\tif (event instanceof InteractingChanged || event instanceof ActorDeath)
\t\t\treturn BridgeEventChannel.INTERACTION;
\t\treturn BridgeEventChannel.OTHER;
\t}

\tprivate static String fingerprint(Object event)
\t{
\t\tif (event == null) return "null";
\t\tif (event instanceof ChatMessage)
\t\t{
\t\t\tChatMessage value = (ChatMessage) event;
\t\t\treturn "chat|" + value.getTimestamp() + '|' + safe(value.getType()) + '|'
\t\t\t\t+ safe(value.getName()) + '|' + safe(value.getSender()) + '|' + safe(value.getMessage());
\t\t}
\t\tif (event instanceof StatChanged)
\t\t{
\t\t\tStatChanged value = (StatChanged) event;
\t\t\treturn "stat|" + value.getSkill() + '|' + value.getXp() + '|'
\t\t\t\t+ value.getLevel() + '|' + value.getBoostedLevel();
\t\t}
\t\tif (event instanceof ItemContainerChanged)
\t\t\treturn "container|" + ((ItemContainerChanged) event).getContainerKey();
\t\tif (event instanceof ItemSpawned)
\t\t{
\t\t\tItemSpawned value = (ItemSpawned) event;
\t\t\treturn "item+|" + safe(value.getTile()) + '|' + itemFingerprint(value.getItem());
\t\t}
\t\tif (event instanceof ItemDespawned)
\t\t{
\t\t\tItemDespawned value = (ItemDespawned) event;
\t\t\treturn "item-|" + safe(value.getTile()) + '|' + itemFingerprint(value.getItem());
\t\t}
\t\tif (event instanceof ItemQuantityChanged)
\t\t{
\t\t\tItemQuantityChanged value = (ItemQuantityChanged) event;
\t\t\treturn "itemq|" + safe(value.getTile()) + '|' + itemFingerprint(value.getItem())
\t\t\t\t+ '|' + value.getOldQuantity() + '|' + value.getNewQuantity();
\t\t}
\t\tif (event instanceof NpcSpawned) return "npc+|" + npcFingerprint(((NpcSpawned) event).getNpc());
\t\tif (event instanceof NpcDespawned) return "npc-|" + npcFingerprint(((NpcDespawned) event).getNpc());
\t\treturn event.getClass().getName() + '|' + safe(event);
\t}

\tprivate static String itemFingerprint(Object item)
\t{
\t\tif (item instanceof GameClient.GroundItemInfo)
\t\t{
\t\t\tGameClient.GroundItemInfo value = (GameClient.GroundItemInfo) item;
\t\t\treturn value.getId() + ":" + value.getQuantity() + ":" + value.getPlane()
\t\t\t\t+ ":" + value.getLocalX() + ":" + value.getLocalY();
\t\t}
\t\treturn safe(item);
\t}

\tprivate static String npcFingerprint(Object npc)
\t{
\t\tif (npc instanceof GameClient.NpcInfo)
\t\t{
\t\t\tGameClient.NpcInfo value = (GameClient.NpcInfo) npc;
\t\t\treturn value.getIndex() + ":" + value.getId() + ":" + value.getPlane();
\t\t}
\t\treturn safe(npc);
\t}

\tprivate static String safe(Object value) { return value == null ? "" : String.valueOf(value); }

\tpublic static final class DeliveryToken
\t{
\t\tprivate final long sequence;
\t\tprivate final MutableStats stats;
\t\tprivate DeliveryToken(long sequence, MutableStats stats)
\t\t{
\t\t\tthis.sequence = sequence;
\t\t\tthis.stats = stats;
\t\t}
\t\tpublic long getSequence() { return sequence; }
\t}

\tprivate static final class MutableStats
\t{
\t\tprivate final BridgeEventChannel channel;
\t\tprivate long produced, delivered, dropped, direct, polled, offClientThread, duplicates;
\t\tprivate String lastEventClass = "", lastThread = "", lastFailure = "", lastFingerprint = "";
\t\tprivate long lastEventNanos;

\t\tprivate MutableStats(BridgeEventChannel channel) { this.channel = channel; }

\t\tprivate synchronized void produced(Object event, EventOrigin origin, String fingerprint,
\t\t\tlong now, boolean clientThread)
\t\t{
\t\t\tproduced++;
\t\t\tif (origin == EventOrigin.DIRECT) direct++;
\t\t\telse if (origin == EventOrigin.POLLED) polled++;
\t\t\tif (channel != BridgeEventChannel.OTHER && !clientThread) offClientThread++;
\t\t\tif (!fingerprint.isEmpty() && fingerprint.equals(lastFingerprint)
\t\t\t\t&& now - lastEventNanos <= DUPLICATE_WINDOW_NANOS) duplicates++;
\t\t\tlastFingerprint = fingerprint;
\t\t\tlastEventNanos = now;
\t\t\tlastEventClass = event == null ? "null" : event.getClass().getSimpleName();
\t\t\tlastThread = Thread.currentThread().getName() + (clientThread ? " [client]" : " [non-client]");
\t\t}

\t\tprivate synchronized void delivered() { delivered++; }
\t\tprivate synchronized void dropped(Throwable failure)
\t\t{
\t\t\tdropped++;
\t\t\tlastFailure = failure == null ? "delivery unavailable"
\t\t\t\t: failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
\t\t}
\t\tprivate synchronized EventConformanceSnapshot snapshot()
\t\t{
\t\t\treturn new EventConformanceSnapshot(channel, produced, delivered, dropped, direct, polled,
\t\t\t\toffClientThread, duplicates, lastEventClass, lastThread, lastFailure, lastEventNanos);
\t\t}
\t\tprivate synchronized void reset()
\t\t{
\t\t\tproduced = delivered = dropped = direct = polled = offClientThread = duplicates = 0L;
\t\t\tlastEventClass = lastThread = lastFailure = lastFingerprint = "";
\t\t\tlastEventNanos = 0L;
\t\t}
\t}
}
''')

write('client/src/net/runelite/client/compatibility/CapabilityStatus.java', '''package net.runelite.client.compatibility;

public enum CapabilityStatus
{
\tUNKNOWN,
\tAVAILABLE,
\tDEGRADED,
\tUNAVAILABLE;

\tpublic boolean isUsable() { return this == AVAILABLE; }
}
''')

write('client/src/net/runelite/client/compatibility/ClientCapability.java', '''package net.runelite.client.compatibility;

public enum ClientCapability
{
\tNATIVE_SCENE_PROJECTION("Native scene projection"),
\tACTOR_OVERHEAD_PROJECTION("Actor overhead projection"),
\tSTABLE_NPC_IDENTITY("Stable NPC identity"),
\tDIRECT_NPC_EVENTS("Direct NPC lifecycle events"),
\tDIRECT_GROUND_ITEM_EVENTS("Direct ground-item events"),
\tDIRECT_SCENE_OBJECT_EVENTS("Direct scene-object events"),
\tDIRECT_STAT_EVENTS("Direct stat events"),
\tDIRECT_ITEM_CONTAINER_EVENTS("Direct item-container events"),
\tAUTHORITATIVE_ITEM_CONTAINERS("Authoritative item containers"),
\tVERIFIED_INVENTORY_CONTAINER("Verified inventory container"),
\tVERIFIED_BANK_CONTAINER("Verified bank container"),
\tVISIBLE_INVENTORY_SLOT_BOUNDS("Visible inventory slot bounds"),
\tVISIBLE_BANK_SLOT_BOUNDS("Visible bank slot bounds"),
\tNATIVE_DRAG_THRESHOLD("Native item drag threshold"),
\tCOMBAT_HEALTH_INFO("Native combat health information"),
\tINSTANCE_COORDINATES("Instanced-region coordinates");

\tprivate final String displayName;
\tClientCapability(String displayName) { this.displayName = displayName; }
\tpublic String getDisplayName() { return displayName; }
}
''')

write('client/src/net/runelite/client/compatibility/CapabilitySnapshot.java', '''package net.runelite.client.compatibility;

public final class CapabilitySnapshot
{
\tprivate final ClientCapability capability;
\tprivate final CapabilityStatus status;
\tprivate final String reason;
\tprivate final long updatedAtNanos;

\tpublic CapabilitySnapshot(ClientCapability capability, CapabilityStatus status,
\t\tString reason, long updatedAtNanos)
\t{
\t\tthis.capability = capability;
\t\tthis.status = status;
\t\tthis.reason = reason == null ? "" : reason;
\t\tthis.updatedAtNanos = updatedAtNanos;
\t}

\tpublic ClientCapability getCapability() { return capability; }
\tpublic CapabilityStatus getStatus() { return status; }
\tpublic String getReason() { return reason; }
\tpublic long getUpdatedAtNanos() { return updatedAtNanos; }
}
''')

write('client/src/net/runelite/client/compatibility/RequiresCapabilities.java', '''package net.runelite.client.compatibility;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RequiresCapabilities
{
\tClientCapability[] value();
}
''')

write('client/src/net/runelite/api/events/ItemContainerChanged.java', '''package net.runelite.api.events;

/** A native item-container update preserving the 634 alternate-namespace bit. */
public class ItemContainerChanged
{
\tprivate int containerId;
\tprivate long containerKey;
\tprivate Object itemContainer;

\tpublic ItemContainerChanged() {}
\tpublic ItemContainerChanged(int containerId, Object itemContainer)
\t{
\t\tthis(containerId, containerId & 0xffffffffL, itemContainer);
\t}
\tpublic ItemContainerChanged(int containerId, long containerKey, Object itemContainer)
\t{
\t\tthis.containerId = containerId;
\t\tthis.containerKey = containerKey;
\t\tthis.itemContainer = itemContainer;
\t}

\tpublic int getContainerId() { return containerId; }
\tpublic void setContainerId(int containerId) { this.containerId = containerId; }
\tpublic long getContainerKey() { return containerKey; }
\tpublic void setContainerKey(long containerKey) { this.containerKey = containerKey; }
\tpublic Object getItemContainer() { return itemContainer; }
\tpublic void setItemContainer(Object itemContainer) { this.itemContainer = itemContainer; }
}
''')

path = 'client/src/net/runelite/client/game/GameEventBridgeHooks.java'
text = Path(path).read_text()
if 'import net.runelite.client.compatibility.EventConformance;' not in text:
    text = text.replace('import net.runelite.client.RuneLite;\n',
        'import net.runelite.client.RuneLite;\nimport net.runelite.client.compatibility.EventConformance;\nimport net.runelite.client.compatibility.EventOrigin;\n', 1)
old = '''\tpublic static void postItemContainerChanged(int containerId, Object itemContainer)
\t{
\t\tpost(new ItemContainerChanged(containerId, itemContainer));
\t}
'''
new = '''\tpublic static void postItemContainerChanged(int containerId, Object itemContainer)
\t{
\t\tpostItemContainerChanged(containerId, false, itemContainer);
\t}

\tpublic static void postItemContainerChanged(int containerId, boolean alternateNamespace, Object itemContainer)
\t{
\t\tlong key = (containerId & 0xffffffffL) | (alternateNamespace ? 0xffffffff80000000L : 0L);
\t\tpost(new ItemContainerChanged(containerId, key, itemContainer));
\t}
'''
if text.count(old) != 1:
    raise SystemExit('GameEventBridgeHooks item-container block mismatch')
text = text.replace(old, new, 1)
# Repair the confirmed duplicate chat producer if it is present.
text = text.replace(
    '\t\tpost(new ChatMessage(null, type, name, message, sender, timestamp));\n'
    '\t\tpost(new ChatMessage(null, type, name, message, sender, timestamp));\n',
    '\t\tpost(new ChatMessage(null, type, name, message, sender, timestamp));\n', 1)
pattern = r'''\tprivate static void post\(Object event\)\n\t\{.*?\n\t\}\n\n\tprivate static void postDeferred\(Object event\)\n\t\{.*?\n\t\}\n'''
replacement = '''\tprivate static void post(Object event)
\t{
\t\tEventConformance.DeliveryToken token = EventConformance.produced(event, EventOrigin.DIRECT);
\t\ttry
\t\t{
\t\t\tif (RuneLite.getInjector() == null)
\t\t\t{
\t\t\t\tEventConformance.dropped(token, null);
\t\t\t\treturn;
\t\t\t}
\t\t\tRuneLite.getInjector().getInstance(EventBus.class).post(event);
\t\t\tEventConformance.delivered(token);
\t\t}
\t\tcatch (Throwable failure)
\t\t{
\t\t\tEventConformance.dropped(token, failure);
\t\t}
\t}

\tprivate static void postDeferred(Object event)
\t{
\t\tEventConformance.DeliveryToken token = EventConformance.produced(event, EventOrigin.DIRECT);
\t\ttry
\t\t{
\t\t\tif (RuneLite.getInjector() == null)
\t\t\t{
\t\t\t\tEventConformance.dropped(token, null);
\t\t\t\treturn;
\t\t\t}
\t\t\tRuneLite.getInjector().getInstance(DeferredEventBus.class).post(event);
\t\t\tEventConformance.delivered(token);
\t\t}
\t\tcatch (Throwable failure)
\t\t{
\t\t\tEventConformance.dropped(token, failure);
\t\t}
\t}
'''
text, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
if count != 1:
    raise SystemExit('GameEventBridgeHooks delivery block mismatch')
Path(path).write_text(text)

path = 'client/src/net/runelite/client/game/GameStateBridge.java'
text = Path(path).read_text()
if 'import net.runelite.client.compatibility.EventConformance;' not in text:
    text = text.replace('import net.runelite.client.eventbus.EventBus;\n',
        'import net.runelite.client.compatibility.EventConformance;\nimport net.runelite.client.compatibility.EventOrigin;\nimport net.runelite.client.eventbus.EventBus;\n', 1)
replacements = {
    'eventBus.post(new ItemSpawned(tileKey(item), item));': 'postPolled(new ItemSpawned(tileKey(item), item));',
    'eventBus.post(new ItemQuantityChanged(item, tileKey(item), previous.getQuantity(), item.getQuantity()));':
        'postPolled(new ItemQuantityChanged(item, tileKey(item), previous.getQuantity(), item.getQuantity()));',
    'eventBus.post(new ItemDespawned(tileKey(item), item));': 'postPolled(new ItemDespawned(tileKey(item), item));',
    'eventBus.post(new NpcSpawned(npc));': 'postPolled(new NpcSpawned(npc));',
    'eventBus.post(new NpcDespawned(npc));': 'postPolled(new NpcDespawned(npc));',
    'eventBus.post(new StatChanged(skill, snapshot.getXp(), snapshot.getLevel(), snapshot.getBoostedLevel()));':
        'postPolled(new StatChanged(skill, snapshot.getXp(), snapshot.getLevel(), snapshot.getBoostedLevel()));'
}
for old_value, new_value in replacements.items():
    text = text.replace(old_value, new_value)
anchor = '\tprivate static boolean changed(GameClient.SkillSnapshot previous, GameClient.SkillSnapshot current)\n'
method = '''\tprivate void postPolled(Object event)
\t{
\t\tEventConformance.DeliveryToken token = EventConformance.produced(event, EventOrigin.POLLED);
\t\ttry
\t\t{
\t\t\teventBus.post(event);
\t\t\tEventConformance.delivered(token);
\t\t}
\t\tcatch (RuntimeException failure)
\t\t{
\t\t\tEventConformance.dropped(token, failure);
\t\t\tthrow failure;
\t\t}
\t}

'''
if text.count(anchor) != 1:
    raise SystemExit('GameStateBridge helper anchor mismatch')
text = text.replace(anchor, method + anchor, 1)
Path(path).write_text(text)

path = 'client/src/Class348_Sub42_Sub8_Sub2.java'
text = Path(path).read_text()
partial_old = 'net.runelite.client.game.GameEventBridgeHooks.postItemContainerChanged(i, null);'
partial_new = 'net.runelite.client.game.GameEventBridgeHooks.postItemContainerChanged(i, bool_221_, null);'
if text.count(partial_old) != 1:
    raise SystemExit('Partial container hook mismatch')
text = text.replace(partial_old, partial_new, 1)
full_old = '''                Canvas_Sub1.method121(i, -364570972, bool_31_, i_33_, i_34_, i_35_ - 1);
            }
            Class199.anIntArray2633[Class139.method1166(31, Class106.anInt1631++)] = i;
'''
full_new = '''                Canvas_Sub1.method121(i, -364570972, bool_31_, i_33_, i_34_, i_35_ - 1);
            }
            net.runelite.client.game.GameEventBridgeHooks.postItemContainerChanged(i, bool_31_, null);
            Class199.anIntArray2633[Class139.method1166(31, Class106.anInt1631++)] = i;
'''
if text.count(full_old) != 1:
    raise SystemExit('Full container hook mismatch')
Path(path).write_text(text.replace(full_old, full_new, 1))

print('Applied wave 2 event-conformance substrate')
