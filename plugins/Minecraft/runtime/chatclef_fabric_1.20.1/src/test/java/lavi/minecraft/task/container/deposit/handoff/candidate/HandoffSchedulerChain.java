package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasksystem.TaskRunner;

//20260831_kpopmodder: Drive the real SingleTaskChain reconciliation for candidate-handoff tests.
final class HandoffSchedulerChain extends SingleTaskChain {
    HandoffSchedulerChain() {
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
        return "deposit-all candidate-handoff test chain";
    }
}
