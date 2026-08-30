package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;
import net.minecraft.util.math.BlockPos;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritoneCalculationSchedulingDiagnostics {
    private BaritoneCalculationSchedulingDiagnostics() {
    }

    public static void log(PathingBehavior behavior,
                           BlockPos start,
                           boolean firstSegment,
                           CalculationContext context,
                           Goal activeGoal,
                           PathExecutor current,
                           PathExecutor next,
                           AbstractNodeCostSearch inProgress,
                           BetterBlockPos expectedSegmentStart) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || inProgress == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(inProgress);
        synchronized (record) {
            record.pathingBehaviorId = BaritonePathObjectFormatters.identity(behavior);
            record.requestedGoalType = BaritonePathObjectFormatters.className(activeGoal);
            record.firstSegment = firstSegment;
        }
        String startIdentity = BaritonePathObjectFormatters.safeValue(start);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_CALCULATION_SCHEDULED",
                Long.toString(record.generationId),
                BaritonePathObjectFormatters.identity(inProgress),
                Boolean.toString(firstSegment),
                startIdentity,
                BaritonePathObjectFormatters.identity(current),
                BaritonePathObjectFormatters.identity(next)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_CALCULATION_SCHEDULED",
                "baritone_calculation_scheduled",
                "baritone_pathing_behavior_observer",
                "find_path_in_new_thread_returned",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                () -> fields(record, behavior, start, firstSegment, context, activeGoal, current, next,
                        inProgress, expectedSegmentStart)
        );
    }

    private static Object[] fields(CalculationDiagnosticRecord record,
                                   PathingBehavior behavior,
                                   BlockPos start,
                                   boolean firstSegment,
                                   CalculationContext context,
                                   Goal activeGoal,
                                   PathExecutor current,
                                   PathExecutor next,
                                   AbstractNodeCostSearch inProgress,
                                   BetterBlockPos expectedSegmentStart) {
        String pathStartSummary = BaritonePathObjectFormatters.safeValue(start);
        String requestedGoalSummary = BaritonePathObjectFormatters.summarizeGoal(activeGoal);
        synchronized (record) {
            record.pathStartSummary = pathStartSummary;
            record.requestedGoalSummary = requestedGoalSummary;
        }
        return new Object[]{
                "calculationGeneration", record.generationId,
                "pathfinderIdentity", BaritonePathObjectFormatters.identity(inProgress),
                "pathingBehaviorIdentity", BaritonePathObjectFormatters.identity(behavior),
                "firstSegment", firstSegment,
                "pathStart", pathStartSummary,
                "expectedSegmentStart", BaritonePathObjectFormatters.safeValue(expectedSegmentStart),
                "requestedGoalType", BaritonePathObjectFormatters.className(activeGoal),
                "requestedGoalSummary", requestedGoalSummary,
                "calculationContextType", BaritonePathObjectFormatters.className(context),
                "currentExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(current),
                "nextExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(next),
                "inProgressSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress)
        };
    }
}
