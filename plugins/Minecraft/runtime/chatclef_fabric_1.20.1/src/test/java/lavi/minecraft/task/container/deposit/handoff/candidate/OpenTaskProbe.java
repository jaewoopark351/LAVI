package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasksystem.Task;
import net.minecraft.block.Block;

//20260831_kpopmodder: Observe when a freshly routed container-open child starts and ticks.
final class OpenTaskProbe extends Task {
    private final Block target;
    private int startCalls;
    private int tickCalls;
    private int stopCalls;

    OpenTaskProbe(Block target) {
        this.target = target;
    }

    Block target() {
        return target;
    }

    int startCalls() {
        return startCalls;
    }

    int tickCalls() {
        return tickCalls;
    }

    int stopCalls() {
        return stopCalls;
    }

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
        stopCalls++;
    }

    @Override
    protected boolean isEqual(Task other) {
        return this == other;
    }

    @Override
    protected String toDebugString() {
        return "newly placed container open probe";
    }
}
