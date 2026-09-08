package lavi.minecraft.diagnostics.container.home.timeout;

import adris.altoclef.util.Dimension;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
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
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260902_kpopmodder: Characterize STORE_HOME terminal ordering and ordered payload segments.
class StoreHomeTimeoutDiagnosticsTerminalContractTest {
    private static final String OPERATION_STARTED =
            "event=STORE_HOME_OPERATION_STARTED";
    private static final String OPERATION_TERMINAL =
            "event=STORE_HOME_OPERATION_TERMINAL_SUMMARY";
    private static final String CANDIDATE_STARTED =
            "event=STORE_HOME_CANDIDATE_ATTEMPT_STARTED";

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
    void terminalEmissionFollowsOperationStartAndPreservesPayloadSegmentOrder() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = new StoreHomeTimeoutDiagnostics(
                73L,
                policy,
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                TestObjects.allocate(HomeStorageTransferExecutor.class)
        );
        StoreHomeOperationProgress operation = StoreHomeOperationProgress.start(73L)
                .withLatestRemainingStacks(4)
                .capacityFailure();
        String output = captureOutput(() -> diagnostics.recordTerminal(
                null,
                null,
                StoreHomePhase.TERMINAL,
                operation,
                StoreHomeTimeoutObservation.initial(policy),
                null,
                null,
                null,
                0,
                StoreHomeResult.PARTIAL_TRUSTED_CAPACITY_EXHAUSTED,
                "trusted_capacity_exhausted"
        ));

        int startedOffset = output.indexOf(OPERATION_STARTED);
        int terminalOffset = output.indexOf(OPERATION_TERMINAL);
        assertTrue(startedOffset >= 0);
        assertTrue(terminalOffset > startedOffset);
        assertEquals(1, occurrences(output, OPERATION_TERMINAL));

        String terminalLine = output.lines()
                .filter(line -> line.contains(OPERATION_TERMINAL))
                .findFirst()
                .orElseThrow();
        assertTrue(terminalLine.contains("reason=trusted_capacity_exhausted"));
        assertOrdered(
                terminalLine,
                "phase=TERMINAL",
                "worldKey=unavailable",
                "candidateId=unavailable",
                "containerSessionActive=false",
                "terminalResult=PARTIAL_TRUSTED_CAPACITY_EXHAUSTED",
                "operationResult=PARTIAL_TRUSTED_CAPACITY_EXHAUSTED",
                "terminalReason=trusted_capacity_exhausted",
                "storedItems=0",
                "touchedStackCount=0",
                "remainingStackCount=4",
                "capacityFailureCount=1",
                "unavailableFailureCount=0",
                "taskBehaviorAffectedByDiagnostics=false",
                "budgetSummaryCapturedBeforeTerminalEmission=true",
                "operationDiagnosticAcceptedCount="
        );
    }

    @Test
    void terminalEmissionPrecedesCandidateLifecycleClear() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = new StoreHomeTimeoutDiagnostics(
                74L,
                policy,
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                new HomeStorageTransferExecutor(
                        new HomeStorageScreenSlotResolver()
                )
        );
        StoreHomeOperationProgress operation =
                StoreHomeOperationProgress.start(74L);
        StoreHomeCandidateAttempt attempt = attempt(candidate(1));

        String output = captureOutput(() -> {
            diagnostics.recordCandidateCatalog(1);
            diagnostics.recordCandidateStarted(
                    null,
                    null,
                    StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                    operation,
                    StoreHomeTimeoutObservation.initial(policy),
                    null,
                    attempt,
                    1
            );
            diagnostics.recordTerminal(
                    null,
                    null,
                    StoreHomePhase.TERMINAL,
                    operation,
                    StoreHomeTimeoutObservation.initial(policy),
                    null,
                    attempt,
                    null,
                    1,
                    StoreHomeResult.INTERRUPTED,
                    "test_terminal_clear"
            );
            diagnostics.recordProgress(
                    null,
                    null,
                    StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                    operation,
                    StoreHomeTimeoutObservation.initial(policy),
                    null,
                    attempt,
                    null,
                    1,
                    null
            );
        });

        assertEquals(2, occurrences(output, CANDIDATE_STARTED));
        int terminalOffset = output.indexOf(OPERATION_TERMINAL);
        int secondStartOffset = output.indexOf(
                CANDIDATE_STARTED,
                output.indexOf(CANDIDATE_STARTED) + CANDIDATE_STARTED.length()
        );
        assertTrue(secondStartOffset > terminalOffset);
        String secondStart = output.lines()
                .filter(line -> line.contains(CANDIDATE_STARTED))
                .skip(1)
                .findFirst()
                .orElseThrow();
        assertTrue(secondStart.contains(
                "reason=candidate_attempt_observed_after_diagnostics_attach"
        ));
    }

    private static AutoDepositTrustedDestinationCandidate candidate(int x) {
        return new AutoDepositTrustedDestinationCandidate(
                new AutoDepositTrustedDestination(
                        "test-world",
                        Dimension.OVERWORLD,
                        new BlockPos(x, 64, 0),
                        true
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
