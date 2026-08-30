package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneExecutorDiagnosticSnapshot;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;
import net.minecraft.util.math.BlockPos;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritonePathAdoptionDiagnostics {
    private BaritonePathAdoptionDiagnostics() {
    }

    public static void logBeforeClear(PathingBehavior behavior,
                                      boolean firstSegment,
                                      BlockPos pathStart,
                                      Goal requestedGoal,
                                      AbstractNodeCostSearch pathfinder,
                                      PathExecutor current,
                                      PathExecutor next,
                                      AbstractNodeCostSearch inProgress,
                                      BetterBlockPos expectedSegmentStart) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(pathfinder);
        IPath resultPath = record.resultPath();
        boolean currentMatchesResultPath = BaritonePathObjectFormatters.executorUsesPath(current, resultPath);
        boolean nextMatchesResultPath = BaritonePathObjectFormatters.executorUsesPath(next, resultPath);
        boolean inProgressMatches = inProgress == pathfinder;
        String adoptionOutcome = deriveAdoptionOutcome(
                record,
                currentMatchesResultPath,
                nextMatchesResultPath
        );
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PATH_ADOPTION_DECISION",
                Long.toString(record.generationId),
                BaritonePathObjectFormatters.identity(pathfinder),
                record.resultType,
                Boolean.toString(record.resultPathPresent),
                adoptionOutcome,
                Boolean.toString(inProgressMatches),
                BaritonePathObjectFormatters.identity(current),
                BaritonePathObjectFormatters.identity(next)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_PATH_ADOPTION_DECISION",
                "baritone_path_adoption_decision",
                "baritone_pathing_behavior_observer",
                "find_path_worker_before_in_progress_clear",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                () -> fields(record, behavior, firstSegment, pathStart, requestedGoal, pathfinder, current, next,
                        inProgress, expectedSegmentStart, resultPath, currentMatchesResultPath,
                        nextMatchesResultPath, inProgressMatches, adoptionOutcome)
        );
        CalculationDiagnosticRegistry.complete(pathfinder, record);
    }

    private static Object[] fields(CalculationDiagnosticRecord record,
                                   PathingBehavior behavior,
                                   boolean firstSegment,
                                   BlockPos pathStart,
                                   Goal requestedGoal,
                                   AbstractNodeCostSearch pathfinder,
                                   PathExecutor current,
                                   PathExecutor next,
                                   AbstractNodeCostSearch inProgress,
                                   BetterBlockPos expectedSegmentStart,
                                   IPath resultPath,
                                   boolean currentMatchesResultPath,
                                   boolean nextMatchesResultPath,
                                   boolean inProgressMatches,
                                   String adoptionOutcome) {
        String resultPathSummary = BaritonePathObjectFormatters.summarizePath(resultPath);
        synchronized (record) {
            record.resultPathSummary = resultPathSummary;
        }
        return MiningDiagnosticEmitter.merge(new Object[]{
                        "calculationGeneration", record.generationId,
                        "pathfinderIdentity", BaritonePathObjectFormatters.identity(pathfinder),
                        "pathingBehaviorIdentity", BaritonePathObjectFormatters.identity(behavior),
                        "firstSegment", firstSegment,
                        "pathStart", BaritonePathObjectFormatters.safeValue(pathStart),
                        "expectedSegmentStart", BaritonePathObjectFormatters.safeValue(expectedSegmentStart),
                        "requestedGoalType", BaritonePathObjectFormatters.className(requestedGoal),
                        "requestedGoalSummary", BaritonePathObjectFormatters.summarizeGoal(requestedGoal),
                        "resultType", record.resultType,
                        "resultPathPresent", record.resultPathPresent,
                        "resultPathIdentity", record.resultPathId,
                        "resultPathSummary", record.resultPathSummary,
                        "adoptionOutcome", adoptionOutcome,
                        "currentMatchesResultPath", currentMatchesResultPath,
                        "nextMatchesResultPath", nextMatchesResultPath,
                        "currentExecutorSummaryAfterDecision", BaritonePathObjectFormatters.summarizeExecutor(current),
                        "nextExecutorSummaryAfterDecision", BaritonePathObjectFormatters.summarizeExecutor(next),
                        "inProgressBeforeClearSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress),
                        "inProgressMatchesCompletingPathfinderBeforeClear", inProgressMatches,
                        "workerThreadName", Thread.currentThread().getName(),
                        "workerThreadId", Thread.currentThread().getId()
                },
                BaritoneExecutorDiagnosticSnapshot.fields("current", current),
                BaritoneExecutorDiagnosticSnapshot.fields("next", next));
    }

    private static String deriveAdoptionOutcome(CalculationDiagnosticRecord record,
                                                boolean currentMatchesResultPath,
                                                boolean nextMatchesResultPath) {
        if (currentMatchesResultPath) {
            return "ADOPTED_AS_CURRENT";
        }
        if (nextMatchesResultPath) {
            return "ADOPTED_AS_NEXT";
        }
        if (!record.resultPathPresent) {
            if ("CANCELLATION".equals(record.resultType)) {
                return "NO_PATH_CANCELLATION";
            }
            if ("EXCEPTION".equals(record.resultType)) {
                return "NO_PATH_EXCEPTION";
            }
            if ("unobserved".equals(record.resultType)) {
                return "RESULT_NOT_OBSERVED";
            }
            return "NO_PATH_RESULT_NOT_ADOPTABLE";
        }
        return "PATH_PRESENT_NOT_ADOPTED_OR_DISCARDED";
    }
}
