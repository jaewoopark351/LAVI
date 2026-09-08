package lavi.minecraft.diagnostics.container.home.timeout.progress;

import adris.altoclef.util.Dimension;
import lavi.minecraft.diagnostics.container.home.timeout.candidate.StoreHomeCandidateBoundaryDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Prove candidate observation projection is immutable and lifecycle-mutation free.
class StoreHomeCandidateObservationCollectorTest {
    @Test
    void collectorOwnsOnlyFinalReadAndProjectionCollaborators() {
        assertTrue(Arrays.stream(
                        StoreHomeCandidateObservationCollector.class
                                .getDeclaredFields()
                )
                .allMatch(field -> Modifier.isFinal(field.getModifiers())));
        assertFalse(Arrays.stream(
                        StoreHomeCandidateObservationCollector.class
                                .getDeclaredFields()
                )
                .map(Field::getType)
                .anyMatch(type -> type.getName().contains("Emitter")));
        assertFalse(Arrays.stream(
                        StoreHomeCandidateObservationCollector.class
                                .getDeclaredMethods()
                )
                .anyMatch(method -> method.getName().startsWith("emit")
                        || Arrays.stream(method.getParameterTypes())
                        .anyMatch(type -> type.getName().contains("Emitter"))));
    }

    @Test
    void immutableProjectionPreservesObservedFlagsWithoutMutatingCandidateState() {
        StoreHomeCandidateObservationCollector collector =
                new StoreHomeCandidateObservationCollector(
                        new StoreHomeProgressSnapshotReader(
                                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                                new HomeStorageTransferExecutor(
                                        new HomeStorageScreenSlotResolver()
                                )
                        ),
                        new StoreHomeCandidateBoundaryDiagnostics()
                );
        StoreHomeProgressSnapshot snapshot = snapshot();
        StoreHomeCandidateProgressState state =
                new StoreHomeCandidateProgressState(
                        41L,
                        candidate(),
                        1,
                        2,
                        3,
                        7L,
                        true,
                        StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                        snapshot,
                        null
                );

        StoreHomeCandidateProgressObservation started =
                collector.startedObservation(
                        "STORE_HOME_CANDIDATE_ATTEMPT_STARTED",
                        41L,
                        StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                        "candidate_attempt_created",
                        state,
                        snapshot
                );
        StoreHomeCandidateProgressObservation observed =
                new StoreHomeCandidateProgressObservation(
                        "PLAYER_MOVED",
                        "ignored_source_fingerprint",
                        false,
                        true
                );
        StoreHomeCandidateProgressObservation projected =
                collector.boundaryObservation(
                        "STORE_HOME_CANDIDATE_TIMEOUT_DECISION",
                        41L,
                        StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                        "candidate_navigation_no_progress",
                        state,
                        snapshot,
                        observed
                );
        Object[] evidence = collector.observedEvidence(
                null,
                state.candidate(),
                state,
                null,
                snapshot,
                observed,
                1,
                true,
                5,
                9L,
                "STORE_HOME_CANDIDATE_TIMEOUT_DECISION",
                41L,
                StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                "candidate_navigation_no_progress"
        );

        assertEquals("NO_NEW_PROGRESS", started.progressKind());
        assertTrue(started.semanticStateChanged());
        assertFalse(started.sampleDue());
        assertTrue(started.fingerprint().contains(
                "STORE_HOME_CANDIDATE_ATTEMPT_STARTED"
        ));
        assertTrue(started.fingerprint().contains("candidate_attempt_created"));
        assertEquals("PLAYER_MOVED", projected.progressKind());
        assertFalse(projected.semanticStateChanged());
        assertTrue(projected.sampleDue());
        assertTrue(projected.fingerprint().contains(
                "STORE_HOME_CANDIDATE_TIMEOUT_DECISION"
        ));
        assertTrue(projected.fingerprint().contains(
                "candidate_navigation_no_progress"
        ));
        assertTrue(Arrays.toString(evidence).contains(
                "STORE_HOME_CANDIDATE_TIMEOUT_DECISION"
        ));
        assertEquals(0, state.suppressedRepeatCount());
        assertEquals(0, state.childTaskRunOrdinal());
    }

    private static AutoDepositTrustedDestinationCandidate candidate() {
        return new AutoDepositTrustedDestinationCandidate(
                new AutoDepositTrustedDestination(
                        "test-world",
                        Dimension.OVERWORLD,
                        new BlockPos(1, 64, 0),
                        true
                ),
                0,
                1,
                "test"
        );
    }

    private static StoreHomeProgressSnapshot snapshot() {
        return new StoreHomeProgressSnapshot(
                new BlockPos(0, 64, 0),
                "0, 64, 0",
                1L,
                "false",
                "NO_VISIBLE_CALCULATION_OR_PATH",
                "false",
                "false",
                "none",
                "unavailable_non_opaque_goal_target",
                "false",
                "none",
                "none",
                "unavailable",
                "false",
                "NO_ACTIVE_SESSION",
                "unavailable",
                "unavailable",
                "false",
                "false",
                0,
                "complete",
                "none"
        );
    }
}
