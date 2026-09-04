package lavi.minecraft.diagnostics.container.gui.emission;

import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticAggregateSnapshot;

import java.util.SortedSet;
import java.util.TreeSet;

//20260904_kpopmodder: Derive canonical completeness and sorted gap evidence without inferring gameplay success.
public final class ContainerGuiEvidenceFields {
    private ContainerGuiEvidenceFields() {
    }

    public static Object[] from(
            ContainerGuiDiagnosticAggregateSnapshot aggregate,
            boolean uncorrelatedTransportEvidence,
            boolean modeTransition) {
        SortedSet<String> gaps = new TreeSet<>();
        if (modeTransition) {
            gaps.add("PARTIAL_MODE_TRANSITION");
        }
        if (uncorrelatedTransportEvidence) {
            gaps.add("PARTIAL_UNCORRELATED_HUB_EVENT");
        }
        if (aggregate != null
                && (aggregate.diagnosticDetailLocalCapSuppressedCount() > 0
                || aggregate.omittedCount() > 0)) {
            gaps.add("PARTIAL_LOCAL_DETAIL_CAP");
        }
        if (aggregate != null
                && aggregate.diagnosticDetailSharedAdmissionRejectedCount() > 0) {
            gaps.add("PARTIAL_SHARED_ADMISSION");
        }

        return new Object[]{
                "diagnosticEvidenceCompleteness", primary(gaps),
                "diagnosticEvidenceGapReasons", String.join(",", gaps)
        };
    }

    private static String primary(SortedSet<String> gaps) {
        if (gaps.isEmpty()) return "COMPLETE_FOR_BOUNDARY_ACTIVATION";
        if (gaps.contains("PARTIAL_LIFECYCLE_OBSERVER_CAPACITY")) {
            return "PARTIAL_LIFECYCLE_OBSERVER_CAPACITY";
        }
        if (gaps.contains("PARTIAL_MODE_TRANSITION")) {
            return "PARTIAL_MODE_TRANSITION";
        }
        if (gaps.contains("PARTIAL_UNCORRELATED_HUB_EVENT")) {
            return "PARTIAL_UNCORRELATED_HUB_EVENT";
        }
        if (gaps.contains("PARTIAL_LOCAL_DETAIL_CAP")) {
            return "PARTIAL_LOCAL_DETAIL_CAP";
        }
        return "PARTIAL_SHARED_ADMISSION";
    }
}
