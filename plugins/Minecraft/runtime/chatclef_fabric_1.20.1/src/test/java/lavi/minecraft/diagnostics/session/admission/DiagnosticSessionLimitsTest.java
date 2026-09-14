package lavi.minecraft.diagnostics.session.admission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticSessionLimitsTest {
    @Test
    void fixedCriticalPartitionPreservesOldPoolsAndReservesResourceBoundaries() {
        assertEquals(5_000, DiagnosticSessionLimits.HARD_CAP);
        assertEquals(3_736, DiagnosticSessionLimits.ORDINARY_CEILING);
        assertEquals(1_264, DiagnosticSessionLimits.CRITICAL_RESERVE);
        assertEquals(1_264, DiagnosticSessionLimits.criticalPartitionTotal());
        assertEquals(128, DiagnosticEventFamily.TOOL_EQUIP_FIRST.slotQuota());
        assertEquals(4, DiagnosticEventFamily.TOOL_PLACEMENT_TERMINAL.slotQuota());
        assertEquals(64, DiagnosticEventFamily.BLOCK_PROTECTION_BOUNDARY.slotQuota());

        assertEquals(1, DiagnosticEventFamily.CANONICAL_CAP.slotQuota());
        assertEquals(1, DiagnosticEventFamily.FINAL_SNAPSHOT.slotQuota());
        assertEquals(16, DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL.slotQuota());
        assertEquals(8, DiagnosticEventFamily.ROUTINE_STORE_TERMINAL.slotQuota());
        assertEquals(16, DiagnosticEventFamily.EXCEPTION_COVERAGE.slotQuota());
        assertEquals(8, DiagnosticEventFamily.AGGREGATE_CHECKPOINT.slotQuota());
        assertEquals(8, DiagnosticEventFamily.NON_STORE_TERMINAL.slotQuota());
        assertEquals(6, DiagnosticEventFamily.SUPPRESSION_CONTROL.slotQuota());
        assertEquals(128, DiagnosticEventFamily.RESOURCE_MINING_FIRST.slotQuota());
        assertEquals(128, DiagnosticEventFamily.RESOURCE_DEPOSIT_FIRST.slotQuota());
        assertEquals(128, DiagnosticEventFamily.RESOURCE_BUILDER_FIRST.slotQuota());
        assertEquals(12, DiagnosticEventFamily.RESOURCE_OBSERVATION_TERMINAL.slotQuota());
        assertEquals(128, DiagnosticEventFamily.RESOURCE_MINING_SUMMARY.slotQuota());
        assertEquals(128, DiagnosticEventFamily.RESOURCE_DEPOSIT_SUMMARY.slotQuota());
        assertEquals(128, DiagnosticEventFamily.RESOURCE_BUILDER_SUMMARY.slotQuota());
        assertEquals(64, DiagnosticEventFamily.BLOCK_COLLECTION_FIRST.slotQuota());
        assertEquals(32, DiagnosticEventFamily.BLOCK_COLLECTION_SUMMARY.slotQuota());
    }

    @Test
    void onlyStoreTerminalPoolsUseAtomicFourSlotUnits() {
        assertTrue(DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL.groupFamily());
        assertTrue(DiagnosticEventFamily.ROUTINE_STORE_TERMINAL.groupFamily());
        assertEquals(4, DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL.admissionUnitSlots());
        assertEquals(4, DiagnosticEventFamily.ROUTINE_STORE_TERMINAL.admissionUnitSlots());
        assertFalse(DiagnosticEventFamily.NON_STORE_TERMINAL.groupFamily());
        assertFalse(DiagnosticEventFamily.EXCEPTION_COVERAGE.groupFamily());
    }
}
