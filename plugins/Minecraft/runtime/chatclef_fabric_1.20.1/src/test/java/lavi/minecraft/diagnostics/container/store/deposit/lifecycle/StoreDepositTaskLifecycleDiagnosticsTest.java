package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositTerminalSummaryEmitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Prove lifecycle emission precedes terminal handoff and descendant stops stay nonterminal.
class StoreDepositTaskLifecycleDiagnosticsTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void rootStopEndEmitsThenTerminalizesAndPurgesOnce() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        StoreDepositTaskLifecycleDiagnostics diagnostics = diagnostics(bindings, gate);
        Task root = new TestTask("root");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        String output = captureOutput(() -> {
            diagnostics.logTaskLifecycleBoundary(root, null, "STOP", "BEGIN", true);
            diagnostics.logTaskLifecycleBoundary(root, null, "STOP", "END", true);
            diagnostics.logTaskLifecycleBoundary(root, null, "STOP", "END", true);
        });

        assertEquals(2, state.lifecycleEventCount());
        assertTrue(state.trueStopObserved());
        assertNull(bindings.stateFor(root));
        assertEquals(2, occurrences(output, "event=STORE_TASK_LIFECYCLE_BOUNDARY"));
        assertEquals(1, occurrences(output, "event=STORE_DEPOSIT_TERMINAL_SUMMARY"));
        assertInOrder(
                output,
                "lifecycleAction=STOP lifecyclePhase=BEGIN",
                "lifecycleAction=STOP lifecyclePhase=END",
                "event=STORE_DEPOSIT_TERMINAL_SUMMARY",
                "event=STORE_DEPOSIT_EFFECT_SUMMARY",
                "event=STORE_BARITONE_OPERATION_SUMMARY",
                "event=STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY"
        );
    }

    @Test
    void descendantStopEndRecordsLifecycleWithoutPurgingTheRoot() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositTaskLifecycleDiagnostics diagnostics = diagnostics(bindings, new StoreDepositEmissionGate());
        Task root = new TestTask("root");
        Task child = new TestTask("child");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");
        bindings.bindChild(root, child);

        String output = captureOutput(() -> diagnostics.logTaskLifecycleBoundary(
                child, null, "STOP", "END", true
        ));

        assertSame(state, bindings.stateFor(root));
        assertEquals(1, state.lifecycleEventCount());
        assertEquals(1, occurrences(output, "event=STORE_TASK_LIFECYCLE_BOUNDARY"));
        assertEquals(0, occurrences(output, "event=STORE_DEPOSIT_TERMINAL_SUMMARY"));
    }

    @Test
    void naturalFinishIsRootOnlyUngatedAndPurgesAfterItsLifecycleEvent() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositTaskLifecycleDiagnostics diagnostics = diagnostics(bindings, new StoreDepositEmissionGate());
        Task root = new TestTask("root");
        Task child = new TestTask("child");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");
        bindings.bindChild(root, child);

        assertEquals("", captureOutput(() -> diagnostics.logNaturalFinish(child)).trim());
        String output = captureOutput(() -> diagnostics.logNaturalFinish(root));

        assertTrue(state.naturalFinishObserved());
        assertEquals(0, state.lifecycleEventCount());
        assertNull(bindings.stateFor(root));
        assertInOrder(
                output,
                "reason=store_task_natural_finish_observed",
                "event=STORE_DEPOSIT_TERMINAL_SUMMARY",
                "event=STORE_DEPOSIT_EFFECT_SUMMARY",
                "event=STORE_BARITONE_OPERATION_SUMMARY",
                "event=STORE_DEPOSIT_DIAGNOSTIC_COVERAGE_SUMMARY"
        );
    }

    @Test
    void offModeDoesNotMutateLifecycleState() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositTaskLifecycleDiagnostics diagnostics = diagnostics(bindings, new StoreDepositEmissionGate());
        Task root = new TestTask("root");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        String output = captureOutput(() -> diagnostics.logTaskLifecycleBoundary(
                root, null, "STOP", "END", true
        ));

        assertEquals("", output.trim());
        assertEquals(0, state.lifecycleEventCount());
        assertSame(state, bindings.stateFor(root));
    }

    private static StoreDepositTaskLifecycleDiagnostics diagnostics(StoreDepositBindingRegistry bindings,
                                                                    StoreDepositEmissionGate gate) {
        return new StoreDepositTaskLifecycleDiagnostics(
                bindings,
                gate,
                new StoreDepositTerminalSummaryEmitter(bindings, gate)
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
