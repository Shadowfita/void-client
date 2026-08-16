package net.runelite.client.plugins.idlenotifier;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("idlenotifier")
public interface IdleNotifierConfig extends Config
{
	@ConfigItem(keyName = "notifyIdle", name = "Idle Notification", description = "Notifies when animation and movement have stopped for the configured delay.")
	default boolean notifyIdle() { return true; }

	@Range(min = 1, max = 120)
	@ConfigItem(keyName = "idleDelay", name = "Idle Delay (seconds)", description = "How long the player must remain idle before notifying.")
	default int idleDelay() { return 5; }

	@ConfigItem(keyName = "notifyLogout", name = "Logout Notification", description = "Notifies when the local player disappears after being logged in.")
	default boolean notifyLogout() { return true; }

	@ConfigItem(keyName = "notifyLowHitpoints", name = "Low Hitpoints", description = "Notifies when boosted/current hitpoints reach the threshold.")
	default boolean notifyLowHitpoints() { return true; }

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "hitpointsThreshold", name = "Hitpoints Threshold", description = "Current hitpoints threshold.")
	default int hitpointsThreshold() { return 10; }

	@ConfigItem(keyName = "notifyLowPrayer", name = "Low Prayer", description = "Notifies when current prayer reaches the threshold.")
	default boolean notifyLowPrayer() { return true; }

	@Range(min = 0, max = 99)
	@ConfigItem(keyName = "prayerThreshold", name = "Prayer Threshold", description = "Current prayer threshold.")
	default int prayerThreshold() { return 5; }
}
