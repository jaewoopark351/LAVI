package lavi.minecraft.diagnostics.container.store.deposit.route;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Prove each route observer records state before applying its shared detail gate.
class StoreDepositRouteDiagnosticsTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void pursuitRecordsEveryDepositAllObservationBeforeDeduplication() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        StoreDepositPursuitDiagnostics diagnostics = new StoreDepositPursuitDiagnostics(bindings, gate);
        Task root = new TestTask("root");
        BlockPos target = new BlockPos(-533, 50, 126);
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");

        String output = captureOutput(() -> {
            diagnostics.logPursuitDecision(root, null, target, "PURSUIT_VALIDATION_ACCEPTED");
            diagnostics.logPursuitDecision(root, null, target, "PURSUIT_VALIDATION_ACCEPTED");
        });

        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_PURSUIT_DECISION"));
        assertEquals(2, state.pursuitDecisionCount());
        assertEquals("{PURSUIT_VALIDATION_ACCEPTED=2}", state.pursuitCounts());
        assertEquals(target, state.routeState().currentPursuit());
        assertEquals("PURSUIT_VALIDATION_ACCEPTED", state.routeState().currentPursuitAction());
        assertTrue(output.contains("routeCorrelationAvailable=true"));
        assertTrue(String.valueOf(field(
                gate.budgetSummaryFields(state.context().operationId()),
                "storeBudgetOperationSuppressedCounts"
        )).contains("STORE_CONTAINER_PURSUIT_DECISION=1"));
    }

    @Test
    void plainPursuitDoesNotMutateOrAppendDepositAllRouteState() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositPursuitDiagnostics diagnostics =
                new StoreDepositPursuitDiagnostics(bindings, new StoreDepositEmissionGate());
        Task root = new TestTask("root");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        String output = captureOutput(() -> diagnostics.logPursuitDecision(
                root, null, new BlockPos(1, 2, 3), "PURSUIT_VALIDATION_ACCEPTED"
        ));

        assertEquals(1, state.pursuitDecisionCount());
        assertEquals(null, state.routeState().currentPursuit());
        assertEquals("NONE", state.routeState().currentPursuitAction());
        assertFalse(output.contains("routeCorrelationAvailable="));
    }

    @Test
    void targetCallbackKeyIgnoresAncillaryFieldsButStillRecordsBothObservations() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        StoreDepositTargetCallbackDiagnostics diagnostics =
                new StoreDepositTargetCallbackDiagnostics(bindings, gate);
        Task root = new TestTask("root");
        BlockPos target = new BlockPos(4, 64, 8);
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");

        String output = captureOutput(() -> {
            diagnostics.logTargetCallbackDecision(root, target, null, false, true, new ItemTarget[0]);
            diagnostics.logTargetCallbackDecision(root, target, target, true, true, new ItemTarget[0]);
        });

        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_TARGET_CALLBACK_DECISION"));
        assertEquals(2, state.targetCallbackDecisionCount());
        assertEquals("{REFERENCE_CHANGED_PROGRESS_RESET=2}", state.targetCallbackCounts());
        assertTrue(output.contains("progressResetBecauseReferenceChanged=true"));
        assertTrue(output.contains("routeCorrelationAvailable=true"));
    }

    @Test
    void routeEventsRecordBeforeExactNameFilteringAndDeduplication() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        StoreDepositContainerRouteEventDiagnostics diagnostics =
                new StoreDepositContainerRouteEventDiagnostics(bindings, gate);
        Task root = new TestTask("root");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");
        Object[] branchFields = new Object[]{
                "decision", "OPEN_EXISTING",
                "costToWalk", 12.5,
                "costToMakeNew", 20.0,
                "nearestPresent", true,
                "nearestPosition", "4,64,8",
                "cachedContainerPosition", "none",
                "openTableTask", false
        };

        String output = captureOutput(() -> {
            diagnostics.observeContainerRouteEvent(
                    "CONTAINER_TASK_BRANCH_OBSERVED", "OPEN_EXISTING", root, new Object[0]
            );
            diagnostics.observeContainerRouteEvent(
                    "CONTAINER_TASK_TARGET_DECISION", "OPEN_EXISTING", root, branchFields
            );
            diagnostics.observeContainerRouteEvent(
                    "CONTAINER_TASK_TARGET_DECISION", "OPEN_EXISTING", root, branchFields
            );
        });

        assertEquals(3, state.craftRouteEventCount());
        assertEquals(1, occurrences(output, "event=STORE_CRAFT_ROUTE_EVALUATION_ENTERED"));
        assertTrue(state.craftRouteCounts().contains("CONTAINER_TASK_BRANCH_OBSERVED:OPEN_EXISTING=1"));
        assertTrue(state.craftRouteCounts().contains("CONTAINER_TASK_TARGET_DECISION:OPEN_EXISTING=2"));
        assertTrue(output.contains("decision=OPEN_EXISTING"));
        assertTrue(output.contains("costToWalk=12.5"));
        assertTrue(String.valueOf(field(
                gate.budgetSummaryFields(state.context().operationId()),
                "storeBudgetOperationSuppressedCounts"
        )).contains("STORE_CRAFT_ROUTE_EVALUATION_ENTERED=1"));
    }

    @Test
    void offModeLeavesEveryRouteObserverUnchanged() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate gate = new StoreDepositEmissionGate();
        StoreDepositPursuitDiagnostics pursuits = new StoreDepositPursuitDiagnostics(bindings, gate);
        StoreDepositTargetCallbackDiagnostics callbacks = new StoreDepositTargetCallbackDiagnostics(bindings, gate);
        StoreDepositContainerRouteEventDiagnostics events =
                new StoreDepositContainerRouteEventDiagnostics(bindings, gate);
        Task root = new TestTask("root");
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");

        String output = captureOutput(() -> {
            pursuits.logPursuitDecision(root, null, null, "NO_CANDIDATE");
            callbacks.logTargetCallbackDecision(root, null, null, false, false, new ItemTarget[0]);
            events.observeContainerRouteEvent(
                    "CONTAINER_TASK_TARGET_DECISION", "NONE", root, new Object[0]
            );
        });

        assertEquals("", output.trim());
        assertEquals(0, state.pursuitDecisionCount());
        assertEquals(0, state.targetCallbackDecisionCount());
        assertEquals(0, state.craftRouteEventCount());
    }

    private static Object field(Object[] fields, String key) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field: " + key);
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
