package net.runelite.client.plugins.entityhider;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("entityhider")
public interface EntityHiderConfig extends Config
{
	@ConfigItem(keyName = "hideOtherPlayers", name = "Hide Other Players", description = "Prevents other player models from being rendered. The local player remains visible.")
	default boolean hideOtherPlayers() { return false; }

	@ConfigItem(keyName = "hideNpcs", name = "Hide NPCs", description = "Prevents NPC models from being rendered.")
	default boolean hideNpcs() { return false; }

	@ConfigItem(keyName = "hideProjectiles", name = "Hide Projectiles", description = "Prevents projectile models from being rendered.")
	default boolean hideProjectiles() { return false; }
}
