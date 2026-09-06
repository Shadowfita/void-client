package net.runelite.client.compatibility;

import java.awt.Point;
import java.awt.Polygon;

public final class ProjectionCalibrationSample
{
	private final CapabilityStatus tileStatus, actorStatus;
	private final String tileReason, actorReason, actorName;
	private final Point playerCenter, nativePlayerCenter, actorGround, actorOverhead,
		nativeActorGround, nativeActorOverhead;
	private final Polygon playerTile;
	private final long generation;

	ProjectionCalibrationSample(CapabilityStatus tileStatus, String tileReason,
		CapabilityStatus actorStatus, String actorReason, Point playerCenter,
		Point nativePlayerCenter, Polygon playerTile, Point actorGround,
		Point actorOverhead, Point nativeActorGround, Point nativeActorOverhead,
		String actorName, long generation)
	{
		this.tileStatus = tileStatus; this.tileReason = tileReason;
		this.actorStatus = actorStatus; this.actorReason = actorReason;
		this.playerCenter = copy(playerCenter); this.nativePlayerCenter = copy(nativePlayerCenter);
		this.playerTile = copy(playerTile); this.actorGround = copy(actorGround);
		this.actorOverhead = copy(actorOverhead); this.nativeActorGround = copy(nativeActorGround);
		this.nativeActorOverhead = copy(nativeActorOverhead);
		this.actorName = actorName == null ? "" : actorName; this.generation = generation;
	}

	static ProjectionCalibrationSample unknown(String reason)
	{
		return new ProjectionCalibrationSample(CapabilityStatus.UNKNOWN, reason,
			CapabilityStatus.UNKNOWN, reason, null, null, null, null, null, null, null, "", -1L);
	}
	private static Point copy(Point point) { return point == null ? null : new Point(point); }
	private static Polygon copy(Polygon polygon)
	{ return polygon == null ? null : new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints); }
	public CapabilityStatus getTileStatus() { return tileStatus; }
	public String getTileReason() { return tileReason; }
	public CapabilityStatus getActorStatus() { return actorStatus; }
	public String getActorReason() { return actorReason; }
	public Point getPlayerCenter() { return copy(playerCenter); }
	public Point getNativePlayerCenter() { return copy(nativePlayerCenter); }
	public Polygon getPlayerTile() { return copy(playerTile); }
	public Point getActorGround() { return copy(actorGround); }
	public Point getActorOverhead() { return copy(actorOverhead); }
	public Point getNativeActorGround() { return copy(nativeActorGround); }
	public Point getNativeActorOverhead() { return copy(nativeActorOverhead); }
	public String getActorName() { return actorName; }
	public long getGeneration() { return generation; }
}
