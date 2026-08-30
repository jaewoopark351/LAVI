package lavi.minecraft.diagnostics.mining.budget;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Emit one bounded correlation-cap coverage summary from administrative detail capacity.
public final class MiningDiagnosticSuppressionEventEmitter {
    private MiningDiagnosticSuppressionEventEmitter() {
    }

    public static void emit(String suppressedEventName,
                            String suppressedReason,
                            String suppressedBucket,
                            String suppressedFingerprint,
                            int suppressedCount,
                            long firstObservedTick,
                            long lastObservedTick,
                            MiningDiagnosticAdmission admission,
                            Object[] commandContextFields) {
        MiningDiagnosticBudgetSnapshot snapshot = admission.snapshot();
        ChatClefDiagnostics.logBoundary(
                "MINING_DIAGNOSTIC_SUPPRESSION_SUMMARY",
                "mining_correlation_detail_cap_reached",
                null,
                merge(new Object[]{
                        "mode", "BOUNDARY",
                        "terminal", false,
                        "behavior_effect", "none",
                        "suppressedEventName", suppressedEventName,
                        "suppressedReason", suppressedReason,
                        "suppressedBucket", suppressedBucket,
                        "suppressedFingerprint", suppressedFingerprint,
                        "gateLimitReason", "CORRELATION_DETAIL_CAP",
                        "diagnosticCorrelationKey", admission.correlationKey(),
                        "correlationDetailEmissions", admission.correlationDetailEmissions(),
                        "correlationDetailLimit", admission.correlationDetailLimit(),
                        "sessionEmissions", snapshot.sessionEmissions(),
                        "sessionHardCap", snapshot.sessionHardCap(),
                        "suppressedCount", suppressedCount,
                        "firstObservedTick", firstObservedTick,
                        "lastObservedTick", lastObservedTick,
                        "coverageGap", "DETAIL_SUPPRESSED_AFTER_CORRELATION_CAP"
                }, commandContextFields)
        );
    }

    private static Object[] merge(Object[] left, Object[] right) {
        Object[] safeRight = right == null ? new Object[0] : right;
        Object[] merged = new Object[left.length + safeRight.length];
        System.arraycopy(left, 0, merged, 0, left.length);
        System.arraycopy(safeRight, 0, merged, left.length, safeRight.length);
        return merged;
    }
}
