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


write('client/src/net/runelite/client/game/ItemContainerRole.java', '''package net.runelite.client.game;

/** Native 634 item-container roles verified from packet IDs. */
public enum ItemContainerRole
{
\tINVENTORY(93, 28),
\tBANK(95, -1),
\tUNKNOWN(-1, -1);

\tprivate final int lowId;
\tprivate final int expectedCapacity;

\tItemContainerRole(int lowId, int expectedCapacity)
\t{
\t\tthis.lowId = lowId;
\t\tthis.expectedCapacity = expectedCapacity;
\t}

\tpublic int getLowId() { return lowId; }
\tpublic int getExpectedCapacity() { return expectedCapacity; }

\tpublic static ItemContainerRole fromKey(long key)
\t{
\t\tint low = (int) (key & 0xffffL);
\t\tfor (ItemContainerRole role : values())
\t\t{
\t\t\tif (role.lowId == low) return role;
\t\t}
\t\treturn UNKNOWN;
\t}
}
''')

write('client/src/net/runelite/client/game/WidgetItemSnapshotStore.java', '''package net.runelite.client.game;

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
\tprivate static final Object LOCK = new Object();
\tprivate static final Map<Long, RawWidgetItem> BUILDING = new LinkedHashMap<>();
\tprivate static volatile FrameSnapshot published = FrameSnapshot.empty();
\tprivate static boolean frameOpen;
\tprivate static long buildingGeneration;
\tprivate static int buildingRootInterface = -1;

\tprivate WidgetItemSnapshotStore() {}

\tpublic static void beginFrame(long generation, int rootInterface)
\t{
\t\tsynchronized (LOCK)
\t\t{
\t\t\tBUILDING.clear();
\t\t\tbuildingGeneration = generation;
\t\t\tbuildingRootInterface = rootInterface;
\t\t\tframeOpen = true;
\t\t}
\t}

\tpublic static void capture(int packedWidgetId, int childIndex, int widgetType,
\t\tint itemId, int quantity, Rectangle bounds, Rectangle clipBounds)
\t{
\t\tif (childIndex < 0 || bounds == null || clipBounds == null
\t\t\t|| bounds.width <= 0 || bounds.height <= 0 || !bounds.intersects(clipBounds)) return;
\t\tsynchronized (LOCK)
\t\t{
\t\t\tif (!frameOpen) return;
\t\t\tlong key = (((long) packedWidgetId) << 32) ^ (childIndex & 0xffffffffL);
\t\t\tBUILDING.put(key, new RawWidgetItem(packedWidgetId, childIndex, widgetType,
\t\t\t\titemId, quantity, bounds, clipBounds, buildingGeneration));
\t\t}
\t}

\tpublic static void endFrame() { publishFrame(true); }
\tpublic static void abortFrame() { publishFrame(false); }

\tprivate static void publishFrame(boolean complete)
\t{
\t\tsynchronized (LOCK)
\t\t{
\t\t\tif (!frameOpen) return;
\t\t\tpublished = new FrameSnapshot(buildingGeneration, buildingRootInterface,
\t\t\t\tSystem.nanoTime(), complete, complete ? new ArrayList<>(BUILDING.values())
\t\t\t\t\t: Collections.<RawWidgetItem>emptyList());
\t\t\tBUILDING.clear();
\t\t\tframeOpen = false;
\t\t}
\t}

\tpublic static FrameSnapshot snapshot() { return published; }

\tpublic static final class RawWidgetItem
\t{
\t\tprivate final int packedWidgetId, childIndex, widgetType, itemId, quantity;
\t\tprivate final Rectangle bounds, clipBounds;
\t\tprivate final long generation;

\t\tpublic RawWidgetItem(int packedWidgetId, int childIndex, int widgetType,
\t\t\tint itemId, int quantity, Rectangle bounds, Rectangle clipBounds, long generation)
\t\t{
\t\t\tthis.packedWidgetId = packedWidgetId;
\t\t\tthis.childIndex = childIndex;
\t\t\tthis.widgetType = widgetType;
\t\t\tthis.itemId = itemId;
\t\t\tthis.quantity = quantity;
\t\t\tthis.bounds = new Rectangle(bounds);
\t\t\tthis.clipBounds = new Rectangle(clipBounds);
\t\t\tthis.generation = generation;
\t\t}

\t\tpublic int getPackedWidgetId() { return packedWidgetId; }
\t\tpublic int getChildIndex() { return childIndex; }
\t\tpublic int getWidgetType() { return widgetType; }
\t\tpublic int getItemId() { return itemId; }
\t\tpublic int getQuantity() { return quantity; }
\t\tpublic Rectangle getBounds() { return new Rectangle(bounds); }
\t\tpublic Rectangle getClipBounds() { return new Rectangle(clipBounds); }
\t\tpublic long getGeneration() { return generation; }
\t}

\tpublic static final class FrameSnapshot
\t{
\t\tprivate final long generation, publishedAtNanos;
\t\tprivate final int rootInterface;
\t\tprivate final boolean complete;
\t\tprivate final List<RawWidgetItem> items;

\t\tprivate FrameSnapshot(long generation, int rootInterface, long publishedAtNanos,
\t\t\tboolean complete, List<RawWidgetItem> items)
\t\t{
\t\t\tthis.generation = generation;
\t\t\tthis.rootInterface = rootInterface;
\t\t\tthis.publishedAtNanos = publishedAtNanos;
\t\t\tthis.complete = complete;
\t\t\tthis.items = Collections.unmodifiableList(new ArrayList<>(items));
\t\t}

\t\tprivate static FrameSnapshot empty()
\t\t{
\t\t\treturn new FrameSnapshot(-1L, -1, 0L, false, Collections.<RawWidgetItem>emptyList());
\t\t}

\t\tpublic long getGeneration() { return generation; }
\t\tpublic int getRootInterface() { return rootInterface; }
\t\tpublic long getPublishedAtNanos() { return publishedAtNanos; }
\t\tpublic boolean isComplete() { return complete; }
\t\tpublic List<RawWidgetItem> getItems() { return items; }
\t}
}
''')

write('client/src/net/runelite/client/game/ItemSnapshotDiagnostics.java', '''package net.runelite.client.game;

public final class ItemSnapshotDiagnostics
{
\tprivate final long widgetGeneration;
\tprivate final int rootInterface, rawWidgetItems;
\tprivate final String inventoryContainer, bankContainer, inventoryGeometry, bankGeometry;

\tItemSnapshotDiagnostics(long widgetGeneration, int rootInterface, int rawWidgetItems,
\t\tString inventoryContainer, String bankContainer, String inventoryGeometry, String bankGeometry)
\t{
\t\tthis.widgetGeneration = widgetGeneration;
\t\tthis.rootInterface = rootInterface;
\t\tthis.rawWidgetItems = rawWidgetItems;
\t\tthis.inventoryContainer = inventoryContainer;
\t\tthis.bankContainer = bankContainer;
\t\tthis.inventoryGeometry = inventoryGeometry;
\t\tthis.bankGeometry = bankGeometry;
\t}

\tpublic long getWidgetGeneration() { return widgetGeneration; }
\tpublic int getRootInterface() { return rootInterface; }
\tpublic int getRawWidgetItems() { return rawWidgetItems; }
\tpublic String getInventoryContainer() { return inventoryContainer; }
\tpublic String getBankContainer() { return bankContainer; }
\tpublic String getInventoryGeometry() { return inventoryGeometry; }
\tpublic String getBankGeometry() { return bankGeometry; }
}
''')

write('client/src/net/runelite/client/game/ItemSnapshotResolver.java', '''package net.runelite.client.game;

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
\tprivate ItemSnapshotResolver() {}

\tpublic static Resolution resolve(ItemContainerRole role,
\t\tGameClient.ItemContainerSnapshot container, WidgetItemSnapshotStore.FrameSnapshot frame)
\t{
\t\tif (role == null || role == ItemContainerRole.UNKNOWN)
\t\t\treturn Resolution.unavailable(role, "No authoritative role requested");
\t\tif (container == null || !container.isAuthoritative() || container.getRole() != role)
\t\t\treturn Resolution.unavailable(role, "Exact native container has not been observed");
\t\tif (frame == null || !frame.isComplete())
\t\t\treturn Resolution.unavailable(role, "The latest native widget frame is incomplete");
\t\tif (frame.getItems().isEmpty())
\t\t\treturn Resolution.unavailable(role, "No visible native widget items were captured");

\t\tMap<Integer, Map<Integer, WidgetItemSnapshotStore.RawWidgetItem>> groups = new LinkedHashMap<>();
\t\tfor (WidgetItemSnapshotStore.RawWidgetItem raw : frame.getItems())
\t\t{
\t\t\tif (raw.getChildIndex() < 0 || raw.getBounds().width <= 0 || raw.getBounds().height <= 0) continue;
\t\t\tgroups.computeIfAbsent(raw.getPackedWidgetId(), ignored -> new LinkedHashMap<>())
\t\t\t\t.put(raw.getChildIndex(), raw);
\t\t}

\t\tList<Candidate> candidates = new ArrayList<>();
\t\tfor (Map.Entry<Integer, Map<Integer, WidgetItemSnapshotStore.RawWidgetItem>> entry : groups.entrySet())
\t\t{
\t\t\tCandidate candidate = score(role, container, entry.getKey(), entry.getValue());
\t\t\tif (candidate.acceptable) candidates.add(candidate);
\t\t}
\t\tif (candidates.isEmpty())
\t\t\treturn Resolution.unavailable(role, "No visible widget group exactly matched container " + container.getId());

\t\tcandidates.sort(Comparator.comparingInt(Candidate::rank).reversed());
\t\tCandidate best = candidates.get(0);
\t\tif (candidates.size() > 1 && candidates.get(1).rank() == best.rank())
\t\t\treturn Resolution.unavailable(role, "Multiple native widget groups matched equally; refusing ambiguous geometry");

\t\tList<GameClient.VisibleItemSlot> slots = new ArrayList<>();
\t\tList<Integer> indexes = new ArrayList<>(best.widgets.keySet());
\t\tCollections.sort(indexes);
\t\tfor (Integer index : indexes)
\t\t{
\t\t\tWidgetItemSnapshotStore.RawWidgetItem raw = best.widgets.get(index);
\t\t\tGameClient.ItemStackInfo expected = container.getItemAt(index);
\t\t\tint itemId = expected == null ? -1 : expected.getId();
\t\t\tint quantity = expected == null ? 0 : expected.getQuantity();
\t\t\tRectangle bounds = raw.getBounds().intersection(raw.getClipBounds());
\t\t\tif (bounds.width <= 0 || bounds.height <= 0) continue;
\t\t\tslots.add(new GameClient.VisibleItemSlot(raw.getPackedWidgetId(), index,
\t\t\t\tcontainer.getId(), role, itemId, quantity, bounds, raw.getClipBounds(),
\t\t\t\tframe.getGeneration(), true));
\t\t}
\t\treturn new Resolution(role, true, "Matched widget " + best.widgetId + " with "
\t\t\t+ best.coverage + " visible slots and " + best.itemMatches + " occupied-item matches",
\t\t\tbest.widgetId, slots);
\t}

\tprivate static Candidate score(ItemContainerRole role, GameClient.ItemContainerSnapshot container,
\t\tint widgetId, Map<Integer, WidgetItemSnapshotStore.RawWidgetItem> widgets)
\t{
\t\tint maxSlots = role == ItemContainerRole.INVENTORY ? 28 : container.getCapacity();
\t\tint coverage = 0, idMismatches = 0, quantityMismatches = 0, itemMatches = 0, emptyMatches = 0;
\t\tfor (Map.Entry<Integer, WidgetItemSnapshotStore.RawWidgetItem> entry : widgets.entrySet())
\t\t{
\t\t\tint slot = entry.getKey();
\t\t\tif (slot < 0 || slot >= maxSlots) continue;
\t\t\tcoverage++;
\t\t\tWidgetItemSnapshotStore.RawWidgetItem raw = entry.getValue();
\t\t\tGameClient.ItemStackInfo expected = container.getItemAt(slot);
\t\t\tint expectedId = expected == null ? -1 : expected.getId();
\t\t\tif (raw.getItemId() != expectedId) { idMismatches++; continue; }
\t\t\tif (expectedId < 0) emptyMatches++;
\t\t\telse
\t\t\t{
\t\t\t\titemMatches++;
\t\t\t\tif (raw.getQuantity() > 0 && raw.getQuantity() != expected.getQuantity()) quantityMismatches++;
\t\t\t}
\t\t}
\t\tboolean acceptable;
\t\tif (role == ItemContainerRole.INVENTORY) acceptable = coverage == 28 && idMismatches == 0;
\t\telse
\t\t{
\t\t\tint minimumCoverage = Math.min(8, Math.max(1, container.getCapacity()));
\t\t\tint requiredItemMatches = Math.min(3, Math.max(1, container.getOccupiedSlots()));
\t\t\tacceptable = coverage >= minimumCoverage && idMismatches == 0
\t\t\t\t&& (container.getOccupiedSlots() == 0 || itemMatches >= requiredItemMatches);
\t\t}
\t\treturn new Candidate(widgetId, widgets, coverage, itemMatches, emptyMatches,
\t\t\tidMismatches, quantityMismatches, acceptable);
\t}

\tprivate static final class Candidate
\t{
\t\tprivate final int widgetId, coverage, itemMatches, emptyMatches, idMismatches, quantityMismatches;
\t\tprivate final Map<Integer, WidgetItemSnapshotStore.RawWidgetItem> widgets;
\t\tprivate final boolean acceptable;
\t\tprivate Candidate(int widgetId, Map<Integer, WidgetItemSnapshotStore.RawWidgetItem> widgets,
\t\t\tint coverage, int itemMatches, int emptyMatches, int idMismatches,
\t\t\tint quantityMismatches, boolean acceptable)
\t\t{
\t\t\tthis.widgetId = widgetId; this.widgets = widgets; this.coverage = coverage;
\t\t\tthis.itemMatches = itemMatches; this.emptyMatches = emptyMatches;
\t\t\tthis.idMismatches = idMismatches; this.quantityMismatches = quantityMismatches;
\t\t\tthis.acceptable = acceptable;
\t\t}
\t\tprivate int rank()
\t\t{
\t\t\treturn coverage * 1000 + itemMatches * 100 + emptyMatches * 2
\t\t\t\t- idMismatches * 10000 - quantityMismatches;
\t\t}
\t}

\tpublic static final class Resolution
\t{
\t\tprivate final ItemContainerRole role;
\t\tprivate final boolean available;
\t\tprivate final String reason;
\t\tprivate final int packedWidgetId;
\t\tprivate final List<GameClient.VisibleItemSlot> slots;
\t\tprivate Resolution(ItemContainerRole role, boolean available, String reason,
\t\t\tint packedWidgetId, List<GameClient.VisibleItemSlot> slots)
\t\t{
\t\t\tthis.role = role == null ? ItemContainerRole.UNKNOWN : role;
\t\t\tthis.available = available; this.reason = reason; this.packedWidgetId = packedWidgetId;
\t\t\tthis.slots = Collections.unmodifiableList(new ArrayList<>(slots));
\t\t}
\t\tprivate static Resolution unavailable(ItemContainerRole role, String reason)
\t\t{
\t\t\treturn new Resolution(role, false, reason, -1, Collections.<GameClient.VisibleItemSlot>emptyList());
\t\t}
\t\tpublic ItemContainerRole getRole() { return role; }
\t\tpublic boolean isAvailable() { return available; }
\t\tpublic String getReason() { return reason; }
\t\tpublic int getPackedWidgetId() { return packedWidgetId; }
\t\tpublic List<GameClient.VisibleItemSlot> getSlots() { return slots; }
\t}
}
''')

write('client/src/net/runelite/client/game/ItemSnapshotService.java', '''package net.runelite.client.game;

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
\tprivate final GameClient client;
\tprivate volatile Snapshot snapshot = Snapshot.empty();

\t@Inject
\tprivate ItemSnapshotService(GameClient client) { this.client = client; }

\t@Subscribe
\tpublic void onClientTick(ClientTick ignored) { refresh(); }

\tprivate void refresh()
\t{
\t\tList<GameClient.ItemContainerSnapshot> containers =
\t\t\tCollections.unmodifiableList(new ArrayList<>(client.getItemContainers()));
\t\tMap<ItemContainerRole, GameClient.ItemContainerSnapshot> exact = new EnumMap<>(ItemContainerRole.class);
\t\tMap<ItemContainerRole, Boolean> duplicate = new EnumMap<>(ItemContainerRole.class);
\t\tfor (GameClient.ItemContainerSnapshot container : containers)
\t\t{
\t\t\tItemContainerRole role = container.getRole();
\t\t\tif (role == ItemContainerRole.UNKNOWN || !container.isAuthoritative()) continue;
\t\t\tif (exact.containsKey(role)) { duplicate.put(role, true); exact.remove(role); }
\t\t\telse if (!Boolean.TRUE.equals(duplicate.get(role))) exact.put(role, container);
\t\t}

\t\tWidgetItemSnapshotStore.FrameSnapshot frame = WidgetItemSnapshotStore.snapshot();
\t\tItemSnapshotResolver.Resolution inventory = ItemSnapshotResolver.resolve(
\t\t\tItemContainerRole.INVENTORY, exact.get(ItemContainerRole.INVENTORY), frame);
\t\tItemSnapshotResolver.Resolution bank = ItemSnapshotResolver.resolve(
\t\t\tItemContainerRole.BANK, exact.get(ItemContainerRole.BANK), frame);
\t\tList<GameClient.VisibleItemSlot> slots = new ArrayList<>();
\t\tslots.addAll(inventory.getSlots());
\t\tslots.addAll(bank.getSlots());
\t\tItemSnapshotDiagnostics diagnostics = new ItemSnapshotDiagnostics(frame.getGeneration(),
\t\t\tframe.getRootInterface(), frame.getItems().size(),
\t\t\tdescribe(exact.get(ItemContainerRole.INVENTORY), duplicate.get(ItemContainerRole.INVENTORY)),
\t\t\tdescribe(exact.get(ItemContainerRole.BANK), duplicate.get(ItemContainerRole.BANK)),
\t\t\tinventory.getReason(), bank.getReason());
\t\tsnapshot = new Snapshot(containers, exact, slots, inventory, bank, diagnostics);
\t}

\tprivate static String describe(GameClient.ItemContainerSnapshot value, Boolean duplicate)
\t{
\t\tif (Boolean.TRUE.equals(duplicate)) return "Ambiguous: multiple exact native containers";
\t\tif (value == null) return "Not observed";
\t\treturn "id=" + value.getId() + ", capacity=" + value.getCapacity()
\t\t\t+ ", occupied=" + value.getOccupiedSlots();
\t}

\tpublic List<GameClient.ItemContainerSnapshot> getContainers() { return snapshot.containers; }
\tpublic GameClient.ItemContainerSnapshot getContainer(ItemContainerRole role) { return snapshot.exact.get(role); }
\tpublic List<GameClient.VisibleItemSlot> getVisibleItemSlots() { return snapshot.slots; }
\tpublic long getGeneration() { return snapshot.diagnostics.getWidgetGeneration(); }
\tpublic ItemSnapshotDiagnostics getDiagnostics() { return snapshot.diagnostics; }
\tpublic boolean hasAuthoritativeContainer(ItemContainerRole role) { return getContainer(role) != null; }
\tpublic boolean hasAuthoritativeGeometry(ItemContainerRole role)
\t{
\t\treturn role == ItemContainerRole.INVENTORY ? snapshot.inventory.isAvailable()
\t\t\t: role == ItemContainerRole.BANK && snapshot.bank.isAvailable();
\t}

\tprivate static final class Snapshot
\t{
\t\tprivate final List<GameClient.ItemContainerSnapshot> containers;
\t\tprivate final Map<ItemContainerRole, GameClient.ItemContainerSnapshot> exact;
\t\tprivate final List<GameClient.VisibleItemSlot> slots;
\t\tprivate final ItemSnapshotResolver.Resolution inventory, bank;
\t\tprivate final ItemSnapshotDiagnostics diagnostics;
\t\tprivate Snapshot(List<GameClient.ItemContainerSnapshot> containers,
\t\t\tMap<ItemContainerRole, GameClient.ItemContainerSnapshot> exact,
\t\t\tList<GameClient.VisibleItemSlot> slots, ItemSnapshotResolver.Resolution inventory,
\t\t\tItemSnapshotResolver.Resolution bank, ItemSnapshotDiagnostics diagnostics)
\t\t{
\t\t\tthis.containers = containers;
\t\t\tEnumMap<ItemContainerRole, GameClient.ItemContainerSnapshot> copy = new EnumMap<>(ItemContainerRole.class);
\t\t\tcopy.putAll(exact);
\t\t\tthis.exact = Collections.unmodifiableMap(copy);
\t\t\tthis.slots = Collections.unmodifiableList(new ArrayList<>(slots));
\t\t\tthis.inventory = inventory; this.bank = bank; this.diagnostics = diagnostics;
\t\t}
\t\tprivate static Snapshot empty()
\t\t{
\t\t\tItemSnapshotResolver.Resolution inventory = ItemSnapshotResolver.resolve(
\t\t\t\tItemContainerRole.INVENTORY, null, WidgetItemSnapshotStore.snapshot());
\t\t\tItemSnapshotResolver.Resolution bank = ItemSnapshotResolver.resolve(
\t\t\t\tItemContainerRole.BANK, null, WidgetItemSnapshotStore.snapshot());
\t\t\treturn new Snapshot(Collections.<GameClient.ItemContainerSnapshot>emptyList(),
\t\t\t\tCollections.<ItemContainerRole, GameClient.ItemContainerSnapshot>emptyMap(),
\t\t\t\tCollections.<GameClient.VisibleItemSlot>emptyList(), inventory, bank,
\t\t\t\tnew ItemSnapshotDiagnostics(-1L, -1, 0, "Not observed", "Not observed",
\t\t\t\t\tinventory.getReason(), bank.getReason()));
\t\t}
\t}
}
''')

# Extend the public compatibility API.
path = 'client/src/com/GameClient.java'
text = Path(path).read_text()
if 'import net.runelite.client.game.ItemContainerRole;' not in text:
    text = text.replace('import net.runelite.api.Skill;\n',
        'import net.runelite.api.Skill;\nimport net.runelite.client.game.ItemContainerRole;\n', 1)
if 'import java.util.ArrayList;' not in text:
    text = text.replace('import java.util.Collections;\n', 'import java.util.ArrayList;\nimport java.util.Collections;\n', 1)
old = '''    public List<ItemContainerSnapshot> getItemContainers()
    {
        return Collections.emptyList();
    }

    public int getLocalPlayerAnimation()
'''
new = '''    public List<ItemContainerSnapshot> getItemContainers()
    {
        return Collections.emptyList();
    }

    public ItemContainerSnapshot getItemContainer(ItemContainerRole role)
    {
        if (role == null || role == ItemContainerRole.UNKNOWN) return null;
        ItemContainerSnapshot match = null;
        for (ItemContainerSnapshot value : getItemContainers())
        {
            if (value.getRole() != role || !value.isAuthoritative()) continue;
            if (match != null) return null;
            match = value;
        }
        return match;
    }

    public List<VisibleItemSlot> getVisibleItemSlots() { return Collections.emptyList(); }
    public long getVisibleItemSlotGeneration() { return -1L; }

    public int getLocalPlayerAnimation()
'''
if text.count(old) != 1:
    raise SystemExit('GameClient item API anchor mismatch')
text = text.replace(old, new, 1)
pattern = r'''    public static final class ItemContainerSnapshot\n    \{.*?\n    \}\n\n    public static final class OpponentInfo'''
replacement = '''    public static final class ItemContainerSnapshot
    {
        private final long id;
        private final List<ItemStackInfo> items;
        private final int capacity;
        private final ItemContainerRole role;
        private final boolean authoritative;
        private final long generation;

        public ItemContainerSnapshot(long id, List<ItemStackInfo> items, int capacity)
        {
            this(id, items, capacity, ItemContainerRole.fromKey(id),
                ItemContainerRole.fromKey(id) != ItemContainerRole.UNKNOWN, 0L);
        }

        public ItemContainerSnapshot(long id, List<ItemStackInfo> items, int capacity,
            ItemContainerRole role, boolean authoritative, long generation)
        {
            this.id = id;
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
            this.capacity = capacity;
            this.role = role == null ? ItemContainerRole.UNKNOWN : role;
            this.authoritative = authoritative;
            this.generation = generation;
        }

        public long getId() { return id; }
        public List<ItemStackInfo> getItems() { return items; }
        public int getCapacity() { return capacity; }
        public ItemContainerRole getRole() { return role; }
        public boolean isAuthoritative() { return authoritative; }
        public long getGeneration() { return generation; }
        public int getOccupiedSlots() { return items.size(); }
        public ItemStackInfo getItemAt(int slot)
        {
            for (ItemStackInfo item : items) if (item.getSlot() == slot) return item;
            return null;
        }
        public int getTotalValue()
        {
            long total = 0L;
            for (ItemStackInfo item : items) total += item.getStackValue();
            return (int) Math.min(Integer.MAX_VALUE, total);
        }
    }

    public static final class VisibleItemSlot
    {
        private final int packedWidgetId, childIndex, itemId, quantity;
        private final long containerId, generation;
        private final ItemContainerRole role;
        private final Rectangle bounds, clipBounds;
        private final boolean authoritative;

        public VisibleItemSlot(int packedWidgetId, int childIndex, long containerId,
            ItemContainerRole role, int itemId, int quantity, Rectangle bounds,
            Rectangle clipBounds, long generation, boolean authoritative)
        {
            this.packedWidgetId = packedWidgetId; this.childIndex = childIndex;
            this.containerId = containerId; this.role = role == null ? ItemContainerRole.UNKNOWN : role;
            this.itemId = itemId; this.quantity = quantity;
            this.bounds = bounds == null ? new Rectangle() : new Rectangle(bounds);
            this.clipBounds = clipBounds == null ? new Rectangle() : new Rectangle(clipBounds);
            this.generation = generation; this.authoritative = authoritative;
        }

        public int getPackedWidgetId() { return packedWidgetId; }
        public int getChildIndex() { return childIndex; }
        public long getContainerId() { return containerId; }
        public ItemContainerRole getRole() { return role; }
        public int getItemId() { return itemId; }
        public int getQuantity() { return quantity; }
        public Rectangle getBounds() { return new Rectangle(bounds); }
        public Rectangle getClipBounds() { return new Rectangle(clipBounds); }
        public long getGeneration() { return generation; }
        public boolean isAuthoritative() { return authoritative; }
        public boolean isOccupied() { return itemId >= 0; }
    }

    public static final class OpponentInfo'''
text, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
if count != 1:
    raise SystemExit('GameClient snapshot class block mismatch')
Path(path).write_text(text)

# Native client implementation: capture physical widget bounds and expose the service snapshot.
path = 'client/src/Applet_Sub1.java'
text = Path(path).read_text()
if 'import net.runelite.client.game.ItemContainerRole;' not in text:
    text = text.replace('import net.runelite.client.RuneLite;\n',
        'import net.runelite.client.RuneLite;\nimport net.runelite.client.game.ItemContainerRole;\n'
        'import net.runelite.client.game.ItemSnapshotService;\nimport net.runelite.client.game.WidgetItemSnapshotStore;\n', 1)
anchor = '''    static void setInterfaceDirtyBounds(Rectangle rectangle, int x, int y, int width, int height) {
        if (!interfaceRenderScaleActive) {
            rectangle.setBounds(x, y, width, height);
            return;
        }
        int left = interfaceToPhysicalX(x);
        int top = interfaceToPhysicalY(y);
        int right = interfaceToPhysicalRight(x + width);
        int bottom = interfaceToPhysicalBottom(y + height);
        rectangle.setBounds(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }
'''
helper = anchor + '''
    static void captureRuneLiteWidgetItem(Class46 widget, int x, int y,
            int clipLeft, int clipTop, int clipRight, int clipBottom) {
        if (widget == null || widget.anInt704 < 0 || widget.anInt709 <= 0 || widget.anInt789 <= 0) return;
        int left = interfaceToPhysicalX(x);
        int top = interfaceToPhysicalY(y);
        int right = interfaceToPhysicalRight(x + widget.anInt709);
        int bottom = interfaceToPhysicalBottom(y + widget.anInt789);
        int clipX = interfaceToPhysicalX(clipLeft);
        int clipY = interfaceToPhysicalY(clipTop);
        int clipR = interfaceToPhysicalRight(clipRight);
        int clipB = interfaceToPhysicalBottom(clipBottom);
        WidgetItemSnapshotStore.capture(widget.anInt830, widget.anInt704, widget.anInt774,
                widget.anInt812, widget.anInt781,
                new Rectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top)),
                new Rectangle(clipX, clipY, Math.max(1, clipR - clipX), Math.max(1, clipB - clipY)));
    }
'''
if text.count(anchor) != 1:
    raise SystemExit('Applet widget capture helper anchor mismatch')
text = text.replace(anchor, helper, 1)
old = '''            Class348_Sub13 container = (Class348_Sub13) node;
            int capacity = Math.max(container.anIntArray6757.length, container.anIntArray6758.length);
            List<ItemStackInfo> items = new ArrayList<>();
'''
new = '''            Class348_Sub13 container = (Class348_Sub13) node;
            ItemContainerRole role = ItemContainerRole.fromKey(node.key);
            int capacity = Math.max(container.anIntArray6757.length, container.anIntArray6758.length);
            if (role.getExpectedCapacity() > 0) capacity = Math.max(capacity, role.getExpectedCapacity());
            List<ItemStackInfo> items = new ArrayList<>();
'''
if text.count(old) != 1:
    raise SystemExit('Applet container capacity anchor mismatch')
text = text.replace(old, new, 1)
old = '            snapshots.add(new ItemContainerSnapshot(node.key, items, capacity));\n'
new = '''            boolean authoritative = role != ItemContainerRole.UNKNOWN;
            snapshots.add(new ItemContainerSnapshot(node.key, items, capacity, role,
                    authoritative, Class367_Sub11.anInt7396));
'''
if text.count(old) != 1:
    raise SystemExit('Applet container snapshot anchor mismatch')
text = text.replace(old, new, 1)
anchor = '''        return snapshots;
    }

    @Override
    public int getLocalPlayerAnimation() {
'''
replacement = '''        return snapshots;
    }

    @Override
    public ItemContainerSnapshot getItemContainer(ItemContainerRole role) {
        try {
            if (RuneLite.getInjector() != null)
                return RuneLite.getInjector().getInstance(ItemSnapshotService.class).getContainer(role);
        } catch (Throwable ignored) {}
        return super.getItemContainer(role);
    }

    @Override
    public List<VisibleItemSlot> getVisibleItemSlots() {
        try {
            if (RuneLite.getInjector() != null)
                return RuneLite.getInjector().getInstance(ItemSnapshotService.class).getVisibleItemSlots();
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    @Override
    public long getVisibleItemSlotGeneration() {
        try {
            if (RuneLite.getInjector() != null)
                return RuneLite.getInjector().getInstance(ItemSnapshotService.class).getGeneration();
        } catch (Throwable ignored) {}
        return -1L;
    }

    @Override
    public int getLocalPlayerAnimation() {
'''
if text.count(anchor) != 1:
    raise SystemExit('Applet item-service API anchor mismatch')
Path(path).write_text(text.replace(anchor, replacement, 1))

path = 'client/src/Class302.java'
text = Path(path).read_text()
old = '''            if (r.anInt9721 != -1) {
                Class348_Sub38.anInt7008 = 0;
                Class88.method842(false);
            }
'''
new = '''            net.runelite.client.game.WidgetItemSnapshotStore.beginFrame(Class367_Sub11.anInt7396, r.anInt9721);
            boolean widgetSnapshotComplete = false;
            try {
                if (r.anInt9721 != -1) {
                    Class348_Sub38.anInt7008 = 0;
                    Class88.method842(false);
                }
                widgetSnapshotComplete = true;
            } finally {
                if (widgetSnapshotComplete) net.runelite.client.game.WidgetItemSnapshotStore.endFrame();
                else net.runelite.client.game.WidgetItemSnapshotStore.abortFrame();
            }
'''
if text.count(old) != 1:
    raise SystemExit('Class302 root render anchor mismatch')
Path(path).write_text(text.replace(old, new, 1))

path = 'client/src/Class348_Sub40_Sub7.java'
text = Path(path).read_text()
old = '''                            if (i_31_ < i_34_ && i_32_ < i_33_) {
                                if (class46.anInt765 != 0) {
'''
new = '''                            if (i_31_ < i_34_ && i_32_ < i_33_) {
                                Applet_Sub1.captureRuneLiteWidgetItem(class46, i_24_, i_25_,
                                        i_31_, i_32_, i_34_, i_33_);
                                if (class46.anInt765 != 0) {
'''
if text.count(old) != 1:
    raise SystemExit('Native widget-render capture anchor mismatch')
Path(path).write_text(text.replace(old, new, 1))

write('client/src/net/runelite/client/plugins/qol/QolItemContainers.java', '''package net.runelite.client.plugins.qol;

import com.GameClient;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.runelite.client.game.ItemContainerRole;

public final class QolItemContainers
{
\tprivate QolItemContainers() {}
\tpublic static GameClient.ItemContainerSnapshot inventory(GameClient client)
\t{
\t\treturn client == null ? null : client.getItemContainer(ItemContainerRole.INVENTORY);
\t}
\tpublic static GameClient.ItemContainerSnapshot bank(GameClient client)
\t{
\t\treturn client == null ? null : client.getItemContainer(ItemContainerRole.BANK);
\t}
\tpublic static List<GameClient.ItemStackInfo> items(GameClient.ItemContainerSnapshot container)
\t{
\t\treturn container == null ? Collections.<GameClient.ItemStackInfo>emptyList() : container.getItems();
\t}
\tpublic static List<GameClient.ItemStackInfo> sortedBySlot(GameClient.ItemContainerSnapshot container)
\t{
\t\tList<GameClient.ItemStackInfo> items = new java.util.ArrayList<>(items(container));
\t\titems.sort(Comparator.comparingInt(GameClient.ItemStackInfo::getSlot));
\t\treturn items;
\t}
}
''')

write('client/src/net/runelite/client/plugins/qol/QolInventoryLayout.java', '''package net.runelite.client.plugins.qol;

import com.GameClient;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import net.runelite.client.game.ItemContainerRole;

/** Authoritative inventory geometry backed by the native widget-frame snapshot. */
public final class QolInventoryLayout
{
\tpublic static final int SIZE = 28;
\tpublic static final int COLUMNS = 4;
\tprivate QolInventoryLayout() {}
\tpublic static double scale(GameClient client)
\t{
\t\treturn Math.max(0.5, Math.min(4.0, client.getInterfaceScalingFactor() / 100.0));
\t}
\tpublic static List<GameClient.VisibleItemSlot> slots(GameClient client)
\t{
\t\tjava.util.ArrayList<GameClient.VisibleItemSlot> result = new java.util.ArrayList<>();
\t\tif (client == null) return java.util.Collections.emptyList();
\t\tfor (GameClient.VisibleItemSlot slot : client.getVisibleItemSlots())
\t\t\tif (slot.isAuthoritative() && slot.getRole() == ItemContainerRole.INVENTORY) result.add(slot);
\t\tresult.sort((left, right) -> Integer.compare(left.getChildIndex(), right.getChildIndex()));
\t\treturn java.util.Collections.unmodifiableList(result);
\t}
\tpublic static Rectangle slotBounds(GameClient client, int index)
\t{
\t\tif (index < 0 || index >= SIZE) return null;
\t\tfor (GameClient.VisibleItemSlot slot : slots(client))
\t\t\tif (slot.getChildIndex() == index) return slot.getBounds();
\t\treturn null;
\t}
\tpublic static int slotAt(GameClient client, Point point)
\t{
\t\tif (point == null) return -1;
\t\tfor (GameClient.VisibleItemSlot slot : slots(client))
\t\t\tif (slot.getBounds().contains(point)) return slot.getChildIndex();
\t\treturn -1;
\t}
}
''')

# Remove the competing AWT canvas renderer call; workflow deletes its class file.
path = 'client/src/Canvas_Sub1.java'
text = Path(path).read_text()
text = text.replace('        net.runelite.client.plugins.inventorygrid.InventoryGridCanvasOverlay.paint(graphics, this);\n', '')
Path(path).write_text(text)

# Rewrite Inventory Grid against authoritative slot snapshots only.
write('client/src/net/runelite/client/plugins/inventorygrid/InventoryGridOverlay.java', '''package net.runelite.client.plugins.inventorygrid;

import com.GameClient;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.game.ItemContainerRole;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.util.AsyncBufferedImage;

class InventoryGridOverlay extends Overlay implements MouseListener
{
\tprivate static final int INVENTORY_SIZE = 28;
\tprivate static final int HOVER_DISTANCE = 5;
\tprivate final InventoryGridConfig config;
\tprivate final GameClient client;
\tprivate final ItemManager itemManager;
\tprivate final List<GameClient.VisibleItemSlot> lastItems = new ArrayList<>();
\tprivate Point mousePoint, initialMousePoint;
\tprivate GameClient.VisibleItemSlot draggedItem;
\tprivate boolean mouseDown, hoverActive;

\t@Inject
\tInventoryGridOverlay(InventoryGridPlugin plugin, InventoryGridConfig config,
\t\tGameClient client, ItemManager itemManager)
\t{
\t\tsuper(plugin); this.config = config; this.client = client; this.itemManager = itemManager;
\t\tsetPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ALWAYS_ON_TOP);
\t\tsetPriority(OverlayPriority.HIGH);
\t}

\t@Override public Dimension render(Graphics2D graphics)
\t{
\t\tList<GameClient.VisibleItemSlot> items = currentItems();
\t\tif (items.size() == INVENTORY_SIZE) { lastItems.clear(); lastItems.addAll(items); }
\t\tif (!mouseDown || mousePoint == null || initialMousePoint == null || draggedItem == null) return null;
\t\tif (!hoverActive && initialMousePoint.distance(mousePoint) < HOVER_DISTANCE) return null;
\t\thoverActive = true;
\t\tif (items.size() != INVENTORY_SIZE) items = new ArrayList<>(lastItems);
\t\tif (items.size() != INVENTORY_SIZE) return null;
\t\tRectangle initial = draggedItem.getBounds();
\t\tif (initial.width <= 0 || initial.height <= 0) return null;
\t\tfor (GameClient.VisibleItemSlot target : items)
\t\t{
\t\t\tRectangle bounds = target.getBounds();
\t\t\tboolean inside = bounds.contains(mousePoint);
\t\t\tif (config.showItem() && inside) { drawItem(graphics, bounds, draggedItem); drawItem(graphics, initial, target); }
\t\t\tif (config.showHighlight() && inside) { graphics.setColor(config.highlightColor()); graphics.fill(bounds); }
\t\t\telse if (config.showGrid()) { graphics.setColor(config.gridColor()); graphics.fill(bounds); }
\t\t}
\t\treturn null;
\t}

\tprivate List<GameClient.VisibleItemSlot> currentItems()
\t{
\t\tList<GameClient.VisibleItemSlot> result = new ArrayList<>();
\t\tfor (GameClient.VisibleItemSlot slot : client.getVisibleItemSlots())
\t\t\tif (slot.isAuthoritative() && slot.getRole() == ItemContainerRole.INVENTORY
\t\t\t\t&& slot.getChildIndex() >= 0 && slot.getChildIndex() < INVENTORY_SIZE) result.add(slot);
\t\tresult.sort(Comparator.comparingInt(GameClient.VisibleItemSlot::getChildIndex));
\t\treturn result;
\t}

\tprivate static GameClient.VisibleItemSlot findAt(List<GameClient.VisibleItemSlot> items, Point point)
\t{
\t\tif (point == null) return null;
\t\tfor (GameClient.VisibleItemSlot item : items)
\t\t\tif (item.isOccupied() && item.getBounds().contains(point)) return item;
\t\treturn null;
\t}

\t@Override public MouseEvent mouseClicked(MouseEvent event) { return event; }
\t@Override public MouseEvent mousePressed(MouseEvent event)
\t{
\t\tmousePoint = event.getPoint(); initialMousePoint = event.getPoint(); hoverActive = false;
\t\tList<GameClient.VisibleItemSlot> items = currentItems();
\t\tif (items.size() == INVENTORY_SIZE) { lastItems.clear(); lastItems.addAll(items); }
\t\tdraggedItem = findAt(lastItems, mousePoint); mouseDown = draggedItem != null;
\t\tif (!mouseDown) initialMousePoint = null;
\t\treturn event;
\t}
\t@Override public MouseEvent mouseReleased(MouseEvent event) { resetDrag(); return event; }
\t@Override public MouseEvent mouseEntered(MouseEvent event) { return event; }
\t@Override public MouseEvent mouseExited(MouseEvent event) { resetDrag(); return event; }
\t@Override public MouseEvent mouseDragged(MouseEvent event) { mousePoint = event.getPoint(); return event; }
\t@Override public MouseEvent mouseMoved(MouseEvent event) { mousePoint = event.getPoint(); return event; }
\tvoid resetDrag() { mouseDown = false; hoverActive = false; mousePoint = null; initialMousePoint = null; draggedItem = null; }

\tprivate void drawItem(Graphics2D graphics, Rectangle bounds, GameClient.VisibleItemSlot item)
\t{
\t\tif (bounds == null || item == null || !item.isOccupied()) return;
\t\tAsyncBufferedImage image;
\t\ttry { image = itemManager.getImage(item.getItemId(), Math.max(1, item.getQuantity()), false); }
\t\tcatch (RuntimeException ex) { return; }
\t\tif (image == null) return;
\t\tComposite previous = graphics.getComposite();
\t\tgraphics.setComposite(AlphaComposite.SrcOver.derive(0.3f));
\t\tgraphics.drawImage((BufferedImage) image, bounds.x, bounds.y, bounds.width, bounds.height, null);
\t\tgraphics.setComposite(previous);
\t}
}
''')

for path, old, new in [
    ('client/src/net/runelite/client/plugins/inventorytags/InventoryTagsOverlay.java',
     '\t\t\tRectangle slot = QolInventoryLayout.slotBounds(client, item.getSlot());\n\t\t\tgraphics.setColor(color);\n',
     '\t\t\tRectangle slot = QolInventoryLayout.slotBounds(client, item.getSlot());\n\t\t\tif (slot == null) continue;\n\t\t\tgraphics.setColor(color);\n'),
    ('client/src/net/runelite/client/plugins/itemcharges/ItemChargesOverlay.java',
     '\t\t\tRectangle bounds = QolInventoryLayout.slotBounds(client, item.getSlot());\n\t\t\tint x = bounds.x + 2;\n',
     '\t\t\tRectangle bounds = QolInventoryLayout.slotBounds(client, item.getSlot());\n\t\t\tif (bounds == null) continue;\n\t\t\tint x = bounds.x + 2;\n')]:
    replace_once(path, old, new, path + ' null bounds')

print('Applied wave 2 authoritative item/widget snapshot substrate')
