package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

//20260907_kpopmodder: Verify bounded one-shot GET count observation and stale-world rejection.
class FabricChatClefGetItemEffectTrackerTest {
    @Test
    void authoritativeCountsProduceExactDeltaAndTerminalReadOccursOnce() {
        Object world = new Object();
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker = tracker(() ->
                reads.getAndIncrement() == 0
                        ? authoritative(2, world, player)
                        : authoritative(3, world, player)
        );

        FabricChatClefGetItemEffectEvidence first = tracker.terminalEvidence();
        FabricChatClefGetItemEffectEvidence duplicate = tracker.terminalEvidence();

        assertEquals("authoritative", first.observationStatus());
        assertEquals(Integer.valueOf(2), first.beforeCountOrNull());
        assertEquals(Integer.valueOf(3), first.afterCountOrNull());
        assertEquals(Integer.valueOf(1), first.deltaOrNull());
        assertEquals(
                "before_and_after_inventory_counts_match_world_binding",
                first.observationReason()
        );
        assertSame(first, duplicate);
        assertEquals(2, reads.get());
    }

    @Test
    void inventoryAndCursorTotalsRemainBoundToTheSameBeforeAfterObservation() {
        Object world = new Object();
        Object player = new Object();
        int[][] inventoryCounts = {
                {1, 0, 2},
                {2, 1, 3}
        };
        int[] cursorCounts = {1, 2};
        AtomicInteger observation = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker = tracker(() -> {
            int index = observation.getAndIncrement();
            int count = FabricChatClefInventoryAndCursorTargetCounter.count(
                    inventoryCounts[index].length,
                    slot -> inventoryCounts[index][slot],
                    () -> cursorCounts[index]
            );
            return authoritative(count, world, player);
        });

        FabricChatClefGetItemEffectEvidence evidence = tracker.terminalEvidence();

        assertEquals(Integer.valueOf(4), evidence.beforeCountOrNull());
        assertEquals(Integer.valueOf(8), evidence.afterCountOrNull());
        assertEquals(Integer.valueOf(4), evidence.deltaOrNull());
        assertEquals(2, observation.get());
    }

    @Test
    void unavailableBeforeCountNeverFabricatesDelta() {
        Object world = new Object();
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker = tracker(() ->
                reads.getAndIncrement() == 0
                        ? FabricChatClefGetItemCountObservation.unavailable("before_failed")
                        : authoritative(1, world, player)
        );

        FabricChatClefGetItemEffectEvidence evidence = tracker.terminalEvidence();

        assertEquals("before_unavailable", evidence.observationStatus());
        assertNull(evidence.beforeCountOrNull());
        assertEquals(Integer.valueOf(1), evidence.afterCountOrNull());
        assertNull(evidence.deltaOrNull());
    }

    @Test
    void changedWorldBindingRejectsOtherwiseValidCounts() {
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker = tracker(() ->
                authoritative(reads.getAndIncrement(), new Object(), player)
        );

        FabricChatClefGetItemEffectEvidence evidence = tracker.terminalEvidence();

        assertEquals("stale_world_binding", evidence.observationStatus());
        assertEquals("world_or_player_identity_changed", evidence.observationReason());
        assertNull(evidence.deltaOrNull());
    }

    @Test
    void nullBeforeReaderObservationBecomesUnavailableEvidence() {
        Object world = new Object();
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker = tracker(() ->
                reads.getAndIncrement() == 0
                        ? null
                        : authoritative(1, world, player)
        );

        FabricChatClefGetItemEffectEvidence evidence = tracker.terminalEvidence();

        assertEquals("before_unavailable", evidence.observationStatus());
        assertEquals("before_reader_returned_null", evidence.observationReason());
        assertNull(evidence.beforeCountOrNull());
        assertEquals(Integer.valueOf(1), evidence.afterCountOrNull());
        assertNull(evidence.deltaOrNull());
    }

    @Test
    void nullAfterReaderObservationBecomesUnavailableEvidence() {
        Object world = new Object();
        Object player = new Object();
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker = tracker(() ->
                reads.getAndIncrement() == 0
                        ? authoritative(0, world, player)
                        : null
        );

        FabricChatClefGetItemEffectEvidence evidence = tracker.terminalEvidence();

        assertEquals("after_unavailable", evidence.observationStatus());
        assertEquals("after_reader_returned_null", evidence.observationReason());
        assertEquals(Integer.valueOf(0), evidence.beforeCountOrNull());
        assertNull(evidence.afterCountOrNull());
        assertNull(evidence.deltaOrNull());
    }

    @Test
    void excludedCommandDoesNotReadInventory() {
        AtomicInteger reads = new AtomicInteger();
        FabricChatClefGetItemEffectTracker tracker =
                FabricChatClefGetItemTestProfiles.tracker(
                        "deposit diamond_pickaxe 2",
                        () -> {
                            reads.incrementAndGet();
                            return FabricChatClefGetItemCountObservation.unavailable("unexpected");
                        }
                );

        assertFalse(tracker.tracked());
        assertEquals(0, reads.get());
    }

    private static FabricChatClefGetItemEffectTracker tracker(
            FabricChatClefGetItemTargetCountReader reader
    ) {
        return FabricChatClefGetItemTestProfiles.tracker(
                "get diamond_pickaxe 1",
                reader
        );
    }

    private static FabricChatClefGetItemCountObservation authoritative(
            int count,
            Object world,
            Object player
    ) {
        return FabricChatClefGetItemCountObservation.authoritative(count, world, player);
    }
}
