//#if MC == 12001
package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.Optional;

/** Native BlockScanner deliberately skips air states. Probe those runtime types without changing its index. */
final class FindUnindexedAirProbe {
    private BlockPos center;
    private BlockPos best;
    private BlockPos cached;
    private int cursor;
    private double distance = Double.POSITIVE_INFINITY;

    Optional<BlockPos> closest(FindTask owner, AltoClef mod) {
        BlockPos feet = mod.getPlayer().getBlockPos();
        // A matching live feet cell already meets arrival; no exhaustive absence claim is needed.
        if (owner.considerBlock(feet)) return Optional.of(feet.toImmutable());
        if (cached != null && owner.considerBlock(cached)) return Optional.of(cached);
        cached = null;
        if (center == null) center = feet.toImmutable();
        int side = FindTask.BLOCK_RADIUS * 2 + 1;
        int end = side * side * side;
        long started = System.nanoTime();
        int steps = 0;
        while (cursor < end && steps++ < FindTask.BLOCKS_PER_TICK
                && System.nanoTime() - started < 2_000_000L) {
            int index = cursor++;
            BlockPos p = center.add(index % side - FindTask.BLOCK_RADIUS,
                    index / (side * side) - FindTask.BLOCK_RADIUS,
                    (index / side) % side - FindTask.BLOCK_RADIUS);
            if (!owner.considerBlock(p)) continue;
            double score = Vec3d.ofCenter(p).squaredDistanceTo(mod.getPlayer().getPos());
            if (score < distance) { best = p.toImmutable(); distance = score; }
        }
        if (cursor < end) {
            owner.observationPending();
            return Optional.empty();
        }
        BlockPos result = best;
        center = null; best = null; cursor = 0; distance = Double.POSITIVE_INFINITY;
        if (result != null && owner.considerBlock(result)) cached = result;
        return Optional.ofNullable(cached);
    }
}
//#endif
