package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.block.Block;

//20260831_kpopmodder: Observe one scheduler-owned placement child's lifecycle without game behavior.
final class PlacementTaskProbe extends PlaceBlockNearbyTask {
    private boolean finished;
    private int tickCalls;
    private int stopCalls;

    private PlacementTaskProbe() {
        super(new Block[0]);
    }

    static PlacementTaskProbe create() {
        PlacementTaskProbe probe = TestObjects.allocate(PlacementTaskProbe.class);
        TaskLifecycleAccess.initialize(probe);
        return probe;
    }

    void finish() {
        finished = true;
    }

    int tickCalls() {
        return tickCalls;
    }

    int stopCalls() {
        return stopCalls;
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
        return "candidate placement lifecycle probe";
    }
}
