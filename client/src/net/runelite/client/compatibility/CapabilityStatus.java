package net.runelite.client.compatibility;

public enum CapabilityStatus
{
	UNKNOWN,
	AVAILABLE,
	DEGRADED,
	UNAVAILABLE;

	public boolean isUsable() { return this == AVAILABLE; }
}
