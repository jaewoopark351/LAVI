package lavi.minecraft.diagnostics.inventory;

//20260805_kpopmodder: Represent bounded inventory scan diagnostic emission decisions.
public final class InventoryScanEmissionDecision {
    private static final InventoryScanEmissionDecision SUPPRESS = new InventoryScanEmissionDecision(
            false,
            false,
            false,
            0
    );

    private final boolean emitEvent;
    private final boolean emitSummary;
    private final boolean emitCap;
    private final int suppressedRepeatCount;

    private InventoryScanEmissionDecision(boolean emitEvent,
                                          boolean emitSummary,
                                          boolean emitCap,
                                          int suppressedRepeatCount) {
        this.emitEvent = emitEvent;
        this.emitSummary = emitSummary;
        this.emitCap = emitCap;
        this.suppressedRepeatCount = suppressedRepeatCount;
    }

    static InventoryScanEmissionDecision event(int suppressedRepeatCount) {
        return new InventoryScanEmissionDecision(true, false, false, suppressedRepeatCount);
    }

    static InventoryScanEmissionDecision summary(int suppressedRepeatCount) {
        return new InventoryScanEmissionDecision(false, true, false, suppressedRepeatCount);
    }

    static InventoryScanEmissionDecision cap() {
        return new InventoryScanEmissionDecision(false, false, true, 0);
    }

    static InventoryScanEmissionDecision suppress() {
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
