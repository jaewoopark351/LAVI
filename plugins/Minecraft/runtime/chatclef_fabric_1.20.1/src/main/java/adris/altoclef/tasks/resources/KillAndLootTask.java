package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.entity.Entity;

import java.util.function.Predicate;

public class KillAndLootTask extends ResourceTask {

    private final Class<?> _toKill;

    private final Task _killTask;

    public KillAndLootTask(Class<?> toKill, Predicate<Entity> shouldKill, ItemTarget... itemTargets) {
        super(itemTargets.clone());
        _toKill = toKill;
        _killTask = new KillEntitiesTask(shouldKill, _toKill);
    }

    public KillAndLootTask(Class<?> toKill, ItemTarget... itemTargets) {
        super(itemTargets.clone());
        _toKill = toKill;
        _killTask = new KillEntitiesTask(_toKill);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        //20260730_kpopmodder: Diagnostics-only LAVI log for entity resource loop investigation; no behavior change.
        ChatClefDiagnostics.logEvent("RESOURCE", "ON_START", "kill_and_loot_start", this,
                "targetClass", ChatClefDiagnostics.classList(new Class<?>[]{_toKill}),
                "killTask", ChatClefDiagnostics.taskSummary(_killTask));
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        boolean entityFound = mod.getEntityTracker().entityFound(_toKill);
        ChatClefDiagnostics.logEvent("RESOURCE", "OBSERVE", "kill_and_loot_entity_found", this,
                "targetClass", ChatClefDiagnostics.classList(new Class<?>[]{_toKill}),
                "entityFound", entityFound,
                "playerPosition", ChatClefDiagnostics.playerPosition(mod),
                "killTask", ChatClefDiagnostics.taskSummary(_killTask));
        if (!entityFound) {
            if (isInWrongDimension(mod)) {
                ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "kill_and_loot_wrong_dimension", this,
                        "targetClass", ChatClefDiagnostics.classList(new Class<?>[]{_toKill}));
                setDebugState("Going to correct dimension.");
                return getToCorrectDimensionTask(mod);
            }
            ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "kill_and_loot_search_wander", this,
                    "targetClass", ChatClefDiagnostics.classList(new Class<?>[]{_toKill}));
            setDebugState("Searching for mob...");
            return new TimeoutWanderTask();
        }
        // We found the mob!
        ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "kill_and_loot_return_kill_task", this,
                "targetClass", ChatClefDiagnostics.classList(new Class<?>[]{_toKill}),
                "killTask", ChatClefDiagnostics.taskSummary(_killTask));
        return _killTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        ChatClefDiagnostics.logEvent("RESOURCE", "ON_STOP", "kill_and_loot_stop", this,
                "targetClass", ChatClefDiagnostics.classList(new Class<?>[]{_toKill}),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof KillAndLootTask task) {
            return task._toKill.equals(_toKill);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect items from " + _toKill.toGenericString();
    }
}
