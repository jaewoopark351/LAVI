package lavi.minecraft.diagnostics.mining.budget;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260830_kpopmodder: Own the canonical exactly-once mining session-cap event projection.
public final class MiningDiagnosticCapEventEmitter {
    private MiningDiagnosticCapEventEmitter() {
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
                "MINING_DIAGNOSTIC_FAMILY_CAP_REACHED",
                "mining_diagnostic_session_detail_cap_reached",
                null,
                merge(new Object[]{
                        "mode", "BOUNDARY",
                        "diagnosticSubsystem", "MINING",
                        "terminal", false,
                        "behavior_effect", "none",
                        "suppressedEventName", suppressedEventName,
                        "suppressedReason", suppressedReason,
                        "suppressedBucket", suppressedBucket,
                        "suppressedFingerprint", suppressedFingerprint,
                        "gateLimitReason", admission.reason(),
                        "diagnosticCorrelationKey", admission.correlationKey(),
                        "correlationDetailEmissions", admission.correlationDetailEmissions(),
                        "correlationDetailLimit", admission.correlationDetailLimit(),
                        "sessionEmissions", snapshot.sessionEmissions(),
                        "sessionDetailEmissions", snapshot.detailEmissions(),
                        "sessionCriticalEmissions", snapshot.criticalEmissions(),
                        "sessionCapSignalEmissions", snapshot.capSignalEmissions(),
                        "sessionDetailLimit", snapshot.sessionDetailLimit(),
                        "sessionHardCap", snapshot.sessionHardCap(),
                        "reservedCriticalEvents", snapshot.reservedCriticalEvents(),
                        "legacyCompatibilityEventName", "MINING_DIAGNOSTIC_GATE_EXHAUSTED",
                        "legacyCompatibilityEventEmitted", false,
                        "suppressedCount", suppressedCount,
                        "firstObservedTick", firstObservedTick,
                        "lastObservedTick", lastObservedTick
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
