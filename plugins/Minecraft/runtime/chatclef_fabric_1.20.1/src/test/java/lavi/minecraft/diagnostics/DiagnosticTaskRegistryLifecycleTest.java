package lavi.minecraft.diagnostics;

import adris.altoclef.tasksystem.Task;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Prove task diagnostic identities and cross-thread stacks do not revive after OFF.
class DiagnosticTaskRegistryLifecycleTest {
    @Test
    void modeEpochInvalidatesATaskStackStoredOnAnotherThread() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            DiagnosticTaskRegistry registry = new DiagnosticTaskRegistry();
            Task task = new TestTask("cross-thread");
            CountDownLatch taskStored = new CountDownLatch(1);
            CountDownLatch continueAfterClear = new CountDownLatch(1);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<Task> result = executor.submit(() -> {
                    registry.enterTask(task);
                    taskStored.countDown();
                    await(continueAfterClear);
                    return registry.currentTask();
                });

                await(taskStored);
                registry.clearForModeTransition();
                continueAfterClear.countDown();

                assertNull(result.get());
            } finally {
                continueAfterClear.countDown();
                executor.shutdownNow();
            }
        });
    }

    @Test
    void clearDropsOldIdentityBindingsWithoutReusingTheirIdentifiers() {
        DiagnosticTaskRegistry registry = new DiagnosticTaskRegistry();
        Task first = new TestTask("first");
        Task second = new TestTask("second");
        long firstInstanceId = registry.instanceId(first);
        registry.createRunId(first);

        registry.clearForModeTransition();

        assertEquals(-1L, registry.existingRunId(first));
        assertTrue(registry.instanceId(second) > firstInstanceId);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
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
