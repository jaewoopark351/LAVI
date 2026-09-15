package lavi.minecraft.task.container.deposit.auto.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.AutoDepositConditions;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositBudgetStatus;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositExecutionBudget;
import lavi.minecraft.task.container.deposit.auto.progress.AutoDepositProgressSample;
import lavi.minecraft.task.container.deposit.auto.progress.AutoDepositProgressTracker;
import lavi.minecraft.task.container.deposit.auto.rearm.diagnostics.AutoDepositRearmDiagnostics;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositWorkingSetStatus;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmPolicy;

//20260914_kpopmodder: Correlate exact root generations with one logical storage unit and its actual execution budget.
public final class AutoDepositRunLedger {
    private final AutoDepositRearmPolicy policy;
    private AutoDepositMaintenanceTask active;
    private AutoDepositMaintenanceTask suspended;
    private AutoDepositMaintenanceTask originalReservationOwner;
    //20260915_kpopmodder: Preparation watermarks belong to the same finite budget, not a disposable root.
    private AutoDepositExecutionBudget progressBudget;
    private AutoDepositProgressTracker preparation = new AutoDepositProgressTracker();
    private long lastMeaningfulProgressTick = -1;
    private String lastMeaningfulProgressKind = "NONE";
    private AutoDepositProgressSample.Target lastProgressTarget;
    private int confirmedCount;
    private int confirmedRecovered;
    private String condition = "unobserved";
    private String scope = "automatic";
    private AutoDepositConditions admissionConditions;

    public AutoDepositRunLedger(AutoDepositRearmPolicy policy) { this.policy = policy; }

    public void begin(AutoDepositMaintenanceTask task, DepositAllInventoryPressureSnapshot pressure,
                      String condition, String scope) {
        begin(task, pressure, condition, scope, null);
    }

    public void begin(AutoDepositMaintenanceTask task, DepositAllInventoryPressureSnapshot pressure,
                      String condition, String scope, AutoDepositConditions admissionConditions) {
        if (active != null) throw new IllegalStateException("Automatic storage already owns a root");
        AutoDepositExecutionBudget budget = policy.beginUnit(pressure, condition, scope);
        if (progressBudget != budget) {
            progressBudget = budget;
            preparation = new AutoDepositProgressTracker();
            lastMeaningfulProgressTick = -1;
            lastMeaningfulProgressKind = "NONE";
            lastProgressTarget = null;
        }
        preparation.breakContinuity();
        if (originalReservationOwner == null) originalReservationOwner = task;
        this.active = task;
        this.suspended = null;
        this.confirmedCount = 0;
        this.confirmedRecovered = 0;
        this.condition = condition;
        this.scope = scope;
        this.admissionConditions = admissionConditions;
    }

    public void executionTick(AutoDepositMaintenanceTask task) {
        if (task == null || task != active) return;
        executionTick(task, task.preparationProgress());
    }

    //20260915_kpopmodder: The test seam supplies facts only; accounting, ownership and termination remain real.
    void executionTick(AutoDepositMaintenanceTask task, AutoDepositProgressSample sample) {
        if (task == null || task != active) return;
        int now = task.confirmedStoredCount();
        int recovered = task.confirmedRecoveredCount();
        boolean storageProgress = now > confirmedCount;
        boolean recoveryProgress = recovered > confirmedRecovered;
        boolean confirmedProgress = storageProgress || recoveryProgress;
        confirmedCount = Math.max(confirmedCount, now);
        confirmedRecovered = Math.max(confirmedRecovered, recovered);
        AutoDepositExecutionBudget budget = policy.activeBudget();
        AutoDepositProgressTracker.Decision preparationDecision = preparation.observe(sample);
        long before = budget.consumedExecutionTicks();
        AutoDepositBudgetStatus status = budget.onExecutionTick(confirmedProgress || preparationDecision.progressed());
        if (budget.consumedExecutionTicks() > before && (confirmedProgress || preparationDecision.progressed())) {
            lastMeaningfulProgressTick = budget.consumedExecutionTicks();
            lastMeaningfulProgressKind = storageProgress ? "CONFIRMED_STORAGE" : recoveryProgress ? "CONFIRMED_RECOVERY"
                    : preparationDecision.navigationAdvanced() ? "APPROACH_ADVANCED" : "PREPARATION_RESOURCE_INCREASED";
            lastProgressTarget = preparationDecision.target();
        }
        if (status != AutoDepositBudgetStatus.AVAILABLE) {
            task.terminate(AutoDepositRunReason.BUDGET_EXHAUSTED, status.name());
        }
        AutoDepositRearmDiagnostics.executionProgress(task, policy, preparationDecision,
                storageProgress, recoveryProgress, lastMeaningfulProgressTick, lastMeaningfulProgressKind, lastProgressTarget, status);
    }

    public boolean settle(AutoDepositMaintenanceTask task, AutoDepositRunResult result, String terminalCondition) {
        return settle(task, result, terminalCondition, false);
    }

    public boolean settle(AutoDepositMaintenanceTask task, AutoDepositRunResult result,
                          String terminalCondition, boolean userRootReplaced) {
        if (task != active) return false;
        // A child may transfer after our parent tick and then be preempted before the next one.
        int finalConfirmed = task.confirmedStoredCount();
        int finalRecovered = task.confirmedRecoveredCount();
        if (finalConfirmed > confirmedCount || finalRecovered > confirmedRecovered) policy.activeBudget().observeConfirmedProgress();
        confirmedCount = Math.max(confirmedCount, finalConfirmed);
        confirmedRecovered = Math.max(confirmedRecovered, finalRecovered);
        active = null;
        preparation.breakContinuity();
        if (result.cleanupComplete() && result.reason() == AutoDepositRunReason.STOPPED) {
            cancelPendingUnit("explicit_stop");
            return true;
        }
        if (userRootReplaced && result.cleanupComplete() && result.reason() == AutoDepositRunReason.CONTEXT_CHANGED) {
            cancelPendingUnit("user_root_replaced");
            return true;
        }
        boolean yielded = result.cleanupComplete() && switch (result.reason()) {
            case SAFETY_INTERRUPTED, AUTOMATION_DISABLED, REPLAN_REQUIRED -> true;
            default -> false;
        };
        if (yielded) {
            suspended = task;
            policy.suspend(result.reason().name());
        } else {
            suspended = null;
            String reason = result.reason().name() + ":" + result.detail();
            String fallbackCondition = admissionConditions == null ? condition : admissionConditions.conditionKey(reason);
            policy.finishUnit(result.normalValidated(), result.endingPressure().orElse(null),
                    reason, scope, terminalCondition == null ? fallbackCondition : terminalCondition);
            originalReservationOwner = null;
        }
        return true;
    }

    public AutoDepositMaintenanceTask suspendedTask() { return suspended; }
    public AutoDepositMaintenanceTask recoverySource() { return originalReservationOwner; }
    public boolean reconcileUserRoot(Task currentRoot) {
        if (suspended == null || !policy.hasPendingUnit()
                || suspended.plan().context().userTaskRoot() == currentRoot) return false;
        cancelPendingUnit("user_root_replaced");
        return true;
    }
    public void cancelPendingUnit(String reason) {
        if (active != null) throw new IllegalStateException("Owned cleanup must precede cancellation");
        policy.cancelPendingUnit(reason);
        suspended = null;
        originalReservationOwner = null;
    }
    public AutoDepositWorkingSetStatus verifyRetainedWorkingSet() {
        if (originalReservationOwner == null || !policy.hasPendingUnit()) return AutoDepositWorkingSetStatus.NOT_APPLICABLE;
        // Reservation satisfaction at handoff is final for that root: later survival consumption
        // belongs to the current plan and must not manufacture debt against a settled reservation.
        AutoDepositWorkingSetStatus captured = originalReservationOwner.result()
                .map(AutoDepositRunResult::workingSetStatus)
                .orElse(AutoDepositWorkingSetStatus.NOT_EVALUATED);
        if (captured.satisfiedOrNotApplicable()) {
            originalReservationOwner = null;
            return captured;
        }
        // A new root is admitted only after this original, unresolved reservation is satisfied.
        // Clearing it lets begin bind the next root; suspension/replanning does not erase debt.
        AutoDepositWorkingSetStatus current = originalReservationOwner.verifyWorkingSetBeforeResume();
        if (current.satisfiedOrNotApplicable()) originalReservationOwner = null;
        return current;
    }
    public boolean mayResumeVerification() {
        return suspended != null && policy.hasPendingUnit()
                && suspended.result().filter(r -> r.cleanupComplete() && r.childrenComplete()).isPresent();
    }
    public void resetContext() {
        active = null;
        suspended = null;
        originalReservationOwner = null;
        policy.resetContext();
        progressBudget = null;
        preparation = new AutoDepositProgressTracker();
        lastMeaningfulProgressTick = -1;
        lastMeaningfulProgressKind = "NONE";
        lastProgressTarget = null;
    }
}
