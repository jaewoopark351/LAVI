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
        return searchPlan(mod, cooldownOrigins).getPlan();
    }

    public Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        return searchPlan(mod, cooldownOrigins, debugLogger).getPlan();
    }

    public EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return searchPlan(mod, cooldownOrigins, null);
    }

    public EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        if (mod.getPlayer() == null) {
            logState(debugLogger, "terrain escape plan skipped player unavailable",
                    "terrain escape plan skipped: player unavailable");
            return EscapePlanSearchResult.unavailable("player unavailable");
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        if (isOriginOnCooldown(origin, cooldownOrigins)) {
            logState(debugLogger, "terrain escape plan skipped cooldown " + origin.toShortString(),
                    "terrain escape plan skipped: origin on cooldown, origin=" + origin.toShortString());
            return EscapePlanSearchResult.unavailable("origin on cooldown, origin=" + origin.toShortString());
        }

        List<Direction> directions = orderedDirections(mod);
        String firstStairFailure = "none";
        for (Direction direction : directions) {
            Optional<EscapePlan> stairPlan = stepPlanner.buildStairPlan(mod, origin, direction);
            if (stairPlan.isPresent()) {
                logState(debugLogger, "terrain escape plan selected " + stairPlan.get().describe(),
                        "terrain escape plan selected: " + stairPlan.get().describe());
                return EscapePlanSearchResult.selected(stairPlan.get());
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
                return EscapePlanSearchResult.selected(sidePlan.get());
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
            return EscapePlanSearchResult.selected(headroomPlan.get());
        }
        String failureReason = describeSearchFailure(origin, directions, firstStairFailure, firstSideFailure,
                stepPlanner.describeVerticalHeadroomPlanFailure(mod, origin));
        logState(debugLogger, "terrain escape plan not found " + origin.toShortString(),
                "terrain escape plan not found: " + failureReason);
        return EscapePlanSearchResult.unavailable(failureReason);
    }

    public String describePlanSearchFailure(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return searchPlan(mod, cooldownOrigins).describeFailure();
    }

    private String describeSearchFailure(BlockPos origin, List<Direction> directions, String firstStairFailure,
                                        String firstSideFailure, String headroomFailure) {
        return "origin=" + origin.toShortString()
                + ", directions=" + describeDirections(directions)
                + ", firstStairFailure=" + firstStairFailure
                + ", firstSideFailure=" + firstSideFailure
                + ", headroomFailure=" + headroomFailure;
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
