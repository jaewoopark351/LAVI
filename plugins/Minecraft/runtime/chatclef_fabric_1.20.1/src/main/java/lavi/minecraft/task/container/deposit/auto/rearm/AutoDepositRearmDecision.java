package lavi.minecraft.task.container.deposit.auto.rearm;

//20260914_kpopmodder: Separate a storage opportunity from current execution priority.
public enum AutoDepositRearmDecision {
    INITIAL(true), PRESSURE_RISE(true), CONTINUE(true), RESUME(true), RELATED_CHANGE(true),
    BELOW_THRESHOLD(false), UNKNOWN_PRESSURE(false), UNKNOWN_CONDITION(false), RUNNING(false),
    WAIT_FOR_PRESSURE_RISE(false), SAME_FAILURE(false), BUDGET_EXHAUSTED(false);

    private final boolean canEvaluate;
    AutoDepositRearmDecision(boolean canEvaluate) { this.canEvaluate = canEvaluate; }
    public boolean canEvaluate() { return canEvaluate; }
    public boolean isResume() { return this == RESUME; }
}
