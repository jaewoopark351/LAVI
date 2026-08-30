package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Prove child binding, state, route mutation, and detail emission stay in their original order.
class StoreDepositChildReconciliationDiagnosticsTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
    }

    @Test
    void recordsEveryReconciliationBeforeSuppressingDuplicateDetail() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositChildReconciliationDiagnostics diagnostics =
                new StoreDepositChildReconciliationDiagnostics(bindings, emissionGate);
        Task root = new TestTask("root");
        Task candidate = new TestTask("candidate");
        Task active = new TestTask("active");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        String output = captureOutput(() -> {
            diagnostics.logChildReconciliation(
                    root, null, candidate, false, true, true, true, false, active
            );
            diagnostics.logChildReconciliation(
                    root, null, candidate, false, true, true, true, false, active
            );
        });

        assertSame(state, bindings.stateFor(candidate));
        assertSame(state, bindings.stateFor(active));
        assertEquals(2, state.childReconciliationCount());
        assertEquals(0, state.routeState().childLifecycleSequence());
        assertEquals(0, state.routeState().childReplacementCount());
        assertEquals(1, occurrences(output, "event=STORE_TASK_CHILD_RECONCILIATION"));
        assertTrue(output.contains("reconciliationRole=ROOT_ROUTE"));
        assertTrue(output.contains("reconciliationOutcome=CHILD_REPLACED"));
        assertEquals(1, field(
                emissionGate.budgetSummaryFields(state.context().operationId()),
                "storeBudgetOperationDetailEmittedCount"
        ));
        assertTrue(String.valueOf(field(
                emissionGate.budgetSummaryFields(state.context().operationId()),
                "storeBudgetOperationSuppressedCounts"
        )).contains("STORE_TASK_CHILD_RECONCILIATION=1"));
        String event = lineContaining(output, "event=STORE_TASK_CHILD_RECONCILIATION");
        assertEquals(1, occurrences(event, "commandContextAvailable="));
        assertInOrder(
                event,
                "diagnosticScope=store_deposit_child_reconciliation",
                "lifecycleTaskRole=ROOT_STORE",
                "reconciliationRole=ROOT_ROUTE",
                "reconciliationOutcome=CHILD_REPLACED",
                "commandContextAvailable="
        );
    }

    @Test
    void preservesAllFiveOutcomeClassifications() {
        ChatClefDiagnostics.setBoundaryEnabled(true);

        assertOutcome(true, false, null, null, "ACTIVE_CHILD_CLEARED");
        assertOutcome(true, false, new TestTask("candidate"), new TestTask("active"), "CHILD_REPLACED");
        assertOutcome(false, true, new TestTask("candidate"), new TestTask("active"), "ACTIVE_CHILD_RETAINED");
        assertOutcome(false, false, null, null, "NULL_CHILD_RESULT");
        assertOutcome(false, false, new TestTask("candidate"), null, "CANDIDATE_NOT_INSTALLED");
    }

    @Test
    void observesPreviousChildStopBeforeMutatingTheChangedDepositAllRoute() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositChildReconciliationDiagnostics diagnostics = diagnostics(bindings);
        Task root = new TestTask("root");
        Task obtainChestChild = new TestTask("obtain-chest");
        Task openExistingChild = new TestTask("open-existing");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");

        state.routeState().recordParentDecision(
                "OBTAIN_CHEST", false, null, false, false, false, false,
                null, "NOT_EVALUATED_EARLY_GET_MISSING_TARGET", "0", null
        );
        diagnostics.logChildReconciliation(
                root, null, obtainChestChild, false, true, true, true, false, obtainChestChild
        );
        state.routeState().recordParentDecision(
                "OPEN_EXISTING", false, null, false, false, false, false,
                null, "NOT_EVALUATED_EARLY_GET_MISSING_TARGET", "0", null
        );
        state.recordLifecycle(
                obtainChestChild,
                "STOP",
                "END",
                false,
                ChatClefDiagnostics.currentClientTickId()
        );

        String output = captureOutput(() -> diagnostics.logChildReconciliation(
                root,
                obtainChestChild,
                openExistingChild,
                false,
                true,
                true,
                true,
                true,
                openExistingChild
        ));

        assertSame(state, bindings.stateFor(openExistingChild));
        assertTrue(state.routeState().isCurrentRouteChild(openExistingChild));
        assertTrue(output.contains("previousRouteChildStopObserved=true"));
        assertTrue(output.contains("resourceAcquisitionInterruptedByBranchChange=true"));
        assertTrue(output.contains("previousBranch=OBTAIN_CHEST"));
        assertTrue(output.contains("currentBranch=OPEN_EXISTING"));
    }

    @Test
    void offModeDoesNotBindOrMutateState() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositChildReconciliationDiagnostics diagnostics = diagnostics(bindings);
        Task root = new TestTask("root");
        Task child = new TestTask("child");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        String output = captureOutput(() -> diagnostics.logChildReconciliation(
                root, null, child, false, true, true, true, false, child
        ));

        assertEquals("", output.trim());
        assertEquals(0, state.childReconciliationCount());
        assertNull(bindings.stateFor(child));
    }

    @Test
    void enabledButUnboundParentIsANoOp() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositChildReconciliationDiagnostics diagnostics = diagnostics(bindings);
        Task parent = new TestTask("unbound-parent");
        Task child = new TestTask("child");

        String output = captureOutput(() -> diagnostics.logChildReconciliation(
                parent, null, child, false, true, true, true, false, child
        ));

        assertEquals("", output.trim());
        assertNull(bindings.stateFor(child));
    }

    private static void assertOutcome(boolean replacementApplied,
                                      boolean subTasksEqual,
                                      Task candidateChild,
                                      Task activeChildAfter,
                                      String expectedOutcome) {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositChildReconciliationDiagnostics diagnostics = diagnostics(bindings);
        Task root = new TestTask("root-" + expectedOutcome);
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        String output = captureOutput(() -> diagnostics.logChildReconciliation(
                root,
                null,
                candidateChild,
                subTasksEqual,
                true,
                true,
                replacementApplied,
                false,
                activeChildAfter
        ));

        assertEquals(1, state.childReconciliationCount());
        assertTrue(output.contains("reconciliationOutcome=" + expectedOutcome));
    }

    private static StoreDepositChildReconciliationDiagnostics diagnostics(StoreDepositBindingRegistry bindings) {
        return new StoreDepositChildReconciliationDiagnostics(bindings, new StoreDepositEmissionGate());
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

    private static Object field(Object[] fields, String key) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private static String lineContaining(String output, String token) {
        for (String line : output.split("\\R")) {
            if (line.contains(token)) {
                return line;
            }
        }
        throw new AssertionError("Missing output token: " + token);
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
