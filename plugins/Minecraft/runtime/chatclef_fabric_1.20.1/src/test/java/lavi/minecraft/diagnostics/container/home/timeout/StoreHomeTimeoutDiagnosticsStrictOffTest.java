package lavi.minecraft.diagnostics.container.home.timeout;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Prove STORE_HOME bookkeeping cannot mutate while diagnostics are OFF.
class StoreHomeTimeoutDiagnosticsStrictOffTest {
    @BeforeEach
    void resetDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void leaveDiagnosticsOff() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void candidateCatalogIsMutationFreeOffAndCapturedWhenEligible() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = new StoreHomeTimeoutDiagnostics(
                17L,
                policy,
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                TestObjects.allocate(HomeStorageTransferExecutor.class)
        );

        diagnostics.recordCandidateCatalog(4);

        ChatClefDiagnostics.setBoundaryEnabled(true);
        String uncapturedOutput = captureOutput(() ->
                diagnostics.recordOperationStarted(
                        null,
                        StoreHomePhase.ACCEPT_REQUEST,
                        StoreHomeOperationProgress.start(17L),
                        StoreHomeTimeoutObservation.initial(policy)
                )
        );
        assertTrue(uncapturedOutput.contains("candidateCatalogCaptured=false"));
        assertTrue(uncapturedOutput.contains("candidateCount=unavailable_not_built"));

        StoreHomeTimeoutDiagnostics eligibleDiagnostics =
                new StoreHomeTimeoutDiagnostics(
                        18L,
                        policy,
                        AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                        TestObjects.allocate(HomeStorageTransferExecutor.class)
                );
        String capturedOutput = captureOutput(() -> {
            eligibleDiagnostics.recordCandidateCatalog(4);
            eligibleDiagnostics.recordOperationStarted(
                    null,
                    StoreHomePhase.ACCEPT_REQUEST,
                    StoreHomeOperationProgress.start(18L),
                    StoreHomeTimeoutObservation.initial(policy)
            );
        });
        assertTrue(capturedOutput.contains("candidateCatalogCaptured=true"));
        assertTrue(capturedOutput.contains("candidateCount=4"));
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
}
