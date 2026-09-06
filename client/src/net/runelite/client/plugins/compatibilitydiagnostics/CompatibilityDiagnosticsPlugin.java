package net.runelite.client.plugins.compatibilitydiagnostics;

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
	description="Shows 634 capability, event, widget/container and projection evidence.",
	tags={"compatibility","diagnostic","projection","events","widgets"},
	enabledByDefault=false, loadWhenOutdated=true)
public class CompatibilityDiagnosticsPlugin extends Plugin
{
	@Inject private ClientCapabilityService capabilities;
	@Inject private ProjectionCalibrationService projection;
	@Inject private ItemSnapshotService items;
	@Inject private CompatibilityDiagnosticsOverlay overlay;
	@Inject private OverlayManager overlayManager;
	@Inject private ClientToolbar toolbar;
	private volatile CompatibilityDiagnosticsPanel panel;
	private NavigationButton navigation;
	private int ticks;

	@Provides CompatibilityDiagnosticsConfig provideConfig(ConfigManager manager)
	{ return manager.getConfig(CompatibilityDiagnosticsConfig.class); }
	@Override protected void startUp()
	{
		panel = injector.getInstance(CompatibilityDiagnosticsPanel.class);
		BufferedImage icon = QolIcon.letter("D", new Color(80,210,190));
		navigation = NavigationButton.builder().tooltip("Compatibility Diagnostics")
			.icon(icon).priority(3).panel(panel).build();
		toolbar.addNavigation(navigation); overlayManager.add(overlay); publish();
	}
	@Override protected void shutDown()
	{
		overlayManager.remove(overlay); if (navigation != null) toolbar.removeNavigation(navigation);
		panel = null; navigation = null; ticks = 0;
	}
	@Subscribe public void onClientTick(ClientTick ignored) { if (++ticks % 25 == 0) publish(); }
	private void publish()
	{
		final CompatibilityDiagnosticsPanel target = panel;
		if (target == null) return;
		final List<CapabilitySnapshot> capabilitySnapshot = capabilities.snapshots();
		final List<EventConformanceSnapshot> eventSnapshot = EventConformance.snapshots();
		final ItemSnapshotDiagnostics itemSnapshot = items.getDiagnostics();
		final ProjectionCalibrationSample projectionSnapshot = projection.getSample();
		SwingUtilities.invokeLater(() ->
		{
			if (target == panel) target.rebuild(capabilitySnapshot, eventSnapshot, itemSnapshot, projectionSnapshot);
		});
	}
}
