package lavi.minecraft.diagnostics.container.home.timeout.progress;

import adris.altoclef.util.Dimension;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
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

//20260902_kpopmodder: Characterize STORE_HOME progress emission and suppression ordering.
class StoreHomeCandidateProgressDiagnosticsContractTest {
    private static final String PROGRESS_EVENT =
            "event=STORE_HOME_CANDIDATE_PROGRESS_SUMMARY";
    private static final String TERMINAL_EVENT =
            "event=STORE_HOME_OPERATION_TERMINAL_SUMMARY";

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
    void semanticSuppressionIsReportedBeforeEmissionAndResetAfterAdmission() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = diagnostics(101L, policy);
        StoreHomeOperationProgress operation =
                StoreHomeOperationProgress.start(101L);
        StoreHomeCandidateAttempt attempt = attempt(candidate(1));

        diagnostics.recordCandidateCatalog(1);
        diagnostics.recordCandidateStarted(
                null, null, StoreHomePhase.NAVIGATE_TO_CANDIDATE, operation,
                observation(policy, StoreHomePhase.NAVIGATE_TO_CANDIDATE, 0),
                null, attempt, 1
        );
        String output = captureOutput(() -> {
            diagnostics.recordProgress(
                    null, null, StoreHomePhase.NAVIGATE_TO_CANDIDATE, operation,
                    observation(policy, StoreHomePhase.NAVIGATE_TO_CANDIDATE, 1),
                    null, attempt, null, 1, null
            );
            diagnostics.recordProgress(
                    null, null, StoreHomePhase.OPEN_AND_BIND_CANDIDATE, operation,
                    observation(policy, StoreHomePhase.OPEN_AND_BIND_CANDIDATE, 5),
                    null, attempt, null, 1, null
            );
            diagnostics.recordProgress(
                    null, null, StoreHomePhase.OPEN_AND_BIND_CANDIDATE, operation,
                    observation(policy, StoreHomePhase.OPEN_AND_BIND_CANDIDATE, 6),
                    null, attempt, null, 1, null
            );
            diagnostics.recordTerminal(
                    null, null, StoreHomePhase.TERMINAL, operation,
                    observation(policy, StoreHomePhase.TERMINAL, 6),
                    null, attempt, null, 1,
                    StoreHomeResult.INTERRUPTED,
                    "test_progress_suppression_terminal"
            );
        });

        assertEquals(1, occurrences(output, PROGRESS_EVENT));
        String progressLine = eventLine(output, PROGRESS_EVENT);
        assertTrue(progressLine.contains("reason=no_new_progress"));
        assertOrdered(
                progressLine,
                "phase=OPEN_AND_BIND_CANDIDATE",
                "worldKey=test-world",
                "candidateOrdinal=1",
                "candidateAttemptOrdinal=1",
                "candidateTicks=5",
                "progressKind=NO_NEW_PROGRESS",
                "semanticStateChanged=true",
                "suppressedRepeatCountBeforeEmission=1",
                "containerSessionActive=false"
        );
        String terminalLine = eventLine(output, TERMINAL_EVENT);
        assertTrue(terminalLine.contains(
                "operationDiagnosticSuppressedEventCount=2"
        ));
        assertTrue(terminalLine.contains(
                "operationSuppressedByEvent={STORE_HOME_CANDIDATE_PROGRESS_SUMMARY%3D2}"
        ));
    }

    @Test
    void candidateProgressCapSuppressesTheNinthSemanticChange() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = diagnostics(102L, policy);
        StoreHomeOperationProgress operation =
                StoreHomeOperationProgress.start(102L);
        StoreHomeCandidateAttempt attempt = attempt(candidate(2));

        diagnostics.recordCandidateCatalog(1);
        diagnostics.recordCandidateStarted(
                null, null, StoreHomePhase.NAVIGATE_TO_CANDIDATE, operation,
                observation(policy, StoreHomePhase.NAVIGATE_TO_CANDIDATE, 0),
                null, attempt, 1
        );
        String output = captureOutput(() -> {
            for (int index = 0; index < 9; index++) {
                StoreHomePhase phase = index % 2 == 0
                        ? StoreHomePhase.OPEN_AND_BIND_CANDIDATE
                        : StoreHomePhase.NAVIGATE_TO_CANDIDATE;
                diagnostics.recordProgress(
                        null, null, phase, operation,
                        observation(policy, phase, index + 1),
                        null, attempt, null, 1, null
                );
            }
            diagnostics.recordTerminal(
                    null, null, StoreHomePhase.TERMINAL, operation,
                    observation(policy, StoreHomePhase.TERMINAL, 9),
                    null, attempt, null, 1,
                    StoreHomeResult.INTERRUPTED,
                    "test_progress_cap_terminal"
            );
        });

        assertEquals(8, occurrences(output, PROGRESS_EVENT));
        String terminalLine = eventLine(output, TERMINAL_EVENT);
        assertTrue(terminalLine.contains(
                "operationDiagnosticSuppressedEventCount=1"
        ));
        assertTrue(terminalLine.contains(
                "operationSuppressedByEvent={STORE_HOME_CANDIDATE_PROGRESS_SUMMARY%3D1}"
        ));
    }

    @Test
    void collaboratorUsesTypedProgressFamilyMethodsWithoutVarargs() {
        List<Method> publicMethods = Arrays.stream(
                        StoreHomeCandidateProgressDiagnostics.class
                                .getDeclaredMethods()
                )
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();

        assertEquals(
                List.of(
                        "admitsSemanticEmission",
                        "admitsSnapshotCapture",
                        "emitProgress",
                        "recordSuppressed"
                ),
                publicMethods.stream().map(Method::getName).sorted().toList()
        );
        assertFalse(publicMethods.stream().anyMatch(Method::isVarArgs));
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

    private static StoreHomeTimeoutObservation observation(
            StoreHomeTimeoutPolicy policy,
            StoreHomePhase phase,
            int candidateTicks) {
        boolean local = phase == StoreHomePhase.OPEN_AND_BIND_CANDIDATE;
        return new StoreHomeTimeoutObservation(
                candidateTicks,
                0,
                candidateTicks,
                local ? 0 : candidateTicks,
                local ? candidateTicks : 0,
                phase.name(),
                local,
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
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String eventLine(String output, String eventToken) {
        return output.lines()
                .filter(line -> line.contains(eventToken))
                .findFirst()
                .orElseThrow();
    }

    private static void assertOrdered(String text, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = text.indexOf(token, previous + 1);
            assertTrue(current > previous, () -> "Missing or out of order: " + token);
            previous = current;
        }
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
