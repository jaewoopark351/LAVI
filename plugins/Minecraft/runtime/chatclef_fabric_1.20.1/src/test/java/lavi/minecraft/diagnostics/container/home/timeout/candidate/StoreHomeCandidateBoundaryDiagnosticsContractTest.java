package lavi.minecraft.diagnostics.container.home.timeout.candidate;

import adris.altoclef.util.Dimension;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.session.HomeStorageActivationBaseline;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivation;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerSession;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySlotSnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageManifest;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import lavi.minecraft.task.container.home.planning.HomeStorageStackLocation;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260902_kpopmodder: Characterize ordered STORE_HOME candidate boundary emission.
class StoreHomeCandidateBoundaryDiagnosticsContractTest {
    private static final String OPERATION_STARTED =
            "event=STORE_HOME_OPERATION_STARTED";
    private static final String CANDIDATE_STARTED =
            "event=STORE_HOME_CANDIDATE_ATTEMPT_STARTED";
    private static final String CANDIDATE_TIMEOUT =
            "event=STORE_HOME_CANDIDATE_TIMEOUT_DECISION";
    private static final String CANDIDATE_REJECTED =
            "event=STORE_HOME_CANDIDATE_REJECTED";
    private static final String CANDIDATE_ACTIVATED =
            "event=STORE_HOME_CANDIDATE_ACTIVATED";

    @BeforeEach
    void enableFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
        ChatClefDiagnostics.setBoundaryEnabled(true);
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void candidateBoundariesPreserveSequencePayloadAndRejectionClear() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = diagnostics(91L, policy);
        StoreHomeOperationProgress operation =
                StoreHomeOperationProgress.start(91L);
        AutoDepositTrustedDestinationCandidate first = candidate(1);
        AutoDepositTrustedDestinationCandidate second = candidate(2);
        AutoDepositTrustedDestinationCandidate third = candidate(3);
        StoreHomeCandidateAttempt secondAttempt = attempt(second);
        StoreHomeCandidateAttempt thirdAttempt = attempt(third);
        HomeStorageContainerSession thirdSession = session(third, 7L);
        String output = captureOutput(() -> {
            diagnostics.recordCandidateCatalog(3);
            diagnostics.recordCandidateRejectedBeforeAttempt(
                    null, null, StoreHomePhase.SELECT_DESTINATION, operation,
                    StoreHomeTimeoutObservation.initial(policy), null, first,
                    2, "candidate_context_mismatch", "CONTEXT_CHANGED"
            );
            diagnostics.recordCandidateStarted(
                    null, null, StoreHomePhase.NAVIGATE_TO_CANDIDATE, operation,
                    candidateObservation(policy, 0, 0, 0,
                            "NAVIGATE_TO_CANDIDATE"),
                    null, secondAttempt, 2
            );
            diagnostics.recordCandidateTimeoutDecision(
                    null, null, StoreHomePhase.NAVIGATE_TO_CANDIDATE, operation,
                    candidateObservation(policy, 2400, 2400, 0,
                            "NAVIGATE_TO_CANDIDATE"),
                    null, secondAttempt, null, 2,
                    StoreHomeTimeoutReason.CANDIDATE_NAVIGATION_NO_PROGRESS,
                    false, null
            );
            diagnostics.recordCandidateRejected(
                    null, null, StoreHomePhase.SELECT_DESTINATION, operation,
                    candidateObservation(policy, 2400, 2400, 0,
                            "NAVIGATE_TO_CANDIDATE"),
                    null, second, null, 2400, 1,
                    "candidate_navigation_no_progress", "UNAVAILABLE", null
            );
            diagnostics.recordCandidateStarted(
                    null, null, StoreHomePhase.OPEN_AND_BIND_CANDIDATE, operation,
                    candidateObservation(policy, 0, 0, 0,
                            "OPEN_AND_BIND_CANDIDATE"),
                    null, thirdAttempt, 1
            );
            diagnostics.recordCandidateActivated(
                    null, null, StoreHomePhase.VALIDATE_CONTAINER, operation,
                    candidateObservation(policy, 8, 0, 8,
                            "OPEN_AND_BIND_CANDIDATE"),
                    null, thirdAttempt, thirdSession, 1
            );
        });

        assertEventOrder(
                output,
                OPERATION_STARTED,
                CANDIDATE_REJECTED,
                CANDIDATE_STARTED,
                CANDIDATE_TIMEOUT,
                CANDIDATE_REJECTED,
                CANDIDATE_STARTED,
                CANDIDATE_ACTIVATED
        );
        assertEquals(1, eventLines(output, OPERATION_STARTED).size());
        assertEquals(2, eventLines(output, CANDIDATE_REJECTED).size());
        assertEquals(2, eventLines(output, CANDIDATE_STARTED).size());
        assertEquals(1, eventLines(output, CANDIDATE_TIMEOUT).size());
        assertEquals(1, eventLines(output, CANDIDATE_ACTIVATED).size());
        assertOrdered(
                eventLines(output, CANDIDATE_REJECTED).get(0),
                "phase=SELECT_DESTINATION",
                "worldKey=test-world",
                "candidateAttemptId=unavailable_not_started",
                "candidateOrdinal=1",
                "candidateCount=3",
                "candidateQueueRemaining=2",
                "rejectionStage=PRE_ATTEMPT_VALIDATION",
                "candidateAttemptStarted=false",
                "candidateActuallyRemoved=true",
                "actualAction=NEXT_CANDIDATE_SELECTION_PENDING"
        );
        assertOrdered(
                eventLines(output, CANDIDATE_STARTED).get(0),
                "phase=NAVIGATE_TO_CANDIDATE",
                "worldKey=test-world",
                "candidateOrdinal=2",
                "candidateAttemptOrdinal=1",
                "candidateCount=3",
                "candidateQueueRemaining=2",
                "candidateTicks=0",
                "diagnosticBoundaryKind=STORE_HOME_CANDIDATE_ATTEMPT_STARTED",
                "containerSessionActive=false"
        );
        assertOrdered(
                eventLines(output, CANDIDATE_TIMEOUT).get(0),
                "candidateOrdinal=2",
                "candidateTicks=2400",
                "diagnosticBoundaryKind=STORE_HOME_CANDIDATE_TIMEOUT_DECISION",
                "timeoutScope=CANDIDATE",
                "candidateTicksBeforeDecision=2400",
                "operationTicksBeforeDecision=0",
                "candidateTimeoutEvaluated=true",
                "candidateTimeoutConditionMatched=true",
                "operationTimeoutEvaluated=true",
                "operationTimeoutConditionMatched=false",
                "plannedTimeoutAction=REJECT_CANDIDATE",
                "plannedRejectionReason=candidate_navigation_no_progress",
                "plannedTerminalReason=not_applicable",
                "candidateDecisionCounterKind=CANDIDATE_NAVIGATION_NO_PROGRESS_TICKS",
                "candidateDecisionCounterLimitTicks=2400"
        );
        assertOrdered(
                eventLines(output, CANDIDATE_REJECTED).get(1),
                "candidateOrdinal=2",
                "candidateIncludedInQueueAtEvent=false",
                "rejectionStage=ACTIVE_ATTEMPT",
                "candidateAttemptStarted=true",
                "candidateActuallyRemoved=true",
                "candidateActiveTicksAtRejection=2400",
                "actualAction=NEXT_CANDIDATE_SELECTION_PENDING"
        );
        assertOrdered(
                eventLines(output, CANDIDATE_ACTIVATED).get(0),
                "candidateOrdinal=3",
                "candidateAttemptOrdinal=2",
                "containerSessionActive=true",
                "containerSessionOrdinal=1",
                "manifestRevision=7",
                "candidateActivated=true",
                "activationResult=EXACT_SESSION_INSTALLED"
        );
    }

    @Test
    void rejectionEmitsKnownCandidateEvidenceBeforeClearingItsLifecycle() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = diagnostics(93L, policy);
        StoreHomeOperationProgress operation =
                StoreHomeOperationProgress.start(93L);
        AutoDepositTrustedDestinationCandidate candidate = candidate(5);
        StoreHomeCandidateAttempt attempt = attempt(candidate);

        String output = captureOutput(() -> {
            diagnostics.recordCandidateCatalog(1);
            diagnostics.recordCandidateStarted(
                    null, null, StoreHomePhase.NAVIGATE_TO_CANDIDATE, operation,
                    candidateObservation(policy, 0, 0, 0,
                            "NAVIGATE_TO_CANDIDATE"),
                    null, attempt, 1
            );
            diagnostics.recordCandidateRejected(
                    null, null, StoreHomePhase.SELECT_DESTINATION, operation,
                    candidateObservation(policy, 8, 8, 0,
                            "NAVIGATE_TO_CANDIDATE"),
                    null, candidate, null, 8, 0,
                    "candidate_navigation_no_progress", "UNAVAILABLE", null
            );
            diagnostics.recordProgress(
                    null, null, StoreHomePhase.NAVIGATE_TO_CANDIDATE, operation,
                    candidateObservation(policy, 9, 9, 0,
                            "NAVIGATE_TO_CANDIDATE"),
                    null, attempt, null, 1, null
            );
        });

        List<String> started = eventLines(output, CANDIDATE_STARTED);
        assertEquals(2, started.size());
        int rejectionOffset = output.indexOf(CANDIDATE_REJECTED);
        int lateAttachOffset = output.indexOf(CANDIDATE_STARTED,
                output.indexOf(CANDIDATE_STARTED) + CANDIDATE_STARTED.length());
        assertTrue(lateAttachOffset > rejectionOffset);
        assertTrue(started.get(1).contains(
                "reason=candidate_attempt_observed_after_diagnostics_attach"
        ));
        assertTrue(started.get(1).contains("candidateOrdinal=2"));
        assertTrue(started.get(1).contains("candidateAttemptOrdinal=2"));
        assertOrdered(
                eventLines(output, CANDIDATE_REJECTED).get(0),
                "candidateOrdinal=1",
                "diagnosticBoundaryKind=STORE_HOME_CANDIDATE_REJECTED",
                "rejectionStage=ACTIVE_ATTEMPT"
        );
    }

    @Test
    void collaboratorUsesTypedCandidateFamilyMethodsWithoutVarargs() {
        List<Method> publicMethods = Arrays.stream(
                        StoreHomeCandidateBoundaryDiagnostics.class
                                .getDeclaredMethods()
                )
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();

        assertEquals(
                List.of(
                        "emitActivated",
                        "emitRejected",
                        "emitRejectedBeforeAttempt",
                        "emitStarted",
                        "emitTimeoutDecision",
                        "observedEvidence",
                        "unavailableEvidence",
                        "unobservedRejectionEvidence"
                ),
                publicMethods.stream().map(Method::getName).sorted().toList()
        );
        assertFalse(publicMethods.stream().anyMatch(Method::isVarArgs));
    }

    @Test
    void localInteractionTimeoutPreservesPendingTransferPrecedence() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = diagnostics(92L, policy);
        StoreHomeOperationProgress operation =
                StoreHomeOperationProgress.start(92L);
        StoreHomeCandidateAttempt attempt = attempt(candidate(4));

        String output = captureOutput(() -> {
            diagnostics.recordCandidateCatalog(1);
            diagnostics.recordCandidateStarted(
                    null, null, StoreHomePhase.OPEN_AND_BIND_CANDIDATE, operation,
                    candidateObservation(policy, 0, 0, 0,
                            "OPEN_AND_BIND_CANDIDATE"),
                    null, attempt, 1
            );
            diagnostics.recordCandidateTimeoutDecision(
                    null, null, StoreHomePhase.OPEN_AND_BIND_CANDIDATE, operation,
                    candidateObservation(policy, 2400, 30, 2400,
                            "OPEN_AND_BIND_CANDIDATE"),
                    null, attempt, null, 1,
                    StoreHomeTimeoutReason.CANDIDATE_LOCAL_INTERACTION_TIMEOUT,
                    true, null
            );
        });

        assertOrdered(
                eventLines(output, CANDIDATE_TIMEOUT).get(0),
                "timeoutScope=CANDIDATE",
                "candidateTicksBeforeDecision=2400",
                "plannedTimeoutAction=FINISH_TRANSFER_UNCONFIRMED",
                "plannedRejectionReason=not_applicable",
                "plannedTerminalReason=candidate_local_interaction_timeout_with_pending_transfer",
                "plannedTerminalResult=TRANSFER_UNCONFIRMED",
                "pendingTransferAtDecision=true",
                "executorPendingAtDecision=true",
                "sessionPendingAtDecision=false",
                "candidateDecisionCounterKind=CANDIDATE_LOCAL_INTERACTION_TICKS",
                "candidateDecisionCounterLimitTicks=2400"
        );
    }

    private static StoreHomeTimeoutDiagnostics diagnostics(
            long operationId,
            StoreHomeTimeoutPolicy policy) {
        return new StoreHomeTimeoutDiagnostics(
                operationId,
                policy,
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                new HomeStorageTransferExecutor(new HomeStorageScreenSlotResolver())
        );
    }

    private static AutoDepositTrustedDestinationCandidate candidate(int x) {
        return new AutoDepositTrustedDestinationCandidate(
                new AutoDepositTrustedDestination(
                        "test-world", Dimension.OVERWORLD,
                        new BlockPos(x, 64, 0), true
                ),
                0,
                x * x,
                "test"
        );
    }

    private static StoreHomeCandidateAttempt attempt(
            AutoDepositTrustedDestinationCandidate candidate) {
        StoreHomeCandidateAttempt attempt =
                TestObjects.allocate(StoreHomeCandidateAttempt.class);
        TestObjects.setField(
                attempt,
                StoreHomeCandidateAttempt.class,
                "candidate",
                candidate
        );
        return attempt;
    }

    private static HomeStorageContainerSession session(
            AutoDepositTrustedDestinationCandidate candidate,
            long revision) {
        HomeStorageInventorySnapshot inventory = new HomeStorageInventorySnapshot(
                List.of(HomeStorageInventorySlotSnapshot.empty(
                        HomeStorageStackLocation.MAIN, 0
                )),
                List.of(),
                0
        );
        HomeStoragePlan plan = new HomeStoragePlan(
                revision,
                List.of(),
                new HomeStorageManifest(revision, List.of())
        );
        return HomeStorageContainerSession.activate(
                candidate,
                1,
                new HomeStorageActivationBaseline(inventory, plan),
                HomeStorageContainerActivation.ready(new Object(), 4)
        );
    }

    private static StoreHomeTimeoutObservation candidateObservation(
            StoreHomeTimeoutPolicy policy,
            int activeTicks,
            int navigationTicks,
            int localInteractionTicks,
            String phase) {
        return new StoreHomeTimeoutObservation(
                activeTicks,
                0,
                activeTicks,
                navigationTicks,
                localInteractionTicks,
                phase,
                localInteractionTicks > 0,
                policy.candidateNavigationNoProgressTicks(),
                policy.candidateLocalInteractionTicks(),
                policy.operationNoProgressTicks(),
                policy.operationEmergencyHardCapTicks(),
                policy.movementJitterBlocks(),
                policy.bestDistanceImprovementEpsilonBlocks()
        );
    }

    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream captured = new PrintStream(
                output, true, StandardCharsets.UTF_8
        );
        System.setOut(captured);
        try {
            action.run();
        } finally {
            System.setOut(original);
            captured.close();
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static List<String> eventLines(String output, String eventToken) {
        return output.lines().filter(line -> line.contains(eventToken)).toList();
    }

    private static void assertEventOrder(String output, String... eventTokens) {
        int previous = -1;
        for (String eventToken : eventTokens) {
            int current = output.indexOf(eventToken, previous + 1);
            assertTrue(current > previous, () -> "Missing event order: " + eventToken);
            previous = current;
        }
    }

    private static void assertOrdered(String text, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = text.indexOf(token, previous + 1);
            assertTrue(current > previous, () -> "Missing or out of order: " + token);
            previous = current;
        }
    }
}
