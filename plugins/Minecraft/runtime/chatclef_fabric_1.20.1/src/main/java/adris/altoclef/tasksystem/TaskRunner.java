package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.logging.StateChangeLogger;

import java.util.ArrayList;
import java.util.Locale;

public class TaskRunner {

    private final ArrayList<TaskChain> chains = new ArrayList<>();
    private final AltoClef mod;
    private boolean active;

    private TaskChain cachedCurrentTaskChain = null;
    private final StateChangeLogger debugLogger = new StateChangeLogger("TaskRunner");

    public String statusReport = " (no chain running) ";

    public TaskRunner(AltoClef mod) {
        this.mod = mod;
        active = false;
    }

    public void tick() {
        if (!active) {
            debugLogger.state("inactive; no chain running");
            statusReport = " (no chain running) ";
            return;
        }
        if (!AltoClef.inGame()) {
            debugLogger.state("not in game; no chain running");
            statusReport = " (no chain running) ";
            return;
        }

        // Get highest priority chain and run
        TaskChain maxChain = null;
        float maxPriority = Float.NEGATIVE_INFINITY;
        for (TaskChain chain : chains) {
            if (!chain.isActive()) continue;
            float priority = chain.getPriority();
            if (priority > maxPriority) {
                maxPriority = priority;
                maxChain = chain;
            }
        }
        if (cachedCurrentTaskChain != null && maxChain != cachedCurrentTaskChain) {
            debugLogger.event("chain switch: " + describeChain(cachedCurrentTaskChain)
                    + " -> " + describeChain(maxChain)
                    + ", newPriority=" + formatPriority(maxPriority));
            cachedCurrentTaskChain.onInterrupt(maxChain);
        }
        cachedCurrentTaskChain = maxChain;
        if (maxChain != null) {
            statusReport = "Chain: "+maxChain.getName() + ", priority: "+maxPriority;
            debugLogger.state("running chain: " + maxChain.getName()
                    + ", priority=" + formatPriority(maxPriority)
                    + ", taskDepth=" + maxChain.getTasks().size());
            maxChain.tick();
        } else {
            debugLogger.state("no active chain selected");
            statusReport = " (no chain running) ";
        }
    }

    public void addTaskChain(TaskChain chain) {
        chains.add(chain);
    }

    public void enable() {
        if (!active) {
            debugLogger.event("enable");
            mod.getBehaviour().push();
            mod.getBehaviour().setPauseOnLostFocus(false);
        }
        active = true;
    }

    public void disable() {
        if (active) {
            debugLogger.event("disable");
            mod.getBehaviour().pop();
        }
        for (TaskChain chain : chains) {
            chain.stop();
        }
        active = false;

        Debug.logMessage("Stopped");
    }

    public boolean isActive() {
        return active;
    }

    public TaskChain getCurrentTaskChain() {
        return cachedCurrentTaskChain;
    }

    // Kinda jank ngl
    public AltoClef getMod() {
        return mod;
    }

    private String describeChain(TaskChain chain) {
        return chain == null ? "none" : chain.getName();
    }

    private String formatPriority(float priority) {
        if (Float.isInfinite(priority)) {
            return "infinity";
        }
        return String.format(Locale.ROOT, "%.1f", priority);
    }
}
