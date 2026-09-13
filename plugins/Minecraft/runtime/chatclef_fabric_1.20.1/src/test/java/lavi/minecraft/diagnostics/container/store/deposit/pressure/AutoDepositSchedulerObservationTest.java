package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260913_kpopmodder: Diagnostic observation must not reevaluate priority or alter safety preemption/exception propagation.
class AutoDepositSchedulerObservationTest {
    @AfterEach
    void resetDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void priorityAndActiveAreEvaluatedOnceAndSafetyKeepsItsOriginalSelection() throws Exception {
        for (boolean enabled : new boolean[]{false, true}) {
            ChatClefDiagnostics.setBoundaryEnabled(enabled);
            try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.inGame()) {
                TaskRunner runner = activeRunner();
                RecordingChain user = new RecordingChain(runner, true, 50);
                RecordingChain safety = new RecordingChain(runner, true, 100);
                RecordingChain inactive = new RecordingChain(runner, false, 1000);
                runner.tick();
                assertSame(safety, runner.getCurrentTaskChain());
                assertEquals(1, user.activeCalls);
                assertEquals(1, user.priorityCalls);
                assertEquals(1, safety.activeCalls);
                assertEquals(1, safety.priorityCalls);
                assertEquals(1, safety.tickCalls);
                assertEquals(0, user.tickCalls);
                assertEquals(1, inactive.activeCalls);
                assertEquals(0, inactive.priorityCalls);
                safety.active = false;
                runner.tick();
                assertSame(user, runner.getCurrentTaskChain());
                assertEquals(1, safety.interruptCalls);
                assertEquals(2, user.priorityCalls);
                assertEquals(1, safety.priorityCalls);
            }
        }
    }

    @Test
    void originalPriorityExceptionStillPropagates() throws Exception {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.inGame()) {
            TaskRunner runner = activeRunner();
            RecordingChain chain = new RecordingChain(runner, true, 100);
            chain.throwFromPriority = true;
            assertThrows(IllegalStateException.class, runner::tick);
            assertEquals(1, chain.priorityCalls);
            assertEquals(0, chain.tickCalls);
        }
    }

    private static TaskRunner activeRunner() throws Exception {
        TaskRunner runner = new TaskRunner(new AltoClef());
        Field active = TaskRunner.class.getDeclaredField("active");
        active.setAccessible(true);
        active.setBoolean(runner, true);
        return runner;
    }

    private static final class RecordingChain extends TaskChain {
        private boolean active;
        private final float priority;
        private int activeCalls;
        private int priorityCalls;
        private int tickCalls;
        private int interruptCalls;
        private boolean throwFromPriority;

        private RecordingChain(TaskRunner runner, boolean active, float priority) {
            super(runner);
            this.active = active;
            this.priority = priority;
        }

        @Override public boolean isActive() { activeCalls++; return active; }
        @Override public float getPriority() {
            priorityCalls++;
            if (throwFromPriority) throw new IllegalStateException("original priority failure");
            return priority;
        }
        @Override protected void onTick() { tickCalls++; }
        @Override protected void onStop() { }
        @Override public void onInterrupt(TaskChain other) { interruptCalls++; }
        @Override public String getName() { return "recording-chain"; }
    }
}
