package lavi.minecraft.diagnostics.toolselect.shaping;

//20260831_kpopmodder: Own one tool-selection channel's semantic and suppression window state.
final class ToolSelectionChannelState {
    private String semanticFingerprint;
    private long lastObservedTick;
    private long summaryAnchorTick;
    private long suppressedRepeatCount;
    private long firstSuppressedTick = -1L;
    private long lastSuppressedTick = -1L;

    ToolSelectionChannelState(String semanticFingerprint, long tick) {
        this.semanticFingerprint = semanticFingerprint;
        lastObservedTick = tick;
        summaryAnchorTick = tick;
    }

    boolean matches(String fingerprint) {
        return semanticFingerprint.equals(fingerprint);
    }

    String semanticFingerprint() {
        return semanticFingerprint;
    }

    long monotonicTick(long observedTick) {
        return Math.max(lastObservedTick, observedTick);
    }

    void recordSuppression(long tick) {
        long effectiveTick = monotonicTick(tick);
        lastObservedTick = effectiveTick;
        if (suppressedRepeatCount == 0L) {
            firstSuppressedTick = effectiveTick;
        }
        lastSuppressedTick = effectiveTick;
        suppressedRepeatCount = ToolSelectionSuppressionCounter.increment(
                suppressedRepeatCount
        );
    }

    boolean summaryDue(long tick, long intervalTicks) {
        long effectiveTick = monotonicTick(tick);
        return effectiveTick >= summaryAnchorTick
                && effectiveTick - summaryAnchorTick >= intervalTicks;
    }

    void transitionTo(String fingerprint, long tick) {
        semanticFingerprint = fingerprint;
        lastObservedTick = monotonicTick(tick);
        summaryAnchorTick = lastObservedTick;
        clearSuppression();
    }

    void settleSummary(long tick) {
        lastObservedTick = monotonicTick(tick);
        summaryAnchorTick = lastObservedTick;
        clearSuppression();
    }

    long suppressedRepeatCount() {
        return suppressedRepeatCount;
    }

    long firstSuppressedTick() {
        return firstSuppressedTick;
    }

    long lastSuppressedTick() {
        return lastSuppressedTick;
    }

    private void clearSuppression() {
        suppressedRepeatCount = 0L;
        firstSuppressedTick = -1L;
        lastSuppressedTick = -1L;
    }
}
