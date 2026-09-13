package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import adris.altoclef.tasksystem.TaskChain;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureChain;

//20260913_kpopmodder: Consume the scheduler's existing local results without invoking chain decisions again.
public final class AutoDepositSchedulerDiagnostics {
    private AutoDepositSchedulerDiagnostics() {
    }

    public static void evaluated(TaskChain chain, boolean active, Float priority) {
        if (!(chain instanceof DepositAllInventoryPressureChain)) return;
        AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_SCHEDULER_EVALUATED",
                active ? "active_chain_priority_observed" : "inactive_priority_not_evaluated", null,
                "chain", AutoDepositObservationFields.identity(chain),
                "activeObserved", active,
                "priorityObserved", priority == null ? "NOT_EVALUATED" : priority,
                "evaluationSource", "TaskRunner_existing_local_values");
    }

    public static void selected(TaskChain selected, float priority, TaskChain previous) {
        AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_SCHEDULER_SELECTED",
                selected == null ? "no_active_chain" : "final_chain_selected", null,
                "selectedChain", AutoDepositObservationFields.identity(selected),
                "selectedChainClass", selected == null ? "NONE" : selected.getClass().getSimpleName(),
                "previousChain", AutoDepositObservationFields.identity(previous),
                "selectedPriority", selected == null ? "NOT_EVALUATED" : priority,
                "selectionPhase", "before_previous_chain_interrupt");
    }
}
