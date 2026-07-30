package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

import java.util.ArrayList;

public class TaskRunner {

    private final ArrayList<TaskChain> chains = new ArrayList<>();
    private final AltoClef mod;
    private boolean active;

    private TaskChain cachedCurrentTaskChain = null;

    public String statusReport = " (no chain running) ";

    public TaskRunner(AltoClef mod) {
        this.mod = mod;
        active = false;
    }

    public void tick() {
        if (!active) {
            ChatClefDiagnostics.logEvent("TASK_RUNNER", "SKIP", "inactive_or_not_in_game", null,
                    "runnerActive", active,
                    "inGame", "not_evaluated");
            statusReport = " (no chain running) ";
            return;
        }
        boolean inGame = AltoClef.inGame();
        if (!inGame) {
            ChatClefDiagnostics.logEvent("TASK_RUNNER", "SKIP", "inactive_or_not_in_game", null,
                    "runnerActive", active,
                    "inGame", false);
            statusReport = " (no chain running) ";
            return;
        }

        ChatClefDiagnostics.logEvent("TASK_RUNNER", "TICK_BEGIN", "runner_tick_begin", null,
                "registeredChains", chains.size(),
                "cachedCurrentTaskChain", ChatClefDiagnostics.chainName(cachedCurrentTaskChain));
        // Get highest priority chain and run
        TaskChain maxChain = null;
        float maxPriority = Float.NEGATIVE_INFINITY;
        for (TaskChain chain : chains) {
            if (!chain.isActive()) continue;
            float priority = chain.getPriority();
            ChatClefDiagnostics.logEvent("TASK_RUNNER", "CHAIN_PRIORITY", "chain_priority_observed", null,
                    "chain", ChatClefDiagnostics.chainName(chain),
                    "priority", priority);
            if (priority > maxPriority) {
                maxPriority = priority;
                maxChain = chain;
            }
        }
        if (cachedCurrentTaskChain != null && maxChain != cachedCurrentTaskChain) {
            ChatClefDiagnostics.logEvent("TASK_RUNNER", "CHAIN_INTERRUPT", "current_chain_interrupted_by_new_chain", null,
                    "previousChain", ChatClefDiagnostics.chainName(cachedCurrentTaskChain),
                    "nextChain", ChatClefDiagnostics.chainName(maxChain),
                    "maxPriority", maxPriority);
            cachedCurrentTaskChain.onInterrupt(maxChain);
        }
        cachedCurrentTaskChain = maxChain;
        if (maxChain != null) {
            statusReport = "Chain: "+maxChain.getName() + ", priority: "+maxPriority;
            ChatClefDiagnostics.logEvent("TASK_RUNNER", "CHAIN_TICK_BEGIN", "selected_chain_tick_begin", null,
                    "chain", ChatClefDiagnostics.chainName(maxChain),
                    "priority", maxPriority);
            maxChain.tick();
            ChatClefDiagnostics.logEvent("TASK_RUNNER", "CHAIN_TICK_END", "selected_chain_tick_end", null,
                    "chain", ChatClefDiagnostics.chainName(maxChain),
                    "priority", maxPriority);
        } else {
            statusReport = " (no chain running) ";
            ChatClefDiagnostics.logEvent("TASK_RUNNER", "NO_CHAIN", "no_active_chain", null);
        }
    }

    public void addTaskChain(TaskChain chain) {
        chains.add(chain);
    }

    public void enable() {
        if (!active) {
            mod.getBehaviour().push();
            mod.getBehaviour().setPauseOnLostFocus(false);
        }
        active = true;
        ChatClefDiagnostics.logEvent("TASK_RUNNER", "ENABLE", "runner_enable", null,
                "runnerActive", active);
    }

    public void disable() {
        if (active) {
            mod.getBehaviour().pop();
        }
        for (TaskChain chain : chains) {
            ChatClefDiagnostics.logEvent("TASK_RUNNER", "CHAIN_STOP", "runner_disable_chain_stop", null,
                    "chain", ChatClefDiagnostics.chainName(chain));
            chain.stop();
        }
        active = false;

        Debug.logMessage("Stopped");
        ChatClefDiagnostics.logEvent("TASK_RUNNER", "DISABLE", "runner_disable", null,
                "runnerActive", active);
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
}
