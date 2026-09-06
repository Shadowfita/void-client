package net.runelite.client.compatibility;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

/** Pure validators comparing RuneLite projection with an independent native reference. */
public final class ProjectionCalibration
{
	private static final int REFERENCE_TOLERANCE_PX = 4;
	private ProjectionCalibration() {}

	public static Validation validateTile(int canvasWidth, int canvasHeight,
		Point projectedCenter, Polygon tile, Point nativeActorGround)
	{
		if (canvasWidth <= 0 || canvasHeight <= 0)
			return Validation.invalid("Physical canvas dimensions are unavailable");
		if (projectedCenter == null || nativeActorGround == null)
			return Validation.invalid("Projected or native player centre is unavailable");
		if (tile == null || tile.npoints != 4)
			return Validation.invalid("Tile projection did not produce four terrain corners");
		long area = Math.abs(signedArea2(tile)) / 2L;
		if (area < 4L) return Validation.invalid("Projected tile area is degenerate: " + area + " px²");
		Rectangle bounds = tile.getBounds();
		if (bounds.width <= 1 || bounds.height <= 1
			|| bounds.width > canvasWidth || bounds.height > canvasHeight)
			return Validation.invalid("Projected tile dimensions are implausible: "
				+ bounds.width + "x" + bounds.height);
		Rectangle tolerance = new Rectangle(bounds);
		tolerance.grow(REFERENCE_TOLERANCE_PX, REFERENCE_TOLERANCE_PX);
		if (!tolerance.contains(projectedCenter) || !tolerance.contains(nativeActorGround))
			return Validation.invalid("Projected tile does not contain both projection references");
		double error = projectedCenter.distance(nativeActorGround);
		if (error > REFERENCE_TOLERANCE_PX)
			return Validation.invalid("RuneLite/native player projection differs by " + Math.round(error) + " px");
		Rectangle extendedCanvas = new Rectangle(-canvasWidth / 2, -canvasHeight / 2,
			canvasWidth * 2, canvasHeight * 2);
		if (!extendedCanvas.contains(projectedCenter))
			return Validation.invalid("Projected local-player centre is far outside the canvas");
		return Validation.valid("Native actor centre and projected terrain tile agree",
			Math.round(error), bounds);
	}

	public static Validation validateActor(int canvasWidth, int canvasHeight,
		Point projectedGround, Point projectedOverhead, Point nativeGround, Point nativeOverhead)
	{
		if (projectedGround == null || projectedOverhead == null
			|| nativeGround == null || nativeOverhead == null)
			return Validation.invalid("Projected or native actor anchors are unavailable");
		double projectedHeight = projectedGround.distance(projectedOverhead);
		double nativeHeight = nativeGround.distance(nativeOverhead);
		if (projectedHeight < 1.0 || nativeHeight < 1.0
			|| projectedHeight > Math.max(canvasWidth, canvasHeight)
			|| nativeHeight > Math.max(canvasWidth, canvasHeight))
			return Validation.invalid("Actor height projected to an implausible screen distance");
		if (projectedOverhead.y >= projectedGround.y || nativeOverhead.y >= nativeGround.y)
			return Validation.invalid("Increasing world height did not move both anchors upward");
		double groundError = projectedGround.distance(nativeGround);
		double overheadError = projectedOverhead.distance(nativeOverhead);
		if (groundError > REFERENCE_TOLERANCE_PX || overheadError > REFERENCE_TOLERANCE_PX)
			return Validation.invalid("RuneLite/native actor anchors differ by ground="
				+ Math.round(groundError) + " px, overhead=" + Math.round(overheadError) + " px");
		return Validation.valid("RuneLite actor anchors agree with the native 634 actor projection",
			Math.round(Math.max(groundError, overheadError)),
			new Rectangle(Math.min(projectedGround.x, projectedOverhead.x),
				Math.min(projectedGround.y, projectedOverhead.y),
				Math.max(1, Math.abs(projectedGround.x - projectedOverhead.x)),
				Math.max(1, Math.abs(projectedGround.y - projectedOverhead.y))));
	}

	private static long signedArea2(Polygon polygon)
	{
		long area = 0L;
		for (int i = 0; i < polygon.npoints; i++)
		{
			int next = (i + 1) % polygon.npoints;
			area += (long) polygon.xpoints[i] * polygon.ypoints[next]
				- (long) polygon.xpoints[next] * polygon.ypoints[i];
		}
		return area;
	}

	public static final class Validation
	{
		private final boolean valid;
		private final String reason;
		private final long metric;
		private final Rectangle bounds;
		private Validation(boolean valid, String reason, long metric, Rectangle bounds)
		{
			this.valid = valid; this.reason = reason; this.metric = metric;
			this.bounds = bounds == null ? null : new Rectangle(bounds);
		}
		private static Validation valid(String reason, long metric, Rectangle bounds)
		{ return new Validation(true, reason, metric, bounds); }
		private static Validation invalid(String reason)
		{ return new Validation(false, reason, 0L, null); }
		public boolean isValid() { return valid; }
		public String getReason() { return reason; }
		public long getMetric() { return metric; }
		public Rectangle getBounds() { return bounds == null ? null : new Rectangle(bounds); }
	}
}
