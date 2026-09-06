package net.runelite.api.events;

/** A native item-container update preserving the 634 alternate-namespace bit. */
public class ItemContainerChanged
{
	private int containerId;
	private long containerKey;
	private Object itemContainer;

	public ItemContainerChanged() {}
	public ItemContainerChanged(int containerId, Object itemContainer)
	{
		this(containerId, containerId & 0xffffffffL, itemContainer);
	}
	public ItemContainerChanged(int containerId, long containerKey, Object itemContainer)
	{
		this.containerId = containerId;
		this.containerKey = containerKey;
		this.itemContainer = itemContainer;
	}

	public int getContainerId() { return containerId; }
	public void setContainerId(int containerId) { this.containerId = containerId; }
	public long getContainerKey() { return containerKey; }
	public void setContainerKey(long containerKey) { this.containerKey = containerKey; }
	public Object getItemContainer() { return itemContainer; }
	public void setItemContainer(Object itemContainer) { this.itemContainer = itemContainer; }
}
