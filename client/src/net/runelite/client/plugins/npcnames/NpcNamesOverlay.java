package net.runelite.client.plugins.npcnames;

import com.GameClient;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class NpcNamesOverlay extends Overlay
{
	private final GameClient client;
	private final NpcNamesConfig config;

	@Inject
	NpcNamesOverlay(GameClient client, NpcNamesPlugin plugin, NpcNamesConfig config)
	{
		super(plugin);
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
		setPriority(OverlayPriority.MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showNames() || !client.hasLocalPlayer())
		{
			return null;
		}

		int localSceneX = client.getLocalPlayerSceneX();
		int localSceneY = client.getLocalPlayerSceneY();
		int maxDistance = config.drawDistance();
		for (GameClient.NpcInfo npc : client.getNpcs())
		{
			if (npc.getPlane() != client.getPlane())
			{
				continue;
			}

			int sceneX = npc.getLocalX() >> Perspective.LOCAL_COORD_BITS;
			int sceneY = npc.getLocalY() >> Perspective.LOCAL_COORD_BITS;
			if (Math.max(Math.abs(sceneX - localSceneX), Math.abs(sceneY - localSceneY)) > maxDistance)
			{
				continue;
			}

			LocalPoint localPoint = new LocalPoint(npc.getLocalX(), npc.getLocalY());
			Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, npc.getName(), npc.getHeight() + 24);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLocation, npc.getName(), config.textColor());
			}
		}

		return null;
	}
}
