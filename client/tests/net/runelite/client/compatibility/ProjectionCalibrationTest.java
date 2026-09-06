package net.runelite.client.compatibility;

import java.awt.Point;
import java.awt.Polygon;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProjectionCalibrationTest
{
	private static Polygon tile()
	{ return new Polygon(new int[]{90,110,110,90}, new int[]{105,105,115,115}, 4); }

	@Test public void acceptsTileWhenNativeAndRuneLiteReferencesAgree()
	{
		ProjectionCalibration.Validation value = ProjectionCalibration.validateTile(
			800, 600, new Point(100,110), tile(), new Point(102,109));
		assertTrue(value.getReason(), value.isValid());
	}

	@Test public void rejectsTileWhenNativeReferenceDisagrees()
	{
		ProjectionCalibration.Validation value = ProjectionCalibration.validateTile(
			800, 600, new Point(100,110), tile(), new Point(145,155));
		assertFalse(value.isValid());
	}

	@Test public void acceptsActorAnchorsWhenIndependentReferencesAgree()
	{
		ProjectionCalibration.Validation value = ProjectionCalibration.validateActor(800,600,
			new Point(200,300), new Point(200,240), new Point(201,299), new Point(201,241));
		assertTrue(value.getReason(), value.isValid());
	}

	@Test public void rejectsActorAnchorWithInvertedHeight()
	{
		ProjectionCalibration.Validation value = ProjectionCalibration.validateActor(800,600,
			new Point(200,300), new Point(200,340), new Point(200,300), new Point(200,340));
		assertFalse(value.isValid());
	}
}
