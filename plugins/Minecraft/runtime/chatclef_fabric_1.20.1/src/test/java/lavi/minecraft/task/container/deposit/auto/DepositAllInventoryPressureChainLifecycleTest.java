package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.player2api.AICommandBridge;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.client.network.ClientPlayerEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Prove automatic-owned cleanup at terminal, safety, world-leave, and disable boundaries.
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

    @Test
    void safetyInterruptionStopsOnlyAnActiveOwnedTreeExactlyOnce() {
        HeadlessAltoClef mod = new HeadlessAltoClef();
        TrackingTaskRunner runner = new TrackingTaskRunner(mod);
        DepositAllInventoryPressureChain chain = new DepositAllInventoryPressureChain(runner);
        TrackingTask child = new TrackingTask(null);
        TrackingTask root = new TrackingTask(child);
        TrackingInterruptingChain safety = new TrackingInterruptingChain(runner);

        moveToRunning(chain);
        chain.setTask(root);
        chain.tick();

        chain.onInterrupt(safety);
        chain.onInterrupt(safety);

        assertEquals(1, root.stopCallbacks);
        assertEquals(1, child.stopCallbacks);
        assertTrue(root.stopped());
        assertTrue(child.stopped());
        assertNull(chain.getCurrentTask());
        assertEquals(DepositAllInventoryPressureState.WAIT_FOR_REARM, stateMachine(chain).state());
        assertEquals(0, runner.disableCalls);
    }

    @Test
    void safetyInterruptionDetachesANeverTickedOwnedRootWithoutInventingAStopCallback() {
        HeadlessAltoClef mod = new HeadlessAltoClef();
        TrackingTaskRunner runner = new TrackingTaskRunner(mod);
        DepositAllInventoryPressureChain chain = new DepositAllInventoryPressureChain(runner);
        TrackingTask root = new TrackingTask(null);
        TrackingInterruptingChain safety = new TrackingInterruptingChain(runner);

        moveToRunning(chain);
        chain.setTask(root);

        chain.onInterrupt(safety);

        assertEquals(0, root.stopCallbacks);
        assertFalse(root.isActive());
        assertNull(chain.getCurrentTask());
        assertEquals(DepositAllInventoryPressureState.WAIT_FOR_REARM, stateMachine(chain).state());
        assertEquals(0, runner.disableCalls);
    }

    @Test
    void inactiveRunnerIsEnabledOnceAtAutomaticStartAndNeverDisabledByOwnedTermination() {
        HeadlessAltoClef mod = new HeadlessAltoClef();
        TrackingTaskRunner runner = new TrackingTaskRunner(mod);
        DepositAllInventoryPressureChain chain = new DepositAllInventoryPressureChain(runner);
        TrackingTask root = new TrackingTask(null);
        TrackingInterruptingChain safety = new TrackingInterruptingChain(runner);
        DepositAllInventoryPressureSnapshot pressure =
                new DepositAllInventoryPressureSnapshot(33, 36);

        assertEquals(
                DepositAllInventoryPressureSignal.THRESHOLD_REACHED,
                stateMachine(chain).observe(pressure)
        );
        invokeStartTask(chain, pressure, root);

        assertEquals(1, runner.enableCalls);
        assertEquals(0, runner.disableCalls);
        assertEquals(DepositAllInventoryPressureState.RUNNING, stateMachine(chain).state());
        assertEquals(root, chain.getCurrentTask());

        chain.onInterrupt(safety);
        chain.onInterrupt(safety);

        assertEquals(1, runner.enableCalls);
        assertEquals(0, runner.disableCalls);
        assertEquals(0, root.stopCallbacks);
        assertNull(chain.getCurrentTask());
        assertEquals(
                DepositAllInventoryPressureState.WAIT_FOR_REARM,
                stateMachine(chain).state()
        );
    }

    @Test
    void worldLeaveStopsTheActiveAutomaticTreeExactlyOnceWithoutDisablingTheRunner() {
        try (HeadlessMinecraftClientSession ignored =
                     HeadlessMinecraftClientSession.outOfGame()) {
            HeadlessAltoClef mod = new HeadlessAltoClef();
            TrackingTaskRunner runner = new TrackingTaskRunner(mod);
            DepositAllInventoryPressureChain chain = new DepositAllInventoryPressureChain(runner);
            TrackingTask child = new TrackingTask(null);
            TrackingTask root = new TrackingTask(child);
            moveToRunning(chain);
            chain.setTask(root);
            chain.tick();

            chain.onEndClientTick();
            chain.onEndClientTick();

            assertEquals(1, root.stopCallbacks);
            assertEquals(1, child.stopCallbacks);
            assertNull(chain.getCurrentTask());
            assertEquals(
                    DepositAllInventoryPressureState.WAIT_FOR_REARM,
                    stateMachine(chain).state()
            );
            assertEquals(0, runner.disableCalls);
        }
    }

    @Test
    void chatClefDisableStopsTheActiveAutomaticTreeExactlyOnceWithoutGlobalDisable() {
        try (HeadlessMinecraftClientSession ignored =
                     HeadlessMinecraftClientSession.inGame()) {
            HeadlessAltoClef mod = new HeadlessAltoClef();
            mod.bridge = TestObjects.allocate(AICommandBridge.class);
            TrackingTaskRunner runner = new TrackingTaskRunner(mod);
            DepositAllInventoryPressureChain chain = new DepositAllInventoryPressureChain(runner);
            TrackingTask child = new TrackingTask(null);
            TrackingTask root = new TrackingTask(child);
            moveToRunning(chain);
            chain.setTask(root);
            chain.tick();

            chain.onEndClientTick();
            chain.onEndClientTick();

            assertEquals(1, root.stopCallbacks);
            assertEquals(1, child.stopCallbacks);
            assertNull(chain.getCurrentTask());
            assertEquals(
                    DepositAllInventoryPressureState.WAIT_FOR_REARM,
                    stateMachine(chain).state()
            );
            assertEquals(0, runner.disableCalls);
        }
    }

    private static void moveToRunning(DepositAllInventoryPressureChain chain) {
        DepositAllInventoryPressureStateMachine machine = stateMachine(chain);
        assertEquals(
                DepositAllInventoryPressureSignal.THRESHOLD_REACHED,
                machine.observe(new DepositAllInventoryPressureSnapshot(33, 36))
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

    private static void invokeStartTask(
            DepositAllInventoryPressureChain chain,
            DepositAllInventoryPressureSnapshot pressure,
            Task task) {
        try {
            Method method = DepositAllInventoryPressureChain.class.getDeclaredMethod(
                    "startTask",
                    DepositAllInventoryPressureSnapshot.class,
                    ItemTarget[].class,
                    Task.class
            );
            method.setAccessible(true);
            method.invoke(chain, pressure, new ItemTarget[0], task);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError("Failed to invoke automatic start boundary", exception);
        } catch (InvocationTargetException exception) {
            throw new AssertionError(
                    "Automatic start boundary failed",
                    exception.getCause()
            );
        }
    }

    private static final class HeadlessAltoClef extends AltoClef {
        private AICommandBridge bridge;

        @Override
        public ClientPlayerEntity getPlayer() {
            return null;
        }

        @Override
        public AICommandBridge getAiBridge() {
            return bridge;
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

    private static final class TrackingInterruptingChain
            extends adris.altoclef.tasksystem.TaskChain {
        private TrackingInterruptingChain(TaskRunner runner) {
            super(runner);
        }

        @Override
        protected void onStop() {
        }

        @Override
        public void onInterrupt(adris.altoclef.tasksystem.TaskChain other) {
        }

        @Override
        protected void onTick() {
        }

        @Override
        public float getPriority() {
            return 100.0f;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public String getName() {
            return "automatic deposit_all safety test chain";
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
