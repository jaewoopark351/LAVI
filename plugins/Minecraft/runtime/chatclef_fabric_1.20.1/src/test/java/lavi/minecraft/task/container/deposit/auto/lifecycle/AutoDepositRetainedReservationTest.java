package lavi.minecraft.task.container.deposit.auto.lifecycle;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunCompletion;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.maintenance.support.AutoDepositMaintenanceFixture;
import lavi.minecraft.task.container.deposit.auto.maintenance.support.ObservedDepositChild;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmPolicy;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Preserve unresolved reservation debt without inventing debt after a settled handoff.
class AutoDepositRetainedReservationTest {
    @Test void satisfiedHandoffIsNotRecountedAgainstLaterUnavailableInventory() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            for (AutoDepositWorkingSetStatus status : List.of(
                    AutoDepositWorkingSetStatus.SATISFIED, AutoDepositWorkingSetStatus.NOT_APPLICABLE)) {
                AutoDepositRunLedger ledger = new AutoDepositRunLedger(new AutoDepositRearmPolicy());
                AutoDepositMaintenanceTask old = unavailableReservation(fixture);
                assertEquals(AutoDepositWorkingSetStatus.UNAVAILABLE, old.verifyWorkingSetBeforeResume());
                suspend(ledger, old, status);
                assertEquals(status, ledger.verifyRetainedWorkingSet());
                // The settled old owner must no longer be read on subsequent admission evaluations.
                assertEquals(AutoDepositWorkingSetStatus.NOT_APPLICABLE, ledger.verifyRetainedWorkingSet());
            }
        }
    }

    @Test void unresolvedHandoffKeepsTheOriginalReservationAcrossRepeatedEvaluation() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            for (AutoDepositWorkingSetStatus status : List.of(AutoDepositWorkingSetStatus.DEFICIT,
                    AutoDepositWorkingSetStatus.UNAVAILABLE, AutoDepositWorkingSetStatus.NOT_EVALUATED)) {
                AutoDepositRunLedger ledger = new AutoDepositRunLedger(new AutoDepositRearmPolicy());
                AutoDepositMaintenanceTask old = unavailableReservation(fixture);
                suspend(ledger, old, status);
                assertEquals(AutoDepositWorkingSetStatus.UNAVAILABLE, ledger.verifyRetainedWorkingSet());
                assertEquals(AutoDepositWorkingSetStatus.UNAVAILABLE, ledger.verifyRetainedWorkingSet());
                assertSame(old, ledger.suspendedTask());
            }
        }
    }

    @Test void nextRootTracksItsOwnUnresolvedReservationAfterPriorHandoffWasSettled() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            AutoDepositRunLedger ledger = new AutoDepositRunLedger(new AutoDepositRearmPolicy());
            AutoDepositMaintenanceTask first = unavailableReservation(fixture);
            suspend(ledger, first, AutoDepositWorkingSetStatus.SATISFIED);
            assertEquals(AutoDepositWorkingSetStatus.SATISFIED, ledger.verifyRetainedWorkingSet());
            AutoDepositMaintenanceTask second = unavailableReservation(fixture);
            suspend(ledger, second, AutoDepositWorkingSetStatus.UNAVAILABLE);
            assertEquals(AutoDepositWorkingSetStatus.UNAVAILABLE, ledger.verifyRetainedWorkingSet());
            assertSame(second, ledger.suspendedTask());
        }
    }

    private static AutoDepositMaintenanceTask unavailableReservation(AutoDepositMaintenanceFixture fixture) {
        Task root = new ObservedDepositChild(false, false, false, 0);
        WorkingSetSnapshot original = new WorkingSetSnapshot(root, List.of(root), new Object(),
                Dimension.OVERWORLD, 1L, Map.of(), Map.of(), Map.of());
        AutoDepositPlan plan = fixture.plan(36);
        TestObjects.setField(plan.context(), AutoDepositContextSnapshot.class, "workingSet", original);
        return new AutoDepositMaintenanceTask(plan, AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(34, 36)), ignored -> { });
    }

    private static void suspend(AutoDepositRunLedger ledger, AutoDepositMaintenanceTask task,
                                AutoDepositWorkingSetStatus capturedStatus) {
        AutoDepositRunCompletion completion = new AutoDepositRunCompletion();
        completion.capture(new AutoDepositRunResult(AutoDepositRunReason.SAFETY_INTERRUPTED,
                Optional.of(new DepositAllInventoryPressureSnapshot(36, 36)),
                Optional.of(new DepositAllInventoryPressureSnapshot(34, 36)), true,
                capturedStatus, true, "captured_at_handoff", AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF));
        AutoDepositRunResult result = completion.finalizeAfterCleanup(true).orElseThrow();
        TestObjects.setField(task, AutoDepositMaintenanceTask.class, "completion", completion);
        ledger.begin(task, new DepositAllInventoryPressureSnapshot(36, 36), "items", "chestA");
        assertTrue(ledger.settle(task, result, "items"));
    }
}
