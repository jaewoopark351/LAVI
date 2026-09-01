package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;

import java.util.Locale;

//20260831_kpopmodder: Classify fixed investigation event purposes without retaining producer keys.
public final class DiagnosticEventFamilyClassifier {
    private DiagnosticEventFamilyClassifier() {
    }

    public static DiagnosticEventFamily classify(String eventName) {
        String normalized = eventName == null ? "" : eventName.toUpperCase(Locale.ROOT);
        if (containsAny(normalized,
                "EXCEPTION",
                "COVERAGE_GAP",
                "MISMATCH",
                "OBSERVATION_FAILED",
                "LINKAGE_FAILURE")) {
            return DiagnosticEventFamily.EXCEPTION_COVERAGE;
        }
        if (containsAny(normalized,
                "SUPPRESSION",
                "RATE_LIMIT",
                "CAP_REACHED",
                "BUDGET_EXHAUSTED",
                "RESERVE_EXHAUSTED")) {
            return DiagnosticEventFamily.SUPPRESSION_CONTROL;
        }
        if (normalized.contains("TERMINAL")) {
            return DiagnosticEventFamily.NON_STORE_TERMINAL;
        }
        if (containsAny(normalized, "SUMMARY", "CHECKPOINT", "SNAPSHOT")) {
            return DiagnosticEventFamily.AGGREGATE_CHECKPOINT;
        }
        return DiagnosticEventFamily.ORDINARY_DETAIL;
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
