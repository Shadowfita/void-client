package net.runelite.client.plugins.menuentryswapper;

public final class MenuEntrySwapperState
{
	public static volatile boolean enabled;
	public static volatile boolean exactMatch;
	public static volatile String preferredOptions = "";

	private MenuEntrySwapperState()
	{
	}

	static void reset()
	{
		enabled = false;
		exactMatch = false;
		preferredOptions = "";
	}
}
