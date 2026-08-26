package adris.altoclef.tasks.container;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.DepositAllStoreTaskGeneration;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class DepositAllTaskStoreGenerationTest {
    @Test
    void returnsTheSameChildTaskForTheSameSelectedTargetWhenRootNotStoredChanges() {
        Fixture fixture = new Fixture();
        DepositAllTask task = newTestSubject(false, fixture.generation);
        BlockPos target = new BlockPos(7, 64, 7);

        Task first = task.storeTaskForSelectedTarget(target, targets(ItemTarget.EMPTY));
        Task second = task.storeTaskForSelectedTarget(new BlockPos(7, 64, 7), new ItemTarget[0]);

        assertSame(first, second);
        assertEquals(1, fixture.createdTasks.size());
        assertEquals(1, fixture.generation.generationId());
    }

    @Test
    void clearingTheGenerationLetsTheParentCreateANewChildWithoutStoppingThePreviousChild() {
        Fixture fixture = new Fixture();
        DepositAllTask task = newTestSubject(false, fixture.generation);
        BlockPos target = new BlockPos(8, 64, 8);

        TestTask first = (TestTask) task.storeTaskForSelectedTarget(target, targets(ItemTarget.EMPTY));
        assertFalse(first.stopped);

        fixture.generation.clear();
        Task second = task.storeTaskForSelectedTarget(target, targets(ItemTarget.EMPTY));

        assertNotSame(first, second);
        assertFalse(first.stopped);
        assertEquals(2, fixture.generation.generationId());
    }

    private static ItemTarget[] targets(ItemTarget... targets) {
        return targets;
    }

    private static DepositAllTask newTestSubject(boolean getIfNotPresent,
                                                 DepositAllStoreTaskGeneration generation) {
        try {
            // MovementProgressChecker field initialization requires a live Minecraft client.
            DepositAllTask task = (DepositAllTask) unsafe().allocateInstance(DepositAllTask.class);
            setField(task, "_getIfNotPresent", getIfNotPresent);
            setField(task, "_storeTaskGeneration", generation);
            return task;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create headless DepositAllTask test subject", exception);
        }
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = DepositAllTask.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class Fixture {
        private final List<TestTask> createdTasks = new ArrayList<>();
        private final DepositAllStoreTaskGeneration generation = new DepositAllStoreTaskGeneration(
                (target, getIfNotPresent, snapshot) -> {
                    TestTask task = new TestTask(target, snapshot);
                    createdTasks.add(task);
                    return task;
                },
                task -> false
        );
    }

    private static final class TestTask extends Task {
        private final BlockPos target;
        private final ItemTarget[] snapshot;
        private boolean stopped;

        private TestTask(BlockPos target, ItemTarget[] snapshot) {
            this.target = target;
            this.snapshot = snapshot;
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
            stopped = true;
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "deposit_all parent store generation test " + target + " " + snapshot.length;
        }
    }
}
