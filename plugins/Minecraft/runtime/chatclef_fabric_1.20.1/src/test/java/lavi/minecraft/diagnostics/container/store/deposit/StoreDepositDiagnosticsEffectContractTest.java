package lavi.minecraft.diagnostics.container.store.deposit;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Characterize predicate-to-effect correlation before extracting it from the facade.
class StoreDepositDiagnosticsEffectContractTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        StoreDepositDiagnostics.clearPredicateSnapshot();
    }

    @Test
    void consumesOnePredicateSnapshotAndKeepsEffectAccountingOnTheSameOperation() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask();
        ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
        BlockPos target = new BlockPos(12, 64, -7);

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.registerBareDepositInvocation(
                    null,
                    false,
                    new ItemTarget[0],
                    root,
                    "BARE_DEPOSIT_COMMAND"
            );
            StoreDepositDiagnostics.onStoreRootStart(root, false, new ItemTarget[0]);
            StoreDepositDiagnostics.bindTargetTracker(root, tracker, target);
            StoreDepositDiagnostics.observeTargetContainerPredicate(
                    tracker,
                    null,
                    target,
                    Optional.of(target),
                    true
            );
            StoreDepositDiagnostics.logEffectObservation(
                    tracker,
                    null,
                    null,
                    null,
                    false,
                    true,
                    true
            );
            StoreDepositDiagnostics.logEffectObservation(
                    tracker,
                    null,
                    null,
                    null,
                    false,
                    true,
                    false
            );
            StoreDepositDiagnostics.logNaturalFinish(root);
        });

        assertEquals(2, occurrences(output, "event=STORE_CONTAINER_EFFECT_OBSERVATION"));
        assertTrue(output.contains("predicateMatchReason=MATCH"));
        assertTrue(output.contains("predicateLastInteractionPosition=12,64,-7"));
        assertTrue(output.contains("predicateMatchReason=UNAVAILABLE"));
        assertTrue(output.contains("observationOutcome=ACCEPT_ZERO_DELTA"));
        assertTrue(output.contains("observationOutcome=REJECT_TARGET_PREDICATE"));
        assertTrue(output.contains("effectObservationCount=2"));
        assertTrue(output.contains("expectedPositiveEffectCount=0"));
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
            return "store-deposit-effect-contract-test";
        }
    }
}
