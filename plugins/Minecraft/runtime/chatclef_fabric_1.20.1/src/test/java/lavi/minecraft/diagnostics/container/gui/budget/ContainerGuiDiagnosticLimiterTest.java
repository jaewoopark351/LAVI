package lavi.minecraft.diagnostics.container.gui.budget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Lock bounded detail, dedupe, summary, and activation accounting.
class ContainerGuiDiagnosticLimiterTest {
    @Test
    void providedFlowActivationIdRemainsStableAcrossAdmissions() {
        String flowActivationId = "screen-boundary-activation-41";
        ContainerGuiDiagnosticLimiter limiter =
                new ContainerGuiDiagnosticLimiter(flowActivationId);

        assertEquals(flowActivationId, limiter.activeActivationIdOrUnavailable());
        assertEquals(
                flowActivationId,
                limiter.evaluate("first-flow-boundary", 10L).diagnosticBoundaryActivationId()
        );
        assertEquals(
                flowActivationId,
                limiter.evaluate("second-flow-boundary", 11L).diagnosticBoundaryActivationId()
        );
        assertEquals(flowActivationId, limiter.activationId());
        assertEquals(flowActivationId, limiter.snapshot().diagnosticBoundaryActivationId());
    }

    @Test
    void readOnlyActivationQueriesDoNotInventAnActivation() {
        ContainerGuiDiagnosticLimiter limiter = new ContainerGuiDiagnosticLimiter();

        assertFalse(limiter.hasActivation());
        assertNull(limiter.snapshotIfActive());
        assertEquals("unavailable", limiter.activeActivationIdOrUnavailable());
        assertFalse(limiter.hasActivation());
        assertNull(limiter.snapshotIfActive());

        limiter.evaluate("first-observation", 1L);

        assertTrue(limiter.hasActivation());
        assertEquals(limiter.activationId(), limiter.snapshotIfActive().diagnosticBoundaryActivationId());
    }

    @Test
    void summarizesAnUnchangedFingerprintNoEarlierThanTwoHundredTicks() {
        ContainerGuiDiagnosticLimiter limiter = new ContainerGuiDiagnosticLimiter();

        assertOutcome(limiter.evaluate("same", 10L), ContainerGuiEmissionDecision.Outcome.DETAIL, 0);
        assertOutcome(
                limiter.evaluate("same", 11L),
                ContainerGuiEmissionDecision.Outcome.SUPPRESSED_DUPLICATE,
                1
        );
        assertOutcome(
                limiter.evaluate("same", 209L),
                ContainerGuiEmissionDecision.Outcome.SUPPRESSED_DUPLICATE,
                2
        );
        assertOutcome(
                limiter.evaluate("same", 210L),
                ContainerGuiEmissionDecision.Outcome.REPEAT_SUMMARY,
                3
        );
        assertOutcome(
                limiter.evaluate("same", 211L),
                ContainerGuiEmissionDecision.Outcome.SUPPRESSED_DUPLICATE,
                1
        );

        ContainerGuiDiagnosticAggregateSnapshot aggregate = limiter.snapshot();
        assertEquals(5L, aggregate.diagnosticDetailObservationCount());
        assertEquals(4L, aggregate.diagnosticDetailDedupeSuppressedCount());
        assertEquals(1L, aggregate.diagnosticDetailLocalAdmissionCount());
        assertEquals(0L, aggregate.diagnosticDetailLocalCapSuppressedCount());
    }

    @Test
    void boundsSemanticBucketsBeforeTheLargerDetailCapCanBeReached() {
        ContainerGuiDiagnosticLimiter limiter = new ContainerGuiDiagnosticLimiter();

        for (int index = 0; index < ContainerGuiDiagnosticLimits.MAX_SEMANTIC_BUCKETS - 1; index++) {
            assertEquals(
                    ContainerGuiEmissionDecision.Outcome.DETAIL,
                    limiter.evaluate("fingerprint-" + index, index).outcome()
            );
        }
        assertEquals(
                ContainerGuiEmissionDecision.Outcome.DETAIL,
                limiter.evaluate("first-overflow-fingerprint", 32L).outcome()
        );
        assertEquals(
                ContainerGuiEmissionDecision.Outcome.SUPPRESSED_DUPLICATE,
                limiter.evaluate("fingerprint-overflow", 33L).outcome()
        );

        ContainerGuiDiagnosticAggregateSnapshot aggregate = limiter.snapshot();
        assertEquals(33L, aggregate.diagnosticDetailObservationCount());
        assertEquals(32L, aggregate.diagnosticDetailLocalAdmissionCount());
        assertEquals(1L, aggregate.diagnosticDetailDedupeSuppressedCount());
        assertEquals(0L, aggregate.diagnosticDetailLocalCapSuppressedCount());
        assertEquals(2L, aggregate.omittedCount());
        assertTrue(aggregate.diagnosticDetailLocalAdmissionCount()
                <= ContainerGuiDiagnosticLimits.DETAIL_LIMIT_PER_ACTIVATION);
    }

    @Test
    void recordsSharedOutcomesAndStartsANewActivationAfterModeCleanup() {
        ContainerGuiDiagnosticLimiter limiter = new ContainerGuiDiagnosticLimiter();
        String firstActivation = limiter.evaluate("first", 1L).diagnosticBoundaryActivationId();
        limiter.recordSharedOutcome(false, false);
        limiter.recordSharedOutcome(true, true);

        ContainerGuiDiagnosticAggregateSnapshot beforeClear = limiter.snapshot();
        assertEquals(1L, beforeClear.diagnosticDetailSharedAdmissionRejectedCount());
        assertEquals(1L, beforeClear.diagnosticDetailPhysicalEmissionCount());

        limiter.clearForModeTransition();
        ContainerGuiDiagnosticAggregateSnapshot afterClear = limiter.snapshot();
        assertNotEquals(firstActivation, afterClear.diagnosticBoundaryActivationId());
        assertEquals(0L, afterClear.diagnosticDetailObservationCount());
        assertEquals(0L, afterClear.diagnosticDetailDedupeSuppressedCount());
        assertEquals(0L, afterClear.diagnosticDetailLocalAdmissionCount());
        assertEquals(0L, afterClear.diagnosticDetailLocalCapSuppressedCount());
        assertEquals(0L, afterClear.diagnosticDetailSharedAdmissionRejectedCount());
        assertEquals(0L, afterClear.diagnosticDetailPhysicalEmissionCount());
        assertEquals(0L, afterClear.omittedCount());
    }

    private static void assertOutcome(
            ContainerGuiEmissionDecision decision,
            ContainerGuiEmissionDecision.Outcome outcome,
            int suppressedRepeatCount) {
        assertEquals(outcome, decision.outcome());
        assertEquals(suppressedRepeatCount, decision.suppressedRepeatCount());
    }
}
