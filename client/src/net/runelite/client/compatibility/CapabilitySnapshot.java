package net.runelite.client.compatibility;

public final class CapabilitySnapshot
{
	private final ClientCapability capability;
	private final CapabilityStatus status;
	private final String reason;
	private final long updatedAtNanos;

	public CapabilitySnapshot(ClientCapability capability, CapabilityStatus status,
		String reason, long updatedAtNanos)
	{
		this.capability = capability;
		this.status = status;
		this.reason = reason == null ? "" : reason;
		this.updatedAtNanos = updatedAtNanos;
	}

	public ClientCapability getCapability() { return capability; }
	public CapabilityStatus getStatus() { return status; }
	public String getReason() { return reason; }
	public long getUpdatedAtNanos() { return updatedAtNanos; }
}
