package lavi.minecraft.task.container.deposit.auto.maintenance.result;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerdict;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerifier;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Prove terminal immutability, cleanup settlement and honest root-local zero delta.
class AutoDepositRunCompletionTest {
    @Test
    void capturesBeforeCleanupAndRejectsLaterCancellationAndSecondSettlement() {
        AutoDepositRunCompletion completion = new AutoDepositRunCompletion();
        AutoDepositRunResult first = result(AutoDepositRunReason.NORMAL, 33, 30, AutoDepositWorkingSetStatus.SATISFIED);
        assertTrue(completion.capture(first));
        assertTrue(completion.result().isEmpty());
        assertFalse(first.normalValidated());
        assertFalse(completion.capture(result(AutoDepositRunReason.UNEXPECTED_STOP, 33, 36,
                AutoDepositWorkingSetStatus.NOT_EVALUATED)));
        AutoDepositRunResult finalized = completion.finalizeAfterCleanup(true).orElseThrow();
        assertTrue(finalized.normalValidated());
        assertEquals(3, finalized.signedFreedSlotDelta().orElseThrow());
        assertSame(finalized, completion.finalizeAfterCleanup(false).orElseThrow());
        assertSame(first, completion.candidate().orElseThrow());
    }

    @Test
    void cleanupFailureCannotBeReportedAsNormalAndPreservesTheOriginalCandidate() {
        AutoDepositRunCompletion completion = new AutoDepositRunCompletion();
        completion.capture(result(AutoDepositRunReason.TRUSTED_CHILD_FAILED, 33, 30,
                AutoDepositWorkingSetStatus.NOT_EVALUATED));
        AutoDepositRunResult value = completion.finalizeAfterCleanup(false).orElseThrow();
        assertEquals(AutoDepositRunReason.CLEANUP_FAILED, value.reason());
        assertFalse(value.normalValidated());
        assertEquals(AutoDepositRunReason.TRUSTED_CHILD_FAILED, completion.candidate().orElseThrow().reason());
    }

    @Test
    void rootLocalZeroCanBeNormalButUnknownMandatoryEvidenceCannot() {
        AutoDepositRunCompletion completion = new AutoDepositRunCompletion();
        completion.capture(result(AutoDepositRunReason.NORMAL, 32, 32, AutoDepositWorkingSetStatus.NOT_APPLICABLE));
        assertTrue(completion.finalizeAfterCleanup(true).orElseThrow().normalValidated());

        AutoDepositRunResult unknown = new AutoDepositRunResult(AutoDepositRunReason.NORMAL,
                Optional.of(new DepositAllInventoryPressureSnapshot(33, 36)), Optional.empty(), true,
                AutoDepositWorkingSetStatus.SATISFIED, true, "unknown pressure", AutoDepositMaintenanceOutcome.UNAVAILABLE);
        assertFalse(unknown.normalValidated());
        assertTrue(unknown.signedFreedSlotDelta().isEmpty());
        AutoDepositRunCompletion missingWorking = new AutoDepositRunCompletion();
        missingWorking.capture(result(AutoDepositRunReason.NORMAL, 33, 30, AutoDepositWorkingSetStatus.UNAVAILABLE));
        assertFalse(missingWorking.finalizeAfterCleanup(true).orElseThrow().normalValidated());
    }

    @Test
    void cleanupReturningACursorStackChangesOnlyTheFinalOccupancyAndReliefEvidence() {
        AutoDepositRunCompletion completion = new AutoDepositRunCompletion();
        AutoDepositRunResult before = result(AutoDepositRunReason.NORMAL, 36, 32, AutoDepositWorkingSetStatus.SATISFIED);
        completion.capture(before);
        AutoDepositRunResult after = completion.finalizeAfterCleanup(true, pressure(36, 33),
                AutoDepositWorkingSetStatus.SATISFIED).orElseThrow();
        assertEquals(32, completion.candidate().orElseThrow().endingPressure().orElseThrow().occupiedSlots());
        assertEquals(33, after.endingPressure().orElseThrow().occupiedSlots());
        assertEquals(3, after.signedFreedSlotDelta().orElseThrow());
        assertEquals(AutoDepositMaintenanceOutcome.PARTIAL_RELIEF, after.reliefOutcome());
        assertTrue(after.normalValidated());
        assertSame(after, completion.finalizeAfterCleanup(true, pressure(36, 20),
                AutoDepositWorkingSetStatus.UNAVAILABLE).orElseThrow());
    }

    @Test
    void unavailablePostCleanupEvidenceDowngradesNormalButCannotReplaceAKnownFailure() {
        for (AutoDepositRunReason successful : new AutoDepositRunReason[]{AutoDepositRunReason.NORMAL, AutoDepositRunReason.REPLAN_REQUIRED}) {
            AutoDepositRunCompletion missingPressure = new AutoDepositRunCompletion();
            missingPressure.capture(result(successful, 36, 32, AutoDepositWorkingSetStatus.SATISFIED));
            assertEquals(AutoDepositRunReason.PRESSURE_UNAVAILABLE,
                    missingPressure.finalizeAfterCleanup(true, pressure(36, null), AutoDepositWorkingSetStatus.SATISFIED)
                            .orElseThrow().reason());
            AutoDepositRunCompletion missingWorking = new AutoDepositRunCompletion();
            missingWorking.capture(result(successful, 36, 32, AutoDepositWorkingSetStatus.SATISFIED));
            assertEquals(AutoDepositRunReason.WORKING_SET_UNAVAILABLE,
                    missingWorking.finalizeAfterCleanup(true, pressure(36, 32), AutoDepositWorkingSetStatus.UNAVAILABLE)
                            .orElseThrow().reason());
        }
        AutoDepositRunCompletion knownFailure = new AutoDepositRunCompletion();
        knownFailure.capture(result(AutoDepositRunReason.WORKING_SET_DEFICIT, 36, 32, AutoDepositWorkingSetStatus.DEFICIT));
        AutoDepositRunResult failure = knownFailure.finalizeAfterCleanup(true, pressure(36, null),
                AutoDepositWorkingSetStatus.UNAVAILABLE).orElseThrow();
        assertEquals(AutoDepositRunReason.WORKING_SET_DEFICIT, failure.reason());
        assertTrue(failure.endingPressure().isEmpty());
        assertEquals(AutoDepositWorkingSetStatus.DEFICIT, knownFailure.candidate().orElseThrow().workingSetStatus());
    }

    private static AutoDepositFreeSlotVerdict pressure(int start, Integer end) {
        return new AutoDepositFreeSlotVerifier(ignored -> end == null ? Optional.empty()
                : Optional.of(new DepositAllInventoryPressureSnapshot(end, 36)))
                .verify(null, new DepositAllInventoryPressureSnapshot(start, 36), 5);
    }

    private static AutoDepositRunResult result(AutoDepositRunReason reason, int start, int end,
            AutoDepositWorkingSetStatus working) {
        return new AutoDepositRunResult(reason, Optional.of(new DepositAllInventoryPressureSnapshot(start, 36)),
                Optional.of(new DepositAllInventoryPressureSnapshot(end, 36)), true, working, false,
                "test", end < start ? AutoDepositMaintenanceOutcome.PARTIAL_RELIEF : AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF);
    }
}
