package lavi.minecraft.task.container.deposit.auto.budget;

//20260914_kpopmodder: Keep finite actual-work accounting across roots and defense interruptions.
public final class AutoDepositExecutionBudget {
    // Two existing trusted-operation windows; a stall window matches its candidate limit.
    public static final int BASE_EXECUTION_TICKS = 12000;
    public static final int NO_PROGRESS_TICKS = 2400;
    public static final int BASE_COMPLETED_UNITS = 3;
    public static final int MAX_RECOVERY_GRANTS = 2;
    public static final int RECOVERY_EXECUTION_TICKS = 6000;

    private final int baseExecutionTicks;
    private final int noProgressLimit;
    private final int baseCompletedUnits;
    private final int maxRecoveryGrants;
    private final int recoveryExecutionTicks;
    private long executionTicks;
    private long cumulativeNoProgress;
    private int consecutiveNoProgress;
    private int completedUnits;
    private int recoveryGrants;

    public AutoDepositExecutionBudget() {
        this(BASE_EXECUTION_TICKS, NO_PROGRESS_TICKS, BASE_COMPLETED_UNITS,
                MAX_RECOVERY_GRANTS, RECOVERY_EXECUTION_TICKS);
    }

    public AutoDepositExecutionBudget(int baseExecutionTicks, int noProgressLimit,
                                      int baseCompletedUnits, int maxRecoveryGrants,
                                      int recoveryExecutionTicks) {
        if (baseExecutionTicks <= 0 || noProgressLimit <= 0 || baseCompletedUnits <= 0
                || maxRecoveryGrants < 0 || recoveryExecutionTicks <= 0) {
            throw new IllegalArgumentException("Storage limits must be positive; grants may be zero");
        }
        this.baseExecutionTicks = baseExecutionTicks;
        this.noProgressLimit = noProgressLimit;
        this.baseCompletedUnits = baseCompletedUnits;
        this.maxRecoveryGrants = maxRecoveryGrants;
        this.recoveryExecutionTicks = recoveryExecutionTicks;
    }

    /** Call exactly once per actual automatic-storage tick, never during safety waiting. */
    public AutoDepositBudgetStatus onExecutionTick(boolean meaningfulProgress) {
        AutoDepositBudgetStatus before = status();
        if (before != AutoDepositBudgetStatus.AVAILABLE) return before;
        executionTicks++;
        if (meaningfulProgress) {
            //20260915_kpopmodder: May be verified preparation; it never proves stored-item success.
            consecutiveNoProgress = 0;
        } else {
            consecutiveNoProgress++;
            cumulativeNoProgress++;
        }
        return status();
    }

    /** Capture a final child-tick transfer without charging another tick or clearing cumulative work. */
    public void observeConfirmedProgress() {
        consecutiveNoProgress = 0;
    }

    /** A logical unit terminates once; registering or suspending its root never calls this. */
    public void completeUnit() {
        if (completedUnits < Integer.MAX_VALUE) completedUnits++;
    }

    /** Only the rearm owner may grant a consumed, cause-related new opportunity. */
    public boolean grantRelatedChange() {
        if (recoveryGrants >= maxRecoveryGrants) return false;
        recoveryGrants++;
        consecutiveNoProgress = 0;
        return true;
    }

    public AutoDepositBudgetStatus status() {
        if (executionTicks >= maxExecutionTicks()) return AutoDepositBudgetStatus.EXECUTION_TICK_LIMIT;
        if (consecutiveNoProgress >= noProgressLimit) return AutoDepositBudgetStatus.NO_PROGRESS_LIMIT;
        if (completedUnits >= (long) baseCompletedUnits + recoveryGrants) {
            return AutoDepositBudgetStatus.FOLLOWUP_LIMIT;
        }
        return AutoDepositBudgetStatus.AVAILABLE;
    }

    public boolean canGrantRelatedChange() { return recoveryGrants < maxRecoveryGrants; }
    public long consumedExecutionTicks() { return executionTicks; }
    public int consecutiveNoProgressTicks() { return consecutiveNoProgress; }
    public long cumulativeNoProgressTicks() { return cumulativeNoProgress; }
    public int completedUnits() { return completedUnits; }
    public int recoveryGrants() { return recoveryGrants; }
    public long maxExecutionTicks() {
        return (long) baseExecutionTicks + (long) recoveryGrants * recoveryExecutionTicks;
    }
}
