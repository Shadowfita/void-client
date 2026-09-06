package net.runelite.client.compatibility;

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
	private static final int REQUIRED_SUCCESS_STREAK = 4;
	private static final int REQUIRED_FAILURE_STREAK = 3;
	private final GameClient client;
	private volatile ProjectionCalibrationSample sample =
		ProjectionCalibrationSample.unknown("Awaiting a logged-in scene");
	private int tickCounter, tileSuccesses, tileFailures, actorSuccesses, actorFailures;
	private long sampleGeneration;

	@Inject
	private ProjectionCalibrationService(GameClient client) { this.client = client; }

	@Subscribe
	public void onClientTick(ClientTick ignored)
	{
		if (++tickCounter % 5 == 0) calibrate();
	}

	private void calibrate()
	{
		if (!client.hasLocalPlayer()) { reset("Awaiting a logged-in scene"); return; }
		Dimension canvas = client.getRealDimensions();
		if (canvas == null || canvas.width <= 0 || canvas.height <= 0)
		{ reset("Physical canvas dimensions are unavailable"); return; }

		LocalPoint player = new LocalPoint(client.getLocalPlayerLocalX(), client.getLocalPlayerLocalY());
		LocalPoint playerTilePoint = LocalPoint.fromScene(client.getLocalPlayerSceneX(), client.getLocalPlayerSceneY());
		int plane = client.getPlane();
		Point playerCenter = toAwt(Perspective.localToCanvas(client, player, plane));
		Polygon playerTile = Perspective.getCanvasTilePoly(client, playerTilePoint);
		GameClient.NativeActorProjection nativePlayer = client.getNativeLocalPlayerProjection();
		Point nativePlayerCenter = nativePlayer == null ? null : toAwt(nativePlayer.getGround());
		ProjectionCalibration.Validation tileValidation = ProjectionCalibration.validateTile(
			canvas.width, canvas.height, playerCenter, playerTile, nativePlayerCenter);
		CapabilityStatus tileStatus = advance(tileValidation.isValid(), true);

		GameClient.NpcInfo actor = nearestVisibleNpc(client.getNpcs(), player, plane);
		Point actorGround = null, actorOverhead = null, nativeActorGround = null, nativeActorOverhead = null;
		String actorName = "";
		CapabilityStatus actorStatus;
		String actorReason;
		if (actor == null)
		{
			actorStatus = CapabilityStatus.UNKNOWN;
			actorReason = "No nearby NPC is available for the overhead-anchor canary";
			actorSuccesses = actorFailures = 0;
		}
		else
		{
			actorName = actor.getName();
			LocalPoint point = new LocalPoint(actor.getLocalX(), actor.getLocalY());
			GameClient.NativeActorProjection nativeActor = client.getNativeNpcProjection(actor.getIndex());
			int height = nativeActor == null ? Math.max(1, actor.getHeight()) : nativeActor.getHeight();
			actorGround = toAwt(Perspective.localToCanvas(client, point, plane, 0));
			actorOverhead = toAwt(Perspective.localToCanvas(client, point, plane, height));
			nativeActorGround = nativeActor == null ? null : toAwt(nativeActor.getGround());
			nativeActorOverhead = nativeActor == null ? null : toAwt(nativeActor.getOverhead());
			ProjectionCalibration.Validation validation = ProjectionCalibration.validateActor(
				canvas.width, canvas.height, actorGround, actorOverhead, nativeActorGround, nativeActorOverhead);
			actorStatus = advance(validation.isValid(), false);
			actorReason = validation.getReason();
		}

		sample = new ProjectionCalibrationSample(tileStatus, tileValidation.getReason(),
			actorStatus, actorReason, playerCenter, nativePlayerCenter, playerTile,
			actorGround, actorOverhead, nativeActorGround, nativeActorOverhead,
			actorName, ++sampleGeneration);
	}

	private CapabilityStatus advance(boolean valid, boolean tile)
	{
		if (tile)
		{
			if (valid) { tileSuccesses++; tileFailures = 0; }
			else { tileFailures++; tileSuccesses = 0; }
			return status(tileSuccesses, tileFailures);
		}
		if (valid) { actorSuccesses++; actorFailures = 0; }
		else { actorFailures++; actorSuccesses = 0; }
		return status(actorSuccesses, actorFailures);
	}
	private static CapabilityStatus status(int successes, int failures)
	{
		if (successes >= REQUIRED_SUCCESS_STREAK) return CapabilityStatus.AVAILABLE;
		if (failures >= REQUIRED_FAILURE_STREAK) return CapabilityStatus.UNAVAILABLE;
		return CapabilityStatus.DEGRADED;
	}
	private void reset(String reason)
	{
		tileSuccesses = tileFailures = actorSuccesses = actorFailures = 0;
		sample = ProjectionCalibrationSample.unknown(reason);
	}
	private static GameClient.NpcInfo nearestVisibleNpc(List<GameClient.NpcInfo> values,
		LocalPoint player, int plane)
	{
		GameClient.NpcInfo nearest = null; int best = Integer.MAX_VALUE;
		for (GameClient.NpcInfo npc : values)
		{
			if (npc.getPlane() != plane || npc.getHeight() <= 0 || npc.getIndex() < 0) continue;
			int distance = Math.max(Math.abs(npc.getLocalX() - player.getX()),
				Math.abs(npc.getLocalY() - player.getY()));
			if (distance < best && distance <= 32 * Perspective.LOCAL_TILE_SIZE)
			{ best = distance; nearest = npc; }
		}
		return nearest;
	}
	private static Point toAwt(net.runelite.api.Point value)
	{ return value == null ? null : new Point(value.getX(), value.getY()); }
	public ProjectionCalibrationSample getSample() { return sample; }
}
