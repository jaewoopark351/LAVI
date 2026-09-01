package lavi.minecraft.diagnostics.crafting.acquisition.target.failure;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Preserve command-owned failure facts even when source detail is suppressed.
class CraftResourceFailureAggregateLedgerTest {
    @Test
    void ownedUnreachableAndBlacklistFactsRemainCommandScopedAndBounded() {
        CraftResourceFailureAggregateLedger ledger = new CraftResourceFailureAggregateLedger();
        CraftResourceTargetTuple tuple = new CraftResourceTargetTuple(
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                CraftResourceTargetRole.IRON_ORE_BLOCK,
                "10,20,30",
                List.of("minecraft:iron_ore")
        );

        ledger.observe(new CraftResourceFailureObservation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceFailureKind.UNREACHABLE_REQUEST,
                "DestroyBlockTask",
                "10,20,30",
                -1,
                -1,
                4,
                Optional.empty(),
                Optional.empty(),
                41,
                false
        ), 7, Optional.of(tuple));
        ledger.observe(new CraftResourceFailureObservation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceFailureKind.BLACKLIST_STATE_CHANGED,
                "DestroyBlockTask",
                "10,20,30",
                3,
                4,
                4,
                Optional.of(false),
                Optional.of(true),
                42,
                true
        ), 7, Optional.of(tuple));

        CraftResourceFailureAggregateSnapshot snapshot = ledger.snapshot();
        assertEquals(1, snapshot.unreachableRequestCount());
        assertEquals(1, snapshot.blacklistTransitionCount());
        assertEquals(3, snapshot.firstFailureCount());
        assertEquals(4, snapshot.lastFailureCount());
        assertEquals(4, snapshot.allowedFailures());
        assertFalse(snapshot.unreachableBefore().orElseThrow());
        assertTrue(snapshot.unreachableAfter().orElseThrow());
        assertEquals(41, snapshot.firstObservedTick());
        assertEquals(42, snapshot.lastObservedTick());
        assertEquals(1, snapshot.suppressedDetailCount());
        assertEquals(7, snapshot.targetAttemptSequence());
        assertEquals(CraftResourceStage.IRON_INPUT_ACQUISITION, snapshot.resourceStage());
        assertEquals(CraftResourceTargetRole.IRON_ORE_BLOCK, snapshot.targetRole());
    }

    @Test
    void unownedFailureCannotPopulateCommandAggregate() {
        CraftResourceFailureAggregateLedger ledger = new CraftResourceFailureAggregateLedger();
        ledger.observe(new CraftResourceFailureObservation(
                CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED,
                CraftResourceFailureKind.BLACKLIST_STATE_CHANGED,
                "ConcurrentTask",
                "1,2,3",
                1,
                2,
                4,
                Optional.of(false),
                Optional.of(false),
                5,
                true
        ), 1, Optional.empty());

        CraftResourceFailureAggregateSnapshot snapshot = ledger.snapshot();
        assertEquals(0, snapshot.blacklistTransitionCount());
        assertEquals(1, snapshot.unownedObservationCount());
    }
}
