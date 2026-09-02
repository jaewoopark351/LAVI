package lavi.minecraft.diagnostics.container.home.timeout.operation;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;
import lavi.minecraft.testsupport.TestObjects;
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

//20260902_kpopmodder: Characterize STORE_HOME operation start and timeout event contracts.
class StoreHomeOperationDiagnosticsContractTest {
    private static final String OPERATION_STARTED =
            "event=STORE_HOME_OPERATION_STARTED";
    private static final String OPERATION_TIMEOUT =
            "event=STORE_HOME_OPERATION_TIMEOUT_DECISION";

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
    void operationStartIsEmittedOnceBeforeEmergencyTimeoutWithOrderedCounters() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = diagnostics(81L, policy);
        StoreHomeOperationProgress operation =
                StoreHomeOperationProgress.start(81L);

        String output = captureOutput(() -> {
            diagnostics.recordOperationStarted(
                    null,
                    StoreHomePhase.ACCEPT_REQUEST,
                    operation,
                    StoreHomeTimeoutObservation.initial(policy)
            );
            diagnostics.recordOperationStarted(
                    null,
                    StoreHomePhase.ACCEPT_REQUEST,
                    operation,
                    StoreHomeTimeoutObservation.initial(policy)
            );
            diagnostics.recordOperationTimeoutDecision(
                    null,
                    null,
                    StoreHomePhase.TERMINAL,
                    operation,
                    observation(policy, 120000, 211),
                    null,
                    null,
                    null,
                    0,
                    StoreHomeTimeoutReason.OPERATION_EMERGENCY_HARD_CAP,
                    false,
                    null
            );
        });

        assertEquals(1, occurrences(output, OPERATION_STARTED));
        assertEquals(1, occurrences(output, OPERATION_TIMEOUT));
        assertTrue(output.indexOf(OPERATION_STARTED) < output.indexOf(OPERATION_TIMEOUT));

        assertOrdered(
                eventLine(output, OPERATION_STARTED),
                "operationTicks=0",
                "phase=ACCEPT_REQUEST",
                "worldKey=unavailable",
                "candidateId=unavailable",
                "candidateCount=unavailable_not_built",
                "operationStartObservation=TASK_ON_START",
                "candidateCatalogCaptured=false"
        );
        assertOrdered(
                eventLine(output, OPERATION_TIMEOUT),
                "operationTicks=211",
                "operationEmergencyElapsedTicks=120000",
                "phase=TERMINAL",
                "worldKey=unavailable",
                "candidateId=unavailable",
                "containerSessionActive=false",
                "diagnosticCaptureStatus=complete_no_active_candidate",
                "diagnosticErrorClass=none",
                "timeoutScope=OPERATION",
                "candidateTicksBeforeDecision=unavailable_no_active_candidate",
                "operationTicksBeforeDecision=120000",
                "candidateTimeoutEvaluated=false",
                "operationTimeoutEvaluated=true",
                "operationTimeoutConditionMatched=true",
                "plannedTimeoutAction=FINISH_EXHAUSTED",
                "plannedTerminalReason=operation_emergency_hard_cap",
                "plannedTerminalResult=EXISTING_EXHAUSTED_CLASSIFIER",
                "pendingTransferAtDecision=false",
                "candidateTicksObservedAtOperationDecision=unavailable_no_active_candidate",
                "operationDecisionCounterKind=OPERATION_ACTIVE_TICKS",
                "operationDecisionCounterLimitTicks=120000"
        );
    }

    @Test
    void operationNoProgressTimeoutPreservesPendingTransferPrecedenceAndCounter() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = diagnostics(82L, policy);
        StoreHomeOperationProgress operation =
                StoreHomeOperationProgress.start(82L);

        String output = captureOutput(() -> {
            diagnostics.recordOperationStarted(
                    null,
                    StoreHomePhase.ACCEPT_REQUEST,
                    operation,
                    StoreHomeTimeoutObservation.initial(policy)
            );
            diagnostics.recordOperationTimeoutDecision(
                    null,
                    null,
                    StoreHomePhase.TERMINAL,
                    operation,
                    observation(policy, 40000, 12000),
                    null,
                    null,
                    null,
                    0,
                    StoreHomeTimeoutReason.OPERATION_NO_PROGRESS,
                    true,
                    null
            );
        });

        String timeoutLine = eventLine(output, OPERATION_TIMEOUT);
        assertOrdered(
                timeoutLine,
                "operationTicks=12000",
                "operationEmergencyElapsedTicks=40000",
                "timeoutScope=OPERATION",
                "operationTicksBeforeDecision=12000",
                "plannedTimeoutAction=FINISH_TRANSFER_UNCONFIRMED",
                "plannedRejectionReason=not_applicable",
                "plannedTerminalReason=operation_no_progress_with_pending_transfer",
                "plannedTerminalResult=TRANSFER_UNCONFIRMED",
                "pendingTransferAtDecision=true",
                "executorPendingAtDecision=true",
                "sessionPendingAtDecision=false",
                "operationDecisionCounterKind=OPERATION_NO_PROGRESS_TICKS",
                "operationDecisionCounterLimitTicks=12000"
        );
    }

    @Test
    void collaboratorUsesTypedOperationFamilyMethodsWithoutVarargs() {
        List<Method> publicMethods = Arrays.stream(
                        StoreHomeOperationDiagnostics.class.getDeclaredMethods()
                )
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();

        assertEquals(
                List.of("emitStarted", "emitTimeoutDecision", "operationFields"),
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
                TestObjects.allocate(HomeStorageTransferExecutor.class)
        );
    }

    private static StoreHomeTimeoutObservation observation(
            StoreHomeTimeoutPolicy policy,
            int operationActiveTicks,
            int operationNoProgressTicks) {
        return new StoreHomeTimeoutObservation(
                operationActiveTicks,
                operationNoProgressTicks,
                0,
                0,
                0,
                "NO_ACTIVE_CANDIDATE",
                false,
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
