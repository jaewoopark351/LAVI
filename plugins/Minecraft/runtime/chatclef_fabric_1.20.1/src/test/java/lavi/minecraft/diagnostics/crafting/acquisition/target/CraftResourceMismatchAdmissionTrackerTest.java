package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Define bounded mismatch admission without world rescans or quota borrowing.
class CraftResourceMismatchAdmissionTrackerTest {
    @Test
    void matchingOrIncompleteEvidenceDoesNotCreateAMismatch() {
        CraftResourceMismatchAdmissionTracker tracker = new CraftResourceMismatchAdmissionTracker();

        CraftResourceMismatchDecision matching = tracker.evaluate(observation(
                "correlation-1", "0,64,0",
                Optional.of(List.of("minecraft:iron_ore")),
                Optional.of("minecraft:iron_ore"), 1L, "task-a"
        ));
        CraftResourceMismatchDecision missingExpected = tracker.evaluate(observation(
                "correlation-1", "0,64,0",
                Optional.empty(), Optional.of("minecraft:chest"), 2L, "task-a"
        ));
        CraftResourceMismatchDecision missingObserved = tracker.evaluate(observation(
                "correlation-1", "0,64,0",
                Optional.of(List.of("minecraft:iron_ore")), Optional.empty(), 3L, "task-a"
        ));

        assertEquals(CraftResourceMismatchDisposition.MATCH, matching.disposition());
        assertEquals(CraftResourceMismatchDisposition.COVERAGE_GAP,
                missingExpected.disposition());
        assertEquals(CraftResourceMismatchDisposition.COVERAGE_GAP,
                missingObserved.disposition());
        assertFalse(matching.emissionRequested());
        assertFalse(missingExpected.emissionRequested());
        assertFalse(missingObserved.emissionRequested());
        assertEquals(0L, tracker.snapshot().ownedMismatchOccurrenceCount());
        assertEquals(2L, tracker.snapshot().coverageGapCount());
    }

    @Test
    void duplicateSignatureIgnoresTickAndTaskIdentityNoise() {
        CraftResourceMismatchAdmissionTracker tracker = new CraftResourceMismatchAdmissionTracker();
        CraftResourceMismatchDecision first = tracker.evaluate(mismatch(
                "correlation-1", "0,64,0", "minecraft:chest", 10L, "task-a"
        ));
        CraftResourceMismatchDecision duplicate = tracker.evaluate(mismatch(
                "correlation-1", "0,64,0", "minecraft:chest", 999L, "task-b"
        ));

        assertEquals(CraftResourceMismatchDisposition.EMISSION_REQUESTED,
                first.disposition());
        assertTrue(first.emissionRequested());
        assertEquals(CraftResourceMismatchDisposition.DUPLICATE_SUPPRESSED,
                duplicate.disposition());
        assertFalse(duplicate.emissionRequested());
        assertEquals(first.fingerprint(), duplicate.fingerprint());
        assertEquals(2L, tracker.snapshot().ownedMismatchOccurrenceCount());
        assertEquals(1L, tracker.snapshot().duplicateSuppressedCount());
    }

    @Test
    void onlyTwoUniqueSignaturesPerCorrelationMayRequestEmission() {
        CraftResourceMismatchAdmissionTracker tracker = new CraftResourceMismatchAdmissionTracker();
        CraftResourceMismatchDecision first = tracker.evaluate(mismatch(
                "correlation-1", "0,64,0", "minecraft:chest", 1L, "task-a"
        ));
        CraftResourceMismatchDecision second = tracker.evaluate(mismatch(
                "correlation-1", "1,64,0", "minecraft:chest", 2L, "task-a"
        ));
        CraftResourceMismatchDecision third = tracker.evaluate(mismatch(
                "correlation-1", "2,64,0", "minecraft:chest", 3L, "task-a"
        ));

        assertTrue(first.emissionRequested());
        assertTrue(second.emissionRequested());
        assertFalse(third.emissionRequested());
        assertEquals(CraftResourceMismatchDisposition.PER_CORRELATION_LIMIT,
                third.disposition());
        assertEquals(2, tracker.snapshot().retainedSignatureCount("correlation-1"));
        assertEquals(1L, tracker.snapshot().perCorrelationLimitSuppressedCount());
    }

    @Test
    void ninthUniqueProducerSignatureUpdatesAccountingWithoutRequestingEmission() {
        CraftResourceMismatchAdmissionTracker tracker = new CraftResourceMismatchAdmissionTracker();
        int requested = 0;
        CraftResourceMismatchDecision ninth = null;
        for (int index = 1; index <= 9; index++) {
            CraftResourceMismatchDecision decision = tracker.evaluate(mismatch(
                    "correlation-" + index,
                    index + ",64,0",
                    "minecraft:chest",
                    index,
                    "task-" + index
            ));
            if (decision.emissionRequested()) {
                requested++;
            }
            ninth = decision;
        }

        assertEquals(8, requested);
        assertEquals(CraftResourceMismatchDisposition.SESSION_LIMIT, ninth.disposition());
        assertFalse(ninth.emissionRequested());
        assertEquals(8, tracker.snapshot().globalRetainedSignatureCount());
        assertEquals(1L, tracker.snapshot().sessionLimitSuppressedCount());
        assertEquals(9L, tracker.snapshot().ownedMismatchOccurrenceCount());
    }

    @Test
    void sharedAdmissionDenialIsRecordedOnceAndNeverRetried() {
        CraftResourceMismatchAdmissionTracker tracker = new CraftResourceMismatchAdmissionTracker();
        CraftResourceMismatchObservation observation = mismatch(
                "correlation-1", "0,64,0", "minecraft:chest", 1L, "task-a"
        );
        CraftResourceMismatchDecision first = tracker.evaluate(observation);

        assertTrue(first.emissionRequested());
        assertTrue(tracker.recordAdmissionDenied(first.fingerprint()));
        assertFalse(tracker.recordAdmissionDenied(first.fingerprint()));
        CraftResourceMismatchDecision repeat = tracker.evaluate(observation);

        assertFalse(repeat.emissionRequested());
        assertEquals(CraftResourceMismatchDisposition.DUPLICATE_SUPPRESSED,
                repeat.disposition());
        assertEquals(1L, tracker.snapshot().mismatchAdmissionDeniedCount());
    }

    @Test
    void unownedAndUnknownDifferencesNeverEnterCommandOwnedMismatchAccounting() {
        CraftResourceMismatchAdmissionTracker tracker = new CraftResourceMismatchAdmissionTracker();
        CraftResourceMismatchDecision unowned = tracker.evaluate(mismatch(
                "correlation-1", CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED,
                "0,64,0", "minecraft:chest", 1L, "task-a"
        ));
        CraftResourceMismatchDecision unknown = tracker.evaluate(mismatch(
                "correlation-1", CraftResourceAssociationStatus.UNKNOWN,
                "0,64,0", "minecraft:chest", 2L, "task-a"
        ));

        assertEquals(CraftResourceMismatchDisposition.ASSOCIATION_NOT_OWNED,
                unowned.disposition());
        assertEquals(CraftResourceMismatchDisposition.ASSOCIATION_NOT_OWNED,
                unknown.disposition());
        assertFalse(unowned.emissionRequested());
        assertFalse(unknown.emissionRequested());
        assertEquals(0L, tracker.snapshot().ownedMismatchOccurrenceCount());
        assertEquals(1L, tracker.snapshot().unownedObservationCount());
        assertEquals(1L, tracker.snapshot().unknownAssociationCount());
    }

    private static CraftResourceMismatchObservation mismatch(
            String correlationId,
            String position,
            String observedBlockId,
            long tick,
            String taskInstanceId) {
        return mismatch(
                correlationId,
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                position,
                observedBlockId,
                tick,
                taskInstanceId
        );
    }

    private static CraftResourceMismatchObservation mismatch(
            String correlationId,
            CraftResourceAssociationStatus association,
            String position,
            String observedBlockId,
            long tick,
            String taskInstanceId) {
        return new CraftResourceMismatchObservation(
                correlationId,
                association,
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                CraftResourceTargetRole.IRON_ORE_BLOCK,
                position,
                Optional.of(List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore")),
                Optional.of(observedBlockId),
                "MINE_TARGET_SELECTION_TRANSITION",
                tick,
                taskInstanceId
        );
    }

    private static CraftResourceMismatchObservation observation(
            String correlationId,
            String position,
            Optional<List<String>> expectedBlockIds,
            Optional<String> observedBlockId,
            long tick,
            String taskInstanceId) {
        return new CraftResourceMismatchObservation(
                correlationId,
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                CraftResourceTargetRole.IRON_ORE_BLOCK,
                position,
                expectedBlockIds,
                observedBlockId,
                "MINE_TARGET_SELECTION_TRANSITION",
                tick,
                taskInstanceId
        );
    }
}
