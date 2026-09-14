//#if MC == 12001
package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/** Uses ChatClef's live block index and closest-object loop; cached positions are revalidated. */
final class FindBlockSearchTask extends DoToClosestBlockTask {
    private final FindTask owner;
    private final FindExploreTask explore;
    private final FindUnindexedAirProbe airProbe;

    FindBlockSearchTask(FindTask owner, String registryId) {
        super(owner::selectBlock, owner::considerBlock, Registries.BLOCK.get(new Identifier(registryId)));
        this.owner = owner;
        explore = new FindExploreTask(owner);
        airProbe = Registries.BLOCK.get(new Identifier(registryId)).getDefaultState().isAir()
                ? new FindUnindexedAirProbe() : null;
    }

    @Override protected Optional<BlockPos> getClosestTo(AltoClef mod, Vec3d pos) {
        owner.beginObservation();
        Optional<BlockPos> result = airProbe == null ? super.getClosestTo(mod, pos)
                : airProbe.closest(owner, mod);
        owner.endObservation(result.isPresent());
        return owner.isFinished() ? Optional.empty() : result;
    }

    @Override protected boolean isValid(AltoClef mod, BlockPos pos) {
        // The upstream default assumes unloaded candidates remain valid. FIND must actually observe them.
        return owner.blockMatches(pos) && super.isValid(mod, pos);
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

    @Override protected boolean isEqual(Task other) { return this == other; }
    @Override protected String toDebugString() { return "FIND native block search"; }
}
//#endif
