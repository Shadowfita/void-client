from pathlib import Path


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


write('client/src/net/runelite/client/compatibility/ProjectionCalibration.java', '''package net.runelite.client.compatibility;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

/** Pure validators comparing RuneLite projection with an independent native reference. */
public final class ProjectionCalibration
{
\tprivate static final int REFERENCE_TOLERANCE_PX = 4;
\tprivate ProjectionCalibration() {}

\tpublic static Validation validateTile(int canvasWidth, int canvasHeight,
\t\tPoint projectedCenter, Polygon tile, Point nativeActorGround)
\t{
\t\tif (canvasWidth <= 0 || canvasHeight <= 0)
\t\t\treturn Validation.invalid("Physical canvas dimensions are unavailable");
\t\tif (projectedCenter == null || nativeActorGround == null)
\t\t\treturn Validation.invalid("Projected or native player centre is unavailable");
\t\tif (tile == null || tile.npoints != 4)
\t\t\treturn Validation.invalid("Tile projection did not produce four terrain corners");
\t\tlong area = Math.abs(signedArea2(tile)) / 2L;
\t\tif (area < 4L) return Validation.invalid("Projected tile area is degenerate: " + area + " px²");
\t\tRectangle bounds = tile.getBounds();
\t\tif (bounds.width <= 1 || bounds.height <= 1
\t\t\t|| bounds.width > canvasWidth || bounds.height > canvasHeight)
\t\t\treturn Validation.invalid("Projected tile dimensions are implausible: "
\t\t\t\t+ bounds.width + "x" + bounds.height);
\t\tRectangle tolerance = new Rectangle(bounds);
\t\ttolerance.grow(REFERENCE_TOLERANCE_PX, REFERENCE_TOLERANCE_PX);
\t\tif (!tolerance.contains(projectedCenter) || !tolerance.contains(nativeActorGround))
\t\t\treturn Validation.invalid("Projected tile does not contain both projection references");
\t\tdouble error = projectedCenter.distance(nativeActorGround);
\t\tif (error > REFERENCE_TOLERANCE_PX)
\t\t\treturn Validation.invalid("RuneLite/native player projection differs by " + Math.round(error) + " px");
\t\tRectangle extendedCanvas = new Rectangle(-canvasWidth / 2, -canvasHeight / 2,
\t\t\tcanvasWidth * 2, canvasHeight * 2);
\t\tif (!extendedCanvas.contains(projectedCenter))
\t\t\treturn Validation.invalid("Projected local-player centre is far outside the canvas");
\t\treturn Validation.valid("Native actor centre and projected terrain tile agree",
\t\t\tMath.round(error), bounds);
\t}

\tpublic static Validation validateActor(int canvasWidth, int canvasHeight,
\t\tPoint projectedGround, Point projectedOverhead, Point nativeGround, Point nativeOverhead)
\t{
\t\tif (projectedGround == null || projectedOverhead == null
\t\t\t|| nativeGround == null || nativeOverhead == null)
\t\t\treturn Validation.invalid("Projected or native actor anchors are unavailable");
\t\tdouble projectedHeight = projectedGround.distance(projectedOverhead);
\t\tdouble nativeHeight = nativeGround.distance(nativeOverhead);
\t\tif (projectedHeight < 1.0 || nativeHeight < 1.0
\t\t\t|| projectedHeight > Math.max(canvasWidth, canvasHeight)
\t\t\t|| nativeHeight > Math.max(canvasWidth, canvasHeight))
\t\t\treturn Validation.invalid("Actor height projected to an implausible screen distance");
\t\tif (projectedOverhead.y >= projectedGround.y || nativeOverhead.y >= nativeGround.y)
\t\t\treturn Validation.invalid("Increasing world height did not move both anchors upward");
\t\tdouble groundError = projectedGround.distance(nativeGround);
\t\tdouble overheadError = projectedOverhead.distance(nativeOverhead);
\t\tif (groundError > REFERENCE_TOLERANCE_PX || overheadError > REFERENCE_TOLERANCE_PX)
\t\t\treturn Validation.invalid("RuneLite/native actor anchors differ by ground="
\t\t\t\t+ Math.round(groundError) + " px, overhead=" + Math.round(overheadError) + " px");
\t\treturn Validation.valid("RuneLite actor anchors agree with the native 634 actor projection",
\t\t\tMath.round(Math.max(groundError, overheadError)),
\t\t\tnew Rectangle(Math.min(projectedGround.x, projectedOverhead.x),
\t\t\t\tMath.min(projectedGround.y, projectedOverhead.y),
\t\t\t\tMath.max(1, Math.abs(projectedGround.x - projectedOverhead.x)),
\t\t\t\tMath.max(1, Math.abs(projectedGround.y - projectedOverhead.y))));
\t}

\tprivate static long signedArea2(Polygon polygon)
\t{
\t\tlong area = 0L;
\t\tfor (int i = 0; i < polygon.npoints; i++)
\t\t{
\t\t\tint next = (i + 1) % polygon.npoints;
\t\t\tarea += (long) polygon.xpoints[i] * polygon.ypoints[next]
\t\t\t\t- (long) polygon.xpoints[next] * polygon.ypoints[i];
\t\t}
\t\treturn area;
\t}

\tpublic static final class Validation
\t{
\t\tprivate final boolean valid;
\t\tprivate final String reason;
\t\tprivate final long metric;
\t\tprivate final Rectangle bounds;
\t\tprivate Validation(boolean valid, String reason, long metric, Rectangle bounds)
\t\t{
\t\t\tthis.valid = valid; this.reason = reason; this.metric = metric;
\t\t\tthis.bounds = bounds == null ? null : new Rectangle(bounds);
\t\t}
\t\tprivate static Validation valid(String reason, long metric, Rectangle bounds)
\t\t{ return new Validation(true, reason, metric, bounds); }
\t\tprivate static Validation invalid(String reason)
\t\t{ return new Validation(false, reason, 0L, null); }
\t\tpublic boolean isValid() { return valid; }
\t\tpublic String getReason() { return reason; }
\t\tpublic long getMetric() { return metric; }
\t\tpublic Rectangle getBounds() { return bounds == null ? null : new Rectangle(bounds); }
\t}
}
''')

write('client/src/net/runelite/client/compatibility/ProjectionCalibrationSample.java', '''package net.runelite.client.compatibility;

import java.awt.Point;
import java.awt.Polygon;

public final class ProjectionCalibrationSample
{
\tprivate final CapabilityStatus tileStatus, actorStatus;
\tprivate final String tileReason, actorReason, actorName;
\tprivate final Point playerCenter, nativePlayerCenter, actorGround, actorOverhead,
\t\tnativeActorGround, nativeActorOverhead;
\tprivate final Polygon playerTile;
\tprivate final long generation;

\tProjectionCalibrationSample(CapabilityStatus tileStatus, String tileReason,
\t\tCapabilityStatus actorStatus, String actorReason, Point playerCenter,
\t\tPoint nativePlayerCenter, Polygon playerTile, Point actorGround,
\t\tPoint actorOverhead, Point nativeActorGround, Point nativeActorOverhead,
\t\tString actorName, long generation)
\t{
\t\tthis.tileStatus = tileStatus; this.tileReason = tileReason;
\t\tthis.actorStatus = actorStatus; this.actorReason = actorReason;
\t\tthis.playerCenter = copy(playerCenter); this.nativePlayerCenter = copy(nativePlayerCenter);
\t\tthis.playerTile = copy(playerTile); this.actorGround = copy(actorGround);
\t\tthis.actorOverhead = copy(actorOverhead); this.nativeActorGround = copy(nativeActorGround);
\t\tthis.nativeActorOverhead = copy(nativeActorOverhead);
\t\tthis.actorName = actorName == null ? "" : actorName; this.generation = generation;
\t}

\tstatic ProjectionCalibrationSample unknown(String reason)
\t{
\t\treturn new ProjectionCalibrationSample(CapabilityStatus.UNKNOWN, reason,
\t\t\tCapabilityStatus.UNKNOWN, reason, null, null, null, null, null, null, null, "", -1L);
\t}
\tprivate static Point copy(Point point) { return point == null ? null : new Point(point); }
\tprivate static Polygon copy(Polygon polygon)
\t{ return polygon == null ? null : new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints); }
\tpublic CapabilityStatus getTileStatus() { return tileStatus; }
\tpublic String getTileReason() { return tileReason; }
\tpublic CapabilityStatus getActorStatus() { return actorStatus; }
\tpublic String getActorReason() { return actorReason; }
\tpublic Point getPlayerCenter() { return copy(playerCenter); }
\tpublic Point getNativePlayerCenter() { return copy(nativePlayerCenter); }
\tpublic Polygon getPlayerTile() { return copy(playerTile); }
\tpublic Point getActorGround() { return copy(actorGround); }
\tpublic Point getActorOverhead() { return copy(actorOverhead); }
\tpublic Point getNativeActorGround() { return copy(nativeActorGround); }
\tpublic Point getNativeActorOverhead() { return copy(nativeActorOverhead); }
\tpublic String getActorName() { return actorName; }
\tpublic long getGeneration() { return generation; }
}
''')

write('client/src/net/runelite/client/compatibility/ProjectionCalibrationService.java', '''package net.runelite.client.compatibility;

import com.GameClient;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.client.eventbus.Subscribe;

/** Runtime canary validating shared projection before world overlays trust it. */
@Singleton
public class ProjectionCalibrationService
{
\tprivate static final int REQUIRED_SUCCESS_STREAK = 4;
\tprivate static final int REQUIRED_FAILURE_STREAK = 3;
\tprivate final GameClient client;
\tprivate volatile ProjectionCalibrationSample sample =
\t\tProjectionCalibrationSample.unknown("Awaiting a logged-in scene");
\tprivate int tickCounter, tileSuccesses, tileFailures, actorSuccesses, actorFailures;
\tprivate long sampleGeneration;

\t@Inject
\tprivate ProjectionCalibrationService(GameClient client) { this.client = client; }

\t@Subscribe
\tpublic void onClientTick(ClientTick ignored)
\t{
\t\tif (++tickCounter % 5 == 0) calibrate();
\t}

\tprivate void calibrate()
\t{
\t\tif (!client.hasLocalPlayer()) { reset("Awaiting a logged-in scene"); return; }
\t\tDimension canvas = client.getRealDimensions();
\t\tif (canvas == null || canvas.width <= 0 || canvas.height <= 0)
\t\t{ reset("Physical canvas dimensions are unavailable"); return; }

\t\tLocalPoint player = new LocalPoint(client.getLocalPlayerLocalX(), client.getLocalPlayerLocalY());
\t\tLocalPoint playerTilePoint = LocalPoint.fromScene(client.getLocalPlayerSceneX(), client.getLocalPlayerSceneY());
\t\tint plane = client.getPlane();
\t\tPoint playerCenter = toAwt(Perspective.localToCanvas(client, player, plane));
\t\tPolygon playerTile = Perspective.getCanvasTilePoly(client, playerTilePoint);
\t\tGameClient.NativeActorProjection nativePlayer = client.getNativeLocalPlayerProjection();
\t\tPoint nativePlayerCenter = nativePlayer == null ? null : toAwt(nativePlayer.getGround());
\t\tProjectionCalibration.Validation tileValidation = ProjectionCalibration.validateTile(
\t\t\tcanvas.width, canvas.height, playerCenter, playerTile, nativePlayerCenter);
\t\tCapabilityStatus tileStatus = advance(tileValidation.isValid(), true);

\t\tGameClient.NpcInfo actor = nearestVisibleNpc(client.getNpcs(), player, plane);
\t\tPoint actorGround = null, actorOverhead = null, nativeActorGround = null, nativeActorOverhead = null;
\t\tString actorName = "";
\t\tCapabilityStatus actorStatus;
\t\tString actorReason;
\t\tif (actor == null)
\t\t{
\t\t\tactorStatus = CapabilityStatus.UNKNOWN;
\t\t\tactorReason = "No nearby NPC is available for the overhead-anchor canary";
\t\t\tactorSuccesses = actorFailures = 0;
\t\t}
\t\telse
\t\t{
\t\t\tactorName = actor.getName();
\t\t\tLocalPoint point = new LocalPoint(actor.getLocalX(), actor.getLocalY());
\t\t\tGameClient.NativeActorProjection nativeActor = client.getNativeNpcProjection(actor.getIndex());
\t\t\tint height = nativeActor == null ? Math.max(1, actor.getHeight()) : nativeActor.getHeight();
\t\t\tactorGround = toAwt(Perspective.localToCanvas(client, point, plane, 0));
\t\t\tactorOverhead = toAwt(Perspective.localToCanvas(client, point, plane, height));
\t\t\tnativeActorGround = nativeActor == null ? null : toAwt(nativeActor.getGround());
\t\t\tnativeActorOverhead = nativeActor == null ? null : toAwt(nativeActor.getOverhead());
\t\t\tProjectionCalibration.Validation validation = ProjectionCalibration.validateActor(
\t\t\t\tcanvas.width, canvas.height, actorGround, actorOverhead, nativeActorGround, nativeActorOverhead);
\t\t\tactorStatus = advance(validation.isValid(), false);
\t\t\tactorReason = validation.getReason();
\t\t}

\t\tsample = new ProjectionCalibrationSample(tileStatus, tileValidation.getReason(),
\t\t\tactorStatus, actorReason, playerCenter, nativePlayerCenter, playerTile,
\t\t\tactorGround, actorOverhead, nativeActorGround, nativeActorOverhead,
\t\t\tactorName, ++sampleGeneration);
\t}

\tprivate CapabilityStatus advance(boolean valid, boolean tile)
\t{
\t\tif (tile)
\t\t{
\t\t\tif (valid) { tileSuccesses++; tileFailures = 0; }
\t\t\telse { tileFailures++; tileSuccesses = 0; }
\t\t\treturn status(tileSuccesses, tileFailures);
\t\t}
\t\tif (valid) { actorSuccesses++; actorFailures = 0; }
\t\telse { actorFailures++; actorSuccesses = 0; }
\t\treturn status(actorSuccesses, actorFailures);
\t}
\tprivate static CapabilityStatus status(int successes, int failures)
\t{
\t\tif (successes >= REQUIRED_SUCCESS_STREAK) return CapabilityStatus.AVAILABLE;
\t\tif (failures >= REQUIRED_FAILURE_STREAK) return CapabilityStatus.UNAVAILABLE;
\t\treturn CapabilityStatus.DEGRADED;
\t}
\tprivate void reset(String reason)
\t{
\t\ttileSuccesses = tileFailures = actorSuccesses = actorFailures = 0;
\t\tsample = ProjectionCalibrationSample.unknown(reason);
\t}
\tprivate static GameClient.NpcInfo nearestVisibleNpc(List<GameClient.NpcInfo> values,
\t\tLocalPoint player, int plane)
\t{
\t\tGameClient.NpcInfo nearest = null; int best = Integer.MAX_VALUE;
\t\tfor (GameClient.NpcInfo npc : values)
\t\t{
\t\t\tif (npc.getPlane() != plane || npc.getHeight() <= 0 || npc.getIndex() < 0) continue;
\t\t\tint distance = Math.max(Math.abs(npc.getLocalX() - player.getX()),
\t\t\t\tMath.abs(npc.getLocalY() - player.getY()));
\t\t\tif (distance < best && distance <= 32 * Perspective.LOCAL_TILE_SIZE)
\t\t\t{ best = distance; nearest = npc; }
\t\t}
\t\treturn nearest;
\t}
\tprivate static Point toAwt(net.runelite.api.Point value)
\t{ return value == null ? null : new Point(value.getX(), value.getY()); }
\tpublic ProjectionCalibrationSample getSample() { return sample; }
}
''')

write('client/src/net/runelite/client/compatibility/ClientCapabilityService.java', '''package net.runelite.client.compatibility;

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
\tprivate final GameClient client;
\tprivate final ProjectionCalibrationService projection;
\tprivate final ItemSnapshotService items;
\tprivate volatile Map<ClientCapability, CapabilitySnapshot> snapshots = emptySnapshots();
\tprivate final Set<String> loggedBlocks = new HashSet<>();

\t@Inject
\tprivate ClientCapabilityService(GameClient client, ProjectionCalibrationService projection,
\t\tItemSnapshotService items)
\t{ this.client = client; this.projection = projection; this.items = items; }

\t@Subscribe public void onClientTick(ClientTick ignored) { refresh(); }

\tprivate void refresh()
\t{
\t\tEnumMap<ClientCapability, CapabilitySnapshot> next = new EnumMap<>(ClientCapability.class);
\t\tlong now = System.nanoTime();
\t\tProjectionCalibrationSample sample = projection.getSample();
\t\tput(next, ClientCapability.NATIVE_SCENE_PROJECTION, sample.getTileStatus(), sample.getTileReason(), now);
\t\tput(next, ClientCapability.ACTOR_OVERHEAD_PROJECTION, sample.getActorStatus(), sample.getActorReason(), now);
\t\tnext.put(ClientCapability.STABLE_NPC_IDENTITY, evaluateNpcIdentity(now));
\t\tputEvent(next, ClientCapability.DIRECT_NPC_EVENTS, BridgeEventChannel.NPC_LIFECYCLE, now);
\t\tputEvent(next, ClientCapability.DIRECT_GROUND_ITEM_EVENTS, BridgeEventChannel.GROUND_ITEM_LIFECYCLE, now);
\t\tputEvent(next, ClientCapability.DIRECT_SCENE_OBJECT_EVENTS, BridgeEventChannel.SCENE_OBJECT, now);
\t\tputEvent(next, ClientCapability.DIRECT_STAT_EVENTS, BridgeEventChannel.STAT, now);
\t\tputEvent(next, ClientCapability.DIRECT_ITEM_CONTAINER_EVENTS, BridgeEventChannel.ITEM_CONTAINER, now);

\t\tboolean inventory = items.hasAuthoritativeContainer(ItemContainerRole.INVENTORY);
\t\tboolean bank = items.hasAuthoritativeContainer(ItemContainerRole.BANK);
\t\tput(next, ClientCapability.VERIFIED_INVENTORY_CONTAINER,
\t\t\tinventory ? CapabilityStatus.AVAILABLE : CapabilityStatus.UNKNOWN,
\t\t\tinventory ? "Exact native container ID 93 observed" : "Container ID 93 has not been observed", now);
\t\tput(next, ClientCapability.VERIFIED_BANK_CONTAINER,
\t\t\tbank ? CapabilityStatus.AVAILABLE : CapabilityStatus.UNKNOWN,
\t\t\tbank ? "Exact native container ID 95 observed" : "Container ID 95 has not been observed", now);
\t\tput(next, ClientCapability.AUTHORITATIVE_ITEM_CONTAINERS,
\t\t\tinventory && bank ? CapabilityStatus.AVAILABLE : inventory ? CapabilityStatus.DEGRADED : CapabilityStatus.UNKNOWN,
\t\t\tinventory && bank ? "Inventory and bank containers are exact"
\t\t\t\t: inventory ? "Inventory is exact; bank has not yet been observed"
\t\t\t\t: "No exact native item container has been observed", now);

\t\tItemSnapshotDiagnostics diagnostics = items.getDiagnostics();
\t\tput(next, ClientCapability.VISIBLE_INVENTORY_SLOT_BOUNDS,
\t\t\titems.hasAuthoritativeGeometry(ItemContainerRole.INVENTORY) ? CapabilityStatus.AVAILABLE : CapabilityStatus.UNKNOWN,
\t\t\tdiagnostics.getInventoryGeometry(), now);
\t\tput(next, ClientCapability.VISIBLE_BANK_SLOT_BOUNDS,
\t\t\titems.hasAuthoritativeGeometry(ItemContainerRole.BANK) ? CapabilityStatus.AVAILABLE : CapabilityStatus.UNKNOWN,
\t\t\tdiagnostics.getBankGeometry(), now);
\t\tput(next, ClientCapability.NATIVE_DRAG_THRESHOLD, CapabilityStatus.UNAVAILABLE,
\t\t\t"No verified 634 widget drag-threshold hook exists yet", now);
\t\tput(next, ClientCapability.COMBAT_HEALTH_INFO, CapabilityStatus.UNAVAILABLE,
\t\t\t"The native combat-info queue is not adapted yet", now);
\t\tput(next, ClientCapability.INSTANCE_COORDINATES, CapabilityStatus.UNAVAILABLE,
\t\t\t"Instance template chunks are not exposed by this deob", now);
\t\tsnapshots = Collections.unmodifiableMap(next);
\t}

\tprivate CapabilitySnapshot evaluateNpcIdentity(long now)
\t{
\t\tif (!client.hasLocalPlayer()) return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
\t\t\tCapabilityStatus.UNKNOWN, "Awaiting a logged-in scene", now);
\t\tList<GameClient.NpcInfo> values = client.getNpcs();
\t\tif (values.isEmpty()) return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
\t\t\tCapabilityStatus.UNKNOWN, "No NPC sample is currently visible", now);
\t\tSet<Integer> indexes = new HashSet<>();
\t\tfor (GameClient.NpcInfo npc : values)
\t\t{
\t\t\tif (npc.getIndex() < 0) return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
\t\t\t\tCapabilityStatus.UNAVAILABLE, "An NPC snapshot has no native index", now);
\t\t\tif (!indexes.add(npc.getIndex())) return new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
\t\t\t\tCapabilityStatus.UNAVAILABLE, "Duplicate native NPC index " + npc.getIndex(), now);
\t\t}
\t\treturn new CapabilitySnapshot(ClientCapability.STABLE_NPC_IDENTITY,
\t\t\tCapabilityStatus.AVAILABLE, "All " + values.size() + " visible NPCs have unique native indices", now);
\t}

\tprivate static void putEvent(Map<ClientCapability, CapabilitySnapshot> target,
\t\tClientCapability capability, BridgeEventChannel channel, long now)
\t{
\t\tEventConformanceSnapshot event = EventConformance.snapshot(channel);
\t\tCapabilityStatus status; String reason;
\t\tif (event.getDirect() > 0 && event.getDelivered() > 0)
\t\t{
\t\t\tboolean clean = event.getDropped() == 0 && event.getOffClientThread() == 0
\t\t\t\t&& event.getDuplicates() == 0 && event.getProduced() == event.getDelivered();
\t\t\tstatus = clean ? CapabilityStatus.AVAILABLE : CapabilityStatus.DEGRADED;
\t\t\tStringBuilder detail = new StringBuilder().append(event.getDirect())
\t\t\t\t.append(" direct events; ").append(event.getDelivered()).append(" delivered");
\t\t\tif (event.getDropped() > 0) detail.append(", ").append(event.getDropped()).append(" dropped");
\t\t\tif (event.getOffClientThread() > 0) detail.append(", ").append(event.getOffClientThread()).append(" off-client-thread");
\t\t\tif (event.getDuplicates() > 0) detail.append(", ").append(event.getDuplicates()).append(" probable duplicates");
\t\t\tif (event.getProduced() != event.getDelivered() + event.getDropped()) detail.append(", accounting mismatch");
\t\t\treason = detail.toString();
\t\t}
\t\telse if (event.getPolled() > 0 && event.getDelivered() > 0)
\t\t{ status = CapabilityStatus.DEGRADED; reason = "Only polling evidence exists (" + event.getPolled() + " events)"; }
\t\telse if (event.getDropped() > 0)
\t\t{ status = CapabilityStatus.UNAVAILABLE; reason = event.getDropped() + " bridge events were dropped: " + event.getLastFailure(); }
\t\telse { status = CapabilityStatus.UNKNOWN; reason = "No runtime event has been observed yet"; }
\t\tput(target, capability, status, reason, now);
\t}

\tprivate static void put(Map<ClientCapability, CapabilitySnapshot> target,
\t\tClientCapability capability, CapabilityStatus status, String reason, long now)
\t{ target.put(capability, new CapabilitySnapshot(capability, status, reason, now)); }
\tprivate static Map<ClientCapability, CapabilitySnapshot> emptySnapshots()
\t{
\t\tEnumMap<ClientCapability, CapabilitySnapshot> result = new EnumMap<>(ClientCapability.class);
\t\tlong now = System.nanoTime();
\t\tfor (ClientCapability capability : ClientCapability.values())
\t\t\tresult.put(capability, new CapabilitySnapshot(capability, CapabilityStatus.UNKNOWN,
\t\t\t\t"Capability service has not sampled the client yet", now));
\t\treturn Collections.unmodifiableMap(result);
\t}
\tpublic CapabilitySnapshot get(ClientCapability capability)
\t{
\t\tCapabilitySnapshot value = snapshots.get(capability);
\t\treturn value == null ? new CapabilitySnapshot(capability, CapabilityStatus.UNKNOWN,
\t\t\t"No capability snapshot exists", System.nanoTime()) : value;
\t}
\tpublic List<CapabilitySnapshot> snapshots()
\t{
\t\tList<CapabilitySnapshot> result = new ArrayList<>();
\t\tfor (ClientCapability capability : ClientCapability.values()) result.add(get(capability));
\t\treturn Collections.unmodifiableList(result);
\t}
\tpublic boolean canUse(Class<?> pluginClass)
\t{
\t\tRequiresCapabilities requirements = pluginClass.getAnnotation(RequiresCapabilities.class);
\t\tif (requirements == null) return true;
\t\tfor (ClientCapability capability : requirements.value())
\t\t\tif (!get(capability).getStatus().isUsable()) { noteBlocked(pluginClass, capability); return false; }
\t\treturn true;
\t}
\tprivate void noteBlocked(Class<?> pluginClass, ClientCapability capability)
\t{
\t\tString key = pluginClass.getName() + ':' + capability.name() + ':' + get(capability).getStatus();
\t\tsynchronized (loggedBlocks)
\t\t{
\t\t\tif (loggedBlocks.add(key)) log.info("Gating {} until {} is available: {}",
\t\t\t\tpluginClass.getSimpleName(), capability.getDisplayName(), get(capability).getReason());
\t\t}
\t}
}
''')

# Independent native actor projection API.
path = 'client/src/com/GameClient.java'
text = Path(path).read_text()
old = '''    public abstract int getLocalTileHeight(int localX, int localY, int plane);

    public abstract Canvas getCanvas();
'''
new = '''    public abstract int getLocalTileHeight(int localX, int localY, int plane);

    public NativeActorProjection getNativeLocalPlayerProjection() { return null; }
    public NativeActorProjection getNativeNpcProjection(int npcIndex) { return null; }

    public abstract Canvas getCanvas();
'''
if text.count(old) != 1:
    raise SystemExit('GameClient native projection method anchor mismatch')
text = text.replace(old, new, 1)
old = '    public static final class OpponentInfo\n'
new = '''    public static final class NativeActorProjection
    {
        private final net.runelite.api.Point ground;
        private final net.runelite.api.Point overhead;
        private final int height;
        public NativeActorProjection(net.runelite.api.Point ground,
            net.runelite.api.Point overhead, int height)
        { this.ground = ground; this.overhead = overhead; this.height = height; }
        public net.runelite.api.Point getGround() { return ground; }
        public net.runelite.api.Point getOverhead() { return overhead; }
        public int getHeight() { return height; }
    }

    public static final class OpponentInfo
'''
if text.count(old) != 1:
    raise SystemExit('GameClient NativeActorProjection class anchor mismatch')
Path(path).write_text(text.replace(old, new, 1))

path = 'client/src/Applet_Sub1.java'
text = Path(path).read_text()
old = '''    @Override
    public int getLocalTileHeight(int localX, int localY, int plane) {
        if (localX < 0 || localY < 0 || plane < 0) {
            return 0;
        }
        return Class275.method2064(localX << 2, plane, 11219, localY << 2);
    }

    @Override
    public boolean isClientThread() {
'''
new = '''    @Override
    public int getLocalTileHeight(int localX, int localY, int plane) {
        if (localX < 0 || localY < 0 || plane < 0) return 0;
        return Class275.method2064(localX << 2, plane, 11219, localY << 2);
    }

    @Override
    public NativeActorProjection getNativeLocalPlayerProjection() {
        return nativeActorProjection(Class132.aPlayer_1907);
    }

    @Override
    public NativeActorProjection getNativeNpcProjection(int npcIndex) {
        if (npcIndex < 0 || Class282.aClass356_3654 == null) return null;
        Class348_Sub22 entry = (Class348_Sub22) Class282.aClass356_3654.method3480(npcIndex, -6008);
        return entry == null ? null : nativeActorProjection(entry.aNpc_6859);
    }

    private NativeActorProjection nativeActorProjection(Class318_Sub1_Sub3_Sub3 actor) {
        if (actor == null || Class348_Sub8.aHa6654 == null) return null;
        int height = Math.max(1, actor.method2426(200));
        net.runelite.api.Point ground = nativeActorPoint(actor, 0);
        net.runelite.api.Point overhead = nativeActorPoint(actor, height);
        return ground == null || overhead == null ? null : new NativeActorProjection(ground, overhead, height);
    }

    private net.runelite.api.Point nativeActorPoint(Class318_Sub1_Sub3_Sub3 actor, int heightOffset) {
        aa_Sub2.method165(actor.plane, 0, heightOffset, 0, actor.x, 0, actor.y, (byte) 110, 0, 0);
        int x = Class239_Sub21.anIntArray6062[0];
        int y = Class239_Sub21.anIntArray6062[1];
        if (x < 0 || y < 0) return null;
        return new net.runelite.api.Point(Class295.anInt3764 + x, Class234.anInt3047 + y);
    }

    @Override
    public boolean isClientThread() {
'''
if text.count(old) != 1:
    raise SystemExit('Applet native projection anchor mismatch')
Path(path).write_text(text.replace(old, new, 1))

# Diagnostics plugin.
write('client/src/net/runelite/client/plugins/compatibilitydiagnostics/CompatibilityDiagnosticsConfig.java', '''package net.runelite.client.plugins.compatibilitydiagnostics;

import net.runelite.client.config.*;

@ConfigGroup("compatibilitydiagnostics")
public interface CompatibilityDiagnosticsConfig extends Config
{
\t@ConfigItem(keyName="showProjectionCanaries", name="Projection Canaries",
\t\tdescription="Draws native/RuneLite projection agreement geometry.")
\tdefault boolean showProjectionCanaries() { return true; }
\t@ConfigItem(keyName="showItemSlotCanaries", name="Item Slot Canaries",
\t\tdescription="Draws authoritative inventory/bank slot rectangles.")
\tdefault boolean showItemSlotCanaries() { return false; }
\t@ConfigItem(keyName="showCanaryLabels", name="Canary Labels",
\t\tdescription="Labels projection and item-slot calibration geometry.")
\tdefault boolean showCanaryLabels() { return true; }
}
''')

write('client/src/net/runelite/client/plugins/compatibilitydiagnostics/CompatibilityDiagnosticsOverlay.java', '''package net.runelite.client.plugins.compatibilitydiagnostics;

import com.GameClient;
import java.awt.*;
import javax.inject.Inject;
import net.runelite.client.compatibility.*;
import net.runelite.client.game.ItemContainerRole;
import net.runelite.client.ui.overlay.*;

class CompatibilityDiagnosticsOverlay extends Overlay
{
\tprivate final GameClient client;
\tprivate final CompatibilityDiagnosticsConfig config;
\tprivate final ProjectionCalibrationService projection;
\t@Inject CompatibilityDiagnosticsOverlay(CompatibilityDiagnosticsPlugin plugin, GameClient client,
\t\tCompatibilityDiagnosticsConfig config, ProjectionCalibrationService projection)
\t{
\t\tsuper(plugin); this.client = client; this.config = config; this.projection = projection;
\t\tsetPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ALWAYS_ON_TOP);
\t\tsetPriority(OverlayPriority.HIGHEST);
\t}
\t@Override public Dimension render(Graphics2D graphics)
\t{
\t\tif (config.showProjectionCanaries()) renderProjection(graphics, projection.getSample());
\t\tif (config.showItemSlotCanaries()) renderSlots(graphics);
\t\treturn null;
\t}
\tprivate void renderProjection(Graphics2D graphics, ProjectionCalibrationSample sample)
\t{
\t\tColor tileColor = color(sample.getTileStatus());
\t\tPolygon tile = sample.getPlayerTile();
\t\tif (tile != null)
\t\t{
\t\t\tgraphics.setStroke(new BasicStroke(2f));
\t\t\tgraphics.setColor(new Color(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue(), 45));
\t\t\tgraphics.fillPolygon(tile); graphics.setColor(tileColor); graphics.drawPolygon(tile);
\t\t}
\t\tPoint center = sample.getPlayerCenter(), nativeCenter = sample.getNativePlayerCenter();
\t\tif (center != null) drawCross(graphics, center, tileColor, 6);
\t\tif (nativeCenter != null) drawCross(graphics, nativeCenter, Color.WHITE, 3);
\t\tif (center != null && nativeCenter != null)
\t\t{
\t\t\tgraphics.setColor(tileColor); graphics.drawLine(center.x, center.y, nativeCenter.x, nativeCenter.y);
\t\t\tif (config.showCanaryLabels()) graphics.drawString("tile agreement " + sample.getTileStatus(), center.x + 8, center.y - 8);
\t\t}
\t\tPoint ground = sample.getActorGround(), overhead = sample.getActorOverhead();
\t\tPoint nativeGround = sample.getNativeActorGround(), nativeOverhead = sample.getNativeActorOverhead();
\t\tColor actorColor = color(sample.getActorStatus());
\t\tif (ground != null && overhead != null)
\t\t{
\t\t\tgraphics.setColor(actorColor); graphics.drawLine(ground.x, ground.y, overhead.x, overhead.y);
\t\t\tdrawCross(graphics, ground, actorColor, 4); drawCross(graphics, overhead, actorColor, 4);
\t\t}
\t\tif (nativeGround != null && nativeOverhead != null)
\t\t{
\t\t\tgraphics.setColor(Color.WHITE); graphics.drawLine(nativeGround.x, nativeGround.y, nativeOverhead.x, nativeOverhead.y);
\t\t\tdrawCross(graphics, nativeGround, Color.WHITE, 2); drawCross(graphics, nativeOverhead, Color.WHITE, 2);
\t\t}
\t\tif (overhead != null && config.showCanaryLabels())
\t\t{ graphics.setColor(actorColor); graphics.drawString("actor agreement: " + sample.getActorName(), overhead.x + 6, overhead.y - 5); }
\t}
\tprivate void renderSlots(Graphics2D graphics)
\t{
\t\tgraphics.setStroke(new BasicStroke(1.5f));
\t\tfor (GameClient.VisibleItemSlot slot : client.getVisibleItemSlots())
\t\t{
\t\t\tRectangle bounds = slot.getBounds();
\t\t\tColor value = slot.getRole() == ItemContainerRole.INVENTORY
\t\t\t\t? new Color(75,220,255,190) : new Color(255,170,65,190);
\t\t\tgraphics.setColor(value); graphics.draw(bounds);
\t\t\tif (config.showCanaryLabels()) graphics.drawString(Integer.toString(slot.getChildIndex()), bounds.x + 2, bounds.y + 11);
\t\t}
\t}
\tprivate static void drawCross(Graphics2D graphics, Point point, Color color, int radius)
\t{
\t\tgraphics.setColor(color); graphics.drawLine(point.x-radius, point.y, point.x+radius, point.y);
\t\tgraphics.drawLine(point.x, point.y-radius, point.x, point.y+radius);
\t}
\tprivate static Color color(CapabilityStatus status)
\t{
\t\tswitch (status)
\t\t{
\t\t\tcase AVAILABLE: return new Color(80,235,120,220);
\t\t\tcase DEGRADED: return new Color(255,195,70,220);
\t\t\tcase UNAVAILABLE: return new Color(245,75,75,220);
\t\t\tdefault: return new Color(145,170,210,210);
\t\t}
\t}
}
''')

write('client/src/net/runelite/client/plugins/compatibilitydiagnostics/CompatibilityDiagnosticsPanel.java', '''package net.runelite.client.plugins.compatibilitydiagnostics;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import net.runelite.client.compatibility.*;
import net.runelite.client.game.ItemSnapshotDiagnostics;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class CompatibilityDiagnosticsPanel extends PluginPanel
{
\tCompatibilityDiagnosticsPanel()
\t{ setLayout(new BorderLayout()); setBackground(ColorScheme.DARK_GRAY_COLOR); }
\tvoid rebuild(List<CapabilitySnapshot> capabilities, List<EventConformanceSnapshot> events,
\t\tItemSnapshotDiagnostics items, ProjectionCalibrationSample projection)
\t{
\t\tremoveAll(); JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
\t\tcontent.setBackground(ColorScheme.DARK_GRAY_COLOR); content.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
\t\tcontent.add(section("Capabilities"));
\t\tfor (CapabilitySnapshot value : capabilities)
\t\t\tcontent.add(row(value.getCapability().getDisplayName(), value.getStatus().name(), value.getReason(), color(value.getStatus())));
\t\tcontent.add(section("Event conformance"));
\t\tfor (EventConformanceSnapshot value : events)
\t\t{
\t\t\tString counts = "direct=" + value.getDirect() + ", polled=" + value.getPolled()
\t\t\t\t+ ", delivered=" + value.getDelivered() + ", dropped=" + value.getDropped()
\t\t\t\t+ ", duplicates=" + value.getDuplicates();
\t\t\tString detail = value.getLastEventClass().isEmpty() ? "No event observed"
\t\t\t\t: value.getLastEventClass() + " on " + value.getLastThread();
\t\t\tcontent.add(row(value.getChannel().getDisplayName(), counts, detail,
\t\t\t\tvalue.getDropped() > 0 || value.getDuplicates() > 0 ? new Color(235,110,85) : new Color(170,200,220)));
\t\t}
\t\tcontent.add(section("Item snapshot conformance"));
\t\tcontent.add(row("Inventory container", items.getInventoryContainer(), items.getInventoryGeometry(), Color.WHITE));
\t\tcontent.add(row("Bank container", items.getBankContainer(), items.getBankGeometry(), Color.WHITE));
\t\tcontent.add(row("Widget frame", "generation=" + items.getWidgetGeneration() + ", root=" + items.getRootInterface(),
\t\t\t"raw dynamic children=" + items.getRawWidgetItems(), Color.WHITE));
\t\tcontent.add(section("Projection canaries"));
\t\tcontent.add(row("Player tile", projection.getTileStatus().name(), projection.getTileReason(), color(projection.getTileStatus())));
\t\tcontent.add(row("Actor anchor", projection.getActorStatus().name(), projection.getActorReason(), color(projection.getActorStatus())));
\t\tadd(content, BorderLayout.NORTH); revalidate(); repaint();
\t}
\tprivate static JLabel section(String text)
\t{
\t\tJLabel label = new JLabel(text); label.setForeground(new Color(255,180,70));
\t\tlabel.setBorder(BorderFactory.createEmptyBorder(10,2,4,2)); return label;
\t}
\tprivate static JPanel row(String name, String value, String detail, Color valueColor)
\t{
\t\tJPanel panel = new JPanel(new BorderLayout(4,2)); panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
\t\tpanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
\t\tJLabel left = new JLabel(name); left.setForeground(new Color(205,205,205));
\t\tJLabel right = new JLabel(value, SwingConstants.RIGHT); right.setForeground(valueColor);
\t\tJLabel bottom = new JLabel("<html><div style='width:210px;color:#999999'>" + escape(detail) + "</div></html>");
\t\tpanel.add(left, BorderLayout.WEST); panel.add(right, BorderLayout.EAST); panel.add(bottom, BorderLayout.SOUTH);
\t\treturn panel;
\t}
\tprivate static Color color(CapabilityStatus status)
\t{
\t\tswitch (status)
\t\t{
\t\t\tcase AVAILABLE: return new Color(90,220,120);
\t\t\tcase DEGRADED: return new Color(255,190,70);
\t\t\tcase UNAVAILABLE: return new Color(235,90,90);
\t\t\tdefault: return new Color(150,170,190);
\t\t}
\t}
\tprivate static String escape(String value)
\t{ return value == null ? "" : value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
}
''')

write('client/src/net/runelite/client/plugins/compatibilitydiagnostics/CompatibilityDiagnosticsPlugin.java', '''package net.runelite.client.plugins.compatibilitydiagnostics;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.events.ClientTick;
import net.runelite.client.compatibility.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.*;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.qol.QolIcon;
import net.runelite.client.ui.*;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name="Compatibility Diagnostics",
\tdescription="Shows 634 capability, event, widget/container and projection evidence.",
\ttags={"compatibility","diagnostic","projection","events","widgets"},
\tenabledByDefault=false, loadWhenOutdated=true)
public class CompatibilityDiagnosticsPlugin extends Plugin
{
\t@Inject private ClientCapabilityService capabilities;
\t@Inject private ProjectionCalibrationService projection;
\t@Inject private ItemSnapshotService items;
\t@Inject private CompatibilityDiagnosticsOverlay overlay;
\t@Inject private OverlayManager overlayManager;
\t@Inject private ClientToolbar toolbar;
\tprivate volatile CompatibilityDiagnosticsPanel panel;
\tprivate NavigationButton navigation;
\tprivate int ticks;

\t@Provides CompatibilityDiagnosticsConfig provideConfig(ConfigManager manager)
\t{ return manager.getConfig(CompatibilityDiagnosticsConfig.class); }
\t@Override protected void startUp()
\t{
\t\tpanel = injector.getInstance(CompatibilityDiagnosticsPanel.class);
\t\tBufferedImage icon = QolIcon.letter("D", new Color(80,210,190));
\t\tnavigation = NavigationButton.builder().tooltip("Compatibility Diagnostics")
\t\t\t.icon(icon).priority(3).panel(panel).build();
\t\ttoolbar.addNavigation(navigation); overlayManager.add(overlay); publish();
\t}
\t@Override protected void shutDown()
\t{
\t\toverlayManager.remove(overlay); if (navigation != null) toolbar.removeNavigation(navigation);
\t\tpanel = null; navigation = null; ticks = 0;
\t}
\t@Subscribe public void onClientTick(ClientTick ignored) { if (++ticks % 25 == 0) publish(); }
\tprivate void publish()
\t{
\t\tfinal CompatibilityDiagnosticsPanel target = panel;
\t\tif (target == null) return;
\t\tfinal List<CapabilitySnapshot> capabilitySnapshot = capabilities.snapshots();
\t\tfinal List<EventConformanceSnapshot> eventSnapshot = EventConformance.snapshots();
\t\tfinal ItemSnapshotDiagnostics itemSnapshot = items.getDiagnostics();
\t\tfinal ProjectionCalibrationSample projectionSnapshot = projection.getSample();
\t\tSwingUtilities.invokeLater(() ->
\t\t{
\t\t\tif (target == panel) target.rebuild(capabilitySnapshot, eventSnapshot, itemSnapshot, projectionSnapshot);
\t\t});
\t}
}
''')

# Register services before plugins consume them.
path = 'client/src/net/runelite/client/RuneLite.java'
text = Path(path).read_text()
if 'import net.runelite.client.game.ItemSnapshotService;' not in text:
    text = text.replace('import net.runelite.client.game.GameStateBridge;\n',
        'import net.runelite.client.game.GameStateBridge;\nimport net.runelite.client.game.ItemSnapshotService;\n'
        'import net.runelite.client.compatibility.ClientCapabilityService;\n'
        'import net.runelite.client.compatibility.ProjectionCalibrationService;\n', 1)
old = '''\t\teventBus.register(clientUI);
\t\teventBus.register(pluginManager);
\t\teventBus.register(injector.getInstance(GameStateBridge.class));
'''
new = '''\t\teventBus.register(clientUI);
\t\teventBus.register(pluginManager);
\t\teventBus.register(injector.getInstance(GameStateBridge.class));
\t\teventBus.register(injector.getInstance(ItemSnapshotService.class));
\t\teventBus.register(injector.getInstance(ProjectionCalibrationService.class));
\t\teventBus.register(injector.getInstance(ClientCapabilityService.class));
'''
if text.count(old) != 1:
    raise SystemExit('RuneLite service registration anchor mismatch')
Path(path).write_text(text.replace(old, new, 1))

path = 'client/src/net/runelite/client/plugins/PluginManager.java'
text = Path(path).read_text()
anchor = '\t\t"chatimprovementsplugin",\n'
if text.count(anchor) != 1:
    raise SystemExit('PluginManager defaults anchor mismatch')
Path(path).write_text(text.replace(anchor, anchor + '\t\t"compatibilitydiagnosticsplugin",\n', 1))

# Capability declarations for diagnostics and fail-closed canary consumers.
annotations = [
    ('client/src/net/runelite/client/plugins/tileindicators/TileIndicatorsPlugin.java',
     '@RequiresCapabilities(ClientCapability.NATIVE_SCENE_PROJECTION)\n'),
    ('client/src/net/runelite/client/plugins/npcnames/NpcNamesPlugin.java',
     '@RequiresCapabilities({ClientCapability.NATIVE_SCENE_PROJECTION, '
     'ClientCapability.ACTOR_OVERHEAD_PROJECTION, ClientCapability.STABLE_NPC_IDENTITY})\n'),
    ('client/src/net/runelite/client/plugins/inventorygrid/InventoryGridPlugin.java',
     '@RequiresCapabilities({ClientCapability.VERIFIED_INVENTORY_CONTAINER, '
     'ClientCapability.VISIBLE_INVENTORY_SLOT_BOUNDS})\n'),
    ('client/src/net/runelite/client/plugins/inventorytags/InventoryTagsPlugin.java',
     '@RequiresCapabilities({ClientCapability.VERIFIED_INVENTORY_CONTAINER, '
     'ClientCapability.VISIBLE_INVENTORY_SLOT_BOUNDS})\n'),
    ('client/src/net/runelite/client/plugins/itemcharges/ItemChargesPlugin.java',
     '@RequiresCapabilities({ClientCapability.VERIFIED_INVENTORY_CONTAINER, '
     'ClientCapability.VISIBLE_INVENTORY_SLOT_BOUNDS})\n'),
    ('client/src/net/runelite/client/plugins/banktags/BankTagsPlugin.java',
     '@RequiresCapabilities(ClientCapability.VERIFIED_BANK_CONTAINER)\n')]
for file, annotation in annotations:
    value = Path(file).read_text()
    if 'net.runelite.client.compatibility.RequiresCapabilities' not in value:
        value = value.replace('import net.runelite.client.plugins.Plugin;\n',
            'import net.runelite.client.compatibility.ClientCapability;\n'
            'import net.runelite.client.compatibility.RequiresCapabilities;\n'
            'import net.runelite.client.plugins.Plugin;\n', 1)
    if '@RequiresCapabilities' not in value:
        value = value.replace('@PluginDescriptor(', annotation + '@PluginDescriptor(', 1)
    Path(file).write_text(value)

# Runtime gates on the two projection canaries.
path = 'client/src/net/runelite/client/plugins/tileindicators/TileIndicatorsOverlay.java'
text = Path(path).read_text()
text = text.replace('import net.runelite.client.ui.overlay.Overlay;\n',
    'import net.runelite.client.compatibility.ClientCapabilityService;\nimport net.runelite.client.ui.overlay.Overlay;\n', 1)
text = text.replace('\tprivate final TileIndicatorsConfig config;\n',
    '\tprivate final TileIndicatorsConfig config;\n\tprivate final ClientCapabilityService capabilities;\n', 1)
text = text.replace('TileIndicatorsOverlay(GameClient client, TileIndicatorsPlugin plugin, TileIndicatorsConfig config)',
    'TileIndicatorsOverlay(GameClient client, TileIndicatorsPlugin plugin, TileIndicatorsConfig config,\n\t\tClientCapabilityService capabilities)', 1)
text = text.replace('\t\tthis.config = config;\n', '\t\tthis.config = config;\n\t\tthis.capabilities = capabilities;\n', 1)
text = text.replace('\t\tif (!client.hasLocalPlayer())\n',
    '\t\tif (!client.hasLocalPlayer() || !capabilities.canUse(TileIndicatorsPlugin.class))\n', 1)
Path(path).write_text(text)

path = 'client/src/net/runelite/client/plugins/npcnames/NpcNamesOverlay.java'
text = Path(path).read_text()
text = text.replace('import net.runelite.client.ui.overlay.Overlay;\n',
    'import net.runelite.client.compatibility.ClientCapabilityService;\nimport net.runelite.client.ui.overlay.Overlay;\n', 1)
text = text.replace('\tprivate final NpcNamesConfig config;\n',
    '\tprivate final NpcNamesConfig config;\n\tprivate final ClientCapabilityService capabilities;\n', 1)
text = text.replace('NpcNamesOverlay(GameClient client, NpcNamesPlugin plugin, NpcNamesConfig config)',
    'NpcNamesOverlay(GameClient client, NpcNamesPlugin plugin, NpcNamesConfig config,\n\t\tClientCapabilityService capabilities)', 1)
text = text.replace('\t\tthis.config = config;\n', '\t\tthis.config = config;\n\t\tthis.capabilities = capabilities;\n', 1)
text = text.replace('\t\tif (!config.showNames() || !client.hasLocalPlayer())\n',
    '\t\tif (!config.showNames() || !client.hasLocalPlayer()\n\t\t\t|| !capabilities.canUse(NpcNamesPlugin.class))\n', 1)
Path(path).write_text(text)

print('Applied wave 2 capability registry, diagnostics and projection canaries')
