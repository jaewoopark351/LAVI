package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import lavi.minecraft.task.container.deposit.DepositAllContainerEligibility;
import lavi.minecraft.task.container.deposit.DepositAllContainerTargetState;
import lavi.minecraft.task.container.deposit.DepositAllStoreTaskGeneration;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllTaskLifecycleReconciliationTest {
    @Test
    void preservesTheActualStoreChildAcrossInterruptResumeAndRefreshesOnlyAtATerminalBoundary() {
        Fixture fixture = new Fixture();
        LifecycleDepositAllTask parent = newTestSubject(fixture);
        ResumableTaskChain chain = new ResumableTaskChain();

        try {
            chain.setTask(parent);
            chain.tick();
            StoreTaskProbe first = fixture.createdTasks.get(0);

            assertSame(first, actualChild(parent));
            assertEquals(1, fixture.generation.generationId());
            assertEquals(1, fixture.createdTasks.size());

            chain.onInterrupt(null);

            assertFalse(first.stopped());
            assertTrue(first.isActive());
            assertEquals(1, first.stopCallbacks);
            assertEquals(1, fixture.rootTracker.stopCalls);

            chain.tick();

            assertSame(first, actualChild(parent));
            assertSame(first, fixture.generation.getOrCreate(
                    fixture.target,
                    false,
                    fixture.initialSnapshot
            ));
            assertEquals(1, fixture.generation.generationId());
            assertEquals(1, fixture.createdTasks.size());
            assertEquals(1, first.stopCallbacks);
            assertFalse(first.stopped());
            assertEquals(2, fixture.rootTracker.startCalls);

            first.finished = true;
            parent.remainingWork = targets(ItemTarget.EMPTY, ItemTarget.EMPTY);
            chain.tick();

            StoreTaskProbe second = fixture.createdTasks.get(1);
            assertSame(second, actualChild(parent));
            assertEquals(2, fixture.generation.generationId());
            assertEquals(2, fixture.createdTasks.size());
            assertTrue(first.stopped());
            assertEquals(2, first.stopCallbacks);
        } finally {
            chain.setTask(null);
        }
    }

    private static LifecycleDepositAllTask newTestSubject(Fixture fixture) {
        try {
            LifecycleDepositAllTask task = allocate(LifecycleDepositAllTask.class);
            initializeTaskBase(task);
            task.generation = fixture.generation;
            task.target = fixture.target;
            task.remainingWork = fixture.initialSnapshot;

            setField(task, DepositAllTask.class, "_toStore", fixture.initialSnapshot);
            setField(task, DepositAllTask.class, "_getIfNotPresent", false);
            setField(task, DepositAllTask.class, "_containerEligibility", new DepositAllContainerEligibility());
            setField(task, DepositAllTask.class, "_targetState", new DepositAllContainerTargetState());
            setField(task, DepositAllTask.class, "_storeTaskGeneration", fixture.generation);
            setField(task, DepositAllTask.class, "_progressChecker", allocate(NoOpMovementProgressChecker.class));
            setField(task, DepositAllTask.class, "_storedItems", fixture.rootTracker);
            return task;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create headless DepositAllTask lifecycle fixture", exception);
        }
    }

    private static Task actualChild(Task parent) {
        try {
            Field field = Task.class.getDeclaredField("sub");
            field.setAccessible(true);
            return (Task) field.get(parent);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to inspect the scheduler-owned child", exception);
        }
    }

    private static void setField(Object target,
                                 Class<?> declaringClass,
                                 String fieldName,
                                 Object value) throws ReflectiveOperationException {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void initializeTaskBase(Task task) throws ReflectiveOperationException {
        setField(task, Task.class, "oldDebugState", "");
        setField(task, Task.class, "debugState", "");
        setField(task, Task.class, "first", true);
        setField(task, Task.class, "stopped", false);
        setField(task, Task.class, "active", false);
    }

    private static <T> T allocate(Class<T> type) throws ReflectiveOperationException {
        return type.cast(unsafe().allocateInstance(type));
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static ItemTarget[] targets(ItemTarget... targets) {
        return targets;
    }

    private static final class Fixture {
        private final BlockPos target = new BlockPos(7, 64, 7);
        private final ItemTarget[] initialSnapshot = targets(ItemTarget.EMPTY);
        private final CountingStoredTracker rootTracker = new CountingStoredTracker();
        private final List<StoreTaskProbe> createdTasks = new ArrayList<>();
        private final DepositAllStoreTaskGeneration generation = new DepositAllStoreTaskGeneration(
                (storeTarget, getIfNotPresent, snapshot) -> {
                    StoreTaskProbe task = new StoreTaskProbe(storeTarget, getIfNotPresent, snapshot);
                    createdTasks.add(task);
                    return task;
                },
                task -> ((StoreTaskProbe) task).finished
        );
    }

    private static final class LifecycleDepositAllTask extends DepositAllTask {
        private DepositAllStoreTaskGeneration generation;
        private BlockPos target;
        private ItemTarget[] remainingWork;

        private LifecycleDepositAllTask() {
            super(false);
        }

        @Override
        protected Task onTick() {
            generation.clearIfFinishedWithRemainingWork(target, remainingWork);
            return storeTaskForSelectedTarget(target, remainingWork);
        }

        @Override
        public boolean isFinished() {
            return false;
        }
    }

    private static final class StoreTaskProbe extends Task {
        private final BlockPos target;
        private final boolean getIfNotPresent;
        private final ItemTarget[] snapshot;
        private boolean finished;
        private int stopCallbacks;

        private StoreTaskProbe(BlockPos target, boolean getIfNotPresent, ItemTarget[] snapshot) {
            this.target = target.toImmutable();
            this.getIfNotPresent = getIfNotPresent;
            this.snapshot = Arrays.copyOf(snapshot, snapshot.length);
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
            stopCallbacks++;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        protected boolean isEqual(Task other) {
            return other instanceof StoreTaskProbe task
                    && target.equals(task.target)
                    && getIfNotPresent == task.getIfNotPresent
                    && Arrays.equals(snapshot, task.snapshot);
        }

        @Override
        protected String toDebugString() {
            return "store lifecycle probe " + target + " " + getIfNotPresent + " " + snapshot.length;
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

    private static final class NoOpMovementProgressChecker extends MovementProgressChecker {
        private NoOpMovementProgressChecker() {
            super();
        }

        @Override
        public void reset() {
        }
    }

    private static final class ResumableTaskChain extends SingleTaskChain {
        private ResumableTaskChain() {
            super(new TaskRunner(null));
        }

        @Override
        protected void onTaskFinish(AltoClef mod) {
            setTask(null);
        }

        @Override
        public float getPriority() {
            return 0;
        }

        @Override
        public String getName() {
            return "deposit_all lifecycle test chain";
        }
    }
}
