package lavi.minecraft.task.container.deposit.auto.maintenance.support;

import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;

//20260914_kpopmodder: Supply native completion evidence independently from the generic finished flag.
public final class ObservedDepositChild extends DepositAllTask {
    private final boolean finished;
    private final boolean stopped;
    private final boolean stored;
    private final int count;
    private int ticks;

    public ObservedDepositChild(boolean finished, boolean stopped, boolean stored, int count) {
        super(false, new ItemTarget[0]);
        this.finished = finished;
        this.stopped = stopped;
        this.stored = stored;
        this.count = count;
    }

    @Override public boolean isFinished() { return finished; }
    @Override public boolean stopped() { return stopped; }
    @Override public boolean automaticStoredTargetsSatisfied() { return stored; }
    @Override public int automaticStoredCount() { return count; }
    @Override protected void onStart() { }
    @Override protected Task onTick() { ticks++; return null; }
    @Override protected void onStop(Task interruptTask) { }
    public int ticks() { return ticks; }
}
