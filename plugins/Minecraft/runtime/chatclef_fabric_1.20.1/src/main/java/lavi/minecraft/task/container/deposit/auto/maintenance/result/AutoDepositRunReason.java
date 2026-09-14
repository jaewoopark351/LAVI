package lavi.minecraft.task.container.deposit.auto.maintenance.result;

//20260914_kpopmodder: Distinguish authoritative maintenance termination from safety preemption.
public enum AutoDepositRunReason {
    NORMAL,
    REPLAN_REQUIRED,
    GENERAL_CHILD_UNCONFIRMED,
    CHILD_STOPPED,
    TRUSTED_CHILD_FAILED,
    WORKING_SET_DEFICIT,
    WORKING_SET_UNAVAILABLE,
    PRESSURE_UNAVAILABLE,
    CONTEXT_CHANGED,
    SAFETY_INTERRUPTED,
    STOPPED,
    AUTOMATION_DISABLED,
    BUDGET_EXHAUSTED,
    CLEANUP_FAILED,
    UNEXPECTED_STOP
}
