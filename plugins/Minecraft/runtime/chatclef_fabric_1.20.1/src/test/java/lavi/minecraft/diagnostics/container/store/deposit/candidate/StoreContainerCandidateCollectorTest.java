package lavi.minecraft.diagnostics.container.store.deposit.candidate;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreContainerCandidateCollectorTest {
    @Test
    void recordsAnAcceptedRawCandidateFromTheExistingPredicateResult() {
        BlockPos raw = new BlockPos(-679, 59, 105);
        TestTask routeChild = new TestTask();
        StoreDepositOperationState state = stateWithOpenRoute(raw, routeChild);

        StoreContainerCandidateCollector.begin(state, routeChild);
        StoreContainerCandidateCollector.observe(raw, StoreContainerCandidateRejectionReason.ACCEPTED);
        StoreContainerCandidateCollector.end(routeChild, true);

        StoreContainerCandidateObservation observation = StoreContainerCandidateCollector.take(state, routeChild);
        assertEquals(StoreContainerRawCandidateOutcome.RAW_ACCEPTED.name(), observation.rawCandidatePredicateOutcome());
        assertEquals(StoreContainerCandidateRejectionReason.ACCEPTED.name(), observation.rawCandidateRejectionReason());
    }

    @Test
    void recordsTheExactRawCandidateReasonWithoutEmittingPerCandidateEvents() {
        BlockPos raw = new BlockPos(-679, 59, 105);
        TestTask routeChild = new TestTask();
        StoreDepositOperationState state = stateWithOpenRoute(raw, routeChild);

        StoreContainerCandidateCollector.begin(state, routeChild);
        StoreContainerCandidateCollector.observe(raw.add(1, 0, 0), StoreContainerCandidateRejectionReason.CONTAINER_CACHE_FULL);
        StoreContainerCandidateCollector.observe(raw, StoreContainerCandidateRejectionReason.CACHED_DUNGEON_CHEST);
        StoreContainerCandidateCollector.observe(raw.add(2, 0, 0), StoreContainerCandidateRejectionReason.ACCEPTED);
        StoreContainerCandidateCollector.end(routeChild, true);

        StoreContainerCandidateObservation observation = StoreContainerCandidateCollector.take(state, routeChild);
        assertTrue(observation.available());
        assertTrue(observation.rawCandidateMatchedByValue());
        assertEquals(1, observation.rawCandidateObservationCount());
        assertEquals(2, observation.rawCandidateEvaluationOrdinal());
        assertEquals(StoreContainerRawCandidateOutcome.RAW_REJECTED.name(), observation.rawCandidatePredicateOutcome());
        assertEquals(StoreContainerCandidateRejectionReason.CACHED_DUNGEON_CHEST.name(), observation.rawCandidateRejectionReason());
        assertEquals("EXACT_STORE_PREDICATE_RESULT", observation.rawCandidateCoverage());
    }

    @Test
    void reportsWhenTheRawCandidateWasNotVisitedByTheFilteredScan() {
        BlockPos raw = new BlockPos(-679, 59, 105);
        TestTask routeChild = new TestTask();
        StoreDepositOperationState state = stateWithOpenRoute(raw, routeChild);

        StoreContainerCandidateCollector.begin(state, routeChild);
        StoreContainerCandidateCollector.observe(raw.add(1, 0, 0), StoreContainerCandidateRejectionReason.ACCEPTED);
        StoreContainerCandidateCollector.end(routeChild, true);

        StoreContainerCandidateObservation observation = StoreContainerCandidateCollector.take(state, routeChild);
        assertFalse(observation.rawCandidateMatchedByValue());
        assertEquals(0, observation.rawCandidateObservationCount());
        assertEquals(StoreContainerRawCandidateOutcome.RAW_NOT_VISITED_BY_FILTERED_SCAN.name(), observation.rawCandidatePredicateOutcome());
        assertEquals("UNAVAILABLE", observation.rawCandidateRejectionReason());
    }

    @Test
    void reportsAnIncompleteFilteredScanWithoutDiscardingObservedRawEvidence() {
        BlockPos raw = new BlockPos(-679, 59, 105);
        TestTask routeChild = new TestTask();
        StoreDepositOperationState state = stateWithOpenRoute(raw, routeChild);

        StoreContainerCandidateCollector.begin(state, routeChild);
        StoreContainerCandidateCollector.observe(raw, StoreContainerCandidateRejectionReason.ACCEPTED);
        StoreContainerCandidateCollector.end(routeChild, false);

        StoreContainerCandidateObservation observation = StoreContainerCandidateCollector.take(state, routeChild);
        assertFalse(observation.scannerCallCompletedNormally());
        assertEquals(
                StoreContainerRawCandidateOutcome.FILTERED_SCAN_DID_NOT_COMPLETE.name(),
                observation.rawCandidatePredicateOutcome()
        );
        assertEquals(StoreContainerCandidateRejectionReason.ACCEPTED.name(), observation.rawCandidateRejectionReason());
        state.routeState().recordFilteredSearch(java.util.Optional.empty(), observation);
        assertEquals("FILTERED_SCAN_DID_NOT_COMPLETE", state.routeState().firstExplicitFailureBoundary());
    }

    @Test
    void reportsWhenTheParentRawCandidateWasUnavailable() {
        TestTask routeChild = new TestTask();
        StoreDepositOperationState state = stateWithOpenRoute(null, routeChild);

        StoreContainerCandidateCollector.begin(state, routeChild);
        StoreContainerCandidateCollector.observe(
                new BlockPos(1, 2, 3),
                StoreContainerCandidateRejectionReason.ACCEPTED
        );
        StoreContainerCandidateCollector.end(routeChild, true);

        StoreContainerCandidateObservation observation = StoreContainerCandidateCollector.take(state, routeChild);
        assertEquals(StoreContainerRawCandidateOutcome.RAW_UNAVAILABLE.name(), observation.rawCandidatePredicateOutcome());
        assertEquals("RAW_PARENT_CANDIDATE_UNAVAILABLE", observation.rawCandidateCoverage());
    }

    @Test
    void bindsAParentOwnedFilteredScanToTheDecisionRecordedAfterTheScan() {
        BlockPos raw = new BlockPos(-679, 59, 105);
        TestTask parent = new TestTask();
        StoreDepositOperationState state = new StoreDepositOperationState(new StoreDepositOperationContext(
                "operation-a",
                "BARE_DEPOSIT_ALL_COMMAND",
                parent,
                0,
                0
        ));

        StoreContainerCandidateCollector.beginParentSelection(state, parent, raw);
        StoreContainerCandidateCollector.observe(raw, StoreContainerCandidateRejectionReason.CACHED_DUNGEON_CHEST);
        StoreContainerCandidateCollector.end(parent, true);
        StoreContainerParentDecision decision = state.routeState().recordParentDecision(
                "OBTAIN_CHEST",
                true,
                raw,
                true,
                true,
                false,
                false,
                null,
                "NO_FILTERED_TARGET",
                "targets-a"
        );

        StoreContainerCandidateObservation observation = StoreContainerCandidateCollector.take(state, parent);
        assertEquals(decision.sequence(), observation.parentDecisionSequence());
        assertEquals(decision.branchEpoch(), observation.branchEpoch());
        assertEquals(raw, observation.parentRawClosest());
        assertEquals(StoreContainerRawCandidateOutcome.RAW_REJECTED.name(), observation.rawCandidatePredicateOutcome());
        assertEquals(StoreContainerCandidateRejectionReason.CACHED_DUNGEON_CHEST.name(), observation.rawCandidateRejectionReason());
    }

    private static StoreDepositOperationState stateWithOpenRoute(BlockPos raw, Task routeChild) {
        StoreDepositOperationState state = new StoreDepositOperationState(new StoreDepositOperationContext(
                "operation-a",
                "BARE_DEPOSIT_ALL_COMMAND",
                null,
                0,
                0
        ));
        state.routeState().recordParentDecision(
                "OPEN_EXISTING",
                true,
                raw,
                true,
                true,
                false,
                false,
                null,
                "RAW_CLOSEST_WITHIN_50",
                "targets-a"
        );
        state.routeState().recordChildReconciliation("ROOT_ROUTE", routeChild, true);
        return state;
    }

    private static final class TestTask extends Task {
        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "candidate-collector-test";
        }
    }
}
