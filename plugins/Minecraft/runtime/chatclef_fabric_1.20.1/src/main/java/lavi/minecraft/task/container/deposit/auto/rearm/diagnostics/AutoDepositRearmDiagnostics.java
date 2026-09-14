package lavi.minecraft.task.container.deposit.auto.rearm.diagnostics;

import adris.altoclef.tasksystem.Task;
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
        AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_ROOT_SETTLED", result.reason().name(), task,
                "detail", result.detail(), "childrenComplete", result.childrenComplete(),
                "workingSet", result.workingSetStatus(), "cleanupComplete", result.cleanupComplete(),
                "rootSlotDelta", result.signedFreedSlotDelta().isPresent() ? result.signedFreedSlotDelta().getAsInt() : "UNAVAILABLE",
                "nextPolicyReason", policy.lastReason(), "episode", policy.episodeSequence(),
                "confirmedStoredItems", task instanceof AutoDepositMaintenanceTask maintenance ? maintenance.confirmedStoredCount() : "UNAVAILABLE",
                "confirmedRecoveredItems", task instanceof AutoDepositMaintenanceTask maintenance ? maintenance.confirmedRecoveredCount() : "UNAVAILABLE");
        decision("AFTER_" + result.reason().name(), policy, result.endingPressure().orElse(null), task);
    }
}
