package lavi.minecraft.diagnostics.toolselect.shaping;

//20260831_kpopmodder: Shape hot-path tool-selection diagnostics without influencing selection behavior.
public final class ToolSelectionDiagnosticShaper {
    public static final int MAX_CHANNEL_STATES = 16;
    public static final long SUMMARY_INTERVAL_TICKS = 200L;

    private final ToolSelectionChannelRegistry channels =
            new ToolSelectionChannelRegistry(MAX_CHANNEL_STATES);

    public synchronized ToolSelectionShapingDecision evaluate(
            boolean enabled,
            String channel,
            String semanticFingerprint,
            long observedTick) {
        if (!enabled) {
            return ToolSelectionShapingDecision.modeOffDecision();
        }

        String normalizedChannel = ToolSelectionSemanticFingerprint.normalizeChannel(channel);
        String normalizedFingerprint =
                ToolSelectionSemanticFingerprint.normalizeFingerprint(semanticFingerprint);
        long normalizedTick = Math.max(0L, observedTick);
        ToolSelectionChannelState state = channels.get(normalizedChannel);
        if (state == null) {
            channels.create(normalizedChannel, normalizedFingerprint, normalizedTick);
            return ToolSelectionShapingDecision.event(normalizedFingerprint);
        }

        long effectiveTick = state.monotonicTick(normalizedTick);
        if (!state.matches(normalizedFingerprint)) {
            ToolSelectionShapingDecision decision = transitionDecision(
                    state,
                    normalizedFingerprint,
                    effectiveTick
            );
            state.transitionTo(normalizedFingerprint, effectiveTick);
            return decision;
        }

        state.recordSuppression(effectiveTick);
        if (!state.summaryDue(effectiveTick, SUMMARY_INTERVAL_TICKS)) {
            return ToolSelectionShapingDecision.suppressed();
        }

        ToolSelectionShapingDecision decision = ToolSelectionShapingDecision.summary(
                state.semanticFingerprint(),
                state.suppressedRepeatCount(),
                state.firstSuppressedTick(),
                state.lastSuppressedTick()
        );
        state.settleSummary(effectiveTick);
        return decision;
    }

    public synchronized void clearForModeOff() {
        channels.clear();
    }

    private static ToolSelectionShapingDecision transitionDecision(
            ToolSelectionChannelState state,
            String nextFingerprint,
            long effectiveTick) {
        if (state.suppressedRepeatCount() == 0L) {
            return ToolSelectionShapingDecision.event(nextFingerprint);
        }
        if (state.summaryDue(effectiveTick, SUMMARY_INTERVAL_TICKS)) {
            return ToolSelectionShapingDecision.eventWithSummary(
                    nextFingerprint,
                    state.semanticFingerprint(),
                    state.suppressedRepeatCount(),
                    state.firstSuppressedTick(),
                    state.lastSuppressedTick()
            );
        }
        return ToolSelectionShapingDecision.eventWithPendingSuppression(
                nextFingerprint,
                state.semanticFingerprint(),
                state.suppressedRepeatCount(),
                state.firstSuppressedTick(),
                state.lastSuppressedTick()
        );
    }

    synchronized int channelStateCount() {
        return channels.size();
    }

    synchronized boolean hasChannel(String channel) {
        return channels.contains(ToolSelectionSemanticFingerprint.normalizeChannel(channel));
    }

    synchronized long totalSuppressedRepeatCount() {
        return channels.totalSuppressedRepeatCount();
    }
}
