package lavi.minecraft.diagnostics.container.store.deposit.candidate;

import lavi.minecraft.diagnostics.container.store.deposit.candidate.snapshot.StoreContainerRouteSnapshot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreContainerRouteStateTest {
    @Test
    void correlatesParentFilteredRouteChildAndChangedCheckpoint() {
        StoreContainerRouteState state = new StoreContainerRouteState(100);
        BlockPos raw = new BlockPos(-679, 59, 105);
        BlockPos filtered = new BlockPos(-533, 50, 126);
        Object routeChild = new Object();

        StoreContainerParentDecision parent = state.recordParentDecision(
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
        state.recordChildReconciliation("ROOT_ROUTE", routeChild, true);
        state.recordFilteredSearch(
                Optional.of(filtered),
                observation(parent, raw)
        );

        assertEquals(1, parent.branchEpoch());
        assertTrue(state.isCurrentRouteChild(routeChild));
        assertFalse(state.isCurrentRouteChild(new Object()));
        assertEquals(filtered, state.currentFilteredCandidate());
        assertEquals(3, state.candidateEvaluationCount());
        assertEquals(1, state.predicateAcceptedCount());
        assertEquals(2, state.predicateRejectedCount());
        assertEquals("BLOCK_SCANNER_INTERNAL_FILTER_UNOBSERVED", state.firstUnobservedBoundary());
        assertEquals("DIFFERENT_POSITION", StoreContainerRouteState.relation(raw, filtered));
        assertTrue(state.checkpoint(1299).isEmpty());

        StoreContainerRouteCheckpoint checkpoint = state.checkpoint(1300).orElseThrow();
        assertSame(raw, checkpoint.currentRawCandidate());
        assertSame(filtered, checkpoint.currentFilteredCandidate());
        assertEquals(3, checkpoint.candidateEvaluationCountSinceLastCheckpoint());
        assertEquals(1, checkpoint.childReplacementCountSinceLastCheckpoint());
        assertTrue(state.checkpoint(2500).isEmpty());
    }

    @Test
    void incrementsEpochOnlyWhenBranchChanges() {
        StoreContainerRouteState state = new StoreContainerRouteState(0);

        state.recordParentDecision("OBTAIN_CHEST", true, null, false, false,
                false, false, null, "NO_RAW_CLOSEST", "targets-a");
        state.recordParentDecision("OBTAIN_CHEST", true, null, false, false,
                false, false, null, "NO_RAW_CLOSEST", "targets-a");
        StoreContainerParentDecision changed = state.recordParentDecision(
                "OPEN_EXISTING",
                true,
                new BlockPos(1, 2, 3),
                true,
                true,
                false,
                false,
                null,
                "RAW_CLOSEST_WITHIN_50",
                "targets-a"
        );

        assertEquals(2, changed.branchEpoch());
        assertEquals("OBTAIN_CHEST", changed.previousBranch());
        assertTrue(changed.branchChanged());
        assertTrue(state.branchCounts().contains("OBTAIN_CHEST=2"));
        assertTrue(state.branchTransitionCounts().contains("OBTAIN_CHEST->OPEN_EXISTING=1"));
    }

    @Test
    void recordsRangeCrossingAndExactResourceChildInterruption() {
        StoreContainerRouteState state = new StoreContainerRouteState(0);
        BlockPos raw = new BlockPos(50, 64, 0);
        Object obtainChild = new Object();
        Object openChild = new Object();

        state.recordParentDecision(
                "OBTAIN_CHEST",
                true,
                raw,
                true,
                false,
                false,
                false,
                null,
                "RAW_PRESENT_BUT_OUTSIDE_RANGES",
                "targets-a",
                new Vec3d(0, 64, 0)
        );
        state.recordChildReconciliation("ROOT_ROUTE", null, obtainChild, true, false);
        state.recordParentDecision(
                "OPEN_EXISTING",
                true,
                raw,
                true,
                true,
                false,
                false,
                null,
                "RAW_CLOSEST_WITHIN_50",
                "targets-a",
                new Vec3d(1, 64, 0)
        );
        boolean interrupted = state.recordChildReconciliation(
                "ROOT_ROUTE",
                obtainChild,
                openChild,
                true,
                true
        );

        assertEquals("OUTSIDE_TO_INSIDE", state.currentRangeTransition().rawRangeCrossing());
        assertTrue(interrupted);
        assertEquals(1, state.rangeSummary().raw50CrossingCount());
        assertEquals(1, state.rangeSummary().branchChangeAtRangeCrossingCount());
        assertEquals(1, state.rangeSummary().resourceChildInterruptedAtRangeCrossingCount());
    }

    @Test
    void distanceMovementWithoutABooleanCrossingDoesNotCreateRangeEvents() {
        StoreContainerRouteState state = new StoreContainerRouteState(0);
        BlockPos raw = new BlockPos(10, 64, 0);

        state.recordParentDecision("OPEN_EXISTING", true, raw, true, true,
                false, false, null, "RAW_CLOSEST_WITHIN_50", "targets-a", new Vec3d(0, 64, 0));
        state.recordParentDecision("OPEN_EXISTING", true, raw, true, true,
                false, false, null, "RAW_CLOSEST_WITHIN_50", "targets-a", new Vec3d(2, 64, 0));

        assertEquals("NONE", state.currentRangeTransition().rawRangeCrossing());
        assertEquals(0, state.rangeSummary().raw50CrossingCount());
    }

    @Test
    void retainsRangeAggregatesUntilCheckpointEmissionIsAcknowledged() {
        StoreContainerRouteState state = new StoreContainerRouteState(0);
        BlockPos raw = new BlockPos(50, 64, 0);

        state.recordParentDecision("OPEN_EXISTING", true, raw, true, false,
                false, false, null, "RAW_PRESENT_BUT_OUTSIDE_RANGES", "targets-a", new Vec3d(0, 64, 0));
        state.recordParentDecision("OPEN_EXISTING", true, raw, true, true,
                false, false, null, "RAW_CLOSEST_WITHIN_50", "targets-a", new Vec3d(1, 64, 0));
        StoreContainerRouteCheckpoint first = state.checkpoint(1200).orElseThrow();
        assertEquals(1, first.rangeAggregateSinceLastCheckpoint().raw50CrossingCount());

        state.recordParentDecision("OPEN_EXISTING", true, raw, true, false,
                false, false, null, "RAW_PRESENT_BUT_OUTSIDE_RANGES", "targets-a", new Vec3d(0, 64, 0));
        StoreContainerRouteCheckpoint second = state.checkpoint(2400).orElseThrow();
        assertEquals(2, second.rangeAggregateSinceLastCheckpoint().raw50CrossingCount());

        state.acknowledgeCheckpointEmission(second.checkpointSequence());
        state.recordParentDecision("OPEN_EXISTING", true, raw, true, true,
                false, false, null, "RAW_CLOSEST_WITHIN_50", "targets-a", new Vec3d(1, 64, 0));
        StoreContainerRouteCheckpoint third = state.checkpoint(3600).orElseThrow();
        assertEquals(1, third.rangeAggregateSinceLastCheckpoint().raw50CrossingCount());
    }

    @Test
    void capturesOneStableFinalSummarySnapshotAcrossLaterRouteMutations() {
        StoreContainerRouteState state = new StoreContainerRouteState(0);
        BlockPos firstRaw = new BlockPos(1, 64, 1);
        BlockPos secondRaw = new BlockPos(2, 64, 2);

        state.recordParentDecision("OPEN_EXISTING", true, firstRaw, true, true,
                false, false, null, "RAW_CLOSEST_WITHIN_50", "targets-a");
        state.recordPursuit(firstRaw, "OPEN_CHEST");
        StoreContainerRouteSnapshot first = state.snapshot();

        state.recordParentDecision("OBTAIN_CHEST", true, secondRaw, true, false,
                false, false, null, "RAW_PRESENT_BUT_OUTSIDE_RANGES", "targets-b");
        state.recordPursuit(secondRaw, "OBTAIN_CHEST");
        state.recordTransferDecision();
        StoreContainerRouteSnapshot second = state.snapshot();

        assertEquals(1, first.candidateDecisionSequence());
        assertEquals(1, first.branchEpoch());
        assertEquals("OPEN_EXISTING", first.currentBranch());
        assertEquals(firstRaw, first.currentRawCandidate());
        assertEquals(firstRaw, first.currentPursuit());
        assertEquals("{OPEN_EXISTING=1}", first.branchCounts());
        assertEquals("{NONE->OPEN_EXISTING=1}", first.branchTransitionCounts());
        assertEquals(0, first.transferDecisionCount());
        assertEquals("PURSUIT_DECISION", first.lastSuccessfulBoundary());

        assertEquals(2, second.candidateDecisionSequence());
        assertEquals(2, second.branchEpoch());
        assertEquals("OBTAIN_CHEST", second.currentBranch());
        assertEquals(secondRaw, second.currentRawCandidate());
        assertEquals(secondRaw, second.currentPursuit());
        assertEquals("{OPEN_EXISTING=1, OBTAIN_CHEST=1}", second.branchCounts());
        assertEquals(
                "{NONE->OPEN_EXISTING=1, OPEN_EXISTING->OBTAIN_CHEST=1}",
                second.branchTransitionCounts()
        );
        assertEquals(1, second.transferDecisionCount());
        assertEquals("TRANSFER_DECISION", second.lastSuccessfulBoundary());
    }

    @Test
    void exposesSnapshotAsOneSynchronizedImmutableCaptureBoundary() throws Exception {
        Method snapshotMethod = StoreContainerRouteState.class.getDeclaredMethod("snapshot");

        assertTrue(Modifier.isPublic(snapshotMethod.getModifiers()));
        assertTrue(Modifier.isSynchronized(snapshotMethod.getModifiers()));
        assertTrue(StoreContainerRouteSnapshot.class.isRecord());
        assertTrue(Modifier.isFinal(StoreContainerRouteSnapshot.class.getModifiers()));
    }

    private static StoreContainerCandidateObservation observation(StoreContainerParentDecision parent,
                                                                  BlockPos raw) {
        return new StoreContainerCandidateObservation(
                true,
                true,
                "operation-a",
                "route-child",
                parent.sequence(),
                parent.branchEpoch(),
                raw,
                3,
                1,
                2,
                1,
                1,
                0,
                0,
                0,
                "{CHEST_ABOVE_BLOCKED_UNBREAKABLE=1, CONTAINER_CACHE_FULL=1}",
                raw,
                "CHEST_ABOVE_BLOCKED_UNBREAKABLE",
                raw.add(1, 0, 0),
                "CONTAINER_CACHE_FULL",
                1,
                1,
                StoreContainerRawCandidateOutcome.RAW_REJECTED.name(),
                StoreContainerCandidateRejectionReason.CHEST_ABOVE_BLOCKED_UNBREAKABLE.name(),
                true,
                "EXACT_STORE_PREDICATE_RESULT"
        );
    }
}
