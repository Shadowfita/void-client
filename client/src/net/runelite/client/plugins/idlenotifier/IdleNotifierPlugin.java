package net.runelite.client.plugins.idlenotifier;

import com.GameClient;
import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Skill;
import net.runelite.api.events.ClientTick;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Idle Notifier",
	description = "Notifies when skilling stops, the player logs out, or vital stats are low.",
	tags = {"idle", "notification", "logout", "hitpoints", "prayer"},
	enabledByDefault = true,
	loadWhenOutdated = true
)
public class IdleNotifierPlugin extends Plugin
{
	@Inject private GameClient client;
	@Inject private Notifier notifier;
	@Inject private IdleNotifierConfig config;

	private boolean hadPlayer;
	private long playerMissingSince;
	private long idleSince;
	private int lastAnimation = -1;
	private int lastX = -1;
	private int lastY = -1;
	private boolean idleNotified;
	private boolean hitpointsNotified;
	private boolean prayerNotified;

	@Provides
	IdleNotifierConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(IdleNotifierConfig.class);
	}

	@Override
	protected void startUp()
	{
		reset();
	}

	@Override
	protected void shutDown()
	{
		reset();
	}

	@Subscribe
	public void onClientTick(ClientTick tick)
	{
		long now = System.currentTimeMillis();
		if (!client.hasLocalPlayer())
		{
			if (hadPlayer)
			{
				if (playerMissingSince == 0L)
				{
					playerMissingSince = now;
				}
				else if (config.notifyLogout() && now - playerMissingSince >= 1500L)
				{
					notifier.notify("You have logged out.");
					hadPlayer = false;
				}
			}
			return;
		}

		hadPlayer = true;
		playerMissingSince = 0L;
		int animation = client.getLocalPlayerAnimation();
		int x = client.getLocalPlayerLocalX();
		int y = client.getLocalPlayerLocalY();
		boolean active = animation != -1 || x != lastX || y != lastY;
		if (active)
		{
			idleSince = 0L;
			idleNotified = false;
		}
		else
		{
			if (idleSince == 0L && (lastAnimation != -1 || lastX != -1))
			{
				idleSince = now;
			}
			if (config.notifyIdle() && !idleNotified && idleSince != 0L && now - idleSince >= config.idleDelay() * 1000L)
			{
				notifier.notify("You are now idle.");
				idleNotified = true;
			}
		}
		lastAnimation = animation;
		lastX = x;
		lastY = y;
		checkVitals();
	}

	private void checkVitals()
	{
		for (GameClient.SkillSnapshot skill : client.getSkillSnapshots())
		{
			if (skill.getSkill() == Skill.HITPOINTS)
			{
				int current = skill.getBoostedLevel();
				if (config.notifyLowHitpoints() && current > 0 && current <= config.hitpointsThreshold())
				{
					if (!hitpointsNotified)
					{
						notifier.notify("Low hitpoints: " + current + ".");
						hitpointsNotified = true;
					}
				}
				else if (current > config.hitpointsThreshold())
				{
					hitpointsNotified = false;
				}
			}
			else if (skill.getSkill() == Skill.PRAYER)
			{
				int current = skill.getBoostedLevel();
				if (config.notifyLowPrayer() && current <= config.prayerThreshold())
				{
					if (!prayerNotified)
					{
						notifier.notify("Low prayer: " + current + ".");
						prayerNotified = true;
					}
				}
				else if (current > config.prayerThreshold())
				{
					prayerNotified = false;
				}
			}
		}
	}

	private void reset()
	{
		hadPlayer = false;
		playerMissingSince = 0L;
		idleSince = 0L;
		lastAnimation = -1;
		lastX = -1;
		lastY = -1;
		idleNotified = false;
		hitpointsNotified = false;
		prayerNotified = false;
	}
}
