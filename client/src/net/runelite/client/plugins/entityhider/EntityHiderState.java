package net.runelite.client.plugins.entityhider;

public final class EntityHiderState
{
	public static volatile boolean hideOtherPlayers;
	public static volatile boolean hideNpcs;
	public static volatile boolean hideProjectiles;

	private EntityHiderState()
	{
	}

	static void reset()
	{
		hideOtherPlayers = false;
		hideNpcs = false;
		hideProjectiles = false;
	}
}
