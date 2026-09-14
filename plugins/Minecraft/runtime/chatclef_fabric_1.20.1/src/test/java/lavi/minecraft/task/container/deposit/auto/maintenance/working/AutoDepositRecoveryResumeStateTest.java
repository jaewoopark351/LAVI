package lavi.minecraft.task.container.deposit.auto.maintenance.working;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceOutcome;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositContextSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.recovery.AutoDepositDestinationManifest;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.testsupport.auto.AutoDepositPlanFixtureFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Recovery resume retains original debt/provenance and cannot invent child success from an empty plan.
class AutoDepositRecoveryResumeStateTest {
    @Test
    void originalDebtAndManifestRemainBoundEvenWhenTheCurrentSnapshotWasRecomputed() {
        Task root = root();
        Object world = new Object();
        WorkingSetSnapshot original = working(root, world, 1);
        WorkingSetSnapshot current = working(root, world, 2);
        AutoDepositPlan plan = plan(current);
        AutoDepositDestinationManifest manifest = new AutoDepositDestinationManifest(world, Dimension.OVERWORLD, 1);

        AutoDepositRecoveryResumeState state = AutoDepositRecoveryResumeState.capture(
                plan.verificationOnlyCopy(), original, manifest, Optional.of(interrupted(false, true)));

        assertTrue(state.refusal().isEmpty());
        assertSame(original, state.snapshot());
        assertNotSame(current, state.snapshot());
        assertSame(manifest, state.manifest());
        assertFalse(state.childrenComplete());
        assertTrue(AutoDepositRecoveryResumeState.capture(plan, original, manifest,
                Optional.of(interrupted(true, true))).childrenComplete());
    }

    @Test
    void absentCleanupDebtAndChangedOwnershipRefuseWithoutReplayingAnyOldChild() {
        Task root = root();
        Object world = new Object();
        WorkingSetSnapshot original = working(root, world, 1);
        AutoDepositPlan plan = plan(original);
        AutoDepositDestinationManifest manifest = new AutoDepositDestinationManifest(world, Dimension.OVERWORLD, 1);
        assertEquals(AutoDepositRunReason.CLEANUP_FAILED,
                AutoDepositRecoveryResumeState.capture(plan, original, manifest, Optional.empty()).refusal().orElseThrow());
        assertEquals(AutoDepositRunReason.CLEANUP_FAILED,
                AutoDepositRecoveryResumeState.capture(plan, original, manifest,
                        Optional.of(interrupted(false, false))).refusal().orElseThrow());
        assertEquals(AutoDepositRunReason.WORKING_SET_UNAVAILABLE,
                AutoDepositRecoveryResumeState.capture(plan, null, manifest,
                        Optional.of(interrupted(false, true))).refusal().orElseThrow());
        assertEquals(AutoDepositRunReason.CONTEXT_CHANGED,
                AutoDepositRecoveryResumeState.capture(plan(working(root(), world, 2)), original, manifest,
                        Optional.of(interrupted(false, true))).refusal().orElseThrow());
        assertEquals(AutoDepositRunReason.CONTEXT_CHANGED,
                AutoDepositRecoveryResumeState.capture(plan(working(root, new Object(), 2)), original, manifest,
                        Optional.of(interrupted(false, true))).refusal().orElseThrow());
    }

    @Test
    void successfulRecoveryRequestsPlanningAndNeverClaimsNormalStorageCompletion() {
        AutoDepositRunResult replan = new AutoDepositRunResult(AutoDepositRunReason.REPLAN_REQUIRED,
                Optional.of(new DepositAllInventoryPressureSnapshot(32, 36)),
                Optional.of(new DepositAllInventoryPressureSnapshot(33, 36)), false,
                AutoDepositWorkingSetStatus.SATISFIED, true, "recovery_satisfied", AutoDepositMaintenanceOutcome.NO_SLOT_RELIEF);
        assertFalse(replan.normalValidated());
        assertEquals(-1, replan.signedFreedSlotDelta().orElseThrow());
    }

    private static AutoDepositRunResult interrupted(boolean children, boolean cleanup) {
        return new AutoDepositRunResult(AutoDepositRunReason.SAFETY_INTERRUPTED,
                Optional.of(new DepositAllInventoryPressureSnapshot(36, 36)),
                Optional.of(new DepositAllInventoryPressureSnapshot(32, 36)), children,
                AutoDepositWorkingSetStatus.DEFICIT, cleanup, "test", AutoDepositMaintenanceOutcome.CANCELLED);
    }

    private static WorkingSetSnapshot working(Task root, Object world, long epoch) {
        return new WorkingSetSnapshot(root, List.of(root), world, Dimension.OVERWORLD, epoch, Map.of(), Map.of(), Map.of());
    }

    private static AutoDepositPlan plan(WorkingSetSnapshot working) {
        AutoDepositContextSnapshot context = new AutoDepositContextSnapshot(working.worldIdentity(), working.dimension(),
                "recovery-resume", working.epoch(), working.userTaskRoot(), working, List.of());
        return AutoDepositPlanFixtureFactory.generalPlan(context, working.userTaskRoot(), 32, 5, 2);
    }

    private static Task root() {
        return new Task() {
            @Override protected void onStart() { }
            @Override protected Task onTick() { return null; }
            @Override protected void onStop(Task interruptTask) { }
            @Override protected boolean isEqual(Task other) { return this == other; }
            @Override protected String toDebugString() { return "recovery-resume-fixture"; }
        };
    }
}
