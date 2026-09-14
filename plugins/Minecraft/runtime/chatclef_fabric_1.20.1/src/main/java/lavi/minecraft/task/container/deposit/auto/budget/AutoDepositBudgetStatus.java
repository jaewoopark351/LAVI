package lavi.minecraft.task.container.deposit.auto.budget;

//20260914_kpopmodder: Report actual storage limits independently of survival preemption.
public enum AutoDepositBudgetStatus {
    AVAILABLE,
    EXECUTION_TICK_LIMIT,
    NO_PROGRESS_LIMIT,
    FOLLOWUP_LIMIT
}
