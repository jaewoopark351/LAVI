package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//20260729_kpopmodder: Added this planner to isolate local escape route geometry from task execution.
public class EscapeStepPlanner {
    private static final int STAIR_STEPS = 2;

    private final EscapeBlockActionPolicy blockActionPolicy;

    EscapeStepPlanner(EscapeBlockActionPolicy blockActionPolicy) {
        this.blockActionPolicy = blockActionPolicy;
    }

    Optional<EscapePlan> buildStairPlan(AltoClef mod, BlockPos origin, Direction direction) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        if (!appendSidePocketBlocks(mod, origin, direction, blocksToClear)) {
            return Optional.empty();
        }
        for (int step = 1; step <= STAIR_STEPS; step++) {
            BlockPos foot = offset(origin, direction, step + 1).up(step);
            if (!blockActionPolicy.hasSafeFloor(mod, foot.down())
                    || !blockActionPolicy.appendClearBlock(mod, foot, blocksToClear)
                    || !blockActionPolicy.appendClearBlock(mod, foot.up(), blocksToClear)) {
                return Optional.empty();
            }
        }
        if (blocksToClear.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EscapePlan(origin, direction, "stair", blocksToClear));
    }

    Optional<EscapePlan> buildSidePocketPlan(AltoClef mod, BlockPos origin, Direction direction) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        if (!appendSidePocketBlocks(mod, origin, direction, blocksToClear) || blocksToClear.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EscapePlan(origin, direction, "side", blocksToClear));
    }

    Optional<EscapePlan> buildVerticalHeadroomPlan(AltoClef mod, BlockPos origin) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        if (!blockActionPolicy.appendClearBlock(mod, origin.up(2), blocksToClear)
                || !blockActionPolicy.appendClearBlock(mod, origin.up(3), blocksToClear)
                || blocksToClear.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EscapePlan(origin, Direction.UP, "headroom", blocksToClear));
    }

    private boolean appendSidePocketBlocks(AltoClef mod, BlockPos origin, Direction direction, List<BlockPos> blocksToClear) {
        BlockPos sideFoot = offset(origin, direction, 1);
        return blockActionPolicy.hasSafeFloor(mod, sideFoot.down())
                && blockActionPolicy.appendClearBlock(mod, sideFoot, blocksToClear)
                && blockActionPolicy.appendClearBlock(mod, sideFoot.up(), blocksToClear);
    }

    private BlockPos offset(BlockPos origin, Direction direction, int distance) {
        return origin.add(
                direction.getOffsetX() * distance,
                direction.getOffsetY() * distance,
                direction.getOffsetZ() * distance);
    }
}
