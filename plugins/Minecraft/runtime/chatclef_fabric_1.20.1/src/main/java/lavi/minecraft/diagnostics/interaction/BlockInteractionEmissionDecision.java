package lavi.minecraft.diagnostics.interaction;

//20260805_kpopmodder: Represent bounded diagnostic emission decisions separately from logging.
public final class BlockInteractionEmissionDecision {
    private static final BlockInteractionEmissionDecision SUPPRESS = new BlockInteractionEmissionDecision(
            false,
            false,
            false,
            0
    );

    private final boolean emitEvent;
    private final boolean emitSummary;
    private final boolean emitCap;
    private final int suppressedRepeatCount;

    private BlockInteractionEmissionDecision(boolean emitEvent,
                                             boolean emitSummary,
                                             boolean emitCap,
                                             int suppressedRepeatCount) {
        this.emitEvent = emitEvent;
        this.emitSummary = emitSummary;
        this.emitCap = emitCap;
        this.suppressedRepeatCount = suppressedRepeatCount;
    }

    public static BlockInteractionEmissionDecision event(int suppressedRepeatCount) {
        return new BlockInteractionEmissionDecision(true, false, false, suppressedRepeatCount);
    }

    public static BlockInteractionEmissionDecision summary(int suppressedRepeatCount) {
        return new BlockInteractionEmissionDecision(false, true, false, suppressedRepeatCount);
    }

    public static BlockInteractionEmissionDecision cap() {
        return new BlockInteractionEmissionDecision(false, false, true, 0);
    }

    public static BlockInteractionEmissionDecision suppress() {
        return SUPPRESS;
    }

    public boolean emitEvent() {
        return emitEvent;
    }

    public boolean emitSummary() {
        return emitSummary;
    }

    public boolean emitCap() {
        return emitCap;
    }

    public int suppressedRepeatCount() {
        return suppressedRepeatCount;
    }
}
