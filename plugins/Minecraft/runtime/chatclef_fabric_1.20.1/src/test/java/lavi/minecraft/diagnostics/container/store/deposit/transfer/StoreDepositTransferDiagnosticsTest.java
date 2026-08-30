package lavi.minecraft.diagnostics.container.store.deposit.transfer;

import adris.altoclef.tasksystem.Task;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Prove transfer observation ordering behind the unchanged StoreDepositDiagnostics facade.
class StoreDepositTransferDiagnosticsTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
    }

    @Test
    void offModeDoesNotMutateOrEmit() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositTransferDiagnostics diagnostics = diagnostics(bindings, new StoreDepositEmissionGate());
        Task root = new TestTask();
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");

        String output = captureOutput(() -> diagnostics.logTransferDecision(
                root,
                new BlockPos(1, 64, 2),
                null,
                1,
                true,
                true,
                true,
                "MOVE_TASK_SELECTED"
        ));

        assertEquals("", output.trim());
        assertEquals(0, state.transferDecisionCount());
        assertEquals(0, state.routeState().transferDecisionCount());
    }

    @Test
    void recordsEveryDepositAllObservationBeforeDeduplicatingTheEvent() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositTransferDiagnostics diagnostics = diagnostics(bindings, emissionGate);
        Task root = new TestTask();
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_ALL_COMMAND");
        BlockPos target = new BlockPos(-533, 50, 126);

        String output = captureOutput(() -> {
            diagnostics.logTransferDecision(root, target, null, 3, true, true, false, "MOVE_TASK_SELECTED");
            diagnostics.logTransferDecision(root, target, null, 3, true, true, false, "MOVE_TASK_SELECTED");
        });

        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_TRANSFER_DECISION"));
        assertEquals(1, occurrences(output, "commandContextAvailable="));
        assertEquals(2, state.transferDecisionCount());
        assertEquals(2, state.routeState().transferDecisionCount());
        assertEquals("{MOVE_TASK_SELECTED=2}", state.transferCounts());
        assertEquals(1, field(emissionGate.budgetSummaryFields(state.context().operationId()),
                "storeBudgetOperationDetailEmittedCount"));
        assertTrue(String.valueOf(field(
                emissionGate.budgetSummaryFields(state.context().operationId()),
                "storeBudgetOperationSuppressedCounts"
        )).contains("STORE_CONTAINER_TRANSFER_DECISION=1"));
        assertInOrder(
                output,
                "diagnosticScope=store_deposit_transfer",
                "owner=store_deposit_transfer_observer",
                "mode=BOUNDARY",
                "trigger=MOVE_TASK_SELECTED",
                "terminal=false",
                "behavior_effect=none",
                "transferAction=MOVE_TASK_SELECTED",
                "targetContainerPosition=-533,50,126",
                "targetItem=null",
                "potentialSourceSlotCount=3",
                "bestSourcePresent=true",
                "destinationSlotEvaluated=true",
                "destinationSlotPresent=false"
        );
    }

    @Test
    void plainDepositDoesNotAdvanceDepositAllRouteState() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositTransferDiagnostics diagnostics = diagnostics(bindings, new StoreDepositEmissionGate());
        Task root = new TestTask();
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        captureOutput(() -> diagnostics.logTransferDecision(
                root,
                new BlockPos(4, 65, 8),
                null,
                1,
                false,
                false,
                false,
                "NO_SOURCE_SLOT"
        ));

        assertEquals(1, state.transferDecisionCount());
        assertEquals(0, state.routeState().transferDecisionCount());
    }

    private static StoreDepositTransferDiagnostics diagnostics(StoreDepositBindingRegistry bindings,
                                                                StoreDepositEmissionGate emissionGate) {
        return new StoreDepositTransferDiagnostics(bindings, emissionGate);
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
            return "store-deposit-transfer-diagnostics-test";
        }
    }
}
