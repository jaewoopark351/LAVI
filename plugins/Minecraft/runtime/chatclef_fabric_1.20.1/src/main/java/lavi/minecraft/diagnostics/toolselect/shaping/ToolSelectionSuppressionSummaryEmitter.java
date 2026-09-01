package lavi.minecraft.diagnostics.toolselect.shaping;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260831_kpopmodder: Emit bounded tool-selection suppression summaries with one stable schema.
public final class ToolSelectionSuppressionSummaryEmitter {
    private ToolSelectionSuppressionSummaryEmitter() {
    }

    public static void emit(
            String eventName,
            String boundary,
            String channel,
            ToolSelectionShapingDecision decision) {
        if (decision == null || !decision.emitSummary()) {
            return;
        }
        ChatClefDiagnostics.logBoundary(
                eventName,
                boundary,
                null,
                "channel", channel,
                "suppressedRepeatCount", decision.suppressedRepeatCount(),
                "firstObservedTick", decision.firstObservedTick(),
                "lastObservedTick", decision.lastObservedTick(),
                "semanticFingerprint", decision.summaryFingerprint(),
                "behavior_effect", "none"
        );
    }
}
