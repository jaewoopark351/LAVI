package lavi.minecraft.task.container.deposit.auto.rearm.diagnostics;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositBudgetStatus;
import lavi.minecraft.task.container.deposit.auto.progress.AutoDepositProgressTracker;
import lavi.minecraft.task.container.deposit.auto.progress.AutoDepositProgressSample;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositBoundaryDiagnostics;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunResult;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmPolicy;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositExecutionBudget;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;

//20260914_kpopmodder: Emit bounded observations after policy decisions; diagnostic state never grants execution.
public final class AutoDepositRearmDiagnostics {
    private AutoDepositRearmDiagnostics() { }
    public static void decision(String reason, AutoDepositRearmPolicy policy,
                                DepositAllInventoryPressureSnapshot pressure, Task task) {
        AutoDepositExecutionBudget budget = policy.activeBudget();
        AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_REARM", reason, task,
                "episode", policy.episodeSequence(), "pendingLogicalUnit", policy.hasPendingUnit(),
                "occupiedSlots", pressure == null ? "UNAVAILABLE" : pressure.occupiedSlots(),
                "logicalStart", policy.logicalStart() == null ? "NOT_ACTIVE" : policy.logicalStart().occupiedSlots(),
                "executionTicks", budget.consumedExecutionTicks(),
                "noProgressTicks", budget.consecutiveNoProgressTicks(),
                "cumulativeNoProgressTicks", budget.cumulativeNoProgressTicks(),
                "completedUnits", budget.completedUnits(), "recoveryGrants", budget.recoveryGrants(),
                "budgetStatus", budget.status());
    }
    public static void result(AutoDepositRunResult result, AutoDepositRearmPolicy policy, Task task) {
        result(result, policy, task, result.reason());
    }
    public static void result(AutoDepositRunResult result, AutoDepositRearmPolicy policy,
                              Task task, AutoDepositRunReason callbackReason) {
        AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_ROOT_SETTLED", result.reason().name(), task,
                "detail", result.detail(), "callbackReason", callbackReason,
                "effectiveTerminalReason", result.reason(), "childrenComplete", result.childrenComplete(),
                "workingSet", result.workingSetStatus(), "cleanupComplete", result.cleanupComplete(),
                "rootSlotDelta", result.signedFreedSlotDelta().isPresent() ? result.signedFreedSlotDelta().getAsInt() : "UNAVAILABLE",
                "nextPolicyReason", policy.lastReason(), "episode", policy.episodeSequence(),
                "confirmedStoredItems", task instanceof AutoDepositMaintenanceTask maintenance ? maintenance.confirmedStoredCount() : "UNAVAILABLE",
                "confirmedRecoveredItems", task instanceof AutoDepositMaintenanceTask maintenance ? maintenance.confirmedRecoveredCount() : "UNAVAILABLE");
        decision("AFTER_" + result.reason().name(), policy, result.endingPressure().orElse(null), task);
    }

    //20260915_kpopmodder: Semantic reasons exclude numeric values, task IDs, goal IDs and per-tick fingerprints.
    public static void executionProgress(AutoDepositMaintenanceTask task, AutoDepositRearmPolicy policy,
                                         AutoDepositProgressTracker.Decision progress, boolean storageProgress, boolean recoveryProgress,
                                         long lastProgressTick, String lastProgressKind,
                                         AutoDepositProgressSample.Target lastProgressTarget,
                                         AutoDepositBudgetStatus status) {
        try {
            boolean exhausted = status != AutoDepositBudgetStatus.AVAILABLE;
            if (!exhausted && !progress.progressed() && !progress.phaseChanged()
                    && !progress.capacityReached() && !storageProgress && !recoveryProgress) return;
            AutoDepositExecutionBudget budget = policy.activeBudget();
            String reason = exhausted ? status.name() : storageProgress ? "CONFIRMED_STORAGE" : recoveryProgress ? "CONFIRMED_RECOVERY"
                    : progress.navigationAdvanced() ? "APPROACH_ADVANCED"
                    : progress.resourceAdvanced() ? "PREPARATION_RESOURCE_INCREASED"
                    : progress.capacityReached() ? "PROGRESS_TRACKING_CAPACITY" : "PROGRESS_PHASE_" + progress.phase().name();
            Object[] fields = {
                    "episode", policy.episodeSequence(), "maintenanceTaskIdentity", Integer.toHexString(System.identityHashCode(task)),
                    "progressPhase", progress.phase(), "navigationAdvanced", progress.navigationAdvanced(),
                    "resourceAdvanced", progress.resourceAdvanced(), "confirmedStorageAdvanced", storageProgress, "confirmedRecoveryAdvanced", recoveryProgress,
                    "target", progress.target() == null ? "UNAVAILABLE" : progress.target().toString(),
                    "distance", Double.isFinite(progress.distance()) ? progress.distance() : "UNAVAILABLE",
                    "previousBestDistance", Double.isFinite(progress.previousBestDistance()) ? progress.previousBestDistance() : "UNAVAILABLE",
                    "advancedResourceCount", progress.advancedResourceCount(), "resourceKey", progress.resourceKey(),
                    "resourceBestBefore", progress.resourceBestBefore(), "resourceHeldAfter", progress.resourceHeldAfter(),
                    "resourceLimit", progress.resourceLimit(), "trackedTargets", progress.trackedTargets(),
                    "trackedResources", progress.trackedResources(), "trackingCapacityReached", progress.capacityReached(),
                    "executionTicks", budget.consumedExecutionTicks(), "maxExecutionTicks", budget.maxExecutionTicks(),
                    "noProgressTicks", budget.consecutiveNoProgressTicks(), "cumulativeNoProgressTicks", budget.cumulativeNoProgressTicks(),
                    "lastMeaningfulProgressTick", lastProgressTick, "lastMeaningfulProgressKind", lastProgressKind,
                    "lastProgressTarget", lastProgressTarget == null ? "UNAVAILABLE" : lastProgressTarget.toString(),
                    "budgetStatus", status,
                    "confirmedStoredItems", task.confirmedStoredCount(), "confirmedRecoveredItems", task.confirmedRecoveredCount(),
                    "storageSuccessAuthority", "CONFIRMED_TRANSFER_AND_POSTCONDITIONS_ONLY",
                    "detailedTracing", ChatClefDiagnostics.isBoundaryEnabled() ? "BOUNDARY_DELIVERY_NOT_VERIFIED" : "DISABLED"
            };
            // Existing normal lifecycle output remains available with diagnostics OFF; sink failure is not success.
            if (exhausted) ChatClefDiagnostics.logLifecycleBoundary("AUTO_DEPOSIT_BUDGET_LIMIT", reason, task, fields);
            AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_EXECUTION_PROGRESS", reason, task, fields);
        } catch (RuntimeException | LinkageError ignored) {
            // Only formatter/emitter failures are isolated; no engine call is enclosed by this catch.
        }
    }
}
