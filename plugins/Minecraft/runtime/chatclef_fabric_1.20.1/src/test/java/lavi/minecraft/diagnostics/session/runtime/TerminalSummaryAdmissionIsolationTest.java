package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.mode.DiagnosticOutputMode;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticFamilySnapshot;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionLimits;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Prove aggregate exhaustion cannot consume StoreHome terminal evidence.
class TerminalSummaryAdmissionIsolationTest {
    @Test
    void exhaustedAggregateQuotaDoesNotSuppressStoreHomeTerminalSummary() {
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                new DiagnosticModeController(DiagnosticOutputMode.BOUNDARY),
                new DiagnosticSessionAdmissionAuthority("terminal-summary-isolation")
        );
        AtomicInteger aggregateEmissions = new AtomicInteger();

        for (int index = 0;
             index < DiagnosticSessionLimits.AGGREGATE_CHECKPOINT_SLOTS;
             index++) {
            String eventName = "INVENTORY_PROGRESS_SUMMARY_" + index;
            DiagnosticDispatchResult result = runtime.dispatch(
                    DiagnosticEventFamilyClassifier.classify(eventName),
                    eventName,
                    aggregateEmissions::incrementAndGet,
                    ignored -> {
                    }
            );

            assertTrue(result.admitted());
            assertTrue(result.emissionCompleted());
        }

        DiagnosticDispatchResult aggregateOverflow = runtime.dispatch(
                DiagnosticEventFamilyClassifier.classify(
                        "TOOL_SELECTION_SNAPSHOT_OVERFLOW"
                ),
                "TOOL_SELECTION_SNAPSHOT_OVERFLOW",
                aggregateEmissions::incrementAndGet,
                ignored -> {
                }
        );
        AtomicInteger terminalEmissions = new AtomicInteger();
        DiagnosticDispatchResult terminal = runtime.dispatch(
                DiagnosticEventFamilyClassifier.classify(
                        "STORE_HOME_OPERATION_TERMINAL_SUMMARY"
                ),
                "STORE_HOME_OPERATION_TERMINAL_SUMMARY",
                terminalEmissions::incrementAndGet,
                ignored -> {
                }
        );

        assertFalse(aggregateOverflow.admitted());
        assertEquals(
                DiagnosticAdmissionDecision.RejectionReason.FAMILY_QUOTA_EXHAUSTED,
                aggregateOverflow.admission().rejectionReason()
        );
        assertEquals(DiagnosticSessionLimits.AGGREGATE_CHECKPOINT_SLOTS,
                aggregateEmissions.get());
        assertTrue(terminal.admitted());
        assertTrue(terminal.emissionCompleted());
        assertEquals(1, terminalEmissions.get());

        DiagnosticSessionSnapshot snapshot = runtime.snapshot();
        DiagnosticFamilySnapshot aggregate = snapshot.family(
                DiagnosticEventFamily.AGGREGATE_CHECKPOINT
        );
        DiagnosticFamilySnapshot terminalFamily = snapshot.family(
                DiagnosticEventFamily.NON_STORE_TERMINAL
        );
        assertEquals(DiagnosticSessionLimits.AGGREGATE_CHECKPOINT_SLOTS,
                aggregate.admittedSlots());
        assertEquals(1, aggregate.suppressedRequests());
        assertEquals(1, terminalFamily.admittedSlots());
        assertEquals(1, terminalFamily.emissionCompleted());
        assertEquals(0, terminalFamily.emissionPending());
        assertFalse(snapshot.capEventClaimed());
    }
}
