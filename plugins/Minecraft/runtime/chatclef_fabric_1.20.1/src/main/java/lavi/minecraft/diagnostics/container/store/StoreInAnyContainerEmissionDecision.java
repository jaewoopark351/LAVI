package lavi.minecraft.diagnostics.container.store;

//20260807_kpopmodder: Represent StoreInAnyContainerTask diagnostic emission decisions without behavior effects.
final class StoreInAnyContainerEmissionDecision {
    private static final StoreInAnyContainerEmissionDecision SUPPRESS =
            new StoreInAnyContainerEmissionDecision(false, false, false, 0);
    private static final StoreInAnyContainerEmissionDecision CAP =
            new StoreInAnyContainerEmissionDecision(false, false, true, 0);

    private final boolean emitEvent;
    private final boolean emitSummary;
    private final boolean emitCap;
    private final int suppressedRepeatCount;

    private StoreInAnyContainerEmissionDecision(boolean emitEvent,
                                                boolean emitSummary,
                                                boolean emitCap,
                                                int suppressedRepeatCount) {
        this.emitEvent = emitEvent;
        this.emitSummary = emitSummary;
        this.emitCap = emitCap;
        this.suppressedRepeatCount = suppressedRepeatCount;
    }

    static StoreInAnyContainerEmissionDecision event(int suppressedRepeatCount) {
        return new StoreInAnyContainerEmissionDecision(true, false, false, suppressedRepeatCount);
    }

    static StoreInAnyContainerEmissionDecision summary(int suppressedRepeatCount) {
        return new StoreInAnyContainerEmissionDecision(false, true, false, suppressedRepeatCount);
    }

    static StoreInAnyContainerEmissionDecision suppress() {
        return SUPPRESS;
    }

    static StoreInAnyContainerEmissionDecision cap() {
        return CAP;
    }

    boolean emitEvent() {
        return emitEvent;
    }

    boolean emitSummary() {
        return emitSummary;
    }

    boolean emitCap() {
        return emitCap;
    }

    int suppressedRepeatCount() {
        return suppressedRepeatCount;
    }
}
