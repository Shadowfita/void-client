package net.runelite.client.game;

import com.GameClient;
import net.runelite.api.Skill;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.EventBus;

public final class GameEventBridgeHooks
{
	private static volatile boolean directGroundItemHooks;
	private static volatile boolean directNpcHooks;
	private static volatile boolean directStatHooks;

	private GameEventBridgeHooks()
	{
	}

	public static boolean hasDirectGroundItemHooks()
	{
		return directGroundItemHooks;
	}

	public static boolean hasDirectStatHooks()
	{
		return directStatHooks;
	}

	public static boolean hasDirectNpcHooks()
	{
		return directNpcHooks;
	}

	public static void postStatChanged(int skillIndex, int boostedLevel, int xp, int level)
	{
		Skill[] skills = Skill.values();
		if (skillIndex < 0 || skillIndex >= skills.length || skills[skillIndex] == Skill.OVERALL)
		{
			return;
		}

		directStatHooks = true;
		post(new StatChanged(skills[skillIndex], xp, level, boostedLevel));
	}

	public static void postGroundItemSpawned(int id, int quantity, String name, int price, int localX, int localY, int plane)
	{
		directGroundItemHooks = true;
		GameClient.GroundItemInfo item = new GameClient.GroundItemInfo(id, quantity, name, price, localX, localY, plane);
		post(new ItemSpawned(tileKey(item), item));
	}

	public static void postGroundItemDespawned(int id, int quantity, String name, int price, int localX, int localY, int plane)
	{
		directGroundItemHooks = true;
		GameClient.GroundItemInfo item = new GameClient.GroundItemInfo(id, quantity, name, price, localX, localY, plane);
		post(new ItemDespawned(tileKey(item), item));
	}

	public static void postGroundItemQuantityChanged(int id, int oldQuantity, int newQuantity, String name, int price, int localX, int localY, int plane)
	{
		directGroundItemHooks = true;
		GameClient.GroundItemInfo item = new GameClient.GroundItemInfo(id, newQuantity, name, price, localX, localY, plane);
		post(new ItemQuantityChanged(item, tileKey(item), oldQuantity, newQuantity));
	}

	public static void postNpcSpawned(GameClient.NpcInfo npc)
	{
		directNpcHooks = true;
		post(new NpcSpawned(npc));
	}

	public static void postNpcDespawned(GameClient.NpcInfo npc)
	{
		directNpcHooks = true;
		post(new NpcDespawned(npc));
	}

	private static String tileKey(GameClient.GroundItemInfo item)
	{
		return item.getPlane() + ":" + item.getLocalX() + ":" + item.getLocalY();
	}

	private static void post(Object event)
	{
		try
		{
			if (RuneLite.getInjector() != null)
			{
				RuneLite.getInjector().getInstance(EventBus.class).post(event);
			}
		}
		catch (Throwable ignored)
		{
			// Packet handlers must not fail because the RuneLite shell is still starting.
		}
	}
}
