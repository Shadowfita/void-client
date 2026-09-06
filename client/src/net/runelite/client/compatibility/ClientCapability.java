package net.runelite.client.compatibility;

public enum ClientCapability
{
	NATIVE_SCENE_PROJECTION("Native scene projection"),
	ACTOR_OVERHEAD_PROJECTION("Actor overhead projection"),
	STABLE_NPC_IDENTITY("Stable NPC identity"),
	DIRECT_NPC_EVENTS("Direct NPC lifecycle events"),
	DIRECT_GROUND_ITEM_EVENTS("Direct ground-item events"),
	DIRECT_SCENE_OBJECT_EVENTS("Direct scene-object events"),
	DIRECT_STAT_EVENTS("Direct stat events"),
	DIRECT_ITEM_CONTAINER_EVENTS("Direct item-container events"),
	AUTHORITATIVE_ITEM_CONTAINERS("Authoritative item containers"),
	VERIFIED_INVENTORY_CONTAINER("Verified inventory container"),
	VERIFIED_BANK_CONTAINER("Verified bank container"),
	VISIBLE_INVENTORY_SLOT_BOUNDS("Visible inventory slot bounds"),
	VISIBLE_BANK_SLOT_BOUNDS("Visible bank slot bounds"),
	NATIVE_DRAG_THRESHOLD("Native item drag threshold"),
	COMBAT_HEALTH_INFO("Native combat health information"),
	INSTANCE_COORDINATES("Instanced-region coordinates");

	private final String displayName;
	ClientCapability(String displayName) { this.displayName = displayName; }
	public String getDisplayName() { return displayName; }
}
