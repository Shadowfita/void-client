package net.runelite.client.plugins.playerindicators;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("playerindicators")
public interface PlayerIndicatorsConfig extends Config
{
    @ConfigItem(keyName = "showNames", name = "Show Player Names", description = "Shows player names above nearby players.")
    default boolean showNames() { return true; }

    @ConfigItem(keyName = "showCombatLevel", name = "Show Combat Level", description = "Appends combat levels to player names.")
    default boolean showCombatLevel() { return true; }

    @ConfigItem(keyName = "showTiles", name = "Highlight Player Tiles", description = "Draws each nearby player's occupied tile.")
    default boolean showTiles() { return false; }

    @ConfigItem(keyName = "showLocalPlayer", name = "Include Local Player", description = "Also labels/highlights your own player.")
    default boolean showLocalPlayer() { return false; }

    @Range(min = 1, max = 64)
    @ConfigItem(keyName = "drawDistance", name = "Draw Distance", description = "Maximum scene-tile distance.")
    default int drawDistance() { return 24; }

    @Alpha
    @ConfigItem(keyName = "playerColor", name = "Player Color", description = "Name and tile highlight colour.")
    default Color playerColor() { return new Color(0, 220, 255, 210); }
}
