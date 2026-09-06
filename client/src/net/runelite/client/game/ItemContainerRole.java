package net.runelite.client.game;

/** Native 634 item-container roles verified from packet IDs. */
public enum ItemContainerRole
{
	INVENTORY(93, 28),
	BANK(95, -1),
	UNKNOWN(-1, -1);

	private final int lowId;
	private final int expectedCapacity;

	ItemContainerRole(int lowId, int expectedCapacity)
	{
		this.lowId = lowId;
		this.expectedCapacity = expectedCapacity;
	}

	public int getLowId() { return lowId; }
	public int getExpectedCapacity() { return expectedCapacity; }

	public static ItemContainerRole fromKey(long key)
	{
		int low = (int) (key & 0xffffL);
		for (ItemContainerRole role : values())
		{
			if (role.lowId == low) return role;
		}
		return UNKNOWN;
	}
}
