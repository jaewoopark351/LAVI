package lavi.minecraft.diagnostics.container.store.deposit.candidate;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.Map;

public final class StoreContainerCandidateCollector {
    private static final ThreadLocal<Scope> ACTIVE_SCOPE = new ThreadLocal<>();
    private static final ThreadLocal<CompletedScope> COMPLETED_SCOPE = new ThreadLocal<>();

    private StoreContainerCandidateCollector() {
    }

    public static void begin(StoreDepositOperationState state, Task routeChild) {
        ACTIVE_SCOPE.remove();
        COMPLETED_SCOPE.remove();
        if (state == null
                || !state.context().isDepositAllOperation()
                || !"OPEN_EXISTING".equals(state.routeState().currentBranch())
                || !state.routeState().isCurrentRouteChild(routeChild)) {
            return;
        }
        StoreContainerParentDecision parent = state.routeState().currentParentDecision();
        ACTIVE_SCOPE.set(new Scope(
                state.context().operationId(),
                routeChild,
                parent.sequence(),
                parent.branchEpoch(),
                parent.rawClosest()
        ));
    }

    public static void beginParentSelection(StoreDepositOperationState state,
                                            Task parent,
                                            BlockPos rawClosest) {
        ACTIVE_SCOPE.remove();
        COMPLETED_SCOPE.remove();
        if (state == null
                || !state.context().isDepositAllOperation()
                || !state.context().isRoot(parent)) {
            return;
        }
        ACTIVE_SCOPE.set(new Scope(
                state.context().operationId(),
                parent,
                -1,
                -1,
                rawClosest
        ));
    }

    public static void observe(BlockPos position, StoreContainerCandidateRejectionReason reason) {
        try {
            Scope scope = ACTIVE_SCOPE.get();
            if (scope == null) {
                return;
            }
            scope.observe(position, reason == null ? StoreContainerCandidateRejectionReason.UNKNOWN : reason);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void end(Task routeChild, boolean completedNormally) {
        Scope scope = ACTIVE_SCOPE.get();
        ACTIVE_SCOPE.remove();
        if (scope == null || scope.routeChild != routeChild) {
            COMPLETED_SCOPE.remove();
            return;
        }
        COMPLETED_SCOPE.set(new CompletedScope(scope, completedNormally));
    }

    public static StoreContainerCandidateObservation take(StoreDepositOperationState state, Task routeChild) {
        CompletedScope completed = COMPLETED_SCOPE.get();
        COMPLETED_SCOPE.remove();
        if (completed == null || state == null) {
            return StoreContainerCandidateObservation.unavailable();
        }
        if (!state.context().operationId().equals(completed.scope().operationId)
                || completed.scope().routeChild != routeChild) {
            return StoreContainerCandidateObservation.unavailable();
        }
        return completed.scope().snapshot(completed.completedNormally(), state);
    }

    private static String identity(Object value) {
        return value == null ? "none" : Integer.toHexString(System.identityHashCode(value));
    }

    private static final class Scope {
        private final String operationId;
        private final Task routeChild;
        private final long parentDecisionSequence;
        private final long branchEpoch;
        private final BlockPos parentRawClosest;
        private final Map<StoreContainerCandidateRejectionReason, Integer> rejectionCounts =
                new EnumMap<>(StoreContainerCandidateRejectionReason.class);
        private int candidateEvaluationCount;
        private int predicateAcceptedCount;
        private int predicateRejectedCount;
        private BlockPos firstRejectedPosition;
        private StoreContainerCandidateRejectionReason firstRejectedReason;
        private BlockPos lastRejectedPosition;
        private StoreContainerCandidateRejectionReason lastRejectedReason;
        private int rawCandidateObservationCount;
        private int rawCandidateEvaluationOrdinal;
        private StoreContainerCandidateRejectionReason rawCandidateReason;

        private Scope(String operationId,
                      Task routeChild,
                      long parentDecisionSequence,
                      long branchEpoch,
                      BlockPos parentRawClosest) {
            this.operationId = operationId;
            this.routeChild = routeChild;
            this.parentDecisionSequence = parentDecisionSequence;
            this.branchEpoch = branchEpoch;
            this.parentRawClosest = parentRawClosest;
        }

        private void observe(BlockPos position, StoreContainerCandidateRejectionReason reason) {
            candidateEvaluationCount++;
            if (parentRawClosest != null && parentRawClosest.equals(position)) {
                rawCandidateObservationCount++;
                if (rawCandidateReason == null) {
                    rawCandidateEvaluationOrdinal = candidateEvaluationCount;
                    rawCandidateReason = reason;
                }
            }
            if (reason.accepted()) {
                predicateAcceptedCount++;
                return;
            }
            predicateRejectedCount++;
            rejectionCounts.put(reason, rejectionCounts.getOrDefault(reason, 0) + 1);
            if (firstRejectedReason == null) {
                firstRejectedPosition = position;
                firstRejectedReason = reason;
            }
            lastRejectedPosition = position;
            lastRejectedReason = reason;
        }

        private StoreContainerCandidateObservation snapshot(boolean completedNormally,
                                                            StoreDepositOperationState state) {
            long resolvedParentDecisionSequence = parentDecisionSequence;
            long resolvedBranchEpoch = branchEpoch;
            if (resolvedParentDecisionSequence < 0 && state != null) {
                StoreContainerParentDecision parent = state.routeState().currentParentDecision();
                resolvedParentDecisionSequence = parent.sequence();
                resolvedBranchEpoch = parent.branchEpoch();
            }
            return new StoreContainerCandidateObservation(
                    true,
                    completedNormally,
                    operationId,
                    identity(routeChild),
                    resolvedParentDecisionSequence,
                    resolvedBranchEpoch,
                    parentRawClosest,
                    candidateEvaluationCount,
                    predicateAcceptedCount,
                    predicateRejectedCount,
                    rejectionCount(StoreContainerCandidateRejectionReason.CHEST_ABOVE_BLOCKED_UNBREAKABLE),
                    rejectionCount(StoreContainerCandidateRejectionReason.CONTAINER_CACHE_FULL),
                    rejectionCount(StoreContainerCandidateRejectionReason.CACHED_DUNGEON_CHEST),
                    rejectionCount(StoreContainerCandidateRejectionReason.SPAWNER_NEAR_CHEST),
                    rejectionCount(StoreContainerCandidateRejectionReason.UNKNOWN),
                    rejectionCounts.toString(),
                    firstRejectedPosition,
                    firstRejectedReason == null ? "NONE" : firstRejectedReason.name(),
                    lastRejectedPosition,
                    lastRejectedReason == null ? "NONE" : lastRejectedReason.name(),
                    rawCandidateObservationCount,
                    rawCandidateEvaluationOrdinal,
                    rawCandidateOutcome(completedNormally).name(),
                    rawCandidateReason == null ? "UNAVAILABLE" : rawCandidateReason.name(),
                    rawCandidateObservationCount > 0,
                    rawCandidateCoverage(completedNormally)
            );
        }

        private StoreContainerRawCandidateOutcome rawCandidateOutcome(boolean completedNormally) {
            if (!completedNormally) {
                return StoreContainerRawCandidateOutcome.FILTERED_SCAN_DID_NOT_COMPLETE;
            }
            if (parentRawClosest == null) {
                return StoreContainerRawCandidateOutcome.RAW_UNAVAILABLE;
            }
            if (rawCandidateObservationCount == 0 || rawCandidateReason == null) {
                return StoreContainerRawCandidateOutcome.RAW_NOT_VISITED_BY_FILTERED_SCAN;
            }
            return rawCandidateReason.accepted()
                    ? StoreContainerRawCandidateOutcome.RAW_ACCEPTED
                    : StoreContainerRawCandidateOutcome.RAW_REJECTED;
        }

        private String rawCandidateCoverage(boolean completedNormally) {
            return switch (rawCandidateOutcome(completedNormally)) {
                case RAW_ACCEPTED, RAW_REJECTED -> "EXACT_STORE_PREDICATE_RESULT";
                case RAW_NOT_VISITED_BY_FILTERED_SCAN -> "RAW_NOT_VISITED_BY_FILTERED_SCAN";
                case RAW_UNAVAILABLE -> "RAW_PARENT_CANDIDATE_UNAVAILABLE";
                case FILTERED_SCAN_DID_NOT_COMPLETE -> "FILTERED_SCAN_DID_NOT_COMPLETE";
            };
        }

        private int rejectionCount(StoreContainerCandidateRejectionReason reason) {
            return rejectionCounts.getOrDefault(reason, 0);
        }
    }

    private record CompletedScope(Scope scope, boolean completedNormally) {
    }
}
