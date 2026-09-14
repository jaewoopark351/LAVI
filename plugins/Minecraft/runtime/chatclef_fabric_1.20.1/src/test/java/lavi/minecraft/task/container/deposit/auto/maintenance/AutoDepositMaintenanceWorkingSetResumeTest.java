package lavi.minecraft.task.container.deposit.auto.maintenance;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.maintenance.support.AutoDepositMaintenanceFixture;
import lavi.minecraft.task.container.deposit.auto.maintenance.support.ObservedDepositChild;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositWorkingSetRecovery;
import lavi.minecraft.task.container.deposit.auto.recovery.RecoverReservedItemsTask;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: A safety resume must retain original reservation debt instead of rebasing to missing inventory.
class AutoDepositMaintenanceWorkingSetResumeTest {
    @Test
    void retainedTaskDoesNotReplaceUnavailableOriginalReservationsWithFreshNotApplicableStatus() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            Task originalRoot = new ObservedDepositChild(false, false, false, 0);
            Object world = new Object();
            WorkingSetSnapshot original = new WorkingSetSnapshot(originalRoot, List.of(originalRoot),
                    world, Dimension.OVERWORLD, 1L,
                    Map.of(), Map.of(), Map.of());

            AutoDepositPlan plan = fixture.plan(36);
            TestObjects.setField(plan.context(), AutoDepositContextSnapshot.class, "workingSet", original);
            AutoDepositMaintenanceTask retained = new AutoDepositMaintenanceTask(plan,
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                    ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(32, 36)));
            assertSame(original, retained.snapshot());
            assertEquals(AutoDepositWorkingSetStatus.UNAVAILABLE, retained.verifyWorkingSetBeforeResume());

            AutoDepositMaintenanceTask fresh = new AutoDepositMaintenanceTask(fixture.plan(32),
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                    ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(32, 36)), ignored -> { }, true);
            assertEquals(AutoDepositWorkingSetStatus.NOT_APPLICABLE, fresh.verifyWorkingSetBeforeResume());
            retained.terminate(AutoDepositRunReason.SAFETY_INTERRUPTED);
            assertEquals(AutoDepositRunReason.SAFETY_INTERRUPTED,
                    retained.completionCandidate().orElseThrow().reason());
            assertEquals(AutoDepositWorkingSetStatus.UNAVAILABLE,
                    retained.completionCandidate().orElseThrow().workingSetStatus());
            assertFalse(retained.finalizeAfterCleanup(true).orElseThrow().normalValidated());
            assertSame(original, retained.snapshot());
            assertEquals(AutoDepositWorkingSetStatus.UNAVAILABLE, retained.verifyWorkingSetBeforeResume());
        }
    }

    @Test
    void unavailableWorkingInventoryIsATypedTerminalNotAssumedSatisfied() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            Task root = new ObservedDepositChild(false, false, false, 0);
            WorkingSetSnapshot original = new WorkingSetSnapshot(root, List.of(root), new Object(),
                    Dimension.OVERWORLD, 1L, Map.of(), Map.of(), Map.of());
            AutoDepositPlan plan = fixture.plan(33);
            TestObjects.setField(plan.context(), AutoDepositContextSnapshot.class, "workingSet", original);
            AutoDepositMaintenanceTask task = new AutoDepositMaintenanceTask(plan,
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                    ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(30, 36)), ignored -> { }, true);
            task.tick(fixture.chain());
            assertEquals(AutoDepositRunReason.WORKING_SET_UNAVAILABLE,
                    task.completionCandidate().orElseThrow().reason());
            task.stop(null);
            assertFalse(task.finalizeAfterCleanup(true).orElseThrow().normalValidated());
        }
    }

    @Test
    void recoveryResumeConstructsANewChildAndRetainsOriginalDebtWithoutInventingCompletedStorage() {
        try (AutoDepositMaintenanceFixture fixture = new AutoDepositMaintenanceFixture()) {
            Task root = new ObservedDepositChild(false, false, false, 0);
            AutoDepositPlan plan = fixture.plan(36);
            WorkingSetSnapshot original = new WorkingSetSnapshot(root, List.of(root), plan.context().worldIdentity(),
                    Dimension.OVERWORLD, 1L, Map.of(), Map.of(), Map.of());
            TestObjects.setField(plan.context(), AutoDepositContextSnapshot.class, "userTaskRoot", root);
            TestObjects.setField(plan.context(), AutoDepositContextSnapshot.class, "workingSet", original);
            AutoDepositMaintenanceTask previous = new AutoDepositMaintenanceTask(plan,
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                    ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(32, 36)));
            AutoDepositWorkingSetRecovery oldOwner = recovery(previous);
            RecoverReservedItemsTask oldChild = oldOwner.begin();
            previous.terminate(AutoDepositRunReason.SAFETY_INTERRUPTED);
            previous.finalizeAfterCleanup(true);

            AutoDepositMaintenanceTask resumed = AutoDepositMaintenanceTask.resumeRecovery(plan, previous,
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                    ignored -> Optional.of(new DepositAllInventoryPressureSnapshot(32, 36)), ignored -> { });
            assertFalse(resumed.plan().hasTargets());
            assertSame(original, resumed.snapshot());
            assertSame(previous.manifest(), resumed.manifest());
            assertNotSame(oldOwner, recovery(resumed));
            assertNotSame(oldChild, recovery(resumed).begin());
            resumed.terminate(AutoDepositRunReason.SAFETY_INTERRUPTED);
            assertFalse(resumed.completionCandidate().orElseThrow().childrenComplete());
        }
    }

    private static AutoDepositWorkingSetRecovery recovery(AutoDepositMaintenanceTask task) {
        try {
            Field field = AutoDepositMaintenanceTask.class.getDeclaredField("workingSetRecovery");
            field.setAccessible(true);
            return (AutoDepositWorkingSetRecovery) field.get(task);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
}
