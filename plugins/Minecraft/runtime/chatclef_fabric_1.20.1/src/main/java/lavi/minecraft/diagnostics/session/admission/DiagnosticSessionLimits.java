package lavi.minecraft.diagnostics.session.admission;

/**
 * Immutable slot limits for one process-lifetime ChatClef diagnostic session.
 */
public final class DiagnosticSessionLimits {
    public static final int HARD_CAP = 5_000;
    public static final int ORDINARY_CEILING = 4_936;
    public static final int CRITICAL_RESERVE = 64;

    public static final int CANONICAL_CAP_SLOTS = 1;
    public static final int FINAL_SNAPSHOT_SLOTS = 1;
    public static final int ABNORMAL_STORE_TERMINAL_SLOTS = 16;
    public static final int ROUTINE_STORE_TERMINAL_SLOTS = 8;
    public static final int EXCEPTION_COVERAGE_SLOTS = 16;
    public static final int AGGREGATE_CHECKPOINT_SLOTS = 8;
    public static final int NON_STORE_TERMINAL_SLOTS = 8;
    public static final int SUPPRESSION_CONTROL_SLOTS = 6;

    public static final int TERMINAL_GROUP_SIZE = 4;

    static {
        if (HARD_CAP - ORDINARY_CEILING != CRITICAL_RESERVE) {
            throw new IllegalStateException("The ordinary ceiling must retain the complete critical reserve.");
        }
        if (criticalPartitionTotal() != CRITICAL_RESERVE) {
            throw new IllegalStateException("The critical subquotas must sum to the critical reserve.");
        }
        if (ABNORMAL_STORE_TERMINAL_SLOTS % TERMINAL_GROUP_SIZE != 0
                || ROUTINE_STORE_TERMINAL_SLOTS % TERMINAL_GROUP_SIZE != 0) {
            throw new IllegalStateException("Store terminal pools must contain complete four-record groups.");
        }
    }

    private DiagnosticSessionLimits() {
    }

    public static int criticalPartitionTotal() {
        return CANONICAL_CAP_SLOTS
                + FINAL_SNAPSHOT_SLOTS
                + ABNORMAL_STORE_TERMINAL_SLOTS
                + ROUTINE_STORE_TERMINAL_SLOTS
                + EXCEPTION_COVERAGE_SLOTS
                + AGGREGATE_CHECKPOINT_SLOTS
                + NON_STORE_TERMINAL_SLOTS
                + SUPPRESSION_CONTROL_SLOTS;
    }
}
