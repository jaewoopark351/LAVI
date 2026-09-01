package lavi.minecraft.task.container.deposit.handoff;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositAllPostPlaceHandoffLifecycleTest {
    @Test
    void stopsTheActualPlacementChildBeforeStartingTheOpenChildOnTheNextTick() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            Block block = TestObjects.allocate(Block.class);
            PlacementProbe placement = placementProbe();
            OpenProbe open = new OpenProbe();
            HandoffParent parent = new HandoffParent(block, placement, open);
            HarnessChain chain = new HarnessChain();

            try {
                chain.setTask(parent);
                chain.tick();

                assertSame(placement, parent.actualChild());
                assertTrue(placement.isActive());
                assertEquals(1, placement.tickCalls);
                assertEquals(0, placement.stopCalls);
                assertEquals(0, open.startCalls);

                placement.finished = true;
                chain.tick();

                assertEquals(1, placement.stopCalls);
                assertFalse(placement.isActive());
                assertEquals(0, open.startCalls);
                assertEquals(0, open.tickCalls);
                assertNull(parent.actualChild());

                chain.tick();

                assertSame(open, parent.actualChild());
                assertEquals(1, open.startCalls);
                assertEquals(1, open.tickCalls);
                assertEquals(1, placement.stopCalls);
            } finally {
                chain.setTask(null);
            }
        }
    }

    @Test
    void interruptionKeepsTheSchedulerOwnedPlacementIdentity() {
        try (HeadlessMinecraftClientSession ignored = HeadlessMinecraftClientSession.outOfGame()) {
            Block block = TestObjects.allocate(Block.class);
            PlacementProbe placement = placementProbe();
            HandoffParent parent = new HandoffParent(block, placement, new OpenProbe());
            HarnessChain chain = new HarnessChain();

            try {
                chain.setTask(parent);
                chain.tick();
                chain.onInterrupt(null);

                assertTrue(placement.isActive());
                assertFalse(placement.stopped());
                assertEquals(1, placement.stopCalls);

                chain.tick();

                assertSame(placement, parent.actualChild());
                assertEquals(2, placement.tickCalls);
                assertEquals(1, placement.stopCalls);
            } finally {
                chain.setTask(null);
            }
        }
    }

    private static final class HandoffParent extends Task {
        private final Block block;
        private final PlacementProbe placement;
        private final OpenProbe open;
        private final DepositAllPlacementTaskOwner placementOwner =
                DepositAllPlacementTaskOwner.retaining();
        private final DepositAllPostPlaceHandoff handoff =
                DepositAllPostPlaceHandoff.singleTick();
        private boolean routeReady;

        private HandoffParent(Block block, PlacementProbe placement, OpenProbe open) {
            this.block = block;
            this.placement = placement;
            this.open = open;
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            PlaceBlockNearbyTask current = placementOwner.currentTask();
            boolean active = current != null && current.isActive();
            boolean finished = active && current.isFinished();
            if (handoff.shouldDefer(current, active, finished)) {
                placementOwner.clear(current);
                routeReady = true;
                return null;
            }
            if (routeReady) {
                return open;
            }
            return placementOwner.getOrCreate(block, () -> placement);
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
            return "deposit-all post-place handoff lifecycle parent";
        }

        private Task actualChild() {
            try {
                Field field = Task.class.getDeclaredField("sub");
                field.setAccessible(true);
                return (Task) field.get(this);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Failed to inspect scheduler-owned child", exception);
            }
        }
    }

    private static final class PlacementProbe extends PlaceBlockNearbyTask {
        private boolean finished;
        private int tickCalls;
        private int stopCalls;

        private PlacementProbe() {
            super(new Block[0]);
        }

        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            tickCalls++;
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
            stopCalls++;
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
            return "placement probe";
        }
    }

    private static PlacementProbe placementProbe() {
        PlacementProbe placement = TestObjects.allocate(PlacementProbe.class);
        setTaskField(placement, "oldDebugState", "");
        setTaskField(placement, "debugState", "");
        setTaskFlag(placement, "first", true);
        setTaskFlag(placement, "stopped", false);
        setTaskFlag(placement, "active", false);
        return placement;
    }

    private static void setTaskField(Task task, String fieldName, Object value) {
        try {
            Field field = Task.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(task, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to initialize Task." + fieldName, exception);
        }
    }

    private static void setTaskFlag(Task task, String fieldName, boolean value) {
        try {
            Field field = Task.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setBoolean(task, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to initialize Task." + fieldName, exception);
        }
    }

    private static final class OpenProbe extends Task {
        private int startCalls;
        private int tickCalls;

        @Override
        protected void onStart() {
            startCalls++;
        }

        @Override
        protected Task onTick() {
            tickCalls++;
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
            return "open probe";
        }
    }

    private static final class HarnessChain extends SingleTaskChain {
        private HarnessChain() {
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
            return "deposit-all post-place handoff lifecycle test chain";
        }
    }
}
