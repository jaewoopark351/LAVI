package lavi.minecraft.task.container.deposit.handoff;

import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllPlacementTaskOwnerTest {
    @Test
    void ephemeralOwnerCreatesANewTaskForEveryRequest() {
        DepositAllPlacementTaskOwner owner = DepositAllPlacementTaskOwner.ephemeral();
        Block block = block();
        AtomicInteger factoryCalls = new AtomicInteger();

        PlaceBlockNearbyTask first = owner.getOrCreate(block, () -> {
            factoryCalls.incrementAndGet();
            return placementTask();
        });
        PlaceBlockNearbyTask second = owner.getOrCreate(block, () -> {
            factoryCalls.incrementAndGet();
            return placementTask();
        });

        assertFalse(owner.retainingIdentity());
        assertNotSame(first, second);
        assertEquals(2, factoryCalls.get());
        assertNull(owner.currentTask());
    }

    @Test
    void retainingOwnerPreservesIdentityForTheSameBlock() {
        DepositAllPlacementTaskOwner owner = DepositAllPlacementTaskOwner.retaining();
        Block block = block();
        AtomicInteger factoryCalls = new AtomicInteger();

        PlaceBlockNearbyTask first = owner.getOrCreate(block, () -> {
            factoryCalls.incrementAndGet();
            return placementTask();
        });
        PlaceBlockNearbyTask second = owner.getOrCreate(block, () -> {
            factoryCalls.incrementAndGet();
            return placementTask();
        });

        assertTrue(owner.retainingIdentity());
        assertSame(first, second);
        assertSame(first, owner.currentTask());
        assertEquals(1, factoryCalls.get());
    }

    @Test
    void clearReleasesOnlyTheExpectedTaskAndAllowsANewIdentity() {
        DepositAllPlacementTaskOwner owner = DepositAllPlacementTaskOwner.retaining();
        Block block = block();
        PlaceBlockNearbyTask first = owner.getOrCreate(block, DepositAllPlacementTaskOwnerTest::placementTask);

        owner.clear(placementTask());
        assertSame(first, owner.currentTask());

        owner.clear(first);
        assertNull(owner.currentTask());

        PlaceBlockNearbyTask second = owner.getOrCreate(block, DepositAllPlacementTaskOwnerTest::placementTask);
        assertNotSame(first, second);
        assertSame(second, owner.currentTask());
    }

    @Test
    void stoppedRetainedTaskIsReplacedForTheSameBlock() {
        DepositAllPlacementTaskOwner owner = DepositAllPlacementTaskOwner.retaining();
        Block block = block();
        AtomicInteger factoryCalls = new AtomicInteger();
        PlaceBlockNearbyTask first = owner.getOrCreate(block, () -> {
            factoryCalls.incrementAndGet();
            return placementTask();
        });
        markStopped(first);

        PlaceBlockNearbyTask second = owner.getOrCreate(block, () -> {
            factoryCalls.incrementAndGet();
            return placementTask();
        });

        assertNotSame(first, second);
        assertSame(second, owner.currentTask());
        assertEquals(2, factoryCalls.get());
    }

    @Test
    void activeSchedulerOwnedTaskSurvivesAChangedBlockCandidate() {
        DepositAllPlacementTaskOwner owner = DepositAllPlacementTaskOwner.retaining();
        AtomicInteger factoryCalls = new AtomicInteger();
        PlaceBlockNearbyTask first = owner.getOrCreate(block(), () -> {
            factoryCalls.incrementAndGet();
            return placementTask();
        });
        markActive(first);

        PlaceBlockNearbyTask second = owner.getOrCreate(block(), () -> {
            factoryCalls.incrementAndGet();
            return placementTask();
        });

        assertSame(first, second);
        assertSame(first, owner.currentTask());
        assertEquals(1, factoryCalls.get());
    }

    @Test
    void inactiveTaskCanBeReplacedWhenTheRequestedBlockChanges() {
        DepositAllPlacementTaskOwner owner = DepositAllPlacementTaskOwner.retaining();
        PlaceBlockNearbyTask first = owner.getOrCreate(block(), DepositAllPlacementTaskOwnerTest::placementTask);

        PlaceBlockNearbyTask second = owner.getOrCreate(block(), DepositAllPlacementTaskOwnerTest::placementTask);

        assertNotSame(first, second);
        assertSame(second, owner.currentTask());
    }

    private static Block block() {
        return TestObjects.allocate(Block.class);
    }

    private static PlaceBlockNearbyTask placementTask() {
        return TestObjects.allocate(PlaceBlockNearbyTask.class);
    }

    private static void markStopped(Task task) {
        setTaskFlag(task, "stopped", true);
    }

    private static void markActive(Task task) {
        setTaskFlag(task, "active", true);
    }

    private static void setTaskFlag(Task task, String fieldName, boolean value) {
        try {
            Field field = Task.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setBoolean(task, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to set Task." + fieldName, exception);
        }
    }
}
