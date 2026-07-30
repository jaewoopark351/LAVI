package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.AbstractDoToClosestObjectTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Finds the closest entity and runs a task on that entity
 */
@SuppressWarnings("rawtypes")
public class DoToClosestEntityTask extends AbstractDoToClosestObjectTask<Entity> {

    private final Class[] targetEntities;

    private final Supplier<Vec3d> getOriginPos;

    private final Function<Entity, Task> getTargetTask;

    private final Predicate<Entity> shouldInteractWith;

    public DoToClosestEntityTask(Supplier<Vec3d> getOriginSupplier, Function<Entity, Task> getTargetTask, Predicate<Entity> shouldInteractWith, Class... entities) {
        getOriginPos = getOriginSupplier;
        this.getTargetTask = getTargetTask;
        this.shouldInteractWith = shouldInteractWith;
        targetEntities = entities;
    }

    public DoToClosestEntityTask(Supplier<Vec3d> getOriginSupplier, Function<Entity, Task> getTargetTask, Class... entities) {
        this(getOriginSupplier, getTargetTask, entity -> true, entities);
    }

    public DoToClosestEntityTask(Function<Entity, Task> getTargetTask, Predicate<Entity> shouldInteractWith, Class... entities) {
        this(null, getTargetTask, shouldInteractWith, entities);
    }

    public DoToClosestEntityTask(Function<Entity, Task> getTargetTask, Class... entities) {
        this(null, getTargetTask, entity -> true, entities);
    }

    @Override
    protected Vec3d getPos(AltoClef mod, Entity obj) {
        return obj.getPos();
    }

    @Override
    protected Optional<Entity> getClosestTo(AltoClef mod, Vec3d pos) {
        boolean entityFound = mod.getEntityTracker().entityFound(targetEntities);
        ChatClefDiagnostics.logEvent("ENTITY_SELECT", "OBSERVE", "entity_found_before_closest", this,
                "targetEntities", ChatClefDiagnostics.classList(targetEntities),
                "origin", ChatClefDiagnostics.vec3d(pos),
                "entityFound", entityFound);
        if (!entityFound) return Optional.empty();
        Optional<Entity> closest = mod.getEntityTracker().getClosestEntity(pos, shouldInteractWith, targetEntities);
        ChatClefDiagnostics.logEvent("ENTITY_SELECT", "OBSERVE", "closest_entity_result", this,
                "targetEntities", ChatClefDiagnostics.classList(targetEntities),
                "origin", ChatClefDiagnostics.vec3d(pos),
                "closestPresent", closest.isPresent(),
                "closestEntity", closest.map(ChatClefDiagnostics::entitySummary).orElse("none"),
                "closestDistanceSqr", closest.map(entity -> ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, entity)).orElse("unavailable"));
        return closest;
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        if (getOriginPos != null) {
            return getOriginPos.get();
        }
        return mod.getPlayer().getPos();
    }

    @Override
    protected Task getGoalTask(Entity obj) {
        Task task = getTargetTask.apply(obj);
        ChatClefDiagnostics.logEvent("ENTITY_SELECT", "DECISION", "create_entity_goal_task", this,
                "entity", ChatClefDiagnostics.entitySummary(obj),
                "goalTask", ChatClefDiagnostics.taskSummary(task));
        return task;
    }

    @Override
    protected boolean isValid(AltoClef mod, Entity obj) {
        boolean alive = obj.isAlive();
        boolean reachable = mod.getEntityTracker().isEntityReachable(obj);
        boolean valid = alive && reachable;
        ChatClefDiagnostics.logEvent("ENTITY_SELECT", "OBSERVE", "entity_validity_check", this,
                "entity", ChatClefDiagnostics.entitySummary(obj),
                "alive", alive,
                "reachable", reachable,
                "valid", valid);
        return valid;
    }

    @Override
    protected void onStart() {
        //20260730_kpopmodder: Diagnostics-only LAVI log for entity selection loop investigation; no behavior change.
        ChatClefDiagnostics.logEvent("ENTITY_SELECT", "ON_START", "do_to_closest_entity_start", this,
                "targetEntities", ChatClefDiagnostics.classList(targetEntities));
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logEvent("ENTITY_SELECT", "ON_STOP", "do_to_closest_entity_stop", this,
                "targetEntities", ChatClefDiagnostics.classList(targetEntities),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DoToClosestEntityTask task) {
            return Arrays.equals(task.targetEntities, targetEntities);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing something to closest entity...";
    }
}
