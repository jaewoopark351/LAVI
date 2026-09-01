package lavi.minecraft.diagnostics.container.store.deposit;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Characterize transfer state accounting before extracting it from the compatibility facade.
class StoreDepositDiagnosticsTransferContractTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void recordsBothDepositAllTransferObservationsBeforeSuppressingTheDuplicateEvent() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask();
        BlockPos targetContainer = new BlockPos(-533, 50, 126);

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.registerBareDepositInvocation(
                    null,
                    false,
                    new ItemTarget[0],
                    root,
                    "BARE_DEPOSIT_ALL_COMMAND"
            );
            StoreDepositDiagnostics.onStoreRootStart(root, false, new ItemTarget[0]);
            StoreDepositDiagnostics.logTransferDecision(
                    root,
                    targetContainer,
                    null,
                    3,
                    true,
                    true,
                    false,
                    "MOVE_TASK_SELECTED"
            );
            StoreDepositDiagnostics.logTransferDecision(
                    root,
                    targetContainer,
                    null,
                    3,
                    true,
                    true,
                    false,
                    "MOVE_TASK_SELECTED"
            );
            StoreDepositDiagnostics.logNaturalFinish(root);
        });

        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_TRANSFER_DECISION"));
        assertTrue(output.contains("reason=store_container_transfer_decision"));
        assertTrue(output.contains("targetContainerPosition=-533,50,126"));
        assertTrue(output.contains("transferAction=MOVE_TASK_SELECTED"));
        assertTrue(output.contains("potentialSourceSlotCount=3"));
        assertTrue(output.contains("bestSourcePresent=true"));
        assertTrue(output.contains("destinationSlotEvaluated=true"));
        assertTrue(output.contains("destinationSlotPresent=false"));
        assertTrue(output.contains("transferDecisionCount=2"));
        assertTrue(output.contains("routeTransferDecisionCount=2"));
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
            return "store-deposit-transfer-contract-test";
        }
    }
}
