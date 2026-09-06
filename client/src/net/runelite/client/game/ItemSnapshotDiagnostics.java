package net.runelite.client.game;

public final class ItemSnapshotDiagnostics
{
	private final long widgetGeneration;
	private final int rootInterface, rawWidgetItems;
	private final String inventoryContainer, bankContainer, inventoryGeometry, bankGeometry;

	ItemSnapshotDiagnostics(long widgetGeneration, int rootInterface, int rawWidgetItems,
		String inventoryContainer, String bankContainer, String inventoryGeometry, String bankGeometry)
	{
		this.widgetGeneration = widgetGeneration;
		this.rootInterface = rootInterface;
		this.rawWidgetItems = rawWidgetItems;
		this.inventoryContainer = inventoryContainer;
		this.bankContainer = bankContainer;
		this.inventoryGeometry = inventoryGeometry;
		this.bankGeometry = bankGeometry;
	}

	public long getWidgetGeneration() { return widgetGeneration; }
	public int getRootInterface() { return rootInterface; }
	public int getRawWidgetItems() { return rawWidgetItems; }
	public String getInventoryContainer() { return inventoryContainer; }
	public String getBankContainer() { return bankContainer; }
	public String getInventoryGeometry() { return inventoryGeometry; }
	public String getBankGeometry() { return bankGeometry; }
}
