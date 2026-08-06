package lavi.minecraft.diagnostics.mining.baritone;

import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.PathingCommand;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.PathCalculationResult;
import baritone.behavior.PathingBehavior;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.path.PathExecutor;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.MiningDiagnosticEmitter;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

//20260806_kpopmodder: Correlate Baritone path calculation results with adoption without changing pathing behavior.
public final class BaritonePathCalculationDiagnostics {
    private static final AtomicLong NEXT_GENERATION = new AtomicLong(1);
    private static final ConcurrentMap<Integer, CalculationRecord> RECORDS = new ConcurrentHashMap<>();
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

    private static CalculationRecord recordFor(AbstractNodeCostSearch pathfinder) {
        if (RECORDS.size() > RECORD_HARD_CAP) {
            RECORDS.clear();
        }
        return RECORDS.computeIfAbsent(System.identityHashCode(pathfinder),
                ignored -> new CalculationRecord(NEXT_GENERATION.getAndIncrement(), pathfinder));
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
        String resultType = "unobserved";
        boolean resultPathPresent;
        String resultPathId = "none";
        String resultPathSummary = "none";
        IPath resultPath;
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
