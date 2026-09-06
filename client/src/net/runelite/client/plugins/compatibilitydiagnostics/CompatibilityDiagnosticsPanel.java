package net.runelite.client.plugins.compatibilitydiagnostics;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import net.runelite.client.compatibility.*;
import net.runelite.client.game.ItemSnapshotDiagnostics;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class CompatibilityDiagnosticsPanel extends PluginPanel
{
	CompatibilityDiagnosticsPanel()
	{ setLayout(new BorderLayout()); setBackground(ColorScheme.DARK_GRAY_COLOR); }
	void rebuild(List<CapabilitySnapshot> capabilities, List<EventConformanceSnapshot> events,
		ItemSnapshotDiagnostics items, ProjectionCalibrationSample projection)
	{
		removeAll(); JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR); content.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		content.add(section("Capabilities"));
		for (CapabilitySnapshot value : capabilities)
			content.add(row(value.getCapability().getDisplayName(), value.getStatus().name(), value.getReason(), color(value.getStatus())));
		content.add(section("Event conformance"));
		for (EventConformanceSnapshot value : events)
		{
			String counts = "direct=" + value.getDirect() + ", polled=" + value.getPolled()
				+ ", delivered=" + value.getDelivered() + ", dropped=" + value.getDropped()
				+ ", duplicates=" + value.getDuplicates();
			String detail = value.getLastEventClass().isEmpty() ? "No event observed"
				: value.getLastEventClass() + " on " + value.getLastThread();
			content.add(row(value.getChannel().getDisplayName(), counts, detail,
				value.getDropped() > 0 || value.getDuplicates() > 0 ? new Color(235,110,85) : new Color(170,200,220)));
		}
		content.add(section("Item snapshot conformance"));
		content.add(row("Inventory container", items.getInventoryContainer(), items.getInventoryGeometry(), Color.WHITE));
		content.add(row("Bank container", items.getBankContainer(), items.getBankGeometry(), Color.WHITE));
		content.add(row("Widget frame", "generation=" + items.getWidgetGeneration() + ", root=" + items.getRootInterface(),
			"raw dynamic children=" + items.getRawWidgetItems(), Color.WHITE));
		content.add(section("Projection canaries"));
		content.add(row("Player tile", projection.getTileStatus().name(), projection.getTileReason(), color(projection.getTileStatus())));
		content.add(row("Actor anchor", projection.getActorStatus().name(), projection.getActorReason(), color(projection.getActorStatus())));
		add(content, BorderLayout.NORTH); revalidate(); repaint();
	}
	private static JLabel section(String text)
	{
		JLabel label = new JLabel(text); label.setForeground(new Color(255,180,70));
		label.setBorder(BorderFactory.createEmptyBorder(10,2,4,2)); return label;
	}
	private static JPanel row(String name, String value, String detail, Color valueColor)
	{
		JPanel panel = new JPanel(new BorderLayout(4,2)); panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
		JLabel left = new JLabel(name); left.setForeground(new Color(205,205,205));
		JLabel right = new JLabel(value, SwingConstants.RIGHT); right.setForeground(valueColor);
		JLabel bottom = new JLabel("<html><div style='width:210px;color:#999999'>" + escape(detail) + "</div></html>");
		panel.add(left, BorderLayout.WEST); panel.add(right, BorderLayout.EAST); panel.add(bottom, BorderLayout.SOUTH);
		return panel;
	}
	private static Color color(CapabilityStatus status)
	{
		switch (status)
		{
			case AVAILABLE: return new Color(90,220,120);
			case DEGRADED: return new Color(255,190,70);
			case UNAVAILABLE: return new Color(235,90,90);
			default: return new Color(150,170,190);
		}
	}
	private static String escape(String value)
	{ return value == null ? "" : value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
}
