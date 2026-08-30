package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.PathCalculationResult;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritoneDiagnosticEmitter;
import lavi.minecraft.diagnostics.mining.baritone.BaritonePathObjectFormatters;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Supplier;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class BaritonePathfinderLifecycleDiagnostics {
    private static final String NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION =
            "NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION";

    private BaritonePathfinderLifecycleDiagnostics() {
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
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(pathfinder);
        synchronized (record) {
            record.pathingBehaviorId = BaritonePathObjectFormatters.identity(behavior);
            record.requestedGoalType = BaritonePathObjectFormatters.className(requestedGoal);
            record.firstSegment = firstSegment;
            record.primaryTimeoutMs = primaryTimeout;
            record.failureTimeoutMs = failureTimeout;
        }
        boolean inProgressMatches = inProgress == pathfinder;
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_CALCULATION_WORKER_STARTED",
                Long.toString(record.generationId),
                BaritonePathObjectFormatters.identity(pathfinder),
                Thread.currentThread().getName(),
                Boolean.toString(inProgressMatches)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_CALCULATION_WORKER_STARTED",
                "baritone_calculation_worker_started",
                "baritone_pathing_behavior_observer",
                "find_path_worker_started",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                () -> workerStartedFields(record, behavior, firstSegment, pathStart, requestedGoal, pathfinder,
                        primaryTimeout, failureTimeout, current, next, inProgress, expectedSegmentStart,
                        inProgressMatches)
        );
    }

    public static void logCalculateStarted(AbstractNodeCostSearch pathfinder,
                                           long primaryTimeout,
                                           long failureTimeout,
                                           Goal pathfinderGoal,
                                           BetterBlockPos realStart,
                                           boolean cancelRequested) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(pathfinder);
        synchronized (record) {
            record.pathfinderGoalType = BaritonePathObjectFormatters.className(pathfinderGoal);
            record.primaryTimeoutMs = primaryTimeout;
            record.failureTimeoutMs = failureTimeout;
            record.calculateStartNanos = System.nanoTime();
        }
        CalculationDiagnosticRegistry.activate(record);
        BaritoneCalculationPhaseDiagnostics.emit(
                record,
                pathfinder,
                "CALCULATE_ENTER",
                "abstract_node_cost_search_calculate_head",
                "calculate",
                -1,
                cancelRequested,
                () -> calculateStartedPhaseFields(record, pathfinderGoal, realStart)
        );
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PATHFINDER_CALCULATE_STARTED",
                Long.toString(record.generationId),
                BaritonePathObjectFormatters.identity(pathfinder),
                Long.toString(primaryTimeout),
                Long.toString(failureTimeout),
                Boolean.toString(cancelRequested)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_PATHFINDER_CALCULATE_STARTED",
                "baritone_pathfinder_calculate_started",
                "baritone_pathfinder_observer",
                "abstract_node_cost_search_calculate_head",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                () -> calculateStartedFields(record, pathfinder, primaryTimeout, failureTimeout, pathfinderGoal,
                        realStart, cancelRequested)
        );
    }

    public static void logCalculateCompleted(AbstractNodeCostSearch pathfinder,
                                             PathCalculationResult result,
                                             long elapsedNanos,
                                             boolean cancelRequested,
                                             Object[] searchStateFields) {
        logCalculateCompleted(pathfinder, result, elapsedNanos, cancelRequested, () -> searchStateFields);
    }

    public static void logCalculateCompleted(AbstractNodeCostSearch pathfinder,
                                             PathCalculationResult result,
                                             long elapsedNanos,
                                             boolean cancelRequested,
                                             Supplier<Object[]> searchStateFieldsSupplier) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(pathfinder);
        IPath path = resultPath(result);
        synchronized (record) {
            record.resultType = result == null ? "null" : String.valueOf(result.getType());
            record.resultPathPresent = path != null;
            record.resultPath = path;
            record.resultPathId = BaritonePathObjectFormatters.identity(path);
            record.elapsedMillis = elapsedNanos / 1_000_000L;
            record.cancelRequestedAfterCalculate = cancelRequested;
        }
        BaritoneCalculationPhaseDiagnostics.emit(
                record,
                pathfinder,
                "CALCULATE_RETURN",
                "abstract_node_cost_search_calculate_return",
                "calculate",
                record.elapsedMillis,
                cancelRequested,
                () -> calculateCompletedPhaseFields(record, path)
        );
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PATHFINDER_CALCULATE_COMPLETED",
                Long.toString(record.generationId),
                BaritonePathObjectFormatters.identity(pathfinder),
                record.resultType,
                Boolean.toString(record.resultPathPresent),
                record.resultPathId,
                Long.toString(record.elapsedMillis),
                Boolean.toString(cancelRequested)
        );
        BaritoneDiagnosticEmitter.emitLazy(
                "BARITONE_PATHFINDER_CALCULATE_COMPLETED",
                "baritone_pathfinder_calculate_completed",
                "baritone_pathfinder_observer",
                "abstract_node_cost_search_calculate_returned",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                () -> calculateCompletedFields(record, pathfinder, path, cancelRequested,
                        searchStateFieldsSupplier == null ? null : searchStateFieldsSupplier.get())
        );
        CalculationDiagnosticRegistry.clearActive();
    }

    public static void logCalculate0Enter(AbstractNodeCostSearch pathfinder,
                                          long primaryTimeout,
                                          long failureTimeout) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(pathfinder);
        synchronized (record) {
            record.primaryTimeoutMs = primaryTimeout;
            record.failureTimeoutMs = failureTimeout;
            record.calculate0StartNanos = System.nanoTime();
        }
        CalculationDiagnosticRegistry.activate(record);
        BaritoneCalculationPhaseDiagnostics.emit(
                record,
                pathfinder,
                "CALCULATE0_ENTER",
                "astar_path_finder_calculate0_head",
                "calculate0",
                -1,
                "unavailable_from_astar_phase_mixin",
                () -> new Object[]{"rawResultPathPresent", "unavailable"}
        );
    }

    public static void logCalculate0Return(AbstractNodeCostSearch pathfinder,
                                           Optional<IPath> result) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(pathfinder);
        IPath rawPath = optionalPath(result);
        long elapsedMillis;
        synchronized (record) {
            record.rawPathPresent = rawPath != null;
            record.rawPath = rawPath;
            record.rawPathId = BaritonePathObjectFormatters.identity(rawPath);
            elapsedMillis = elapsedMillisSince(record.calculate0StartNanos);
        }
        BaritoneCalculationPhaseDiagnostics.emit(
                record,
                pathfinder,
                "CALCULATE0_RETURN",
                "astar_path_finder_calculate0_return",
                "calculate0|" + record.rawPathId,
                elapsedMillis,
                "unavailable_from_astar_phase_mixin",
                () -> rawPathFields(record, rawPath)
        );
    }

    private static Object[] workerStartedFields(CalculationDiagnosticRecord record,
                                                PathingBehavior behavior,
                                                boolean firstSegment,
                                                BlockPos pathStart,
                                                Goal requestedGoal,
                                                AbstractNodeCostSearch pathfinder,
                                                long primaryTimeout,
                                                long failureTimeout,
                                                PathExecutor current,
                                                PathExecutor next,
                                                AbstractNodeCostSearch inProgress,
                                                BetterBlockPos expectedSegmentStart,
                                                boolean inProgressMatches) {
        String pathStartSummary = BaritonePathObjectFormatters.safeValue(pathStart);
        String requestedGoalSummary = BaritonePathObjectFormatters.summarizeGoal(requestedGoal);
        synchronized (record) {
            record.pathStartSummary = pathStartSummary;
            record.requestedGoalSummary = requestedGoalSummary;
        }
        return new Object[]{
                "calculationGeneration", record.generationId,
                "pathfinderIdentity", BaritonePathObjectFormatters.identity(pathfinder),
                "pathingBehaviorIdentity", BaritonePathObjectFormatters.identity(behavior),
                "workerThreadName", Thread.currentThread().getName(),
                "workerThreadId", Thread.currentThread().getId(),
                "firstSegment", firstSegment,
                "pathStart", pathStartSummary,
                "expectedSegmentStart", BaritonePathObjectFormatters.safeValue(expectedSegmentStart),
                "requestedGoalType", BaritonePathObjectFormatters.className(requestedGoal),
                "requestedGoalSummary", requestedGoalSummary,
                "primaryTimeoutMs", primaryTimeout,
                "failureTimeoutMs", failureTimeout,
                "currentExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(current),
                "nextExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(next),
                "inProgressBeforeCalculateSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress),
                "inProgressMatchesWorkerPathfinder", inProgressMatches
        };
    }

    private static Object[] calculateStartedPhaseFields(CalculationDiagnosticRecord record,
                                                        Goal pathfinderGoal,
                                                        BetterBlockPos realStart) {
        captureCalculateStartDetails(record, pathfinderGoal, realStart);
        return new Object[]{
                "pathfinderFinishedBeforeCalculate", NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION
        };
    }

    private static Object[] calculateStartedFields(CalculationDiagnosticRecord record,
                                                   AbstractNodeCostSearch pathfinder,
                                                   long primaryTimeout,
                                                   long failureTimeout,
                                                   Goal pathfinderGoal,
                                                   BetterBlockPos realStart,
                                                   boolean cancelRequested) {
        captureCalculateStartDetails(record, pathfinderGoal, realStart);
        return new Object[]{
                "calculationGeneration", record.generationId,
                "pathfinderIdentity", BaritonePathObjectFormatters.identity(pathfinder),
                "pathfinderType", BaritonePathObjectFormatters.className(pathfinder),
                "pathfinderStart", record.pathfinderStartSummary,
                "pathfinderGoalType", BaritonePathObjectFormatters.className(pathfinderGoal),
                "pathfinderGoalSummary", record.pathfinderGoalSummary,
                "primaryTimeoutMs", primaryTimeout,
                "failureTimeoutMs", failureTimeout,
                "cancelRequestedBeforeCalculate", cancelRequested,
                "workerThreadName", Thread.currentThread().getName(),
                "workerThreadId", Thread.currentThread().getId()
        };
    }

    private static void captureCalculateStartDetails(CalculationDiagnosticRecord record,
                                                     Goal pathfinderGoal,
                                                     BetterBlockPos realStart) {
        String goalSummary = BaritonePathObjectFormatters.summarizeGoal(pathfinderGoal);
        String startSummary = BaritonePathObjectFormatters.safeValue(realStart);
        synchronized (record) {
            record.pathfinderGoalSummary = goalSummary;
            record.pathfinderStartSummary = startSummary;
        }
    }

    private static Object[] calculateCompletedPhaseFields(CalculationDiagnosticRecord record, IPath path) {
        captureResultPathSummary(record, path);
        return new Object[]{
                "resultType", record.resultType,
                "resultPathPresent", record.resultPathPresent,
                "resultPathIdentity", record.resultPathId,
                "resultPathSummary", record.resultPathSummary,
                "pathfinderFinishedAfterCalculate", NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION
        };
    }

    private static Object[] calculateCompletedFields(CalculationDiagnosticRecord record,
                                                     AbstractNodeCostSearch pathfinder,
                                                     IPath path,
                                                     boolean cancelRequested,
                                                     Object[] searchStateFields) {
        captureResultPathSummary(record, path);
        return MiningDiagnosticEmitter.merge(new Object[]{
                "calculationGeneration", record.generationId,
                "pathfinderIdentity", BaritonePathObjectFormatters.identity(pathfinder),
                "resultType", record.resultType,
                "resultPathPresent", record.resultPathPresent,
                "resultPathIdentity", record.resultPathId,
                "resultPathSummary", record.resultPathSummary,
                "elapsedMillis", record.elapsedMillis,
                "cancelRequestedAfterCalculate", cancelRequested,
                "pathfinderFinishedAfterCalculate", NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION,
                "workerThreadName", Thread.currentThread().getName(),
                "workerThreadId", Thread.currentThread().getId()
        }, searchStateFields);
    }

    private static void captureResultPathSummary(CalculationDiagnosticRecord record, IPath path) {
        String summary = BaritonePathObjectFormatters.summarizePath(path);
        synchronized (record) {
            record.resultPathSummary = summary;
        }
    }

    private static Object[] rawPathFields(CalculationDiagnosticRecord record, IPath rawPath) {
        String summary = BaritonePathObjectFormatters.summarizePath(rawPath);
        synchronized (record) {
            record.rawPathSummary = summary;
        }
        return new Object[]{
                "rawResultPathPresent", record.rawPathPresent,
                "rawResultPathIdentity", record.rawPathId,
                "rawResultPathSummary", record.rawPathSummary
        };
    }

    private static IPath optionalPath(Optional<IPath> result) {
        if (result == null) {
            return null;
        }
        try {
            return result.orElse(null);
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private static IPath resultPath(PathCalculationResult result) {
        if (result == null) {
            return null;
        }
        try {
            return result.getPath().orElse(null);
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
    }

    private static long elapsedMillisSince(long startedAtNanos) {
        if (startedAtNanos <= 0) {
            return -1;
        }
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
