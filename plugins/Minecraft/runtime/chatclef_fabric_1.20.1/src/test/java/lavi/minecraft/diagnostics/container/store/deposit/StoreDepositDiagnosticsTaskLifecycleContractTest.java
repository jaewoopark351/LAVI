package lavi.minecraft.diagnostics.container.store.deposit;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Characterize root lifecycle-to-terminal ordering before extracting it from the facade.
class StoreDepositDiagnosticsTaskLifecycleContractTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
    }

    @Test
    void emitsStopBoundariesBeforeTheTerminalGroupAndPurgesExactlyOnce() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask();

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.registerBareDepositInvocation(
                    null,
                    false,
                    new ItemTarget[0],
                    root,
                    "BARE_DEPOSIT_COMMAND"
            );
            StoreDepositDiagnostics.onStoreRootStart(root, false, new ItemTarget[0]);
            StoreDepositDiagnostics.logTaskLifecycleBoundary(root, null, "STOP", "BEGIN", true);
            StoreDepositDiagnostics.logTaskLifecycleBoundary(root, null, "STOP", "END", true);
            StoreDepositDiagnostics.logTaskLifecycleBoundary(root, null, "STOP", "END", true);
        });

        assertEquals(2, occurrences(output, "event=STORE_TASK_LIFECYCLE_BOUNDARY"));
        assertEquals(1, occurrences(output, "event=STORE_DEPOSIT_TERMINAL_SUMMARY"));
        assertEquals(1, occurrences(output, "event=STORE_DEPOSIT_EFFECT_SUMMARY"));
        assertEquals(1, occurrences(output, "event=STORE_BARITONE_OPERATION_SUMMARY"));
        assertEquals(1, occurrences(output, "event=STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY"));
        assertInOrder(
                output,
                "lifecycleAction=STOP lifecyclePhase=BEGIN",
                "lifecycleAction=STOP lifecyclePhase=END",
                "event=STORE_DEPOSIT_TERMINAL_SUMMARY",
                "event=STORE_DEPOSIT_EFFECT_SUMMARY",
                "event=STORE_BARITONE_OPERATION_SUMMARY",
                "event=STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY"
        );
        assertTrue(output.contains("diagnosticClassification=UNKNOWN_STOP"));
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

    private static int occurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static void assertInOrder(String text, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = text.indexOf(token);
            assertTrue(current > previous, "Expected token after index " + previous + ": " + token);
            previous = current;
        }
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
            return "store-deposit-task-lifecycle-contract-test";
        }
    }
}
