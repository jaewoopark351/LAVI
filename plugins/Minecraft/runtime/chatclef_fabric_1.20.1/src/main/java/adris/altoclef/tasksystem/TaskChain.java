package adris.altoclef.tasksystem;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.ArrayList;
import java.util.List;

public abstract class TaskChain {

    private final List<Task> cachedTaskChain = new ArrayList<>();

    public TaskChain(TaskRunner runner) {
        runner.addTaskChain(this);
    }

    public void tick() {
        ChatClefDiagnostics.logEvent("TASK_CHAIN", "TICK_BEGIN", "task_chain_tick_begin", null,
                "chain", ChatClefDiagnostics.chainName(this));
        cachedTaskChain.clear();
        onTick();
        ChatClefDiagnostics.logEvent("TASK_CHAIN", "TICK_END", "task_chain_tick_end", null,
                "chain", ChatClefDiagnostics.chainName(this),
                "cachedTaskChainSize", cachedTaskChain.size());
    }

    public void stop() {
        ChatClefDiagnostics.logEvent("TASK_CHAIN", "STOP_BEGIN", "task_chain_stop_begin", null,
                "chain", ChatClefDiagnostics.chainName(this));
        cachedTaskChain.clear();
        onStop();
        ChatClefDiagnostics.logEvent("TASK_CHAIN", "STOP_END", "task_chain_stop_end", null,
                "chain", ChatClefDiagnostics.chainName(this));
    }

    protected abstract void onStop();

    public abstract void onInterrupt(TaskChain other);

    protected abstract void onTick();

    public abstract float getPriority();

    public abstract boolean isActive();

    public abstract String getName();

    public List<Task> getTasks() {
        return cachedTaskChain;
    }

    void addTaskToChain(Task task) {
        cachedTaskChain.add(task);
    }

    public String toString() {
        return getName();
    }

}
