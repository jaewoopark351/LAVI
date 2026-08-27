package lavi.minecraft.task.container.home.command;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

//20260827_kpopmodder: Added focused tests for one-request/one-UserTask command semantics.
class StoreHomeCommandTest {
    @Test
    void acceptedCommandCreatesExactlyOneCurrentUserTask() throws Exception {
        RecordingAltoClef mod = new RecordingAltoClef();
        DummyTask expected = new DummyTask();
        AtomicInteger creations = new AtomicInteger();
        StoreHomeCommand command = new StoreHomeCommand(
                () -> {
                    creations.incrementAndGet();
                    return expected;
                },
                ignored -> true
        );

        command.run(mod, StoreHomeCommand.COMMAND_NAME, () -> { });

        assertEquals(1, creations.get());
        assertEquals(1, mod.runCount);
        assertSame(expected, mod.task);
    }

    @Test
    void cursorGateRejectionCreatesNoTask() throws Exception {
        RecordingAltoClef mod = new RecordingAltoClef();
        AtomicInteger creations = new AtomicInteger();
        StoreHomeCommand command = new StoreHomeCommand(
                () -> {
                    creations.incrementAndGet();
                    return new DummyTask();
                },
                ignored -> false
        );

        command.run(mod, StoreHomeCommand.COMMAND_NAME, () -> { });

        assertEquals(0, creations.get());
        assertEquals(0, mod.runCount);
    }

    private static final class RecordingAltoClef extends AltoClef {
        private int runCount;
        private Task task;

        @Override
        public void runUserTask(Task task, Runnable onFinish) {
            runCount++;
            this.task = task;
        }
    }

    private static final class DummyTask extends Task {
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
            return "store-home-command-test";
        }
    }
}
