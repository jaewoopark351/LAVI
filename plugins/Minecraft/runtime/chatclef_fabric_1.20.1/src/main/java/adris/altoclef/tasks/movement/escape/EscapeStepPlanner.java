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

    String describeStairPlanFailure(AltoClef mod, BlockPos origin, Direction direction) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        String sidePocketFailure = appendSidePocketFailure(mod, origin, direction, blocksToClear);
        if (sidePocketFailure != null) {
            return "stair side pocket failed: " + sidePocketFailure;
        }
        for (int step = 1; step <= STAIR_STEPS; step++) {
            BlockPos foot = offset(origin, direction, step + 1).up(step);
            if (!blockActionPolicy.hasSafeFloor(mod, foot.down())) {
                return "stair step floor failed: step=" + step
                        + ", floor=" + foot.down().toShortString()
                        + ", reason=" + blockActionPolicy.describeFloorSafety(mod, foot.down());
            }
            if (!blockActionPolicy.appendClearBlock(mod, foot, blocksToClear)) {
                return "stair step foot failed: step=" + step
                        + ", target=" + foot.toShortString()
                        + ", space=" + blockActionPolicy.describeEscapeSpace(mod, foot)
                        + ", break=" + blockActionPolicy.describeBreakSafety(mod, foot);
            }
            if (!blockActionPolicy.appendClearBlock(mod, foot.up(), blocksToClear)) {
                return "stair step head failed: step=" + step
                        + ", target=" + foot.up().toShortString()
                        + ", space=" + blockActionPolicy.describeEscapeSpace(mod, foot.up())
                        + ", break=" + blockActionPolicy.describeBreakSafety(mod, foot.up());
            }
        }
        if (blocksToClear.isEmpty()) {
            return "stair route already clear; no blocks to clear";
        }
        return "stair route appears available";
    }

    String describeSidePocketPlanFailure(AltoClef mod, BlockPos origin, Direction direction) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        String sidePocketFailure = appendSidePocketFailure(mod, origin, direction, blocksToClear);
        if (sidePocketFailure != null) {
            return sidePocketFailure;
        }
        if (blocksToClear.isEmpty()) {
            return "side pocket already clear; no blocks to clear";
        }
        return "side pocket appears available";
    }

    String describeVerticalHeadroomPlanFailure(AltoClef mod, BlockPos origin) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        BlockPos lowerHeadroom = origin.up(2);
        if (!blockActionPolicy.appendClearBlock(mod, lowerHeadroom, blocksToClear)) {
            return "lower headroom failed: target=" + lowerHeadroom.toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, lowerHeadroom)
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, lowerHeadroom);
        }
        BlockPos upperHeadroom = origin.up(3);
        if (!blockActionPolicy.appendClearBlock(mod, upperHeadroom, blocksToClear)) {
            return "upper headroom failed: target=" + upperHeadroom.toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, upperHeadroom)
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, upperHeadroom);
        }
        if (blocksToClear.isEmpty()) {
            return "headroom already clear; no blocks to clear";
        }
        return "headroom appears available";
    }

    private boolean appendSidePocketBlocks(AltoClef mod, BlockPos origin, Direction direction, List<BlockPos> blocksToClear) {
        BlockPos sideFoot = offset(origin, direction, 1);
        return blockActionPolicy.hasSafeFloor(mod, sideFoot.down())
                && blockActionPolicy.appendClearBlock(mod, sideFoot, blocksToClear)
                && blockActionPolicy.appendClearBlock(mod, sideFoot.up(), blocksToClear);
    }

    private String appendSidePocketFailure(AltoClef mod, BlockPos origin, Direction direction, List<BlockPos> blocksToClear) {
        BlockPos sideFoot = offset(origin, direction, 1);
        if (!blockActionPolicy.hasSafeFloor(mod, sideFoot.down())) {
            return "side floor failed: floor=" + sideFoot.down().toShortString()
                    + ", reason=" + blockActionPolicy.describeFloorSafety(mod, sideFoot.down());
        }
        if (!blockActionPolicy.appendClearBlock(mod, sideFoot, blocksToClear)) {
            return "side foot failed: target=" + sideFoot.toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, sideFoot)
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, sideFoot);
        }
        if (!blockActionPolicy.appendClearBlock(mod, sideFoot.up(), blocksToClear)) {
            return "side head failed: target=" + sideFoot.up().toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, sideFoot.up())
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, sideFoot.up());
        }
        return null;
    }

    private BlockPos offset(BlockPos origin, Direction direction, int distance) {
        return origin.add(
                direction.getOffsetX() * distance,
                direction.getOffsetY() * distance,
                direction.getOffsetZ() * distance);
    }
}
