package net.runelite.client.plugins.antidrag;

import com.GameClient;
import com.google.inject.Provides;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.qol.QolInventoryLayout;

@PluginDescriptor(
	name = "Anti Drag",
	description = "Prevents accidental inventory drags during fast clicking and gear switches.",
	tags = {"drag", "inventory", "switching", "gear"},
	enabledByDefault = true,
	loadWhenOutdated = true
)
public class AntiDragPlugin extends Plugin
{
	@Inject private GameClient client;
	@Inject private MouseManager mouseManager;
	@Inject private AntiDragConfig config;
	private final DragListener listener = new DragListener();

	@Provides AntiDragConfig provideConfig(ConfigManager manager) { return manager.getConfig(AntiDragConfig.class); }
	@Override protected void startUp() { mouseManager.registerMouseListener(0, listener); }
	@Override protected void shutDown() { mouseManager.unregisterMouseListener(listener); listener.reset(); }

	private final class DragListener extends MouseAdapter
	{
		private long pressedAt;
		private Point pressedPoint;
		private boolean inventoryPress;

		@Override public MouseEvent mousePressed(MouseEvent event)
		{
			if (event.getButton() == MouseEvent.BUTTON1 && QolInventoryLayout.slotAt(client, event.getPoint()) >= 0)
			{
				pressedAt = System.currentTimeMillis();
				pressedPoint = event.getPoint();
				inventoryPress = true;
			}
			else
			{
				reset();
			}
			return event;
		}

		@Override public MouseEvent mouseDragged(MouseEvent event)
		{
			if (!inventoryPress || pressedPoint == null)
			{
				return event;
			}
			long elapsed = System.currentTimeMillis() - pressedAt;
			double distance = pressedPoint.distance(event.getPoint());
			boolean distanceAllows = config.dragDistance() > 0 && distance >= config.dragDistance();
			if (elapsed < config.dragDelay() && !distanceAllows)
			{
				event.consume();
			}
			return event;
		}

		@Override public MouseEvent mouseReleased(MouseEvent event) { reset(); return event; }
		@Override public MouseEvent mouseExited(MouseEvent event) { reset(); return event; }
		private void reset() { pressedAt = 0L; pressedPoint = null; inventoryPress = false; }
	}
}
