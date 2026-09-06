package net.runelite.client.compatibility;

import net.runelite.api.events.StatChanged;
import net.runelite.api.Skill;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class EventConformanceTest
{
	@Before public void before() { EventConformance.resetForTests(); }
	@After public void after() { EventConformance.resetForTests(); }

	@Test public void accountsForProducedAndDeliveredEvents()
	{
		StatChanged event = new StatChanged(Skill.ATTACK, 1000, 10, 10);
		EventConformance.DeliveryToken token = EventConformance.produced(event, EventOrigin.DIRECT);
		EventConformance.delivered(token);
		EventConformanceSnapshot snapshot = EventConformance.snapshot(BridgeEventChannel.STAT);
		assertEquals(1L, snapshot.getProduced());
		assertEquals(1L, snapshot.getDelivered());
		assertEquals(1L, snapshot.getDirect());
		assertEquals(0L, snapshot.getDropped());
	}

	@Test public void detectsAnImmediateDuplicateAfterFirstUseOverhead()
	{
		StatChanged first = new StatChanged(Skill.PRAYER, 2000, 20, 20);
		EventConformance.delivered(EventConformance.produced(first, EventOrigin.DIRECT));
		StatChanged second = new StatChanged(Skill.PRAYER, 2000, 20, 20);
		EventConformance.delivered(EventConformance.produced(second, EventOrigin.DIRECT));
		assertEquals(1L, EventConformance.snapshot(BridgeEventChannel.STAT).getDuplicates());
	}
}
