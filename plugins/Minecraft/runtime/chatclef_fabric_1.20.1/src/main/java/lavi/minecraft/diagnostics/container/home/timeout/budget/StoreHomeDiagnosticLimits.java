package lavi.minecraft.diagnostics.container.home.timeout.budget;

//20260828_kpopmodder: Centralize STORE_HOME diagnostic-only emission limits without owning Task behavior.
public final class StoreHomeDiagnosticLimits {
    public static final long PROGRESS_SAMPLE_INTERVAL_CLIENT_TICKS = 200L;
    public static final int CANDIDATE_PROGRESS_EVENT_CAP = 8;
    public static final int OPERATION_PROGRESS_EVENT_CAP = 64;
    public static final int OPERATION_HARD_CAP = 256;
    public static final int OPERATION_RESERVED_BOUNDARY_CAP = 32;
    public static final int SESSION_HARD_CAP = 5000;
    public static final int SESSION_RESERVED_BOUNDARY_CAP = 32;
    public static final int MAX_EVENT_UTF8_BYTES = 8 * 1024;
    public static final int MAX_SUPPRESSION_FAMILIES = 32;
    public static final int MAX_BUDGET_KEY_LENGTH = 160;

    private StoreHomeDiagnosticLimits() {
    }
}
