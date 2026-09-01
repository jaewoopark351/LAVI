package lavi.minecraft.diagnostics.container.store.deposit.effect;

import adris.altoclef.tasks.container.ContainerStoredTracker;
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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Prove single-consumption predicate correlation and pre-dedupe effect accounting.
class StoreDepositEffectDiagnosticsTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void consumesThePredicateSnapshotExactlyOnceAndPreservesOrderedEffectFields() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEffectDiagnostics diagnostics = diagnostics(bindings, new StoreDepositEmissionGate());
        Task root = new TestTask();
        ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
        BlockPos target = new BlockPos(12, 64, -7);
        bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");
        bindings.bindTracker(root, tracker, "TARGET_CONTAINER", target);

        diagnostics.observeTargetContainerPredicate(tracker, null, target, Optional.of(target), true);
        String output = captureOutput(() -> {
            diagnostics.logEffectObservation(tracker, null, null, null, false, true, true);
            diagnostics.logEffectObservation(tracker, null, null, null, false, true, false);
        });

        assertEquals(2, occurrences(output, "event=STORE_CONTAINER_EFFECT_OBSERVATION"));
        assertTrue(output.contains("predicateMatchReason=MATCH"));
        assertTrue(output.contains("predicateLastInteractionPosition=12,64,-7"));
        assertTrue(output.contains("predicateMatchReason=UNAVAILABLE"));
        assertInOrder(
                firstEvent(output),
                "diagnosticScope=store_deposit_effect",
                "owner=store_deposit_effect_observer",
                "mode=BOUNDARY",
                "trigger=ACCEPT_ZERO_DELTA",
                "terminal=false",
                "behavior_effect=none",
                "trackerRole=TARGET_CONTAINER",
                "expectedContainerPosition=12,64,-7",
                "slotStackBefore=none",
                "slotStackAfter=none",
                "predicateMatchReason=MATCH",
                "observationOutcome=ACCEPT_ZERO_DELTA",
                "beforeItem=empty",
                "beforeCount=0",
                "afterItem=empty",
                "afterCount=0",
                "deltaComponentCount=0"
        );
    }

    @Test
    void recordsEveryObservationBeforeDeduplicatingItsEvent() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEmissionGate emissionGate = new StoreDepositEmissionGate();
        StoreDepositEffectDiagnostics diagnostics = diagnostics(bindings, emissionGate);
        Task root = new TestTask();
        ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");
        bindings.bindTracker(root, tracker, "TARGET_CONTAINER", new BlockPos(1, 2, 3));

        String output = captureOutput(() -> {
            diagnostics.logEffectObservation(tracker, null, null, null, false, true, true);
            diagnostics.logEffectObservation(tracker, null, null, null, false, true, true);
        });

        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_EFFECT_OBSERVATION"));
        assertEquals(2, state.effectObservationCount());
        assertEquals(0, state.expectedPositiveEffectCount());
        assertEquals("{ACCEPT_ZERO_DELTA=2}", state.effectCounts());
        assertEquals(1, field(emissionGate.budgetSummaryFields(state.context().operationId()),
                "storeBudgetOperationDetailEmittedCount"));
        assertTrue(String.valueOf(field(
                emissionGate.budgetSummaryFields(state.context().operationId()),
                "storeBudgetOperationSuppressedCounts"
        )).contains("STORE_CONTAINER_EFFECT_OBSERVATION=1"));
    }

    @Test
    void offAndUnboundPathsConsumePendingPredicateStateWithoutMutation() {
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEffectDiagnostics diagnostics = diagnostics(bindings, new StoreDepositEmissionGate());
        Task root = new TestTask();
        ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
        BlockPos target = new BlockPos(8, 70, 9);
        StoreDepositOperationState state = bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");

        diagnostics.observeTargetContainerPredicate(tracker, null, target, Optional.of(target), true);
        assertEquals("", captureOutput(() -> diagnostics.logEffectObservation(
                tracker, null, null, null, false, true, true
        )).trim());
        assertEquals(0, state.effectObservationCount());

        ChatClefDiagnostics.setBoundaryEnabled(true);
        diagnostics.observeTargetContainerPredicate(tracker, null, target, Optional.of(target), true);
        assertEquals("", captureOutput(() -> diagnostics.logEffectObservation(
                tracker, null, null, null, false, true, true
        )).trim());

        bindings.bindTracker(root, tracker, "TARGET_CONTAINER", target);
        String output = captureOutput(() -> diagnostics.logEffectObservation(
                tracker, null, null, null, false, true, false
        ));
        assertTrue(output.contains("predicateMatchReason=UNAVAILABLE"));
        assertEquals(1, state.effectObservationCount());
    }

    @Test
    void modeTransitionInvalidatesAPreOffPredicateOwnedByAnotherThread() throws Exception {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        StoreDepositBindingRegistry bindings = new StoreDepositBindingRegistry();
        StoreDepositEffectDiagnostics diagnostics = diagnostics(
                bindings,
                new StoreDepositEmissionGate()
        );
        Task root = new TestTask();
        ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
        BlockPos target = new BlockPos(4, 65, 7);
        bindings.activateRoot(root, "BARE_DEPOSIT_COMMAND");
        bindings.bindTracker(root, tracker, "TARGET_CONTAINER", target);
        CountDownLatch predicateRecorded = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        AtomicReference<String> output = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            diagnostics.observeTargetContainerPredicate(
                    tracker,
                    null,
                    target,
                    Optional.of(target),
                    true
            );
            predicateRecorded.countDown();
            await(resume);
            output.set(captureOutput(() -> diagnostics.logEffectObservation(
                    tracker,
                    null,
                    null,
                    null,
                    false,
                    true,
                    true
            )));
        });

        worker.start();
        predicateRecorded.await();
        diagnostics.clearForModeTransition();
        resume.countDown();
        worker.join();

        assertTrue(output.get().contains("predicateMatchReason=UNAVAILABLE"));
        assertTrue(!output.get().contains("predicateMatchReason=MATCH"));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static StoreDepositEffectDiagnostics diagnostics(StoreDepositBindingRegistry bindings,
                                                              StoreDepositEmissionGate emissionGate) {
        return new StoreDepositEffectDiagnostics(bindings, emissionGate);
    }

    private static Object field(Object[] fields, String key) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private static String firstEvent(String output) {
        int newline = output.indexOf(System.lineSeparator());
        return newline < 0 ? output : output.substring(0, newline);
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
            return "store-deposit-effect-diagnostics-test";
        }
    }
}
