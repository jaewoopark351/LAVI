package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import net.minecraft.client.network.ClientPlayerEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllInventoryPressureChainLifecycleTest {
    @Test
    void naturalFinishStopsTheOwnedRootAndChildBeforeWaitingForRearm() {
        HeadlessAltoClef mod = new HeadlessAltoClef();
        TrackingTaskRunner runner = new TrackingTaskRunner(mod);
        DepositAllInventoryPressureChain chain = new DepositAllInventoryPressureChain(runner);
        TrackingTask child = new TrackingTask(null);
        TrackingTask root = new TrackingTask(child);

        moveToRunning(chain);
        chain.setTask(root);
        chain.tick();

        assertEquals(1, root.tracker.startCalls);
        assertEquals(1, child.tracker.startCalls);
        assertTrue(root.isActive());
        assertTrue(child.isActive());

        root.finished = true;
        chain.tick();

        assertEquals(1, root.stopCallbacks);
        assertEquals(1, child.stopCallbacks);
        assertEquals(1, root.tracker.stopCalls);
        assertEquals(1, child.tracker.stopCalls);
        assertTrue(root.stopped());
        assertTrue(child.stopped());
        assertNull(chain.getCurrentTask());
        assertEquals(DepositAllInventoryPressureState.WAIT_FOR_REARM, stateMachine(chain).state());
        assertEquals(0, runner.enableCalls);
        assertEquals(0, runner.disableCalls);
        assertFalse(runner.isActive());
    }

    private static void moveToRunning(DepositAllInventoryPressureChain chain) {
        DepositAllInventoryPressureStateMachine machine = stateMachine(chain);
        assertEquals(
                DepositAllInventoryPressureSignal.THRESHOLD_REACHED,
                machine.observe(new DepositAllInventoryPressureSnapshot(29, 36))
        );
        machine.markRunStarted();
    }

    private static DepositAllInventoryPressureStateMachine stateMachine(
            DepositAllInventoryPressureChain chain) {
        try {
            Field field = DepositAllInventoryPressureChain.class.getDeclaredField("stateMachine");
            field.setAccessible(true);
            return (DepositAllInventoryPressureStateMachine) field.get(chain);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect automatic deposit_all state", exception);
        }
    }

    private static final class HeadlessAltoClef extends AltoClef {
        @Override
        public ClientPlayerEntity getPlayer() {
            return null;
        }
    }

    private static final class TrackingTaskRunner extends TaskRunner {
        private int enableCalls;
        private int disableCalls;

        private TrackingTaskRunner(AltoClef mod) {
            super(mod);
        }

        @Override
        public void enable() {
            enableCalls++;
        }

        @Override
        public void disable() {
            disableCalls++;
        }
    }

    private static final class TrackingTask extends Task {
        private final Task child;
        private final CountingStoredTracker tracker = new CountingStoredTracker();
        private boolean finished;
        private int stopCallbacks;

        private TrackingTask(Task child) {
            this.child = child;
        }

        @Override
        protected void onStart() {
            tracker.startTracking();
        }

        @Override
        protected Task onTick() {
            return child;
        }

        @Override
        protected void onStop(Task interruptTask) {
            stopCallbacks++;
            tracker.stopTracking();
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "automatic deposit_all lifecycle test task";
        }
    }

    private static final class CountingStoredTracker extends ContainerStoredTracker {
        private int startCalls;
        private int stopCalls;

        private CountingStoredTracker() {
            super(slot -> true);
        }

        @Override
        public void startTracking() {
            startCalls++;
        }

        @Override
        public void stopTracking() {
            stopCalls++;
        }
    }
}
