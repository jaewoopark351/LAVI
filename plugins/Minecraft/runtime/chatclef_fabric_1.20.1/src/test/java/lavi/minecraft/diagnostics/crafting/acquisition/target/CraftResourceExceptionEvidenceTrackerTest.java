package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Define bounded existing-exception evidence without adding an engine catch.
class CraftResourceExceptionEvidenceTrackerTest {
    @Test
    void firstUniqueEngineExceptionCarriesOneBoundedStackExcerpt() {
        CraftResourceExceptionEvidenceTracker tracker = new CraftResourceExceptionEvidenceTracker();
        List<String> frames = IntStream.range(0, 12)
                .mapToObj(index -> "frame-" + index + "-" + "x".repeat(200))
                .toList();

        CraftResourceExceptionDecision decision = tracker.evaluate(
                CraftResourceExceptionObservation.engineException(
                        "correlation-1",
                        CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                        "BLOCK_OPTIONAL_META_GET_MANAGER",
                        "java.lang.NullPointerException",
                        "manager unavailable",
                        frames,
                        10L,
                        "place-task-a"
                )
        );

        assertEquals(CraftResourceExceptionDisposition.EMISSION_REQUESTED,
                decision.disposition());
        assertEquals("BLOCK_OPTIONAL_META_MANAGER_EXCEPTION", decision.eventName());
        assertFalse(decision.eventName().contains("OBSERVATION_FAILED"));
        assertTrue(decision.emissionRequested());
        assertTrue(decision.stackIncluded());
        assertEquals(12, decision.framesAvailable());
        assertEquals(8, decision.framesSelected());
        assertEquals(4, decision.framesOmitted());
        assertTrue(decision.selectedStackFrames().stream()
                .allMatch(frame -> utf8Length(frame) <= 256));
        assertTrue(decision.selectedStackFrames().stream()
                .mapToInt(CraftResourceExceptionEvidenceTrackerTest::utf8Length)
                .sum() <= 2_048);
    }

    @Test
    void repeatedExceptionUpdatesFirstLastAndCountWithoutAnotherStackOrEmission() {
        CraftResourceExceptionEvidenceTracker tracker = new CraftResourceExceptionEvidenceTracker();
        CraftResourceExceptionDecision first = tracker.evaluate(exception(
                "correlation-1", "BOUNDARY_A", 10L, "task-a"
        ));
        CraftResourceExceptionDecision repeat = tracker.evaluate(exception(
                "correlation-1", "BOUNDARY_A", 30L, "task-b"
        ));

        assertTrue(first.emissionRequested());
        assertTrue(first.stackIncluded());
        assertEquals(CraftResourceExceptionDisposition.DUPLICATE_SUPPRESSED,
                repeat.disposition());
        assertFalse(repeat.emissionRequested());
        assertFalse(repeat.stackIncluded());
        assertTrue(repeat.selectedStackFrames().isEmpty());
        assertEquals(first.fingerprint(), repeat.fingerprint());
        assertEquals(2L, tracker.snapshot().occurrenceCount(first.fingerprint()));
        assertEquals(10L, tracker.snapshot().firstObservedTick(first.fingerprint()));
        assertEquals(30L, tracker.snapshot().lastObservedTick(first.fingerprint()));
    }

    @Test
    void coverageGapContainsNoFabricatedThrowableFields() {
        CraftResourceExceptionEvidenceTracker tracker = new CraftResourceExceptionEvidenceTracker();
        CraftResourceExceptionDecision gap = tracker.evaluate(
                CraftResourceExceptionObservation.coverageGap(
                        "correlation-1",
                        CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                        "BLOCK_OPTIONAL_META_GET_MANAGER",
                        "NORMAL_RETURN_AND_THROWABLE_UNAVAILABLE",
                        10L,
                        "place-task-a"
                )
        );

        assertEquals(CraftResourceExceptionDisposition.COVERAGE_GAP_REQUESTED,
                gap.disposition());
        assertEquals("BLOCK_OPTIONAL_META_MANAGER_COVERAGE_GAP", gap.eventName());
        assertTrue(gap.emissionRequested());
        assertFalse(gap.exceptionType().isPresent());
        assertFalse(gap.exceptionMessage().isPresent());
        assertFalse(gap.stackIncluded());
        assertTrue(gap.selectedStackFrames().isEmpty());
        assertEquals(0L, tracker.snapshot().ownedExceptionOccurrenceCount());
        assertEquals(1L, tracker.snapshot().ownedCoverageGapCount());
    }

    @Test
    void fifthUniqueExceptionInOneCorrelationIsAccountingOnly() {
        CraftResourceExceptionEvidenceTracker tracker = new CraftResourceExceptionEvidenceTracker();
        CraftResourceExceptionDecision fifth = null;
        int requested = 0;
        for (int index = 1; index <= 5; index++) {
            CraftResourceExceptionDecision decision = tracker.evaluate(exception(
                    "correlation-1", "BOUNDARY_" + index, index, "task-" + index
            ));
            if (decision.emissionRequested()) {
                requested++;
            }
            fifth = decision;
        }

        assertEquals(4, requested);
        assertEquals(CraftResourceExceptionDisposition.PER_CORRELATION_LIMIT,
                fifth.disposition());
        assertFalse(fifth.emissionRequested());
        assertEquals(4, tracker.snapshot().retainedSignatureCount("correlation-1"));
        assertEquals(5L, tracker.snapshot().ownedExceptionOccurrenceCount());
    }

    @Test
    void seventeenthUniqueProducerExceptionIsAccountingOnly() {
        CraftResourceExceptionEvidenceTracker tracker = new CraftResourceExceptionEvidenceTracker();
        CraftResourceExceptionDecision seventeenth = null;
        int requested = 0;
        for (int index = 1; index <= 17; index++) {
            CraftResourceExceptionDecision decision = tracker.evaluate(exception(
                    "correlation-" + index,
                    "BOUNDARY_" + index,
                    index,
                    "task-" + index
            ));
            if (decision.emissionRequested()) {
                requested++;
            }
            seventeenth = decision;
        }

        assertEquals(16, requested);
        assertEquals(CraftResourceExceptionDisposition.SESSION_LIMIT,
                seventeenth.disposition());
        assertFalse(seventeenth.emissionRequested());
        assertEquals(16, tracker.snapshot().globalRetainedSignatureCount());
        assertEquals(17L, tracker.snapshot().ownedExceptionOccurrenceCount());
    }

    @Test
    void unownedAndUnknownExceptionsCannotBecomeCommandFailureEvidence() {
        CraftResourceExceptionEvidenceTracker tracker = new CraftResourceExceptionEvidenceTracker();
        CraftResourceExceptionDecision unowned = tracker.evaluate(exception(
                "correlation-1",
                CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED,
                "BOUNDARY_A",
                1L,
                "task-a"
        ));
        CraftResourceExceptionDecision unknown = tracker.evaluate(exception(
                "correlation-1",
                CraftResourceAssociationStatus.UNKNOWN,
                "BOUNDARY_A",
                2L,
                "task-a"
        ));

        assertEquals(CraftResourceExceptionDisposition.ASSOCIATION_NOT_OWNED,
                unowned.disposition());
        assertEquals(CraftResourceExceptionDisposition.ASSOCIATION_NOT_OWNED,
                unknown.disposition());
        assertFalse(unowned.emissionRequested());
        assertFalse(unknown.emissionRequested());
        assertEquals(0L, tracker.snapshot().ownedExceptionOccurrenceCount());
        assertEquals(1L, tracker.snapshot().unownedObservationCount());
        assertEquals(1L, tracker.snapshot().unknownAssociationCount());
    }

    private static CraftResourceExceptionObservation exception(
            String correlationId,
            String boundary,
            long tick,
            String taskInstanceId) {
        return exception(
                correlationId,
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                boundary,
                tick,
                taskInstanceId
        );
    }

    private static CraftResourceExceptionObservation exception(
            String correlationId,
            CraftResourceAssociationStatus association,
            String boundary,
            long tick,
            String taskInstanceId) {
        return CraftResourceExceptionObservation.engineException(
                correlationId,
                association,
                boundary,
                "java.lang.NullPointerException",
                "manager unavailable",
                List.of("baritone.api.utils.BlockOptionalMeta.getManager(BlockOptionalMeta.java:1)"),
                tick,
                taskInstanceId
        );
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
