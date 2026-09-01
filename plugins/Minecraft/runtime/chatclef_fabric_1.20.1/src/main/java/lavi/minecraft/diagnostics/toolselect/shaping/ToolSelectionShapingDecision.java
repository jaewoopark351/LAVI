package lavi.minecraft.diagnostics.toolselect.shaping;

//20260831_kpopmodder: Carry one side-effect-free tool-selection emission decision.
public final class ToolSelectionShapingDecision {
    private static final ToolSelectionShapingDecision MODE_OFF =
            new ToolSelectionShapingDecision(true, false, false, "", "", 0L, -1L, -1L);
    private static final ToolSelectionShapingDecision SUPPRESSED =
            new ToolSelectionShapingDecision(false, false, false, "", "", 0L, -1L, -1L);

    private final boolean modeOff;
    private final boolean emitEvent;
    private final boolean emitSummary;
    private final String eventFingerprint;
    private final String summaryFingerprint;
    private final long suppressedRepeatCount;
    private final long firstObservedTick;
    private final long lastObservedTick;

    private ToolSelectionShapingDecision(
            boolean modeOff,
            boolean emitEvent,
            boolean emitSummary,
            String eventFingerprint,
            String summaryFingerprint,
            long suppressedRepeatCount,
            long firstObservedTick,
            long lastObservedTick) {
        this.modeOff = modeOff;
        this.emitEvent = emitEvent;
        this.emitSummary = emitSummary;
        this.eventFingerprint = eventFingerprint;
        this.summaryFingerprint = summaryFingerprint;
        this.suppressedRepeatCount = suppressedRepeatCount;
        this.firstObservedTick = firstObservedTick;
        this.lastObservedTick = lastObservedTick;
    }

    static ToolSelectionShapingDecision modeOffDecision() {
        return MODE_OFF;
    }

    static ToolSelectionShapingDecision suppressed() {
        return SUPPRESSED;
    }

    static ToolSelectionShapingDecision event(String fingerprint) {
        return new ToolSelectionShapingDecision(
                false,
                true,
                false,
                fingerprint,
                "",
                0L,
                -1L,
                -1L
        );
    }

    static ToolSelectionShapingDecision summary(
            String fingerprint,
            long suppressedRepeatCount,
            long firstObservedTick,
            long lastObservedTick) {
        return new ToolSelectionShapingDecision(
                false,
                false,
                true,
                "",
                fingerprint,
                suppressedRepeatCount,
                firstObservedTick,
                lastObservedTick
        );
    }

    static ToolSelectionShapingDecision eventWithSummary(
            String eventFingerprint,
            String summaryFingerprint,
            long suppressedRepeatCount,
            long firstObservedTick,
            long lastObservedTick) {
        return new ToolSelectionShapingDecision(
                false,
                true,
                true,
                eventFingerprint,
                summaryFingerprint,
                suppressedRepeatCount,
                firstObservedTick,
                lastObservedTick
        );
    }

    static ToolSelectionShapingDecision eventWithPendingSuppression(
            String eventFingerprint,
            String suppressedFingerprint,
            long suppressedRepeatCount,
            long firstObservedTick,
            long lastObservedTick) {
        return new ToolSelectionShapingDecision(
                false,
                true,
                false,
                eventFingerprint,
                suppressedFingerprint,
                suppressedRepeatCount,
                firstObservedTick,
                lastObservedTick
        );
    }

    public boolean modeOff() {
        return modeOff;
    }

    public boolean emitEvent() {
        return emitEvent;
    }

    public boolean emitSummary() {
        return emitSummary;
    }

    public String eventFingerprint() {
        return eventFingerprint;
    }

    public String summaryFingerprint() {
        return summaryFingerprint;
    }

    public long suppressedRepeatCount() {
        return suppressedRepeatCount;
    }

    public long firstObservedTick() {
        return firstObservedTick;
    }

    public long lastObservedTick() {
        return lastObservedTick;
    }
}
