package net.runelite.client.plugins.compatibilitydiagnostics;

import com.GameClient;
import java.awt.*;
import javax.inject.Inject;
import net.runelite.client.compatibility.*;
import net.runelite.client.game.ItemContainerRole;
import net.runelite.client.ui.overlay.*;

class CompatibilityDiagnosticsOverlay extends Overlay
{
	private final GameClient client;
	private final CompatibilityDiagnosticsConfig config;
	private final ProjectionCalibrationService projection;
	@Inject CompatibilityDiagnosticsOverlay(CompatibilityDiagnosticsPlugin plugin, GameClient client,
		CompatibilityDiagnosticsConfig config, ProjectionCalibrationService projection)
	{
		super(plugin); this.client = client; this.config = config; this.projection = projection;
		setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.HIGHEST);
	}
	@Override public Dimension render(Graphics2D graphics)
	{
		if (config.showProjectionCanaries()) renderProjection(graphics, projection.getSample());
		if (config.showItemSlotCanaries()) renderSlots(graphics);
		return null;
	}
	private void renderProjection(Graphics2D graphics, ProjectionCalibrationSample sample)
	{
		Color tileColor = color(sample.getTileStatus());
		Polygon tile = sample.getPlayerTile();
		if (tile != null)
		{
			graphics.setStroke(new BasicStroke(2f));
			graphics.setColor(new Color(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue(), 45));
			graphics.fillPolygon(tile); graphics.setColor(tileColor); graphics.drawPolygon(tile);
		}
		Point center = sample.getPlayerCenter(), nativeCenter = sample.getNativePlayerCenter();
		if (center != null) drawCross(graphics, center, tileColor, 6);
		if (nativeCenter != null) drawCross(graphics, nativeCenter, Color.WHITE, 3);
		if (center != null && nativeCenter != null)
		{
			graphics.setColor(tileColor); graphics.drawLine(center.x, center.y, nativeCenter.x, nativeCenter.y);
			if (config.showCanaryLabels()) graphics.drawString("tile agreement " + sample.getTileStatus(), center.x + 8, center.y - 8);
		}
		Point ground = sample.getActorGround(), overhead = sample.getActorOverhead();
		Point nativeGround = sample.getNativeActorGround(), nativeOverhead = sample.getNativeActorOverhead();
		Color actorColor = color(sample.getActorStatus());
		if (ground != null && overhead != null)
		{
			graphics.setColor(actorColor); graphics.drawLine(ground.x, ground.y, overhead.x, overhead.y);
			drawCross(graphics, ground, actorColor, 4); drawCross(graphics, overhead, actorColor, 4);
		}
		if (nativeGround != null && nativeOverhead != null)
		{
			graphics.setColor(Color.WHITE); graphics.drawLine(nativeGround.x, nativeGround.y, nativeOverhead.x, nativeOverhead.y);
			drawCross(graphics, nativeGround, Color.WHITE, 2); drawCross(graphics, nativeOverhead, Color.WHITE, 2);
		}
		if (overhead != null && config.showCanaryLabels())
		{ graphics.setColor(actorColor); graphics.drawString("actor agreement: " + sample.getActorName(), overhead.x + 6, overhead.y - 5); }
	}
	private void renderSlots(Graphics2D graphics)
	{
		graphics.setStroke(new BasicStroke(1.5f));
		for (GameClient.VisibleItemSlot slot : client.getVisibleItemSlots())
		{
			Rectangle bounds = slot.getBounds();
			Color value = slot.getRole() == ItemContainerRole.INVENTORY
				? new Color(75,220,255,190) : new Color(255,170,65,190);
			graphics.setColor(value); graphics.draw(bounds);
			if (config.showCanaryLabels()) graphics.drawString(Integer.toString(slot.getChildIndex()), bounds.x + 2, bounds.y + 11);
		}
	}
	private static void drawCross(Graphics2D graphics, Point point, Color color, int radius)
	{
		graphics.setColor(color); graphics.drawLine(point.x-radius, point.y, point.x+radius, point.y);
		graphics.drawLine(point.x, point.y-radius, point.x, point.y+radius);
	}
	private static Color color(CapabilityStatus status)
	{
		switch (status)
		{
			case AVAILABLE: return new Color(80,235,120,220);
			case DEGRADED: return new Color(255,195,70,220);
			case UNAVAILABLE: return new Color(245,75,75,220);
			default: return new Color(145,170,210,210);
		}
	}
}
