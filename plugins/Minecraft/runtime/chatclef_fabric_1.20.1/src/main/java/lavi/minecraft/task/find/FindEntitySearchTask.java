//#if MC == 12001
package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.entity.DoToClosestEntityTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/** Reuses ATTACK's native candidate selection/pursuit loop, but not its kill callback. */
final class FindEntitySearchTask extends DoToClosestEntityTask {
    private final FindTask owner;
    private final FindExploreTask explore;

    FindEntitySearchTask(FindTask owner) {
        // A null class filter means all runtime entity classes, exactly as in KillEntitiesTask.
        super(owner::selectEntity, owner::considerEntity, (Class<?>[]) null);
        this.owner = owner;
        explore = new FindExploreTask(owner);
    }

    @Override protected Optional<Entity> getClosestTo(AltoClef mod, Vec3d pos) {
        owner.beginObservation();
        Optional<Entity> result = super.getClosestTo(mod, pos);
        owner.endObservation(result.isPresent());
        return owner.isFinished() ? Optional.empty() : result;
    }

    @Override protected boolean isValid(AltoClef mod, Entity entity) {
        return owner.entityMatches(entity) && super.isValid(mod, entity);
    }

    @Override protected Task getWanderTask(AltoClef mod) {
        if (owner.isFinished()) return null;
        owner.searching();
        return explore;
    }

    @Override protected Task onTick() {
        try {
            Task child = super.onTick();
            return owner.isFinished() ? null : child;
        } catch (RuntimeException error) {
            owner.movementFailed(error);
            return null;
        }
    }

    // Do not inherit equality by class-filter alone: a different FIND has a different owner/predicate.
    @Override protected boolean isEqual(Task other) { return this == other; }
    @Override protected String toDebugString() { return "FIND native entity search"; }
}
//#endif
