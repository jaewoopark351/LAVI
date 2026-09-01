package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260901_kpopmodder: Lock event-family precedence so terminal summaries retain terminal reserve.
class DiagnosticEventFamilyClassifierTest {
    @Test
    void terminalMeaningOutranksGenericSummaryMeaning() {
        assertEquals(
                DiagnosticEventFamily.NON_STORE_TERMINAL,
                DiagnosticEventFamilyClassifier.classify(
                        "STORE_HOME_OPERATION_TERMINAL_SUMMARY"
                )
        );
    }

    @Test
    void genericSummaryCheckpointAndSnapshotRemainAggregateEvents() {
        assertEquals(
                DiagnosticEventFamily.AGGREGATE_CHECKPOINT,
                DiagnosticEventFamilyClassifier.classify(
                        "STORE_HOME_CANDIDATE_PROGRESS_SUMMARY"
                )
        );
        assertEquals(
                DiagnosticEventFamily.AGGREGATE_CHECKPOINT,
                DiagnosticEventFamilyClassifier.classify(
                        "DIAGNOSTIC_SESSION_CHECKPOINT"
                )
        );
        assertEquals(
                DiagnosticEventFamily.AGGREGATE_CHECKPOINT,
                DiagnosticEventFamilyClassifier.classify(
                        "DIAGNOSTIC_SESSION_SNAPSHOT"
                )
        );
    }

    @Test
    void failureAndSuppressionMeaningStillOutrankTerminalMeaning() {
        assertEquals(
                DiagnosticEventFamily.EXCEPTION_COVERAGE,
                DiagnosticEventFamilyClassifier.classify(
                        "STORE_HOME_TERMINAL_OBSERVATION_FAILED_SUMMARY"
                )
        );
        assertEquals(
                DiagnosticEventFamily.SUPPRESSION_CONTROL,
                DiagnosticEventFamilyClassifier.classify(
                        "STORE_HOME_TERMINAL_BUDGET_EXHAUSTED_SUMMARY"
                )
        );
    }
}
