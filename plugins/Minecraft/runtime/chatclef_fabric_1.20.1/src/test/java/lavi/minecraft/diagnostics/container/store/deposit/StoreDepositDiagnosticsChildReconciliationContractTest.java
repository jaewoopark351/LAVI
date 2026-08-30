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

//20260829_kpopmodder: Characterize child reconciliation state and route ordering before extraction.
class StoreDepositDiagnosticsChildReconciliationContractTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
    }

    @Test
    void recordsEveryReconciliationBeforeSuppressingDuplicateDetail() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask("root");
        Task child = new TestTask("child");

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.registerBareDepositInvocation(
                    null,
                    false,
                    new ItemTarget[0],
                    root,
                    "BARE_DEPOSIT_COMMAND"
            );
            StoreDepositDiagnostics.onStoreRootStart(root, false, new ItemTarget[0]);
            StoreDepositDiagnostics.logChildReconciliation(
                    root, null, child, false, true, true, true, false, child
            );
            StoreDepositDiagnostics.logChildReconciliation(
                    root, null, child, false, true, true, true, false, child
            );
            StoreDepositDiagnostics.logNaturalFinish(root);
        });

        assertEquals(1, occurrences(output, "event=STORE_TASK_CHILD_RECONCILIATION"));
        assertTrue(output.contains("reconciliationRole=ROOT_ROUTE"));
        assertTrue(output.contains("reconciliationOutcome=CHILD_REPLACED"));
        assertTrue(output.contains("childReconciliationCount=2"));
    }

    @Test
    void observesThePreviousChildStopBeforeRecordingAChangedDepositAllRoute() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask("root");
        Task obtainChestChild = new TestTask("obtain-chest");
        Task openExistingChild = new TestTask("open-existing");

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.registerBareDepositInvocation(
                    null,
                    false,
                    new ItemTarget[0],
                    root,
                    "BARE_DEPOSIT_ALL_COMMAND"
            );
            StoreDepositDiagnostics.onStoreRootStart(root, false, new ItemTarget[0]);
            StoreDepositDiagnostics.logDepositAllParentCandidateDecision(
                    root,
                    "OBTAIN_CHEST",
                    false,
                    null,
                    false,
                    false,
                    false,
                    false,
                    null,
                    new ItemTarget[0]
            );
            StoreDepositDiagnostics.logChildReconciliation(
                    root, null, obtainChestChild, false, true, true, true, false, obtainChestChild
            );
            StoreDepositDiagnostics.logDepositAllParentCandidateDecision(
                    root,
                    "OPEN_EXISTING",
                    false,
                    null,
                    false,
                    false,
                    false,
                    false,
                    null,
                    new ItemTarget[0]
            );
            StoreDepositDiagnostics.logTaskLifecycleBoundary(
                    obtainChestChild, null, "STOP", "END", true
            );
            StoreDepositDiagnostics.logChildReconciliation(
                    root,
                    obtainChestChild,
                    openExistingChild,
                    false,
                    true,
                    true,
                    true,
                    true,
                    openExistingChild
            );
            StoreDepositDiagnostics.logNaturalFinish(root);
        });

        String interruptedHandoff = lineContaining(
                output,
                "resourceAcquisitionInterruptedByBranchChange=true"
        );
        assertTrue(interruptedHandoff.contains("event=STORE_TASK_CHILD_RECONCILIATION"));
        assertTrue(interruptedHandoff.contains("previousRouteChildStopObserved=true"));
        assertTrue(interruptedHandoff.contains("previousBranch=OBTAIN_CHEST"));
        assertTrue(interruptedHandoff.contains("currentBranch=OPEN_EXISTING"));
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

    private static String lineContaining(String output, String token) {
        for (String line : output.split("\\R")) {
            if (line.contains(token)) {
                return line;
            }
        }
        throw new AssertionError("Missing output token: " + token);
    }

    private static final class TestTask extends Task {
        private final String name;

        private TestTask(String name) {
            this.name = name;
        }

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
            return name;
        }
    }
}
