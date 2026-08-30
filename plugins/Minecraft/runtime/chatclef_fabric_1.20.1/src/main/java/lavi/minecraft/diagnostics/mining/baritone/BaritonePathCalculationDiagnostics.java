package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.PathingCommand;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.PathCalculationResult;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.calc.PathNode;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.mining.baritone.calculation.BaritoneCalculationSchedulingDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.calculation.BaritoneGoalRequestDecisionDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.calculation.BaritonePathAdoptionDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.calculation.BaritonePathBuildDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.calculation.BaritonePathCancelDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.calculation.BaritonePathPostProcessDiagnostics;
import lavi.minecraft.diagnostics.mining.baritone.calculation.BaritonePathfinderLifecycleDiagnostics;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Supplier;

//20260806_kpopmodder: Preserve the established Baritone calculation diagnostics API as a thin compatibility facade.
public final class BaritonePathCalculationDiagnostics {
    private BaritonePathCalculationDiagnostics() {
    }

    public static void logGoalRequestDecision(PathingBehavior behavior,
                                              PathingCommand command,
                                              boolean accepted,
                                              PathExecutor current,
                                              PathExecutor next,
                                              AbstractNodeCostSearch inProgress,
                                              Goal activeGoal,
                                              BetterBlockPos expectedSegmentStart,
                                              boolean cancelRequested,
                                              boolean calcFailedLastTick) {
        BaritoneGoalRequestDecisionDiagnostics.log(
                behavior, command, accepted, current, next, inProgress, activeGoal,
                expectedSegmentStart, cancelRequested, calcFailedLastTick
        );
    }

    public static void logCalculationScheduled(PathingBehavior behavior,
                                               BlockPos start,
                                               boolean firstSegment,
                                               CalculationContext context,
                                               Goal activeGoal,
                                               PathExecutor current,
                                               PathExecutor next,
                                               AbstractNodeCostSearch inProgress,
                                               BetterBlockPos expectedSegmentStart) {
        BaritoneCalculationSchedulingDiagnostics.log(
                behavior, start, firstSegment, context, activeGoal, current, next,
                inProgress, expectedSegmentStart
        );
    }

    public static void logWorkerStarted(PathingBehavior behavior,
                                        boolean firstSegment,
                                        BlockPos pathStart,
                                        Goal requestedGoal,
                                        AbstractNodeCostSearch pathfinder,
                                        long primaryTimeout,
                                        long failureTimeout,
                                        PathExecutor current,
                                        PathExecutor next,
                                        AbstractNodeCostSearch inProgress,
                                        BetterBlockPos expectedSegmentStart) {
        BaritonePathfinderLifecycleDiagnostics.logWorkerStarted(
                behavior, firstSegment, pathStart, requestedGoal, pathfinder,
                primaryTimeout, failureTimeout, current, next, inProgress, expectedSegmentStart
        );
    }

    public static void logPathfinderCalculateStarted(AbstractNodeCostSearch pathfinder,
                                                     long primaryTimeout,
                                                     long failureTimeout,
                                                     Goal pathfinderGoal,
                                                     BetterBlockPos realStart,
                                                     boolean cancelRequested) {
        BaritonePathfinderLifecycleDiagnostics.logCalculateStarted(
                pathfinder, primaryTimeout, failureTimeout, pathfinderGoal, realStart, cancelRequested
        );
    }

    public static void logPathfinderCalculateCompleted(AbstractNodeCostSearch pathfinder,
                                                       PathCalculationResult result,
                                                       long elapsedNanos,
                                                       boolean cancelRequested,
                                                       Object[] searchStateFields) {
        BaritonePathfinderLifecycleDiagnostics.logCalculateCompleted(
                pathfinder, result, elapsedNanos, cancelRequested, searchStateFields
        );
    }

    public static void logPathfinderCalculateCompleted(AbstractNodeCostSearch pathfinder,
                                                       PathCalculationResult result,
                                                       long elapsedNanos,
                                                       boolean cancelRequested,
                                                       Supplier<Object[]> searchStateFieldsSupplier) {
        BaritonePathfinderLifecycleDiagnostics.logCalculateCompleted(
                pathfinder, result, elapsedNanos, cancelRequested, searchStateFieldsSupplier
        );
    }

    public static void logCalculate0Enter(AbstractNodeCostSearch pathfinder,
                                          long primaryTimeout,
                                          long failureTimeout) {
        BaritonePathfinderLifecycleDiagnostics.logCalculate0Enter(pathfinder, primaryTimeout, failureTimeout);
    }

    public static void logCalculate0Return(AbstractNodeCostSearch pathfinder,
                                           Optional<IPath> result) {
        BaritonePathfinderLifecycleDiagnostics.logCalculate0Return(pathfinder, result);
    }

    public static void logPathBuildEnter(Object path,
                                         BetterBlockPos realStart,
                                         PathNode startNode,
                                         PathNode endNode,
                                         int numNodes,
                                         Goal goal,
                                         CalculationContext context) {
        BaritonePathBuildDiagnostics.logEnter(path, realStart, startNode, endNode, numNodes, goal, context);
    }

    public static void logPathBuildReturn(Object path,
                                          BetterBlockPos realStart,
                                          PathNode startNode,
                                          PathNode endNode,
                                          int numNodes,
                                          Goal goal,
                                          CalculationContext context) {
        BaritonePathBuildDiagnostics.logReturn(path, realStart, startNode, endNode, numNodes, goal, context);
    }

    public static void logPathPostProcessEnter(IPath path) {
        BaritonePathPostProcessDiagnostics.logEnter(path);
    }

    public static void logPathPostProcessReturn(IPath path, IPath result) {
        BaritonePathPostProcessDiagnostics.logReturn(path, result);
    }

    public static void logAdoptionDecisionBeforeClear(PathingBehavior behavior,
                                                      boolean firstSegment,
                                                      BlockPos pathStart,
                                                      Goal requestedGoal,
                                                      AbstractNodeCostSearch pathfinder,
                                                      PathExecutor current,
                                                      PathExecutor next,
                                                      AbstractNodeCostSearch inProgress,
                                                      BetterBlockPos expectedSegmentStart) {
        BaritonePathAdoptionDiagnostics.logBeforeClear(
                behavior, firstSegment, pathStart, requestedGoal, pathfinder,
                current, next, inProgress, expectedSegmentStart
        );
    }

    public static void logForceCancelBoundary(PathingBehavior behavior,
                                              String phase,
                                              PathExecutor current,
                                              PathExecutor next,
                                              AbstractNodeCostSearch inProgress,
                                              Goal activeGoal,
                                              boolean cancelRequested,
                                              boolean calcFailedLastTick) {
        BaritonePathCancelDiagnostics.log(
                behavior, phase, current, next, inProgress, activeGoal,
                cancelRequested, calcFailedLastTick
        );
    }
}
