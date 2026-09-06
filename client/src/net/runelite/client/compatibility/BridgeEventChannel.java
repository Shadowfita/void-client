package net.runelite.client.compatibility;

public enum BridgeEventChannel
{
	NPC_LIFECYCLE("NPC lifecycle"),
	GROUND_ITEM_LIFECYCLE("Ground-item lifecycle"),
	STAT("Skill/stat updates"),
	ITEM_CONTAINER("Item-container updates"),
	SCENE_OBJECT("Scene-object lifecycle"),
	CHAT("Chat messages"),
	GAME_STATE("Game-state transitions"),
	WIDGET("Widget lifecycle"),
	INTERACTION("Actor interaction/combat"),
	OTHER("Other bridge events");

	private final String displayName;

	BridgeEventChannel(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
