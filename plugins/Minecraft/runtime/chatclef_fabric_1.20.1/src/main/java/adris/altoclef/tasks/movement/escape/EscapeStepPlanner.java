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
        return planStairBlocks(mod, origin, direction).toPlan(origin, direction, "stair");
    }

    Optional<EscapePlan> buildSidePocketPlan(AltoClef mod, BlockPos origin, Direction direction) {
        return planSidePocketBlocks(mod, origin, direction).toPlan(origin, direction, "side");
    }

    Optional<EscapePlan> buildVerticalHeadroomPlan(AltoClef mod, BlockPos origin) {
        return planVerticalHeadroomBlocks(mod, origin).toPlan(origin, Direction.UP, "headroom");
    }

    Optional<EscapePlan> buildSpiralPlan(AltoClef mod, BlockPos origin, Direction direction) {
        Optional<EscapePlan> clockwisePlan = buildSpiralClockwisePlan(mod, origin, direction);
        if (clockwisePlan.isPresent()) {
            return clockwisePlan;
        }
        return buildSpiralCounterClockwisePlan(mod, origin, direction);
    }

    Optional<EscapePlan> buildSpiralClockwisePlan(AltoClef mod, BlockPos origin, Direction direction) {
        return planSpiralBlocks(mod, origin, direction, true).toPlan(origin, direction, "spiral_clockwise");
    }

    Optional<EscapePlan> buildSpiralCounterClockwisePlan(AltoClef mod, BlockPos origin, Direction direction) {
        return planSpiralBlocks(mod, origin, direction, false).toPlan(origin, direction, "spiral_counterclockwise");
    }

    String describeStairPlanFailure(AltoClef mod, BlockPos origin, Direction direction) {
        return planStairBlocks(mod, origin, direction).describe(
                "stair route already clear; no blocks to clear",
                "stair route appears available");
    }

    String describeSidePocketPlanFailure(AltoClef mod, BlockPos origin, Direction direction) {
        return planSidePocketBlocks(mod, origin, direction).describe(
                "side pocket already clear; no blocks to clear",
                "side pocket appears available");
    }

    String describeVerticalHeadroomPlanFailure(AltoClef mod, BlockPos origin) {
        return planVerticalHeadroomBlocks(mod, origin).describe(
                "headroom already clear; no blocks to clear",
                "headroom appears available");
    }

    String describeSpiralPlanFailure(AltoClef mod, BlockPos origin, Direction direction) {
        StepPlanAttempt clockwiseAttempt = planSpiralBlocks(mod, origin, direction, true);
        StepPlanAttempt counterClockwiseAttempt = planSpiralBlocks(mod, origin, direction, false);
        return "clockwise=" + clockwiseAttempt.describe(
                "spiral clockwise route already clear; no blocks to clear",
                "spiral clockwise appears available")
                + ", counterclockwise=" + counterClockwiseAttempt.describe(
                "spiral counterclockwise route already clear; no blocks to clear",
                "spiral counterclockwise appears available");
    }

    String describeSpiralClockwisePlanFailure(AltoClef mod, BlockPos origin, Direction direction) {
        return planSpiralBlocks(mod, origin, direction, true).describe(
                "spiral clockwise route already clear; no blocks to clear",
                "spiral clockwise appears available");
    }

    String describeSpiralCounterClockwisePlanFailure(AltoClef mod, BlockPos origin, Direction direction) {
        return planSpiralBlocks(mod, origin, direction, false).describe(
                "spiral counterclockwise route already clear; no blocks to clear",
                "spiral counterclockwise appears available");
    }

    //20260729_kpopmodder: Build and failure diagnostics now share the same route-planning result.
    private StepPlanAttempt planStairBlocks(AltoClef mod, BlockPos origin, Direction direction) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        StepPlanAttempt sidePocketAttempt = appendSidePocketBlocks(mod, origin, direction, blocksToClear);
        if (sidePocketAttempt.hasFailure()) {
            return StepPlanAttempt.failed("stair side pocket failed: " + sidePocketAttempt.getFailureReason());
        }
        for (int step = 1; step <= STAIR_STEPS; step++) {
            BlockPos foot = offset(origin, direction, step + 1).up(step);
            if (!blockActionPolicy.hasSafeFloor(mod, foot.down())) {
                return StepPlanAttempt.failed(describeFloorFailure(mod,
                        "stair step floor failed: step=" + step,
                        foot.down(),
                        "stair step " + step + " support"));
            }
            if (!blockActionPolicy.appendClearBlock(mod, foot, blocksToClear)) {
                return StepPlanAttempt.failed("stair step foot failed: step=" + step
                        + ", target=" + foot.toShortString()
                        + ", space=" + blockActionPolicy.describeEscapeSpace(mod, foot)
                        + ", break=" + blockActionPolicy.describeBreakSafety(mod, foot));
            }
            if (!blockActionPolicy.appendClearBlock(mod, foot.up(), blocksToClear)) {
                return StepPlanAttempt.failed("stair step head failed: step=" + step
                        + ", target=" + foot.up().toShortString()
                        + ", space=" + blockActionPolicy.describeEscapeSpace(mod, foot.up())
                        + ", break=" + blockActionPolicy.describeBreakSafety(mod, foot.up()));
            }
        }
        return StepPlanAttempt.available(blocksToClear);
    }

    private StepPlanAttempt planSidePocketBlocks(AltoClef mod, BlockPos origin, Direction direction) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        StepPlanAttempt sidePocketAttempt = appendSidePocketBlocks(mod, origin, direction, blocksToClear);
        if (sidePocketAttempt.hasFailure()) {
            return sidePocketAttempt;
        }
        return StepPlanAttempt.available(blocksToClear);
    }

    private StepPlanAttempt planVerticalHeadroomBlocks(AltoClef mod, BlockPos origin) {
        List<BlockPos> blocksToClear = new ArrayList<>();
        BlockPos lowerHeadroom = origin.up(2);
        if (!blockActionPolicy.appendClearBlock(mod, lowerHeadroom, blocksToClear)) {
            return StepPlanAttempt.failed("lower headroom failed: target=" + lowerHeadroom.toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, lowerHeadroom)
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, lowerHeadroom));
        }
        BlockPos upperHeadroom = origin.up(3);
        if (!blockActionPolicy.appendClearBlock(mod, upperHeadroom, blocksToClear)) {
            return StepPlanAttempt.failed("upper headroom failed: target=" + upperHeadroom.toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, upperHeadroom)
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, upperHeadroom));
        }
        return StepPlanAttempt.available(blocksToClear);
    }

    //20260729_kpopmodder: Spiral routes are kept as the final fallback so existing escape ordering stays stable.
    private StepPlanAttempt planSpiralBlocks(AltoClef mod, BlockPos origin, Direction direction, boolean clockwise) {
        String turnName = clockwise ? "clockwise" : "counterclockwise";
        Direction turnDirection = clockwise ? direction.rotateYClockwise() : direction.rotateYCounterclockwise();
        List<BlockPos> blocksToClear = new ArrayList<>();

        StepPlanAttempt sidePocketAttempt = appendSidePocketBlocks(mod, origin, direction, blocksToClear);
        if (sidePocketAttempt.hasFailure()) {
            return StepPlanAttempt.failed("spiral " + turnName + " side pocket failed: "
                    + sidePocketAttempt.getFailureReason());
        }

        BlockPos cornerFoot = offset(offset(origin, direction, 1), turnDirection, 1).up(1);
        StepPlanAttempt cornerAttempt = appendSpiralStepBlocks(mod, cornerFoot, blocksToClear, turnName, 1);
        if (cornerAttempt.hasFailure()) {
            return cornerAttempt;
        }

        BlockPos exitFoot = offset(origin, turnDirection, 1).up(2);
        StepPlanAttempt exitAttempt = appendSpiralStepBlocks(mod, exitFoot, blocksToClear, turnName, 2);
        if (exitAttempt.hasFailure()) {
            return exitAttempt;
        }

        return StepPlanAttempt.available(blocksToClear);
    }

    private StepPlanAttempt appendSpiralStepBlocks(AltoClef mod, BlockPos foot, List<BlockPos> blocksToClear,
                                                   String turnName, int step) {
        if (!blockActionPolicy.hasSafeFloor(mod, foot.down())) {
            return StepPlanAttempt.failed(describeFloorFailure(mod,
                    "spiral " + turnName + " step floor failed: step=" + step,
                    foot.down(),
                    "spiral " + turnName + " step " + step + " support"));
        }
        if (!blockActionPolicy.appendClearBlock(mod, foot, blocksToClear)) {
            return StepPlanAttempt.failed("spiral " + turnName + " step foot failed: step=" + step
                    + ", target=" + foot.toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, foot)
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, foot));
        }
        if (!blockActionPolicy.appendClearBlock(mod, foot.up(), blocksToClear)) {
            return StepPlanAttempt.failed("spiral " + turnName + " step head failed: step=" + step
                    + ", target=" + foot.up().toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, foot.up())
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, foot.up()));
        }
        return StepPlanAttempt.available(blocksToClear);
    }

    private StepPlanAttempt appendSidePocketBlocks(AltoClef mod, BlockPos origin, Direction direction,
                                                   List<BlockPos> blocksToClear) {
        BlockPos sideFoot = offset(origin, direction, 1);
        if (!blockActionPolicy.hasSafeFloor(mod, sideFoot.down())) {
            return StepPlanAttempt.failed(describeFloorFailure(mod,
                    "side floor failed",
                    sideFoot.down(),
                    "side pocket support"));
        }
        if (!blockActionPolicy.appendClearBlock(mod, sideFoot, blocksToClear)) {
            return StepPlanAttempt.failed("side foot failed: target=" + sideFoot.toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, sideFoot)
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, sideFoot));
        }
        if (!blockActionPolicy.appendClearBlock(mod, sideFoot.up(), blocksToClear)) {
            return StepPlanAttempt.failed("side head failed: target=" + sideFoot.up().toShortString()
                    + ", space=" + blockActionPolicy.describeEscapeSpace(mod, sideFoot.up())
                    + ", break=" + blockActionPolicy.describeBreakSafety(mod, sideFoot.up()));
        }
        return StepPlanAttempt.available(blocksToClear);
    }

    private BlockPos offset(BlockPos origin, Direction direction, int distance) {
        return origin.add(
                direction.getOffsetX() * distance,
                direction.getOffsetY() * distance,
                direction.getOffsetZ() * distance);
    }

    private String describeFloorFailure(AltoClef mod, String prefix, BlockPos floor, String placeReason) {
        return prefix
                + ", floor=" + floor.toShortString()
                + ", reason=" + blockActionPolicy.describeFloorSafety(mod, floor)
                + ", placeCandidate=" + blockActionPolicy.describeSupportPlaceCandidate(mod, floor, placeReason);
    }

    private static final class StepPlanAttempt {
        private final List<BlockPos> blocksToClear;
        private final List<EscapePlaceCandidate> placeCandidates;
        private final String failureReason;

        private StepPlanAttempt(List<BlockPos> blocksToClear, List<EscapePlaceCandidate> placeCandidates,
                                String failureReason) {
            this.blocksToClear = blocksToClear;
            this.placeCandidates = placeCandidates;
            this.failureReason = failureReason;
        }

        private static StepPlanAttempt available(List<BlockPos> blocksToClear) {
            return available(blocksToClear, List.of());
        }

        private static StepPlanAttempt available(List<BlockPos> blocksToClear,
                                                 List<EscapePlaceCandidate> placeCandidates) {
            return new StepPlanAttempt(blocksToClear, placeCandidates, null);
        }

        private static StepPlanAttempt failed(String failureReason) {
            return new StepPlanAttempt(List.of(), List.of(), failureReason);
        }

        private boolean hasFailure() {
            return failureReason != null;
        }

        private String getFailureReason() {
            return failureReason;
        }

        private Optional<EscapePlan> toPlan(BlockPos origin, Direction direction, String kind) {
            if (hasFailure() || blocksToClear.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new EscapePlan(origin, direction, kind, blocksToClear, placeCandidates));
        }

        private String describe(String emptyMessage, String availableMessage) {
            if (hasFailure()) {
                return failureReason;
            }
            if (blocksToClear.isEmpty()) {
                return emptyMessage;
            }
            return availableMessage;
        }
    }
}
