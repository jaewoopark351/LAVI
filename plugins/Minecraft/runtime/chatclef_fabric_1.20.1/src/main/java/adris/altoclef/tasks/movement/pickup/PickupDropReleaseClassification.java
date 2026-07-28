package adris.altoclef.tasks.movement.pickup;

//20260728_kpopmodder: Added this class to keep pickup drop release classification out of task control flow.
final class PickupDropReleaseClassification {

    private static final String TARGET_REMOVED_REASON = "target removed";
    private static final String STACK_EMPTY_REASON = "stack empty";

    private final boolean consumedOrRemoved;

    private PickupDropReleaseClassification(boolean consumedOrRemoved) {
        this.consumedOrRemoved = consumedOrRemoved;
    }

    static PickupDropReleaseClassification classify(String reason, boolean blacklistEntity, boolean hasFailure, DropSnapshot snapshot) {
        return new PickupDropReleaseClassification(
                !blacklistEntity
                        && !hasFailure
                        && snapshot != null
                        && (TARGET_REMOVED_REASON.equals(reason) || STACK_EMPTY_REASON.equals(reason))
        );
    }

    boolean isConsumedOrRemoved() {
        return consumedOrRemoved;
    }
}
