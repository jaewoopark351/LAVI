package lavi.minecraft.task.container.deposit.auto.maintenance.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositOperationObservation;
import lavi.minecraft.diagnostics.observation.ObservationScope;
import lavi.minecraft.task.container.deposit.auto.DepositAllAutoDiagnostics;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenancePhase;
import lavi.minecraft.task.container.deposit.auto.maintenance.relief.AutoDepositFreeSlotVerdict;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;

import java.util.Objects;

//20260831_kpopmodder: Keep automatic-maintenance observation separate from phase decisions.
/** Owns bounded observations for one automatic-deposit maintenance Task. */
public final class AutoDepositMaintenanceDiagnostics {
    private final boolean automaticRunEnabled;
    private Task registeredChild;
    private boolean terminalRecorded;
    //20260913_kpopmodder: Preserve exact command provenance when maintenance ends after root replacement.
    private final AutoDepositOperationObservation observation;

    public AutoDepositMaintenanceDiagnostics(AutoDepositPlan plan) {
        Objects.requireNonNull(plan, "plan");
        boolean enabled = false;
        AutoDepositOperationObservation captured = null;
        try {
            enabled = plan.diagnosticPolicyObservationRetained() && ChatClefDiagnostics.isBoundaryEnabled();
            captured = new AutoDepositOperationObservation(plan.context().userTaskRoot());
        } catch (RuntimeException | LinkageError ignored) { }
        automaticRunEnabled = enabled;
        observation = captured;
    }

    public ObservationScope observationScope() {
        try { return observation == null ? ObservationScope.NOOP : observation.scope(); }
        catch (RuntimeException | LinkageError ignored) { return ObservationScope.NOOP; }
    }

    public boolean automaticRunEnabled() {
        return automaticRunEnabled;
    }

    public void registerTrustedChild(
            AltoClef mod,
            Task maintenanceTask,
            Task childTask,
            ItemTarget[] targets) {
        registerChild(mod, maintenanceTask, childTask, 0, targets);
    }

    public void registerGeneralChild(
            AltoClef mod,
            Task maintenanceTask,
            Task childTask,
            int generalTaskIndex,
            boolean trustedStepPresent,
            ItemTarget target) {
        int childIndex = trustedStepPresent ? generalTaskIndex + 1 : generalTaskIndex;
        registerChild(
                mod,
                maintenanceTask,
                childTask,
                childIndex,
                new ItemTarget[]{target}
        );
    }

    public void recordTransition(
            long operationEpoch,
            AutoDepositMaintenancePhase previous,
            AutoDepositMaintenancePhase next,
            String reason,
            int deficitTypes,
            Task maintenanceTask) {
        try {
        DepositAllAutoDiagnostics.logMaintenanceTransition(
                operationEpoch,
                previous,
                next,
                reason,
                deficitTypes,
                maintenanceTask
        );
        if (next == AutoDepositMaintenancePhase.DONE
                || next == AutoDepositMaintenancePhase.CANCELLED) {
            recordTerminal(maintenanceTask, reason, next.name());
        }
        } catch (RuntimeException | LinkageError ignored) { }
    }

    public void recordFreeSlotVerdict(
            long operationEpoch,
            int startingOccupiedSlots,
            int expectedFreedSlots,
            AutoDepositFreeSlotVerdict verdict,
            Task maintenanceTask) {
        //20260914_kpopmodder: Unavailable observations must not pass through an arithmetic-only diagnostic.
        if (!verdict.available()) return;
        try {
        DepositAllAutoDiagnostics.logFreeSlotOutcome(
                operationEpoch,
                startingOccupiedSlots,
                verdict.endingOccupiedSlots(),
                expectedFreedSlots,
                verdict.outcome(),
                maintenanceTask
        );
        } catch (RuntimeException | LinkageError ignored) { }
    }

    //20260914_kpopmodder: Observe immutable root results; emission never selects or replaces the decision.
    public void recordRunResult(AutoDepositRunResult result, Task maintenanceTask, boolean finalized) {
        try {
            String reason = (finalized ? "FINAL_" : "CANDIDATE_") + result.reason().name();
            boolean terminal = finalized && result.reason() != AutoDepositRunReason.SAFETY_INTERRUPTED
                    && result.reason() != AutoDepositRunReason.AUTOMATION_DISABLED
                    && result.reason() != AutoDepositRunReason.REPLAN_REQUIRED;
            observationScope().record("AUTO_DEPOSIT_MAINTENANCE_RESULT", reason, reason, terminal,
                    AutoDepositRunResultFields.of(result, finalized));
        } catch (RuntimeException | LinkageError ignored) { }
    }

    public void recordTerminal(Task maintenanceTask, String reason, String terminalState) {
        try {
        if (!automaticRunEnabled
                || terminalRecorded
                || !ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        StoreDepositDiagnostics.recordAutomaticMaintenanceTerminal(
                maintenanceTask,
                reason,
                terminalState
        );
        terminalRecorded = true;
        } catch (RuntimeException | LinkageError ignored) { }
    }

    private void registerChild(
            AltoClef mod,
            Task maintenanceTask,
            Task childTask,
            int childIndex,
            ItemTarget[] targets) {
        try {
        if (!automaticRunEnabled
                || !ChatClefDiagnostics.isBoundaryEnabled()
                || registeredChild == childTask) {
            return;
        }
        StoreDepositDiagnostics.registerAutomaticMaintenanceChild(
                mod,
                maintenanceTask,
                childTask,
                childIndex,
                targets
        );
        registeredChild = childTask;
        } catch (RuntimeException | LinkageError ignored) { }
    }
}
