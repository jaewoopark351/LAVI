package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;

import java.util.Locale;

//20260831_kpopmodder: Classify fixed investigation event purposes without retaining producer keys.
public final class DiagnosticEventFamilyClassifier {
    private DiagnosticEventFamilyClassifier() {
    }

    public static DiagnosticEventFamily classify(String eventName) {
        String normalized = eventName == null ? "" : eventName.toUpperCase(Locale.ROOT);
        // Exact wrapper names only: producer detail/reason cannot consume a first-event reserve.
        switch (normalized) {
            //#if MC == 12001
            //20260913_kpopmodder: Only exact producer event names may spend the new first/terminal reserves.
            case "MINING_OPERATION_TOOL_EXACT_RESULT": return DiagnosticEventFamily.TOOL_EQUIP_FIRST;
            case "MINING_OPERATION_TOOL_PLACEMENT_FAILURE": return DiagnosticEventFamily.TOOL_PLACEMENT_TERMINAL;
            case "BLOCK_PROTECTION_PUBLICATION_BOUNDARY": return DiagnosticEventFamily.BLOCK_PROTECTION_BOUNDARY;
            //#endif
            case "RESOURCE_OBSERVATION_MINING_FIRST": return DiagnosticEventFamily.RESOURCE_MINING_FIRST;
            case "RESOURCE_OBSERVATION_DEPOSIT_FIRST": return DiagnosticEventFamily.RESOURCE_DEPOSIT_FIRST;
            case "RESOURCE_OBSERVATION_BUILDER_FIRST": return DiagnosticEventFamily.RESOURCE_BUILDER_FIRST;
            case "RESOURCE_OBSERVATION_TERMINAL": return DiagnosticEventFamily.RESOURCE_OBSERVATION_TERMINAL;
            case "RESOURCE_OBSERVATION_MINING_SUMMARY": return DiagnosticEventFamily.RESOURCE_MINING_SUMMARY;
            case "RESOURCE_OBSERVATION_DEPOSIT_SUMMARY": return DiagnosticEventFamily.RESOURCE_DEPOSIT_SUMMARY;
            case "RESOURCE_OBSERVATION_BUILDER_SUMMARY": return DiagnosticEventFamily.RESOURCE_BUILDER_SUMMARY;
            case "BLOCK_COLLECTION_FIRST": return DiagnosticEventFamily.BLOCK_COLLECTION_FIRST;
            case "BLOCK_COLLECTION_SUMMARY": return DiagnosticEventFamily.BLOCK_COLLECTION_SUMMARY;
            case "RESOURCE_OBSERVATION_DETAIL": return DiagnosticEventFamily.ORDINARY_DETAIL;
            default: break;
        }
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
