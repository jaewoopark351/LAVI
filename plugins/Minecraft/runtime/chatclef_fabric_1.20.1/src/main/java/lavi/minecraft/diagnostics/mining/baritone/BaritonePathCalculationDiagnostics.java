package lavi.minecraft.diagnostics.mining.baritone;

import baritone.Baritone;
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
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

//20260806_kpopmodder: Correlate Baritone path calculation results with adoption without changing pathing behavior.
public final class BaritonePathCalculationDiagnostics {
    private static final AtomicLong NEXT_GENERATION = new AtomicLong(1);
    private static final ConcurrentMap<Integer, CalculationRecord> RECORDS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, CalculationRecord> PATH_RECORDS = new ConcurrentHashMap<>();
    private static final ThreadLocal<CalculationRecord> ACTIVE_CALCULATION = new ThreadLocal<>();
    private static final int RECORD_HARD_CAP = 1024;

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
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String rejectReason = deriveRejectReason(accepted, command, current, inProgress);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_GOAL_REQUEST_DECISION",
                BaritonePathObjectFormatters.identity(behavior),
                BaritonePathObjectFormatters.commandType(command),
                Boolean.toString(accepted),
                rejectReason,
                BaritonePathObjectFormatters.identity(current),
                BaritonePathObjectFormatters.identity(next),
                BaritonePathObjectFormatters.identity(inProgress),
                Boolean.toString(cancelRequested),
                Boolean.toString(calcFailedLastTick)
        );
        BaritoneDiagnosticEmitter.emit("BARITONE_GOAL_REQUEST_DECISION", "baritone_goal_request_decision",
                "baritone_pathing_behavior_observer", "secret_internal_set_goal_and_path_returned",
                "baritone_goal_request|" + BaritonePathObjectFormatters.identity(behavior),
                fingerprint,
                new Object[]{
                        "requestAccepted", accepted,
                        "requestRejectedReason", rejectReason,
                        "commandType", BaritonePathObjectFormatters.commandType(command),
                        "commandGoalType", BaritonePathObjectFormatters.commandGoalType(command),
                        "commandGoalSummary", BaritonePathObjectFormatters.commandGoalSummary(command),
                        "pathingGoalType", BaritonePathObjectFormatters.className(activeGoal),
                        "pathingGoalSummary", BaritonePathObjectFormatters.summarizeGoal(activeGoal),
                        "currentExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(current),
                        "nextExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(next),
                        "inProgressSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress),
                        "expectedSegmentStart", BaritonePathObjectFormatters.safeValue(expectedSegmentStart),
                        "cancelRequested", cancelRequested,
                        "calcFailedLastTick", calcFailedLastTick
                });
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
        if (!ChatClefDiagnostics.isBoundaryEnabled() || inProgress == null) {
            return;
        }
        CalculationRecord record = recordFor(inProgress);
        synchronized (record) {
            record.pathingBehaviorId = BaritonePathObjectFormatters.identity(behavior);
            record.requestedGoalType = BaritonePathObjectFormatters.className(activeGoal);
            record.requestedGoalSummary = BaritonePathObjectFormatters.summarizeGoal(activeGoal);
            record.pathStartSummary = BaritonePathObjectFormatters.safeValue(start);
            record.firstSegment = firstSegment;
        }
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_CALCULATION_SCHEDULED",
                Long.toString(record.generationId),
                BaritonePathObjectFormatters.identity(inProgress),
                Boolean.toString(firstSegment),
                BaritonePathObjectFormatters.safeValue(start),
                BaritonePathObjectFormatters.identity(current),
                BaritonePathObjectFormatters.identity(next)
        );
        BaritoneDiagnosticEmitter.emit("BARITONE_CALCULATION_SCHEDULED", "baritone_calculation_scheduled",
                "baritone_pathing_behavior_observer", "find_path_in_new_thread_returned",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                new Object[]{
                        "calculationGeneration", record.generationId,
                        "pathfinderIdentity", BaritonePathObjectFormatters.identity(inProgress),
                        "pathingBehaviorIdentity", BaritonePathObjectFormatters.identity(behavior),
                        "firstSegment", firstSegment,
                        "pathStart", BaritonePathObjectFormatters.safeValue(start),
                        "expectedSegmentStart", BaritonePathObjectFormatters.safeValue(expectedSegmentStart),
                        "requestedGoalType", BaritonePathObjectFormatters.className(activeGoal),
                        "requestedGoalSummary", BaritonePathObjectFormatters.summarizeGoal(activeGoal),
                        "calculationContextType", BaritonePathObjectFormatters.className(context),
                        "currentExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(current),
                        "nextExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(next),
                        "inProgressSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress)
                });
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
        CalculationRecord record = recordFor(pathfinder);
        synchronized (record) {
            record.pathingBehaviorId = BaritonePathObjectFormatters.identity(behavior);
            record.requestedGoalType = BaritonePathObjectFormatters.className(requestedGoal);
            record.requestedGoalSummary = BaritonePathObjectFormatters.summarizeGoal(requestedGoal);
            record.pathStartSummary = BaritonePathObjectFormatters.safeValue(pathStart);
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
        BaritoneDiagnosticEmitter.emit("BARITONE_CALCULATION_WORKER_STARTED", "baritone_calculation_worker_started",
                "baritone_pathing_behavior_observer", "find_path_worker_started",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                new Object[]{
                        "calculationGeneration", record.generationId,
                        "pathfinderIdentity", BaritonePathObjectFormatters.identity(pathfinder),
                        "pathingBehaviorIdentity", BaritonePathObjectFormatters.identity(behavior),
                        "workerThreadName", Thread.currentThread().getName(),
                        "workerThreadId", Thread.currentThread().getId(),
                        "firstSegment", firstSegment,
                        "pathStart", BaritonePathObjectFormatters.safeValue(pathStart),
                        "expectedSegmentStart", BaritonePathObjectFormatters.safeValue(expectedSegmentStart),
                        "requestedGoalType", BaritonePathObjectFormatters.className(requestedGoal),
                        "requestedGoalSummary", BaritonePathObjectFormatters.summarizeGoal(requestedGoal),
                        "primaryTimeoutMs", primaryTimeout,
                        "failureTimeoutMs", failureTimeout,
                        "currentExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(current),
                        "nextExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(next),
                        "inProgressBeforeCalculateSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress),
                        "inProgressMatchesWorkerPathfinder", inProgressMatches
                });
    }

    public static void logPathfinderCalculateStarted(AbstractNodeCostSearch pathfinder,
                                                     long primaryTimeout,
                                                     long failureTimeout,
                                                     Goal pathfinderGoal,
                                                     BetterBlockPos realStart,
                                                     boolean cancelRequested) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationRecord record = recordFor(pathfinder);
        synchronized (record) {
            record.pathfinderGoalType = BaritonePathObjectFormatters.className(pathfinderGoal);
            record.pathfinderGoalSummary = BaritonePathObjectFormatters.summarizeGoal(pathfinderGoal);
            record.pathfinderStartSummary = BaritonePathObjectFormatters.safeValue(realStart);
            record.primaryTimeoutMs = primaryTimeout;
            record.failureTimeoutMs = failureTimeout;
            record.calculateStartNanos = System.nanoTime();
        }
        ACTIVE_CALCULATION.set(record);
        emitCalculationPhase(record, pathfinder, "CALCULATE_ENTER",
                "abstract_node_cost_search_calculate_head", "calculate",
                -1, cancelRequested, new Object[]{
                        "pathfinderFinishedBeforeCalculate", ChatClefDiagnostics.safeValueForDiagnosticLog(pathfinder::isFinished)
                });
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PATHFINDER_CALCULATE_STARTED",
                Long.toString(record.generationId),
                BaritonePathObjectFormatters.identity(pathfinder),
                Long.toString(primaryTimeout),
                Long.toString(failureTimeout),
                Boolean.toString(cancelRequested)
        );
        BaritoneDiagnosticEmitter.emit("BARITONE_PATHFINDER_CALCULATE_STARTED", "baritone_pathfinder_calculate_started",
                "baritone_pathfinder_observer", "abstract_node_cost_search_calculate_head",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                new Object[]{
                        "calculationGeneration", record.generationId,
                        "pathfinderIdentity", BaritonePathObjectFormatters.identity(pathfinder),
                        "pathfinderType", BaritonePathObjectFormatters.className(pathfinder),
                        "pathfinderStart", BaritonePathObjectFormatters.safeValue(realStart),
                        "pathfinderGoalType", BaritonePathObjectFormatters.className(pathfinderGoal),
                        "pathfinderGoalSummary", BaritonePathObjectFormatters.summarizeGoal(pathfinderGoal),
                        "primaryTimeoutMs", primaryTimeout,
                        "failureTimeoutMs", failureTimeout,
                        "cancelRequestedBeforeCalculate", cancelRequested,
                        "workerThreadName", Thread.currentThread().getName(),
                        "workerThreadId", Thread.currentThread().getId()
                });
    }

    public static void logPathfinderCalculateCompleted(AbstractNodeCostSearch pathfinder,
                                                       PathCalculationResult result,
                                                       long elapsedNanos,
                                                       boolean cancelRequested,
                                                       Object[] searchStateFields) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationRecord record = recordFor(pathfinder);
        IPath path = resultPath(result);
        synchronized (record) {
            record.resultType = result == null ? "null" : String.valueOf(result.getType());
            record.resultPathPresent = path != null;
            record.resultPath = path;
            record.resultPathId = BaritonePathObjectFormatters.identity(path);
            record.resultPathSummary = BaritonePathObjectFormatters.summarizePath(path);
            record.elapsedMillis = elapsedNanos / 1_000_000L;
            record.cancelRequestedAfterCalculate = cancelRequested;
        }
        emitCalculationPhase(record, pathfinder, "CALCULATE_RETURN",
                "abstract_node_cost_search_calculate_return", "calculate",
                record.elapsedMillis, cancelRequested, new Object[]{
                        "resultType", record.resultType,
                        "resultPathPresent", record.resultPathPresent,
                        "resultPathIdentity", record.resultPathId,
                        "resultPathSummary", record.resultPathSummary,
                        "pathfinderFinishedAfterCalculate", ChatClefDiagnostics.safeValueForDiagnosticLog(pathfinder::isFinished)
                });
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
        BaritoneDiagnosticEmitter.emit("BARITONE_PATHFINDER_CALCULATE_COMPLETED", "baritone_pathfinder_calculate_completed",
                "baritone_pathfinder_observer", "abstract_node_cost_search_calculate_returned",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                MiningDiagnosticEmitter.merge(new Object[]{
                        "calculationGeneration", record.generationId,
                        "pathfinderIdentity", BaritonePathObjectFormatters.identity(pathfinder),
                        "resultType", record.resultType,
                        "resultPathPresent", record.resultPathPresent,
                        "resultPathIdentity", record.resultPathId,
                        "resultPathSummary", record.resultPathSummary,
                        "elapsedMillis", record.elapsedMillis,
                        "cancelRequestedAfterCalculate", cancelRequested,
                        "pathfinderFinishedAfterCalculate", ChatClefDiagnostics.safeValueForDiagnosticLog(pathfinder::isFinished),
                        "workerThreadName", Thread.currentThread().getName(),
                        "workerThreadId", Thread.currentThread().getId()
                }, searchStateFields));
        ACTIVE_CALCULATION.remove();
    }

    public static void logCalculate0Enter(AbstractNodeCostSearch pathfinder,
                                          long primaryTimeout,
                                          long failureTimeout) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationRecord record = recordFor(pathfinder);
        synchronized (record) {
            record.primaryTimeoutMs = primaryTimeout;
            record.failureTimeoutMs = failureTimeout;
            record.calculate0StartNanos = System.nanoTime();
        }
        ACTIVE_CALCULATION.set(record);
        emitCalculationPhase(record, pathfinder, "CALCULATE0_ENTER",
                "astar_path_finder_calculate0_head", "calculate0",
                -1, "unavailable_from_astar_phase_mixin", new Object[]{
                        "rawResultPathPresent", "unavailable"
                });
    }

    public static void logCalculate0Return(AbstractNodeCostSearch pathfinder,
                                           Optional<IPath> result) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationRecord record = recordFor(pathfinder);
        IPath rawPath = optionalPath(result);
        long elapsedMillis;
        synchronized (record) {
            record.rawPathPresent = rawPath != null;
            record.rawPath = rawPath;
            record.rawPathId = BaritonePathObjectFormatters.identity(rawPath);
            record.rawPathSummary = BaritonePathObjectFormatters.summarizePath(rawPath);
            elapsedMillis = elapsedMillisSince(record.calculate0StartNanos);
        }
        emitCalculationPhase(record, pathfinder, "CALCULATE0_RETURN",
                "astar_path_finder_calculate0_return", "calculate0|" + record.rawPathId,
                elapsedMillis, "unavailable_from_astar_phase_mixin", new Object[]{
                        "rawResultPathPresent", record.rawPathPresent,
                        "rawResultPathIdentity", record.rawPathId,
                        "rawResultPathSummary", record.rawPathSummary
                });
    }

    public static void logPathBuildEnter(Object path,
                                         BetterBlockPos realStart,
                                         PathNode startNode,
                                         PathNode endNode,
                                         int numNodes,
                                         Goal goal,
                                         CalculationContext context) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        CalculationRecord record = activeRecordForPath(path);
        if (record == null || path == null) {
            return;
        }
        synchronized (record) {
            record.pathBuildStartNanos = System.nanoTime();
            record.rawPathId = BaritonePathObjectFormatters.identity(path);
        }
        putPathRecord(path, record);
        emitCalculationPhase(record, record.pathfinder, "PATH_BUILD_ENTER",
                "baritone_path_constructor_head", "path_build|" + BaritonePathObjectFormatters.identity(path),
                -1, "unavailable_from_path_phase_mixin",
                pathBuildFields(path, realStart, startNode, endNode, numNodes, goal, context, false));
    }

    public static void logPathBuildReturn(Object path,
                                          BetterBlockPos realStart,
                                          PathNode startNode,
                                          PathNode endNode,
                                          int numNodes,
                                          Goal goal,
                                          CalculationContext context) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        CalculationRecord record = activeRecordForPath(path);
        if (record == null || path == null) {
            return;
        }
        putPathRecord(path, record);
        IPath rawPath = path instanceof IPath ? (IPath) path : null;
        long elapsedMillis;
        synchronized (record) {
            record.rawPathPresent = rawPath != null;
            record.rawPath = rawPath;
            record.rawPathId = BaritonePathObjectFormatters.identity(path);
            record.rawPathSummary = BaritonePathObjectFormatters.summarizePath(rawPath);
            elapsedMillis = elapsedMillisSince(record.pathBuildStartNanos);
        }
        emitCalculationPhase(record, record.pathfinder, "PATH_BUILD_RETURN",
                "baritone_path_constructor_return", "path_build|" + BaritonePathObjectFormatters.identity(path),
                elapsedMillis, "unavailable_from_path_phase_mixin",
                MiningDiagnosticEmitter.merge(
                        pathBuildFields(path, realStart, startNode, endNode, numNodes, goal, context, true),
                        new Object[]{
                                "rawResultPathPresent", record.rawPathPresent,
                                "rawResultPathIdentity", record.rawPathId,
                                "rawResultPathSummary", record.rawPathSummary
                        }));
    }

    public static void logPathPostProcessEnter(IPath path) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || path == null) {
            return;
        }
        CalculationRecord record = activeRecordForPath(path);
        if (record == null) {
            return;
        }
        synchronized (record) {
            record.postProcessStartNanos = System.nanoTime();
        }
        emitCalculationPhase(record, record.pathfinder, "POST_PROCESS_ENTER",
                "baritone_path_post_process_head", "post_process|" + BaritonePathObjectFormatters.identity(path),
                -1, "unavailable_from_path_phase_mixin", new Object[]{
                        "rawResultPathPresent", true,
                        "rawResultPathIdentity", BaritonePathObjectFormatters.identity(path),
                        "rawResultPathSummary", BaritonePathObjectFormatters.summarizePath(path)
                });
    }

    public static void logPathPostProcessReturn(IPath path,
                                                IPath result) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || path == null) {
            return;
        }
        CalculationRecord record = activeRecordForPath(path);
        if (record == null) {
            return;
        }
        long elapsedMillis;
        synchronized (record) {
            record.postProcessedPathPresent = result != null;
            record.postProcessedPath = result;
            record.postProcessedPathId = BaritonePathObjectFormatters.identity(result);
            record.postProcessedPathSummary = BaritonePathObjectFormatters.summarizePath(result);
            elapsedMillis = elapsedMillisSince(record.postProcessStartNanos);
        }
        emitCalculationPhase(record, record.pathfinder, "POST_PROCESS_RETURN",
                "baritone_path_post_process_return", "post_process|"
                        + BaritonePathObjectFormatters.identity(path)
                        + "|"
                        + record.postProcessedPathId,
                elapsedMillis, "unavailable_from_path_phase_mixin", new Object[]{
                        "rawResultPathPresent", true,
                        "rawResultPathIdentity", BaritonePathObjectFormatters.identity(path),
                        "rawResultPathSummary", BaritonePathObjectFormatters.summarizePath(path),
                        "postProcessedPathPresent", record.postProcessedPathPresent,
                        "postProcessedPathIdentity", record.postProcessedPathId,
                        "postProcessedPathSummary", record.postProcessedPathSummary
                });
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
        if (!ChatClefDiagnostics.isBoundaryEnabled() || pathfinder == null) {
            return;
        }
        CalculationRecord record = recordFor(pathfinder);
        IPath resultPath = record.resultPath();
        boolean currentMatchesResultPath = BaritonePathObjectFormatters.executorUsesPath(current, resultPath);
        boolean nextMatchesResultPath = BaritonePathObjectFormatters.executorUsesPath(next, resultPath);
        boolean inProgressMatches = inProgress == pathfinder;
        String adoptionOutcome = deriveAdoptionOutcome(record, currentMatchesResultPath, nextMatchesResultPath);
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
        BaritoneDiagnosticEmitter.emit("BARITONE_PATH_ADOPTION_DECISION", "baritone_path_adoption_decision",
                "baritone_pathing_behavior_observer", "find_path_worker_before_in_progress_clear",
                "baritone_calculation|" + record.generationId,
                fingerprint,
                MiningDiagnosticEmitter.merge(new Object[]{
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
                BaritoneExecutorDiagnosticSnapshot.fields("next", next)));
        removePathRecord(record.rawPath);
        removePathRecord(record.postProcessedPath);
        removePathRecord(record.resultPath);
        RECORDS.remove(System.identityHashCode(pathfinder), record);
    }

    public static void logForceCancelBoundary(PathingBehavior behavior,
                                              String phase,
                                              PathExecutor current,
                                              PathExecutor next,
                                              AbstractNodeCostSearch inProgress,
                                              Goal activeGoal,
                                              boolean cancelRequested,
                                              boolean calcFailedLastTick) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_PATHING_FORCE_CANCEL_BOUNDARY",
                BaritonePathObjectFormatters.identity(behavior),
                phase,
                BaritonePathObjectFormatters.identity(current),
                BaritonePathObjectFormatters.identity(next),
                BaritonePathObjectFormatters.identity(inProgress),
                Boolean.toString(cancelRequested),
                Boolean.toString(calcFailedLastTick)
        );
        BaritoneDiagnosticEmitter.emit("BARITONE_PATHING_FORCE_CANCEL_BOUNDARY", "baritone_pathing_force_cancel_boundary",
                "baritone_pathing_behavior_observer", "pathing_behavior_force_cancel_" + phase,
                "baritone_force_cancel|" + BaritonePathObjectFormatters.identity(behavior),
                fingerprint,
                new Object[]{
                        "phase", phase,
                        "pathingBehaviorIdentity", BaritonePathObjectFormatters.identity(behavior),
                        "pathingGoalType", BaritonePathObjectFormatters.className(activeGoal),
                        "pathingGoalSummary", BaritonePathObjectFormatters.summarizeGoal(activeGoal),
                        "currentExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(current),
                        "nextExecutorSummary", BaritonePathObjectFormatters.summarizeExecutor(next),
                        "inProgressSummary", BaritonePathObjectFormatters.summarizeFinder(inProgress),
                        "cancelRequested", cancelRequested,
                        "calcFailedLastTick", calcFailedLastTick
                });
    }

    private static void emitCalculationPhase(CalculationRecord record,
                                             AbstractNodeCostSearch pathfinder,
                                             String phase,
                                             String trigger,
                                             String phaseKey,
                                             long elapsedMillis,
                                             Object cancelRequested,
                                             Object[] phaseFields) {
        TimeoutSnapshot timeout = timeoutSnapshot(record.primaryTimeoutMs, record.failureTimeoutMs);
        String pathfinderIdentity = pathfinder == null
                ? Integer.toHexString(record.pathfinderIdentity)
                : BaritonePathObjectFormatters.identity(pathfinder);
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "BARITONE_CALCULATION_PHASE_BOUNDARY",
                Long.toString(record.generationId),
                pathfinderIdentity,
                phase,
                phaseKey
        );
        BaritoneDiagnosticEmitter.emit("BARITONE_CALCULATION_PHASE_BOUNDARY", "baritone_calculation_phase_boundary",
                "baritone_pathfinder_phase_observer", trigger,
                "baritone_calculation|" + record.generationId,
                fingerprint,
                MiningDiagnosticEmitter.merge(new Object[]{
                        "calculationGeneration", record.generationId,
                        "phase", phase,
                        "pathfinderIdentity", pathfinderIdentity,
                        "pathfinderType", BaritonePathObjectFormatters.className(pathfinder),
                        "pathingBehaviorIdentity", record.pathingBehaviorId,
                        "firstSegment", record.firstSegment,
                        "pathStart", record.pathStartSummary,
                        "pathfinderStart", record.pathfinderStartSummary,
                        "requestedGoalType", record.requestedGoalType,
                        "requestedGoalSummary", record.requestedGoalSummary,
                        "pathfinderGoalType", record.pathfinderGoalType,
                        "pathfinderGoalSummary", record.pathfinderGoalSummary,
                        "primaryTimeoutMs", record.primaryTimeoutMs,
                        "failureTimeoutMs", record.failureTimeoutMs,
                        "slowPath", timeout.slowPath(),
                        "slowPathTimeoutMs", timeout.slowPathTimeoutMs(),
                        "effectivePrimaryTimeoutMs", timeout.effectivePrimaryTimeoutMs(),
                        "effectiveFailureTimeoutMs", timeout.effectiveFailureTimeoutMs(),
                        "elapsedMillis", elapsedMillis,
                        "cancelRequested", cancelRequested,
                        "workerThreadName", Thread.currentThread().getName(),
                        "workerThreadId", Thread.currentThread().getId()
                }, phaseFields));
    }

    private static Object[] pathBuildFields(Object path,
                                            BetterBlockPos realStart,
                                            PathNode startNode,
                                            PathNode endNode,
                                            int numNodes,
                                            Goal goal,
                                            CalculationContext context,
                                            boolean includePathSummary) {
        IPath rawPath = includePathSummary && path instanceof IPath ? (IPath) path : null;
        return new Object[]{
                "pathIdentity", BaritonePathObjectFormatters.identity(path),
                "pathType", BaritonePathObjectFormatters.className(path),
                "pathSummary", includePathSummary ? BaritonePathObjectFormatters.summarizePath(rawPath) : "unavailable_before_constructor_return",
                "pathRealStart", BaritonePathObjectFormatters.safeValue(realStart),
                "pathStartNodePresent", startNode != null,
                "pathStartNodeSummary", summarizeNode(startNode),
                "pathEndNodePresent", endNode != null,
                "pathEndNodeSummary", summarizeNode(endNode),
                "pathEndNodePreviousPresent", endNode != null && endNode.previous != null,
                "pathNumNodesConsidered", numNodes,
                "pathGoalType", BaritonePathObjectFormatters.className(goal),
                "pathGoalSummary", BaritonePathObjectFormatters.summarizeGoal(goal),
                "calculationContextType", BaritonePathObjectFormatters.className(context)
        };
    }

    private static CalculationRecord activeRecordForPath(Object path) {
        CalculationRecord active = ACTIVE_CALCULATION.get();
        if (active != null) {
            return active;
        }
        if (path == null) {
            return null;
        }
        return PATH_RECORDS.get(System.identityHashCode(path));
    }

    private static void putPathRecord(Object path, CalculationRecord record) {
        if (path == null || record == null) {
            return;
        }
        if (PATH_RECORDS.size() > RECORD_HARD_CAP) {
            PATH_RECORDS.clear();
        }
        PATH_RECORDS.put(System.identityHashCode(path), record);
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

    private static long elapsedMillisSince(long startedAtNanos) {
        if (startedAtNanos <= 0) {
            return -1;
        }
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private static String summarizeNode(PathNode node) {
        if (node == null) {
            return "none";
        }
        return "PathNode{x="
                + node.x
                + ",y="
                + node.y
                + ",z="
                + node.z
                + ",cost="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> node.cost)
                + ",estimatedCostToGoal="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> node.estimatedCostToGoal)
                + ",combinedCost="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(() -> node.combinedCost)
                + ",open="
                + ChatClefDiagnostics.safeValueForDiagnosticLog(node::isOpen)
                + "}";
    }

    private static TimeoutSnapshot timeoutSnapshot(long primaryTimeoutMs, long failureTimeoutMs) {
        String slowPath = ChatClefDiagnostics.safeValueForDiagnosticLog(() -> Baritone.settings().slowPath.value);
        long slowPathTimeoutMs = safeLongSetting(() -> Baritone.settings().slowPathTimeoutMS.value);
        boolean slowPathEnabled = "true".equals(slowPath);
        long effectivePrimaryTimeoutMs = slowPathEnabled && slowPathTimeoutMs >= 0
                ? slowPathTimeoutMs
                : primaryTimeoutMs;
        long effectiveFailureTimeoutMs = slowPathEnabled && slowPathTimeoutMs >= 0
                ? slowPathTimeoutMs
                : failureTimeoutMs;
        return new TimeoutSnapshot(slowPath, slowPathTimeoutMs, effectivePrimaryTimeoutMs, effectiveFailureTimeoutMs);
    }

    private static long safeLongSetting(Supplier<?> supplier) {
        try {
            Object value = supplier.get();
            if (value instanceof Number number) {
                return number.longValue();
            }
            return -1;
        } catch (RuntimeException | LinkageError error) {
            return -1;
        }
    }

    private static CalculationRecord recordFor(AbstractNodeCostSearch pathfinder) {
        if (RECORDS.size() > RECORD_HARD_CAP) {
            RECORDS.clear();
            PATH_RECORDS.clear();
        }
        return RECORDS.computeIfAbsent(System.identityHashCode(pathfinder),
                ignored -> new CalculationRecord(NEXT_GENERATION.getAndIncrement(), pathfinder));
    }

    private static void removePathRecord(IPath path) {
        if (path != null) {
            PATH_RECORDS.remove(System.identityHashCode(path));
        }
    }

    private static String deriveRejectReason(boolean accepted,
                                             PathingCommand command,
                                             PathExecutor current,
                                             AbstractNodeCostSearch inProgress) {
        if (accepted) {
            return "ACCEPTED";
        }
        if (command == null) {
            return "COMMAND_NULL";
        }
        if (command.goal == null) {
            return "GOAL_NULL";
        }
        if (current != null) {
            return "CURRENT_PATH_PRESENT";
        }
        if (inProgress != null) {
            return "IN_PROGRESS_PRESENT";
        }
        return "UNKNOWN_REJECTED_OR_ALREADY_IN_GOAL";
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

    private static String deriveAdoptionOutcome(CalculationRecord record,
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

    private record TimeoutSnapshot(String slowPath,
                                   long slowPathTimeoutMs,
                                   long effectivePrimaryTimeoutMs,
                                   long effectiveFailureTimeoutMs) {
    }

    private static final class CalculationRecord {
        final long generationId;
        final int pathfinderIdentity;
        final AbstractNodeCostSearch pathfinder;
        String pathingBehaviorId = "unavailable";
        String requestedGoalType = "unavailable";
        String requestedGoalSummary = "unavailable";
        String pathfinderGoalType = "unavailable";
        String pathfinderGoalSummary = "unavailable";
        String pathStartSummary = "unavailable";
        String pathfinderStartSummary = "unavailable";
        boolean firstSegment;
        long primaryTimeoutMs = -1;
        long failureTimeoutMs = -1;
        long calculateStartNanos = -1;
        long calculate0StartNanos = -1;
        long pathBuildStartNanos = -1;
        long postProcessStartNanos = -1;
        String resultType = "unobserved";
        boolean resultPathPresent;
        String resultPathId = "none";
        String resultPathSummary = "none";
        IPath resultPath;
        boolean rawPathPresent;
        String rawPathId = "none";
        String rawPathSummary = "none";
        IPath rawPath;
        boolean postProcessedPathPresent;
        String postProcessedPathId = "none";
        String postProcessedPathSummary = "none";
        IPath postProcessedPath;
        long elapsedMillis = -1;
        boolean cancelRequestedAfterCalculate;

        CalculationRecord(long generationId, AbstractNodeCostSearch pathfinder) {
            this.generationId = generationId;
            this.pathfinderIdentity = System.identityHashCode(pathfinder);
            this.pathfinder = pathfinder;
        }

        IPath resultPath() {
            try {
                if (!resultPathPresent) {
                    return null;
                }
                return resultPath;
            } catch (RuntimeException | LinkageError error) {
                return null;
            }
        }
    }
}
