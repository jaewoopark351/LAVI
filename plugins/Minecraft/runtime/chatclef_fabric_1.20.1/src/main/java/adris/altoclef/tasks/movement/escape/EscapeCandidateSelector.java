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
        DirectionalPlanSearch stairSearch = searchDirectionalPlans(mod, origin, directions,
                stepPlanner::buildStairPlan,
                stepPlanner::describeStairPlanFailure,
                debugLogger);
        if (stairSearch.getPlan().isPresent()) {
            return EscapePlanSearchResult.selected(stairSearch.getPlan().get());
        }

        DirectionalPlanSearch sideSearch = searchDirectionalPlans(mod, origin, directions,
                stepPlanner::buildSidePocketPlan,
                stepPlanner::describeSidePocketPlanFailure,
                debugLogger);
        if (sideSearch.getPlan().isPresent()) {
            return EscapePlanSearchResult.selected(sideSearch.getPlan().get());
        }

        Optional<EscapePlan> headroomPlan = stepPlanner.buildVerticalHeadroomPlan(mod, origin);
        if (headroomPlan.isPresent()) {
            logState(debugLogger, "terrain escape plan selected " + headroomPlan.get().describe(),
                    "terrain escape plan selected: " + headroomPlan.get().describe());
            return EscapePlanSearchResult.selected(headroomPlan.get());
        }
        String failureReason = describeSearchFailure(origin, directions, stairSearch.getFirstFailure(), sideSearch.getFirstFailure(),
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

    //20260729_kpopmodder: Keep directional candidate scanning reusable before adding riskier escape shapes.
    private DirectionalPlanSearch searchDirectionalPlans(AltoClef mod, BlockPos origin, List<Direction> directions,
                                                         DirectionalPlanBuilder planBuilder,
                                                         DirectionalFailureDescriber failureDescriber,
                                                         StateChangeLogger debugLogger) {
        String firstFailure = "none";
        for (Direction direction : directions) {
            Optional<EscapePlan> plan = planBuilder.build(mod, origin, direction);
            if (plan.isPresent()) {
                logState(debugLogger, "terrain escape plan selected " + plan.get().describe(),
                        "terrain escape plan selected: " + plan.get().describe());
                return DirectionalPlanSearch.selected(plan.get(), firstFailure);
            }
            if ("none".equals(firstFailure)) {
                firstFailure = direction.getName() + ": " + failureDescriber.describe(mod, origin, direction);
            }
        }
        return DirectionalPlanSearch.unavailable(firstFailure);
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

    @FunctionalInterface
    private interface DirectionalPlanBuilder {
        Optional<EscapePlan> build(AltoClef mod, BlockPos origin, Direction direction);
    }

    @FunctionalInterface
    private interface DirectionalFailureDescriber {
        String describe(AltoClef mod, BlockPos origin, Direction direction);
    }

    private static final class DirectionalPlanSearch {
        private final Optional<EscapePlan> plan;
        private final String firstFailure;

        private DirectionalPlanSearch(Optional<EscapePlan> plan, String firstFailure) {
            this.plan = plan;
            this.firstFailure = firstFailure;
        }

        private static DirectionalPlanSearch selected(EscapePlan plan, String firstFailure) {
            return new DirectionalPlanSearch(Optional.of(plan), firstFailure);
        }

        private static DirectionalPlanSearch unavailable(String firstFailure) {
            return new DirectionalPlanSearch(Optional.empty(), firstFailure);
        }

        private Optional<EscapePlan> getPlan() {
            return plan;
        }

        private String getFirstFailure() {
            return firstFailure;
        }
    }
}
