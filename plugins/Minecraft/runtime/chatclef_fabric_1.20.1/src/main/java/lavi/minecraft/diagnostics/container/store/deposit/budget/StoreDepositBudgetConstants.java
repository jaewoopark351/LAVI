package lavi.minecraft.diagnostics.container.store.deposit.budget;

public final class StoreDepositBudgetConstants {
    public static final int TOTAL_SESSION_CAP = 5000;
    public static final int CRITICAL_RESERVE_CAP = 64;
    public static final int NONCRITICAL_DETAIL_CAP = TOTAL_SESSION_CAP - CRITICAL_RESERVE_CAP;
    public static final int MAX_DETAIL_KEYS_PER_EVENT = 256;
    public static final int MAX_EXCEPTION_SIGNATURES = 16;
    public static final int MAX_TERMINAL_GROUPS = 8;
    public static final int TERMINAL_GROUP_EVENT_COUNT = 4;
    public static final int MAX_LATE_SUMMARIES = 8;
    public static final int MAX_CONTROL_EVENTS = 8;
    public static final int MAX_KEY_LENGTH = 360;

    private StoreDepositBudgetConstants() {
    }
}
