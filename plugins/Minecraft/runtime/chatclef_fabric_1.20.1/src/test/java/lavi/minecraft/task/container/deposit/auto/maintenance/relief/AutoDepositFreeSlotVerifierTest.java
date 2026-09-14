package lavi.minecraft.task.container.deposit.auto.maintenance.relief;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositFreeSlotVerifierTest {
    @Test
    void preservesFullPartialAndNoReliefClassification() {
        assertVerdict(28, 5, AutoDepositMaintenanceOutcome.FULL_RELIEF);
        assertVerdict(31, 2, AutoDepositMaintenanceOutcome.PARTIAL_RELIEF);
        assertVerdict(33, 0, AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF);
    }

    @Test
    void unavailableSnapshotDoesNotInventAnEndingOccupancyOrZeroProgress() {
        AutoDepositFreeSlotVerifier verifier = new AutoDepositFreeSlotVerifier(
                ignored -> Optional.empty()
        );

        AutoDepositFreeSlotVerdict verdict = verifier.verify(null, 33, 5);

        assertEquals(-1, verdict.endingOccupiedSlots());
        assertEquals(-1, verdict.freedSlots());
        assertTrue(verdict.endingPressure().isEmpty());
        assertTrue(verdict.signedDelta().isEmpty());
        assertEquals(AutoDepositPressureObservationStatus.UNAVAILABLE, verdict.observationStatus());
        assertEquals(AutoDepositMaintenanceOutcome.UNAVAILABLE, verdict.outcome());
    }

    //20260914_kpopmodder: Preserve negative inventory delta and incompatible scope independently of transfer progress.
    @Test
    void retainsNegativeDeltaAndRejectsAnIncompatibleInventoryScope() {
        AutoDepositFreeSlotVerdict negative = new AutoDepositFreeSlotVerifier(
                ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(35, 36))).verify(null, 33, 5);
        assertEquals(-2, negative.signedDelta().orElseThrow());
        assertEquals(0, negative.freedSlots());
        assertEquals(AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF, negative.outcome());

        AutoDepositFreeSlotVerdict wrongScope = new AutoDepositFreeSlotVerifier(
                ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(27, 27))).verify(null, 33, 5);
        assertFalse(wrongScope.available());
        assertEquals(AutoDepositPressureObservationStatus.INCOMPATIBLE_SCOPE, wrongScope.observationStatus());
        assertEquals(27, wrongScope.endingOccupiedSlots());
        assertTrue(wrongScope.signedDelta().isEmpty());
    }

    @Test
    void aThrowingReadIsUnavailableAndIsInvokedOnlyOnce() {
        int[] reads = {0};
        AutoDepositFreeSlotVerdict result = new AutoDepositFreeSlotVerifier(ignored -> {
            reads[0]++;
            throw new IllegalStateException("read unavailable");
        }).verify(null, 33, 5);
        assertEquals(1, reads[0]);
        assertEquals(AutoDepositMaintenanceOutcome.UNAVAILABLE, result.outcome());
        assertTrue(result.endingPressure().isEmpty());
    }

    private static void assertVerdict(
            int endingOccupiedSlots,
            int expectedFreedSlots,
            AutoDepositMaintenanceOutcome expectedOutcome) {
        AutoDepositFreeSlotVerifier verifier = new AutoDepositFreeSlotVerifier(
                ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(
                        endingOccupiedSlots,
                        36
                ))
        );

        AutoDepositFreeSlotVerdict verdict = verifier.verify(null, 33, 5);

        assertEquals(endingOccupiedSlots, verdict.endingOccupiedSlots());
        assertEquals(expectedFreedSlots, verdict.freedSlots());
        assertEquals(expectedOutcome, verdict.outcome());
    }
}
