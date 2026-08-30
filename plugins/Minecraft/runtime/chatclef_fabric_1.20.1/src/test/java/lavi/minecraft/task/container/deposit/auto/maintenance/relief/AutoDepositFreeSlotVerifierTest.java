package lavi.minecraft.task.container.deposit.auto.maintenance.relief;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoDepositFreeSlotVerifierTest {
    @Test
    void preservesFullPartialAndNoReliefClassification() {
        assertVerdict(28, 5, AutoDepositMaintenanceOutcome.FULL_RELIEF);
        assertVerdict(31, 2, AutoDepositMaintenanceOutcome.PARTIAL_RELIEF);
        assertVerdict(33, 0, AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF);
    }

    @Test
    void unavailableSnapshotPreservesStartingOccupancyAndReportsNoRelief() {
        AutoDepositFreeSlotVerifier verifier = new AutoDepositFreeSlotVerifier(
                ignored -> Optional.empty()
        );

        AutoDepositFreeSlotVerdict verdict = verifier.verify(null, 33, 5);

        assertEquals(33, verdict.endingOccupiedSlots());
        assertEquals(0, verdict.freedSlots());
        assertEquals(AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF, verdict.outcome());
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
