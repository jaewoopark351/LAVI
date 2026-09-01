package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPlacementTaskOwner;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPostPlaceHandoff;

//20260831_kpopmodder: Own only the placement handoff and its cleanup-only scheduler barrier.
final class ContainerCandidateHandoffParent extends Task {
    private final ContainerRouteProbe route;
    private final ContainerCandidateTaskFactory taskFactory;
    private final DepositAllPlacementTaskOwner placementOwner =
            DepositAllPlacementTaskOwner.retaining();
    private final DepositAllPostPlaceHandoff handoff =
            DepositAllPostPlaceHandoff.singleTick();
    private int barrierCalls;

    ContainerCandidateHandoffParent(
            ContainerRouteProbe route,
            ContainerCandidateTaskFactory taskFactory) {
        this.route = route;
        this.taskFactory = taskFactory;
    }

    PlaceBlockNearbyTask ownedPlacement() {
        return placementOwner.currentTask();
    }

    int barrierCalls() {
        return barrierCalls;
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
            barrierCalls++;
            return null;
        }

        return taskFactory.create(route.evaluate(), placementOwner);
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
        return "deposit-all candidate-switch handoff lifecycle parent";
    }
}
