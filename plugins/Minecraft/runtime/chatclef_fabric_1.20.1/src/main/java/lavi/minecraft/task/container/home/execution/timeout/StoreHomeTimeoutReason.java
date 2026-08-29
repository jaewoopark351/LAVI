package lavi.minecraft.task.container.home.execution.timeout;

//20260828_kpopmodder: Preserve phase-specific timeout reasons as stable STORE_HOME values.
public enum StoreHomeTimeoutReason {
    CANDIDATE_NAVIGATION_NO_PROGRESS("candidate_navigation_no_progress", true),
    CANDIDATE_LOCAL_INTERACTION_TIMEOUT(
            "candidate_local_interaction_timeout", true
    ),
    OPERATION_NO_PROGRESS("operation_no_progress", false),
    OPERATION_EMERGENCY_HARD_CAP("operation_emergency_hard_cap", false);

    private final String stableReason;
    private final boolean candidateScoped;

    StoreHomeTimeoutReason(String stableReason, boolean candidateScoped) {
        this.stableReason = stableReason;
        this.candidateScoped = candidateScoped;
    }

    public String stableReason() {
        return stableReason;
    }

    public boolean candidateScoped() {
        return candidateScoped;
    }
}
