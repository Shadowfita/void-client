package net.runelite.client.plugins.playerindicators;

import com.GameClient;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

class PlayerIndicatorsOverlay extends Overlay
{
    private static final BasicStroke STROKE = new BasicStroke(2f);
    private final GameClient client;
    private final PlayerIndicatorsConfig config;

    @Inject
    PlayerIndicatorsOverlay(GameClient client, PlayerIndicatorsPlugin plugin, PlayerIndicatorsConfig config)
    {
        super(plugin);
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!client.hasLocalPlayer()) return null;
        int localX = client.getLocalPlayerSceneX();
        int localY = client.getLocalPlayerSceneY();
        for (GameClient.PlayerInfo player : client.getPlayers())
        {
            if (player.getPlane() != client.getPlane() || (player.isLocalPlayer() && !config.showLocalPlayer())) continue;
            int sceneX = player.getLocalX() >> Perspective.LOCAL_COORD_BITS;
            int sceneY = player.getLocalY() >> Perspective.LOCAL_COORD_BITS;
            if (Math.max(Math.abs(sceneX - localX), Math.abs(sceneY - localY)) > config.drawDistance()) continue;

            LocalPoint point = new LocalPoint(player.getLocalX(), player.getLocalY());
            Color color = config.playerColor();
            if (config.showTiles())
            {
                Polygon tile = Perspective.getCanvasTilePoly(client, point);
                if (tile != null) OverlayUtil.renderPolygon(graphics, tile, color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 35), STROKE);
            }
            if (config.showNames())
            {
                String name = player.getName();
                if (config.showCombatLevel() && player.getCombatLevel() >= 0) name += " (level-" + player.getCombatLevel() + ")";
                Point location = Perspective.getCanvasTextLocation(client, graphics, point, name, player.getHeight() + 24);
                if (location != null) OverlayUtil.renderTextLocation(graphics, location, name, color);
            }
        }
        return null;
    }
}
