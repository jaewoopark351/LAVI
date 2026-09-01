package lavi.minecraft.diagnostics.crafting.acquisition.target;

//20260901_kpopmodder: Separate authoritative target transitions from diagnostic noise.
public enum CraftResourceTargetObservationKind {
    ACTIVE_TARGET_PROVEN,
    TARGET_ABANDONED,
    OWNER_STOP,
    OWNER_INTERRUPT,
    COMMAND_TERMINAL,
    UNREACHABLE_REQUEST,
    BLACKLIST_STATE_CHANGED,
    GOAL_SUBMISSION,
    CANDIDATE_RETURN,
    EQUAL_RECONCILIATION,
    CHAIN_SWITCH,
    DETAIL_SUPPRESSED,
    ADMISSION_DENIED
}
