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
        return searchPlan(mod, cooldownOrigins, Set.of()).getPlan();
    }

    public Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                         Set<EscapeCandidateKey> cooldownCandidates) {
        return searchPlan(mod, cooldownOrigins, cooldownCandidates).getPlan();
    }

    public Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        return searchPlan(mod, cooldownOrigins, Set.of(), debugLogger).getPlan();
    }

    public Optional<EscapePlan> findPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                         Set<EscapeCandidateKey> cooldownCandidates,
                                         StateChangeLogger debugLogger) {
        return searchPlan(mod, cooldownOrigins, cooldownCandidates, debugLogger).getPlan();
    }

    public EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return searchPlan(mod, cooldownOrigins, Set.of(), null);
    }

    public EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                             Set<EscapeCandidateKey> cooldownCandidates) {
        return searchPlan(mod, cooldownOrigins, cooldownCandidates, null);
    }

    public EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        return searchPlan(mod, cooldownOrigins, Set.of(), debugLogger);
    }

    public EscapePlanSearchResult searchPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                             Set<EscapeCandidateKey> cooldownCandidates,
                                             StateChangeLogger debugLogger) {
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
                "stair",
                stepPlanner::buildStairPlan,
                stepPlanner::describeStairPlanFailure,
                cooldownCandidates,
                debugLogger);
        if (stairSearch.getPlan().isPresent()) {
            return EscapePlanSearchResult.selected(stairSearch.getPlan().get());
        }

        DirectionalPlanSearch sideSearch = searchDirectionalPlans(mod, origin, directions,
                "side",
                stepPlanner::buildSidePocketPlan,
                stepPlanner::describeSidePocketPlanFailure,
                cooldownCandidates,
                debugLogger);
        if (sideSearch.getPlan().isPresent()) {
            return EscapePlanSearchResult.selected(sideSearch.getPlan().get());
        }

        String headroomFailure;
        Optional<EscapePlan> headroomPlan = Optional.empty();
        EscapeCandidateKey headroomCandidate = new EscapeCandidateKey(origin, "headroom", Direction.UP);
        if (isCandidateOnCooldown(headroomCandidate, cooldownCandidates)) {
            headroomFailure = "headroom candidate on cooldown, " + headroomCandidate.describe();
            logState(debugLogger, "terrain escape plan skipped candidate cooldown " + headroomCandidate.describe(),
                    "terrain escape plan skipped: candidate on cooldown, " + headroomCandidate.describe());
        } else {
            headroomPlan = stepPlanner.buildVerticalHeadroomPlan(mod, origin);
            headroomFailure = stepPlanner.describeVerticalHeadroomPlanFailure(mod, origin);
        }
        if (headroomPlan.isPresent()) {
            logState(debugLogger, "terrain escape plan selected " + headroomPlan.get().describe(),
                    "terrain escape plan selected: " + headroomPlan.get().describe());
            return EscapePlanSearchResult.selected(headroomPlan.get());
        }

        DirectionalPlanSearch spiralSearch = searchSpiralDirectionalPlans(mod, origin, directions,
                cooldownCandidates,
                debugLogger);
        if (spiralSearch.getPlan().isPresent()) {
            return EscapePlanSearchResult.selected(spiralSearch.getPlan().get());
        }

        String failureReason = describeSearchFailure(origin, directions, stairSearch.getFirstFailure(), sideSearch.getFirstFailure(),
                headroomFailure, spiralSearch.getFirstFailure());
        logState(debugLogger, "terrain escape plan not found " + origin.toShortString(),
                "terrain escape plan not found: " + failureReason);
        return EscapePlanSearchResult.unavailable(failureReason);
    }

    public EscapePlanSearchResult searchSpiralPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return searchSpiralPlan(mod, cooldownOrigins, Set.of(), null);
    }

    public EscapePlanSearchResult searchSidePlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return searchSidePlan(mod, cooldownOrigins, Set.of(), null);
    }

    public EscapePlanSearchResult searchSidePlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                 Set<EscapeCandidateKey> cooldownCandidates) {
        return searchSidePlan(mod, cooldownOrigins, cooldownCandidates, null);
    }

    public EscapePlanSearchResult searchSidePlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        return searchSidePlan(mod, cooldownOrigins, Set.of(), debugLogger);
    }

    public EscapePlanSearchResult searchSidePlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                 Set<EscapeCandidateKey> cooldownCandidates,
                                                 StateChangeLogger debugLogger) {
        if (mod.getPlayer() == null) {
            logState(debugLogger, "terrain side escape plan skipped player unavailable",
                    "terrain side escape plan skipped: player unavailable");
            return EscapePlanSearchResult.unavailable("player unavailable");
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        if (isOriginOnCooldown(origin, cooldownOrigins)) {
            logState(debugLogger, "terrain side escape plan skipped cooldown " + origin.toShortString(),
                    "terrain side escape plan skipped: origin on cooldown, origin=" + origin.toShortString());
            return EscapePlanSearchResult.unavailable("origin on cooldown, origin=" + origin.toShortString());
        }

        List<Direction> directions = orderedDirections(mod);
        DirectionalPlanSearch sideSearch = searchDirectionalPlans(mod, origin, directions,
                "side",
                stepPlanner::buildSidePocketPlan,
                stepPlanner::describeSidePocketPlanFailure,
                cooldownCandidates,
                debugLogger);
        if (sideSearch.getPlan().isPresent()) {
            return EscapePlanSearchResult.selected(sideSearch.getPlan().get());
        }

        String failureReason = "origin=" + origin.toShortString()
                + ", directions=" + describeDirections(directions)
                + ", firstSideFailure=" + sideSearch.getFirstFailure();
        logState(debugLogger, "terrain side escape plan not found " + origin.toShortString(),
                "terrain side escape plan not found: " + failureReason);
        return EscapePlanSearchResult.unavailable(failureReason);
    }

    public EscapePlanSearchResult searchHeadroomPlan(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return searchHeadroomPlan(mod, cooldownOrigins, Set.of(), null);
    }

    public EscapePlanSearchResult searchHeadroomPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                     Set<EscapeCandidateKey> cooldownCandidates) {
        return searchHeadroomPlan(mod, cooldownOrigins, cooldownCandidates, null);
    }

    public EscapePlanSearchResult searchHeadroomPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                     StateChangeLogger debugLogger) {
        return searchHeadroomPlan(mod, cooldownOrigins, Set.of(), debugLogger);
    }

    public EscapePlanSearchResult searchHeadroomPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                     Set<EscapeCandidateKey> cooldownCandidates,
                                                     StateChangeLogger debugLogger) {
        if (mod.getPlayer() == null) {
            logState(debugLogger, "terrain headroom escape plan skipped player unavailable",
                    "terrain headroom escape plan skipped: player unavailable");
            return EscapePlanSearchResult.unavailable("player unavailable");
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        if (isOriginOnCooldown(origin, cooldownOrigins)) {
            logState(debugLogger, "terrain headroom escape plan skipped cooldown " + origin.toShortString(),
                    "terrain headroom escape plan skipped: origin on cooldown, origin=" + origin.toShortString());
            return EscapePlanSearchResult.unavailable("origin on cooldown, origin=" + origin.toShortString());
        }

        String headroomFailure;
        Optional<EscapePlan> headroomPlan = Optional.empty();
        EscapeCandidateKey headroomCandidate = new EscapeCandidateKey(origin, "headroom", Direction.UP);
        if (isCandidateOnCooldown(headroomCandidate, cooldownCandidates)) {
            headroomFailure = "headroom candidate on cooldown, " + headroomCandidate.describe();
            logState(debugLogger, "terrain escape plan skipped candidate cooldown " + headroomCandidate.describe(),
                    "terrain escape plan skipped: candidate on cooldown, " + headroomCandidate.describe());
        } else {
            headroomPlan = stepPlanner.buildVerticalHeadroomPlan(mod, origin);
            headroomFailure = stepPlanner.describeVerticalHeadroomPlanFailure(mod, origin);
        }
        if (headroomPlan.isPresent()) {
            logState(debugLogger, "terrain escape plan selected " + headroomPlan.get().describe(),
                    "terrain escape plan selected: " + headroomPlan.get().describe());
            return EscapePlanSearchResult.selected(headroomPlan.get());
        }

        String failureReason = "origin=" + origin.toShortString()
                + ", headroomFailure=" + headroomFailure;
        logState(debugLogger, "terrain headroom escape plan not found " + origin.toShortString(),
                "terrain headroom escape plan not found: " + failureReason);
        return EscapePlanSearchResult.unavailable(failureReason);
    }

    public EscapePlanSearchResult searchSpiralPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                   Set<EscapeCandidateKey> cooldownCandidates) {
        return searchSpiralPlan(mod, cooldownOrigins, cooldownCandidates, null);
    }

    public EscapePlanSearchResult searchSpiralPlan(AltoClef mod, Set<BlockPos> cooldownOrigins, StateChangeLogger debugLogger) {
        return searchSpiralPlan(mod, cooldownOrigins, Set.of(), debugLogger);
    }

    public EscapePlanSearchResult searchSpiralPlan(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                                   Set<EscapeCandidateKey> cooldownCandidates,
                                                   StateChangeLogger debugLogger) {
        if (mod.getPlayer() == null) {
            logState(debugLogger, "terrain spiral escape plan skipped player unavailable",
                    "terrain spiral escape plan skipped: player unavailable");
            return EscapePlanSearchResult.unavailable("player unavailable");
        }

        BlockPos origin = mod.getPlayer().getBlockPos();
        if (isOriginOnCooldown(origin, cooldownOrigins)) {
            logState(debugLogger, "terrain spiral escape plan skipped cooldown " + origin.toShortString(),
                    "terrain spiral escape plan skipped: origin on cooldown, origin=" + origin.toShortString());
            return EscapePlanSearchResult.unavailable("origin on cooldown, origin=" + origin.toShortString());
        }

        List<Direction> directions = orderedDirections(mod);
        DirectionalPlanSearch spiralSearch = searchSpiralDirectionalPlans(mod, origin, directions,
                cooldownCandidates,
                debugLogger);
        if (spiralSearch.getPlan().isPresent()) {
            return EscapePlanSearchResult.selected(spiralSearch.getPlan().get());
        }

        String failureReason = describeSpiralSearchFailure(origin, directions, spiralSearch.getFirstFailure());
        logState(debugLogger, "terrain spiral escape plan not found " + origin.toShortString(),
                "terrain spiral escape plan not found: " + failureReason);
        return EscapePlanSearchResult.unavailable(failureReason);
    }

    public String describePlanSearchFailure(AltoClef mod, Set<BlockPos> cooldownOrigins) {
        return searchPlan(mod, cooldownOrigins, Set.of()).describeFailure();
    }

    public String describePlanSearchFailure(AltoClef mod, Set<BlockPos> cooldownOrigins,
                                            Set<EscapeCandidateKey> cooldownCandidates) {
        return searchPlan(mod, cooldownOrigins, cooldownCandidates).describeFailure();
    }

    private String describeSearchFailure(BlockPos origin, List<Direction> directions, String firstStairFailure,
                                        String firstSideFailure, String headroomFailure, String firstSpiralFailure) {
        return "origin=" + origin.toShortString()
                + ", directions=" + describeDirections(directions)
                + ", firstStairFailure=" + firstStairFailure
                + ", firstSideFailure=" + firstSideFailure
                + ", headroomFailure=" + headroomFailure
                + ", firstSpiralFailure=" + firstSpiralFailure;
    }

    private String describeSpiralSearchFailure(BlockPos origin, List<Direction> directions, String firstSpiralFailure) {
        return "origin=" + origin.toShortString()
                + ", directions=" + describeDirections(directions)
                + ", firstSpiralFailure=" + firstSpiralFailure;
    }

    //20260729_kpopmodder: Keep directional candidate scanning reusable before adding riskier escape shapes.
    private DirectionalPlanSearch searchDirectionalPlans(AltoClef mod, BlockPos origin, List<Direction> directions,
                                                         String kind,
                                                         DirectionalPlanBuilder planBuilder,
                                                         DirectionalFailureDescriber failureDescriber,
                                                         Set<EscapeCandidateKey> cooldownCandidates,
                                                         StateChangeLogger debugLogger) {
        String firstFailure = "none";
        for (Direction direction : directions) {
            DirectionalPlanSearch candidateSearch = searchCandidatePlan(mod, origin, direction, kind,
                    planBuilder, failureDescriber, cooldownCandidates, debugLogger);
            if (candidateSearch.getPlan().isPresent()) {
                return DirectionalPlanSearch.selected(candidateSearch.getPlan().get(), firstFailure);
            }
            if ("none".equals(firstFailure)) {
                firstFailure = direction.getName() + ": " + candidateSearch.getFirstFailure();
            }
        }
        return DirectionalPlanSearch.unavailable(firstFailure);
    }

    //20260729_kpopmodder: Spiral clockwise/counterclockwise are separate cooldown candidates.
    private DirectionalPlanSearch searchSpiralDirectionalPlans(AltoClef mod, BlockPos origin, List<Direction> directions,
                                                               Set<EscapeCandidateKey> cooldownCandidates,
                                                               StateChangeLogger debugLogger) {
        String firstFailure = "none";
        for (Direction direction : directions) {
            DirectionalPlanSearch clockwiseSearch = searchCandidatePlan(mod, origin, direction, "spiral_clockwise",
                    stepPlanner::buildSpiralClockwisePlan,
                    stepPlanner::describeSpiralClockwisePlanFailure,
                    cooldownCandidates,
                    debugLogger);
            if (clockwiseSearch.getPlan().isPresent()) {
                return DirectionalPlanSearch.selected(clockwiseSearch.getPlan().get(), firstFailure);
            }

            DirectionalPlanSearch counterClockwiseSearch = searchCandidatePlan(mod, origin, direction,
                    "spiral_counterclockwise",
                    stepPlanner::buildSpiralCounterClockwisePlan,
                    stepPlanner::describeSpiralCounterClockwisePlanFailure,
                    cooldownCandidates,
                    debugLogger);
            if (counterClockwiseSearch.getPlan().isPresent()) {
                return DirectionalPlanSearch.selected(counterClockwiseSearch.getPlan().get(), firstFailure);
            }
            if ("none".equals(firstFailure)) {
                firstFailure = direction.getName()
                        + ": clockwise=" + clockwiseSearch.getFirstFailure()
                        + ", counterclockwise=" + counterClockwiseSearch.getFirstFailure();
            }
        }
        return DirectionalPlanSearch.unavailable(firstFailure);
    }

    private DirectionalPlanSearch searchCandidatePlan(AltoClef mod, BlockPos origin, Direction direction, String kind,
                                                      DirectionalPlanBuilder planBuilder,
                                                      DirectionalFailureDescriber failureDescriber,
                                                      Set<EscapeCandidateKey> cooldownCandidates,
                                                      StateChangeLogger debugLogger) {
        EscapeCandidateKey candidateKey = new EscapeCandidateKey(origin, kind, direction);
        if (isCandidateOnCooldown(candidateKey, cooldownCandidates)) {
            logState(debugLogger, "terrain escape plan skipped candidate cooldown " + candidateKey.describe(),
                    "terrain escape plan skipped: candidate on cooldown, " + candidateKey.describe());
            return DirectionalPlanSearch.unavailable("candidate on cooldown, " + candidateKey.describe());
        }

        Optional<EscapePlan> plan = planBuilder.build(mod, origin, direction);
        if (plan.isPresent()) {
            logState(debugLogger, "terrain escape plan selected " + plan.get().describe(),
                    "terrain escape plan selected: " + plan.get().describe());
            return DirectionalPlanSearch.selected(plan.get(), "none");
        }
        return DirectionalPlanSearch.unavailable(failureDescriber.describe(mod, origin, direction));
    }

    private boolean isOriginOnCooldown(BlockPos origin, Set<BlockPos> cooldownOrigins) {
        return cooldownOrigins != null && cooldownOrigins.contains(origin);
    }

    private boolean isCandidateOnCooldown(EscapeCandidateKey candidateKey, Set<EscapeCandidateKey> cooldownCandidates) {
        return cooldownCandidates != null && cooldownCandidates.contains(candidateKey);
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
