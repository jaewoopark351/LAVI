package lavi.minecraft.task.container.deposit;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllStoreTaskGenerationTest {
    @Test
    void reusesActiveTaskForTheSameCoordinateDespiteSnapshotChanges() {
        Fixture fixture = new Fixture();
        BlockPos firstTargetInstance = new BlockPos(10, 64, 10);
        BlockPos secondTargetInstance = new BlockPos(10, 64, 10);
        ItemTarget[] firstSnapshot = targets(ItemTarget.EMPTY);

        Task first = fixture.generation.getOrCreate(firstTargetInstance, false, firstSnapshot);
        firstSnapshot[0] = null;
        Task second = fixture.generation.getOrCreate(secondTargetInstance, false, new ItemTarget[0]);

        assertSame(first, second);
        assertEquals(1, fixture.createdTasks.size());
        assertEquals(1, fixture.generation.generationId());
        assertArrayEquals(targets(ItemTarget.EMPTY), fixture.generation.activeStoreSnapshot());
        assertEquals(firstTargetInstance, fixture.generation.activeStoreTarget().orElseThrow());
    }

    @Test
    void createsNewTaskOnlyWhenTheOwnedTargetChangesOrGenerationIsCleared() {
        Fixture fixture = new Fixture();
        BlockPos firstTarget = new BlockPos(1, 64, 1);
        BlockPos secondTarget = new BlockPos(2, 64, 2);

        Task first = fixture.generation.getOrCreate(firstTarget, false, targets(ItemTarget.EMPTY));
        Task second = fixture.generation.getOrCreate(secondTarget, false, targets(ItemTarget.EMPTY));

        assertNotSame(first, second);
        assertEquals(2, fixture.generation.generationId());

        assertTrue(fixture.generation.clear());
        assertFalse(((TestTask) second).stoppedByGeneration);

        Task third = fixture.generation.getOrCreate(secondTarget, false, targets(ItemTarget.EMPTY));
        assertNotSame(second, third);
        assertEquals(3, fixture.generation.generationId());
    }

    @Test
    void refreshesOnlyAfterTheActiveChildFinishedAndRootStillHasRemainingWork() {
        Fixture fixture = new Fixture();
        BlockPos target = new BlockPos(3, 64, 3);

        TestTask first = (TestTask) fixture.generation.getOrCreate(target, false, targets(ItemTarget.EMPTY));
        assertFalse(fixture.generation.clearIfFinishedWithRemainingWork(target, targets(ItemTarget.EMPTY)));
        assertSame(first, fixture.generation.getOrCreate(target, false, new ItemTarget[0]));

        first.finished = true;
        assertFalse(fixture.generation.clearIfFinishedWithRemainingWork(target, new ItemTarget[0]));
        assertSame(first, fixture.generation.getOrCreate(target, false, new ItemTarget[0]));

        assertTrue(fixture.generation.clearIfFinishedWithRemainingWork(target, targets(ItemTarget.EMPTY)));
        assertFalse(first.stoppedByGeneration);

        Task second = fixture.generation.getOrCreate(target, false, targets(ItemTarget.EMPTY));
        assertNotSame(first, second);
        assertEquals(2, fixture.generation.generationId());
    }

    private static ItemTarget[] targets(ItemTarget... targets) {
        return targets;
    }

    private static final class Fixture {
        private final List<TestTask> createdTasks = new ArrayList<>();
        private final DepositAllStoreTaskGeneration generation = new DepositAllStoreTaskGeneration(
                (target, getIfNotPresent, snapshot) -> {
                    TestTask task = new TestTask(target, getIfNotPresent, snapshot);
                    createdTasks.add(task);
                    return task;
                },
                task -> ((TestTask) task).finished
        );
    }

    private static final class TestTask extends Task {
        private final BlockPos target;
        private final boolean getIfNotPresent;
        private final ItemTarget[] snapshot;
        private boolean finished;
        private boolean stoppedByGeneration;

        private TestTask(BlockPos target, boolean getIfNotPresent, ItemTarget[] snapshot) {
            this.target = target;
            this.getIfNotPresent = getIfNotPresent;
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
            stoppedByGeneration = true;
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "test deposit_all store generation task " + target + " " + getIfNotPresent + " " + snapshot.length;
        }
    }
}
