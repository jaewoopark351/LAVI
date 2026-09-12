package lavi.minecraft.task.movement.gotopreflight;

/** Immutable local aerial collection budget, created only at a selected preparation site. */
public record GotoMaterialPlan(int startY, int targetY, int requiredHeld) {

    public static final int RESERVE = 3;
    public static final int SEARCH_RADIUS = 8;
    public static final int SEARCH_VERTICAL_RADIUS = 4;
    public static final int MAX_CANDIDATES = 512;
    public static final int MAX_ACQUISITION_TICKS = 12_000;
    public static final int MAX_NO_PROGRESS_TICKS = 600;
    public static final int MAX_SOURCE_TICKS = 300;
    public static final int MAX_CLEANUP_TICKS = 40;

    public GotoMaterialPlan {
        if (requiredHeld < 0) {
            throw new IllegalArgumentException("Negative material budget");
        }
    }

    /**
     * placements is a local geometric budget, not a verified path placement count.
     * The reserve is acquired only when that local budget is not already held.
     */
    public static GotoMaterialPlan forPlacements(int startY, int targetY, int placements) {
        if (placements <= 0 || placements > Integer.MAX_VALUE - RESERVE) {
            throw new IllegalArgumentException("Invalid placement demand: " + placements);
        }
        return new GotoMaterialPlan(startY, targetY, placements + RESERVE);
    }

    public int shortage(int usableHeld) {
        if (usableHeld < 0) {
            throw new IllegalArgumentException("Unknown count is not zero");
        }
        return Math.max(0, requiredHeld - usableHeld);
    }

    public enum FailureReason {
        WORLD_CHANGED,
        ROOT_REPLACED,
        NOT_SURVIVAL,
        SETTINGS_CHANGED,
        PLACING_DISABLED,
        BREAKING_DISABLED,
        INTERACTION_PAUSED,
        INVENTORY_UNAVAILABLE,
        UNSAFE_ACCEPTED_STACK,
        INVENTORY_FULL,
        NO_SAFE_SOURCE,
        CANDIDATE_LIMIT,
        ACQUISITION_TIMEOUT,
        NO_PROGRESS,
        SOURCE_TIMEOUT,
        SOURCE_INVALIDATED,
        DROP_NOT_OBSERVED,
        DROP_LOST,
        PICKUP_UNCONFIRMED,
        TOOL_NOT_READY,
        OUT_OF_BOUNDS,
        CLEANUP_TIMEOUT,
        HANDOFF_SHORTAGE,
        ARRIVAL_LOST,
        AIR_COLUMN_CHANGED,
        FALLBACK_UNAVAILABLE,
        INTERNAL_ERROR
    }

    public static final class Failure extends RuntimeException {
        private final FailureReason reason;

        public Failure(FailureReason reason) {
            this(reason, reason.name());
        }

        public Failure(FailureReason reason, String detail) {
            super(detail);
            this.reason = reason;
        }

        public FailureReason reason() {
            return reason;
        }
    }
}
