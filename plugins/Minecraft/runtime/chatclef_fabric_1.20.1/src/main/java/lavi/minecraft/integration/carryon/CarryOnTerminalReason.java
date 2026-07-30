package lavi.minecraft.integration.carryon;

//20260730_kpopmodder: Represent Carry On diagnostic terminal outcomes without engine fallback policy.
public enum CarryOnTerminalReason {
    SUCCESS,
    FAILED,
    INTERRUPTED,
    RETRY_EXHAUSTED,
    CAPABILITY_ABSENT,
    CAPABILITY_INCOMPATIBLE,
    STATE_UNREADABLE,
    OBSERVATION_FAILED,
    OBSERVATION_WINDOW_EXPIRED,
    SESSION_ENDED,
    UNAVAILABLE
}
