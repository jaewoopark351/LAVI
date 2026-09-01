package lavi.minecraft.diagnostics.crafting.acquisition.association;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Keep command ownership transitions and counts bounded per exact scope.
class CraftResourceAssociationLedgerTest {
    @Test
    void repeatedStatusCountsObservationsWithoutInventingTransitions() {
        CraftResourceAssociationLedger ledger = new CraftResourceAssociationLedger();

        ledger.observe(CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT);
        ledger.observe(CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT);
        ledger.observe(CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED);
        ledger.observe(CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED);
        ledger.observe(CraftResourceAssociationStatus.UNKNOWN);

        CraftResourceAssociationLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(CraftResourceAssociationStatus.UNKNOWN, snapshot.currentStatus());
        assertEquals(2L, snapshot.chainOwnerTransitionCount());
        assertEquals(2L, snapshot.commandDescendantObservationCount());
        assertEquals(2L, snapshot.unownedObservationCount());
        assertEquals(1L, snapshot.unknownAssociationCount());
        assertFalse(snapshot.counterSaturated());
    }

    @Test
    void projectionObservationGapsAreBoundedAndRemainDistinctFromEngineExceptions() {
        CraftResourceAssociationLedger ledger = new CraftResourceAssociationLedger();
        String oversized = "관찰실패".repeat(100);

        ledger.observeObservationGap("MINE_TARGET_GOAL_REQUEST", oversized);
        ledger.observeObservationGap("TASK_CHILD_RECONCILIATION", "LinkageError");

        CraftResourceAssociationLedgerSnapshot snapshot = ledger.snapshot();
        assertEquals(2L, snapshot.observationGapCount());
        assertEquals("TASK_CHILD_RECONCILIATION", snapshot.lastObservationGapBoundary());
        assertEquals("LinkageError", snapshot.lastObservationGapReason());
        assertTrue(snapshot.lastObservationGapReason().getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                <= 256);
        assertFalse(snapshot.counterSaturated());
    }

    @Test
    void registryRefusesNinthActiveScopeWithoutEviction() {
        CraftResourceAssociationDiagnosticsRegistry registry =
                new CraftResourceAssociationDiagnosticsRegistry();
        for (int index = 1; index <= 8; index++) {
            assertTrue(registry.activate(key(index)));
        }

        assertFalse(registry.activate(key(9)));
        assertEquals(8, registry.activeScopeCount());
        assertEquals(1L, registry.activationRefusalCount());
        assertTrue(registry.snapshot(key(1)).isPresent());
        assertEquals(Optional.empty(), registry.snapshot(key(9)));
    }

    @Test
    void unknownOrRetiredScopeCannotCreateStateFromAnObservation() {
        CraftResourceAssociationDiagnosticsRegistry registry =
                new CraftResourceAssociationDiagnosticsRegistry();
        IronPickaxeAcquisitionScopeKey key = key(1);

        assertFalse(registry.observe(
                key,
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT
        ));
        assertTrue(registry.activate(key));
        assertTrue(registry.observe(key, CraftResourceAssociationStatus.UNKNOWN));
        assertTrue(registry.retire(key));
        assertFalse(registry.observe(
                key,
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT
        ));
        assertEquals(0, registry.activeScopeCount());
    }

    private static IronPickaxeAcquisitionScopeKey key(int index) {
        return new IronPickaxeAcquisitionScopeKey(
                "session-" + index,
                1L,
                "request-" + index,
                "correlation-" + index,
                "root-assignment-" + index,
                index,
                "root-instance-" + index
        );
    }
}
