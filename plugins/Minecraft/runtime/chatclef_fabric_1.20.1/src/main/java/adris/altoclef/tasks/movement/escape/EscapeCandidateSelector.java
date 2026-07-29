package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

//20260729_kpopmodder: Added this selector to separate escape candidate ordering and cooldown checks from task execution.
public class EscapeCandidateSelector {
    private final EscapeStepPlanner stepPlanner;

    public EscapeCandidateSelector() {
        this(new EscapeStepPlanner(new EscapeBlockActionPolicy()));
    }

    EscapeCandidateSelector(EscapeStepPlanner stepPlanner) {
        this.stepPlanner = stepPlanner;
    }

    public Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return findPlan(mod, cooldownOrigins, null);
    }

    public Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        if (mod.getPlayer() == null) {
            logState(debugLogger, "terrain escape plan skipped player unavailable",
                    "terrain escape plan skipped: player unavailable");
            return Optional.empty();
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        if (isOriginOnCooldown(origin, cooldownOrigins)) {
            logState(debugLogger, "terrain escape plan skipped cooldown " + origin.toShortString(),
                    "terrain escape plan skipped: origin on cooldown, origin=" + origin.toShortString());
            return Optional.empty();
        }

        List<Direction> directions = orderedDirections(mod);
        String firstStairFailure = "none";
        for (Direction direction : directions) {
            Optional<EscapePlan> stairPlan = stepPlanner.buildStairPlan(mod, origin, direction);
            if (stairPlan.isPresent()) {
                logState(debugLogger, "terrain escape plan selected " + stairPlan.get().describe(),
                        "terrain escape plan selected: " + stairPlan.get().describe());
                return stairPlan;
            }
            if ("none".equals(firstStairFailure)) {
                firstStairFailure = direction.getName() + ": "
                        + stepPlanner.describeStairPlanFailure(mod, origin, direction);
            }
        }
        String firstSideFailure = "none";
        for (Direction direction : directions) {
            Optional<EscapePlan> sidePlan = stepPlanner.buildSidePocketPlan(mod, origin, direction);
            if (sidePlan.isPresent()) {
                logState(debugLogger, "terrain escape plan selected " + sidePlan.get().describe(),
                        "terrain escape plan selected: " + sidePlan.get().describe());
                return sidePlan;
            }
            if ("none".equals(firstSideFailure)) {
                firstSideFailure = direction.getName() + ": "
                        + stepPlanner.describeSidePocketPlanFailure(mod, origin, direction);
            }
        }
        Optional<EscapePlan> headroomPlan = stepPlanner.buildVerticalHeadroomPlan(mod, origin);
        if (headroomPlan.isPresent()) {
            logState(debugLogger, "terrain escape plan selected " + headroomPlan.get().describe(),
                    "terrain escape plan selected: " + headroomPlan.get().describe());
            return headroomPlan;
        }
        logState(debugLogger, "terrain escape plan not found " + origin.toShortString(),
                "terrain escape plan not found: origin=" + origin.toShortString()
                        + ", directions=" + describeDirections(directions)
                        + ", firstStairFailure=" + firstStairFailure
                        + ", firstSideFailure=" + firstSideFailure
                        + ", headroomFailure=" + stepPlanner.describeVerticalHeadroomPlanFailure(mod, origin));
        return Optional.empty();
    }

    public String describePlanSearchFailure(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        if (mod.getPlayer() == null) {
            return "player unavailable";
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        if (isOriginOnCooldown(origin, cooldownOrigins)) {
            return "origin on cooldown, origin=" + origin.toShortString();
        }

        List<Direction> directions = orderedDirections(mod);
        String firstStairFailure = "none";
        for (Direction direction : directions) {
            if ("none".equals(firstStairFailure)) {
                firstStairFailure = direction.getName() + ": "
                        + stepPlanner.describeStairPlanFailure(mod, origin, direction);
            }
        }

        String firstSideFailure = "none";
        for (Direction direction : directions) {
            if ("none".equals(firstSideFailure)) {
                firstSideFailure = direction.getName() + ": "
                        + stepPlanner.describeSidePocketPlanFailure(mod, origin, direction);
            }
        }

        return "origin=" + origin.toShortString()
                + ", directions=" + describeDirections(directions)
                + ", firstStairFailure=" + firstStairFailure
                + ", firstSideFailure=" + firstSideFailure
                + ", headroomFailure=" + stepPlanner.describeVerticalHeadroomPlanFailure(mod, origin);
    }

    private boolean isOriginOnCooldown(BlockPos origin, Set<BlockPos> cooldownOrigins) {
        return cooldownOrigins != null && cooldownOrigins.contains(origin);
    }

    private List<Direction> orderedDirections(AltoClef mod) {
        Direction facing = mod.getPlayer().getHorizontalFacing();
        List<Direction> result = new ArrayList<>();
        addDirection(result, facing);
        addDirection(result, facing.rotateYClockwise());
        addDirection(result, facing.rotateYCounterclockwise());
        addDirection(result, facing.getOpposite());
        addDirection(result, Direction.NORTH);
        addDirection(result, Direction.SOUTH);
        addDirection(result, Direction.EAST);
        addDirection(result, Direction.WEST);
        return result;
    }

    private void addDirection(List<Direction> directions, Direction direction) {
        if (direction != null && direction.getAxis().isHorizontal() && !directions.contains(direction)) {
            directions.add(direction);
        }
    }

    private String describeDirections(List<Direction> directions) {
        List<String> names = new ArrayList<>();
        for (Direction direction : directions) {
            names.add(direction.getName());
        }
        return names.toString();
    }

    private void logState(StateChangeLogger debugLogger, String stateKey, String detail) {
        if (debugLogger != null) {
            debugLogger.state(stateKey, detail);
        }
    }
}
