package net.runelite.client.compatibility;

public final class EventConformanceSnapshot
{
	private final BridgeEventChannel channel;
	private final long produced;
	private final long delivered;
	private final long dropped;
	private final long direct;
	private final long polled;
	private final long offClientThread;
	private final long duplicates;
	private final String lastEventClass;
	private final String lastThread;
	private final String lastFailure;
	private final long lastEventNanos;

	EventConformanceSnapshot(BridgeEventChannel channel, long produced, long delivered,
		long dropped, long direct, long polled, long offClientThread, long duplicates,
		String lastEventClass, String lastThread, String lastFailure, long lastEventNanos)
	{
		this.channel = channel;
		this.produced = produced;
		this.delivered = delivered;
		this.dropped = dropped;
		this.direct = direct;
		this.polled = polled;
		this.offClientThread = offClientThread;
		this.duplicates = duplicates;
		this.lastEventClass = lastEventClass;
		this.lastThread = lastThread;
		this.lastFailure = lastFailure;
		this.lastEventNanos = lastEventNanos;
	}

	public BridgeEventChannel getChannel() { return channel; }
	public long getProduced() { return produced; }
	public long getDelivered() { return delivered; }
	public long getDropped() { return dropped; }
	public long getDirect() { return direct; }
	public long getPolled() { return polled; }
	public long getOffClientThread() { return offClientThread; }
	public long getDuplicates() { return duplicates; }
	public String getLastEventClass() { return lastEventClass; }
	public String getLastThread() { return lastThread; }
	public String getLastFailure() { return lastFailure; }
	public long getLastEventNanos() { return lastEventNanos; }
}
