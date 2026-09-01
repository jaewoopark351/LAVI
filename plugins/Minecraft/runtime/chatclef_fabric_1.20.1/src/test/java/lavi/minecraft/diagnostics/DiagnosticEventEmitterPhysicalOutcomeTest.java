package lavi.minecraft.diagnostics;

import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.mode.DiagnosticOutputMode;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticSessionRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Prove source projections can depend on completed shared diagnostic emission.
class DiagnosticEventEmitterPhysicalOutcomeTest {
    @Test
    void boundaryEmissionReturnsTheCompletedSharedDispatchOutcome() {
        DiagnosticEventEmitter emitter = emitter(DiagnosticOutputMode.BOUNDARY, "source-admitted");

        DiagnosticDispatchResult result = emitter.emitEventWithOutcome(
                "BOUNDARY",
                "[LAVI ChatClefBoundary]",
                "SOURCE_EVENT",
                "source_event",
                null,
                new Object[]{"behavior_effect", "none"},
                false
        );

        assertTrue(result.admitted());
        assertTrue(result.emissionCompleted());
    }

    @Test
    void offEmissionCannotAuthorizeASourceProjection() {
        DiagnosticEventEmitter emitter = emitter(DiagnosticOutputMode.OFF, "source-rejected");

        DiagnosticDispatchResult result = emitter.emitEventWithOutcome(
                "BOUNDARY",
                "[LAVI ChatClefBoundary]",
                "SOURCE_EVENT",
                "source_event",
                null,
                new Object[0],
                false
        );

        assertFalse(result.admitted());
        assertFalse(result.emissionCompleted());
    }

    @Test
    void boundedBoundaryReturnsTheCompletedSharedDispatchOutcome() {
        DiagnosticEventEmitter emitter = emitter(DiagnosticOutputMode.BOUNDARY, "bounded-admitted");

        DiagnosticDispatchResult result = emitter.emitBoundedBoundaryEventWithOutcome(
                "[LAVI ChatClefBoundary]",
                "CRAFT_RESOURCE_EXPECTED_OBSERVED_MISMATCH",
                "craft_resource_expected_observed_mismatch",
                null,
                8_192,
                new Object[]{"behavior_effect", "none"},
                new Object[0]
        );

        assertTrue(result.admitted());
        assertTrue(result.emissionCompleted());
    }

    private static DiagnosticEventEmitter emitter(DiagnosticOutputMode mode, String sessionId) {
        DiagnosticTraceState traceState = new DiagnosticTraceState();
        return new DiagnosticEventEmitter(
                traceState,
                new DiagnosticTaskRegistry(),
                new DiagnosticSessionRuntime(
                        new DiagnosticModeController(mode),
                        new DiagnosticSessionAdmissionAuthority(sessionId)
                )
        );
    }
}
