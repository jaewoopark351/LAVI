package adris.altoclef.tasks.interaction.block;

import adris.altoclef.tasks.movement.escape.TimeoutWanderTask;

//20260730_kpopmodder: Added this policy to own retry counters and wander recovery for block interaction.
final class BlockInteractionRecoveryPolicy {

    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(5, true);
    private int waitingForClickTicks;

    void resetWanderForStart() {
        wanderTask.resetWander();
    }

    boolean isWandering() {
        return wanderTask.isActive() && !wanderTask.isFinished();
    }

    TimeoutWanderTask getWanderTask() {
        return wanderTask;
    }

    int incrementWaitingForClickTicks() {
        waitingForClickTicks++;
        return waitingForClickTicks;
    }

    int getWaitingForClickTicks() {
        return waitingForClickTicks;
    }

    void resetWaitingForClickTicks() {
        waitingForClickTicks = 0;
    }
}
