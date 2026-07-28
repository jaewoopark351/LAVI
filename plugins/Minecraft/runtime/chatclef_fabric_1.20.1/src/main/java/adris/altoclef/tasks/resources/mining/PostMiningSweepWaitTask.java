package adris.altoclef.tasks.resources.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;

//20260728_kpopmodder: Tiny wait task used by post-mining sweep without starting wander movement.
final class PostMiningSweepWaitTask extends Task {
    private final PostMiningSweepPolicy postMiningSweepPolicy;

    PostMiningSweepWaitTask(PostMiningSweepPolicy postMiningSweepPolicy) {
        this.postMiningSweepPolicy = postMiningSweepPolicy;
    }

    @Override
    protected void onStart() {
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
    }

    @Override
    protected Task onTick() {
        setDebugState("Waiting for nearby drops");
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
    }

    @Override
    public boolean isFinished() {
        return !postMiningSweepPolicy.isWaitingForPotentialDrops();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof PostMiningSweepWaitTask;
    }

    @Override
    protected String toDebugString() {
        return "Post mining pickup sweep";
    }
}
