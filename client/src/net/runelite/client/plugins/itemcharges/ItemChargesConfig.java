package net.runelite.client.plugins.itemcharges;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("itemcharges")
public interface ItemChargesConfig extends Config
{
	@ConfigItem(keyName = "showPotionDoses", name = "Potion Doses", description = "Shows numeric dose suffixes such as (3) and (4).")
	default boolean showPotionDoses() { return true; }

	@ConfigItem(keyName = "showNamedCharges", name = "Named Charges", description = "Shows charges or uses parsed from item names.")
	default boolean showNamedCharges() { return true; }

	@ConfigItem(keyName = "textColor", name = "Text Color", description = "Configures charge text color.")
	default Color textColor() { return new Color(255, 235, 90); }
}
