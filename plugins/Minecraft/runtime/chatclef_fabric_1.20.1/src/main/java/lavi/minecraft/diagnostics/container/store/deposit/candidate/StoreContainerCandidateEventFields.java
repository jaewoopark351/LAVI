package lavi.minecraft.diagnostics.container.store.deposit.candidate;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.snapshot.StoreContainerRouteSnapshot;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;

public final class StoreContainerCandidateEventFields {
    private StoreContainerCandidateEventFields() {
    }

    public static Object[] parentDecisionFields(StoreDepositOperationState state,
                                                Task task,
                                                StoreContainerParentDecision decision,
                                                ItemTarget[] notStored,
                                                Object[] extraFields) {
        Object[] fields = StoreDepositEventFields.merge(
                StoreDepositEventFields.operationFields(state),
                new Object[]{
                        "diagnosticScope", "store_deposit_parent_candidate",
                        "owner", "store_container_candidate_observer",
                        "mode", "BOUNDARY",
                        "trigger", decision.selectedBranch(),
                        "dedupe_key", "store_parent_candidate|"
                                + StoreDepositEventFields.operationId(state)
                                + "|" + decision.selectedBranch()
                                + "|" + position(decision.rawClosest())
                                + "|" + position(decision.currentChestTry())
                                + "|" + decision.rangeDecisionOutcome()
                                + "|" + decision.notStoredStateHash(),
                        "max_emission", "state_change_only,operation_family=44,operation_total=256,session_total=4936",
                        "correlation", "storeOperationId=" + StoreDepositEventFields.operationId(state)
                                + ",candidateDecisionSequence=" + decision.sequence()
                                + ",branchEpoch=" + decision.branchEpoch(),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "candidateDecisionSequence", decision.sequence(),
                        "branchEpoch", decision.branchEpoch(),
                        "previousBranch", decision.previousBranch(),
                        "selectedBranch", decision.selectedBranch(),
                        "branchChanged", decision.branchChanged(),
                        "closestEvaluated", decision.closestEvaluated(),
                        "candidateSearchPerformed", decision.closestEvaluated(),
                        "rawClosestContainerPresent", decision.rawClosest() != null,
                        "rawClosestContainerPosition", ChatClefDiagnostics.blockPos(decision.rawClosest()),
                        "rawClosestBlockType", "UNAVAILABLE_NOT_OBSERVED",
                        "rawClosestScannerSource", "UNFILTERED_BY_STORE_PREDICATE",
                        "closestWithin50Evaluated", decision.closestWithinRangeEvaluated(),
                        "closestWithin50", decision.closestWithinRange(),
                        "currentChestTry", ChatClefDiagnostics.blockPos(decision.currentChestTry()),
                        "currentChestTryBefore", ChatClefDiagnostics.blockPos(decision.currentChestTry()),
                        "currentTryWithin70Evaluated", decision.currentTryWithinExtraRangeEvaluated(),
                        "currentTryWithin70", decision.currentTryWithinExtraRange(),
                        "rawClosestEqualsCurrentTryByValue", decision.rawClosest() != null
                                && decision.rawClosest().equals(decision.currentChestTry()),
                        "rangeDecisionOutcome", decision.rangeDecisionOutcome(),
                        "notStoredStateHash", decision.notStoredStateHash(),
                        "notStoredTargets", ChatClefDiagnostics.itemTargets(notStored),
                        "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
                }
        );
        return StoreDepositEventFields.merge(fields, extraFields);
    }

    public static Object[] filteredSearchFields(StoreDepositOperationState state,
                                                Task task,
                                                Optional<BlockPos> result,
                                                Block[] targetBlocks,
                                                StoreContainerCandidateObservation observation) {
        StoreContainerCandidateObservation resolved = observation == null
                ? StoreContainerCandidateObservation.unavailable()
                : observation;
        BlockPos filtered = result == null ? null : result.orElse(null);
        StoreContainerParentDecision currentParent = state.routeState().currentParentDecision();
        BlockPos originatingRaw = resolved.available() ? resolved.parentRawClosest() : currentParent.rawClosest();
        long originatingSequence = resolved.available() ? resolved.parentDecisionSequence() : currentParent.sequence();
        long originatingEpoch = resolved.available() ? resolved.branchEpoch() : currentParent.branchEpoch();
        String relation = StoreContainerRouteState.relation(originatingRaw, filtered);
        return StoreDepositEventFields.merge(
                StoreDepositEventFields.operationFields(state),
                new Object[]{
                        "diagnosticScope", "store_deposit_filtered_search",
                        "owner", "store_container_candidate_observer",
                        "mode", "BOUNDARY",
                        "trigger", !resolved.scannerCallCompletedNormally()
                                ? "FILTERED_SCAN_DID_NOT_COMPLETE"
                                : filtered == null ? "FILTERED_TARGET_ABSENT" : "FILTERED_TARGET_PRESENT",
                        "dedupe_key", "store_filtered_search|"
                                + StoreDepositEventFields.operationId(state)
                                + "|" + relation
                                + "|" + position(originatingRaw)
                                + "|" + position(filtered)
                                + "|" + resolved.candidateEvaluationCount()
                                 + "|" + resolved.rejectionCountsByReason()
                                 + "|" + resolved.firstRejectedReason()
                                 + "|" + resolved.lastRejectedReason()
                                 + "|" + resolved.rawCandidatePredicateOutcome()
                                 + "|" + resolved.rawCandidateRejectionReason(),
                        "max_emission", "state_change_or_first_reason,operation_family=44,operation_total=256,session_total=4936",
                        "correlation", "storeOperationId=" + StoreDepositEventFields.operationId(state)
                                + ",filteredSearchId=" + filteredSearchId(state)
                                + ",parentDecisionSequenceAtScan=" + originatingSequence
                                + ",branchEpoch=" + originatingEpoch,
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "filteredSearchId", filteredSearchId(state),
                        "routeChildIdentity", StoreDepositEventFields.identity(task),
                        "parentDecisionSequenceAtScan", originatingSequence,
                        "originatingParentDecisionSequence", originatingSequence,
                        "branchEpoch", originatingEpoch,
                        "originatingBranchEpoch", originatingEpoch,
                        "originatingParentRawClosestPresent", originatingRaw != null,
                        "originatingParentRawClosestPosition", ChatClefDiagnostics.blockPos(originatingRaw),
                        "filteredResultPresent", filtered != null,
                        "filteredResultPosition", ChatClefDiagnostics.blockPos(filtered),
                        "rawAndFilteredRelation", relation,
                        "candidateEvaluationCount", resolved.candidateEvaluationCount(),
                        "predicateAcceptedCount", resolved.predicateAcceptedCount(),
                        "predicateRejectedCount", resolved.predicateRejectedCount(),
                        "rejectBlockedAboveUnbreakableCount", resolved.rejectBlockedAboveUnbreakableCount(),
                        "rejectContainerCacheFullCount", resolved.rejectContainerCacheFullCount(),
                        "rejectCachedDungeonCount", resolved.rejectCachedDungeonCount(),
                        "rejectSpawnerNearbyCount", resolved.rejectSpawnerNearbyCount(),
                        "rejectUnknownCount", resolved.rejectUnknownCount(),
                        "rejectionCountsByReason", resolved.rejectionCountsByReason(),
                        "firstRejectedPosition", ChatClefDiagnostics.blockPos(resolved.firstRejectedPosition()),
                        "firstRejectedReason", resolved.firstRejectedReason(),
                         "lastRejectedPosition", ChatClefDiagnostics.blockPos(resolved.lastRejectedPosition()),
                         "lastRejectedReason", resolved.lastRejectedReason(),
                         "rawCandidateEvaluationObserved", resolved.rawCandidateObservationCount() > 0,
                         "rawCandidateEvaluationOrdinal", resolved.rawCandidateEvaluationOrdinal(),
                         "rawCandidatePredicateOutcome", resolved.rawCandidatePredicateOutcome(),
                         "rawCandidateRejectionReason", resolved.rawCandidateRejectionReason(),
                         "rawCandidateMatchedByValue", resolved.rawCandidateMatchedByValue(),
                         "rawCandidateObservationCount", resolved.rawCandidateObservationCount(),
                         "rawCandidateCoverage", resolved.rawCandidateCoverage(),
                         "scannerCallCompletedNormally", resolved.available()
                                ? resolved.scannerCallCompletedNormally()
                                : "UNAVAILABLE",
                        "scannerCallPerformed", true,
                        "scannerFilterDetailAvailable", resolved.available(),
                        "scannerFilterCoverageGap", resolved.available()
                                ? "BLOCK_STATE_AND_UNREACHABLE_FILTERING_NOT_OBSERVED"
                                : "STORE_PREDICATE_AGGREGATE_UNAVAILABLE",
                        "targetBlocks", Arrays.toString(targetBlocks),
                        "taskSummary", ChatClefDiagnostics.taskSummaryForDiagnosticLog(task)
                }
        );
    }

    public static Object[] routeCorrelationFields(StoreDepositOperationState state) {
        if (state == null) {
            return new Object[]{"routeCorrelationAvailable", false};
        }
        StoreContainerRouteState route = state.routeState();
        StoreContainerParentDecision parent = route.currentParentDecision();
        return new Object[]{
                "routeCorrelationAvailable", true,
                "candidateDecisionSequence", parent.sequence(),
                "childLifecycleSequence", route.childLifecycleSequence(),
                "branchEpoch", parent.branchEpoch(),
                "originatingBranchEpoch", parent.branchEpoch(),
                "previousBranch", parent.previousBranch(),
                "currentBranch", parent.selectedBranch(),
                "nextBranch", parent.selectedBranch(),
                "currentRawCandidate", ChatClefDiagnostics.blockPos(parent.rawClosest()),
                "currentFilteredCandidate", ChatClefDiagnostics.blockPos(route.currentFilteredCandidate()),
                "currentPursuit", ChatClefDiagnostics.blockPos(route.currentPursuit()),
                "currentPursuitAction", route.currentPursuitAction(),
                "currentRouteChildIdentity", route.currentRouteChildIdentity(),
                "currentRouteChildClass", route.currentRouteChildClass(),
                "activeStoreAttemptSequence", route.activeStoreAttemptSequence(),
                "activeStoreAttemptId", StoreDepositEventFields.operationId(state)
                        + "-attempt-" + route.activeStoreAttemptSequence(),
                "activeStoreAttemptCandidateDecisionSequence",
                route.activeStoreAttemptCandidateDecisionSequence(),
                "activeStoreAttemptBranchEpoch", route.activeStoreAttemptBranchEpoch(),
                "activeStoreAttemptBranch", route.activeStoreAttemptBranch(),
                "activeStoreAttemptRouteChildLifecycleSequence",
                route.activeStoreAttemptRouteChildLifecycleSequence(),
                "activeStoreAttemptRouteChildReplacementCount",
                route.activeStoreAttemptRouteChildReplacementCount()
        };
    }

    public static Object[] childHandoffFields(StoreDepositOperationState state,
                                              Task activeChildBefore,
                                              Task candidateChild,
                                              Task activeChildAfter) {
        return StoreDepositEventFields.merge(
                routeCorrelationFields(state),
                new Object[]{
                        "activeChildClassBefore", className(activeChildBefore),
                        "activeChildIdentityBefore", StoreDepositEventFields.identity(activeChildBefore),
                        "candidateChildClass", className(candidateChild),
                        "candidateChildIdentity", StoreDepositEventFields.identity(candidateChild),
                        "activeChildClassAfter", className(activeChildAfter),
                        "activeChildIdentityAfter", StoreDepositEventFields.identity(activeChildAfter)
                }
        );
    }

    public static Object[] checkpointFields(StoreDepositOperationState state,
                                            StoreContainerRouteCheckpoint checkpoint) {
        return StoreDepositEventFields.merge(
                StoreDepositEventFields.merge(
                        StoreDepositEventFields.operationFields(state),
                        new Object[]{
                        "diagnosticScope", "store_deposit_checkpoint",
                        "owner", "store_container_route_state",
                        "mode", "BOUNDARY",
                        "trigger", "changed_state_interval",
                        "dedupe_key", "store_deposit_checkpoint|"
                                + StoreDepositEventFields.operationId(state)
                                + "|" + checkpoint.checkpointSequence(),
                        "max_emission", "changed_only,interval_ticks=1200,operation_max=64,operation_total=256,session_total=4936",
                        "correlation", "storeOperationId=" + StoreDepositEventFields.operationId(state)
                                + ",checkpointSequence=" + checkpoint.checkpointSequence(),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "checkpointSequence", checkpoint.checkpointSequence(),
                        "gameTick", checkpoint.gameTick(),
                        "elapsedTicks", checkpoint.elapsedTicks(),
                        "candidateDecisionSequence", checkpoint.candidateDecisionSequence(),
                        "branchEpoch", checkpoint.branchEpoch(),
                        "currentBranch", checkpoint.currentBranch(),
                        "branchCountsSinceLastCheckpoint", checkpoint.branchCountsSinceLastCheckpoint(),
                        "branchTransitionCountsSinceLastCheckpoint", checkpoint.branchTransitionCountsSinceLastCheckpoint(),
                        "currentRawCandidate", ChatClefDiagnostics.blockPos(checkpoint.currentRawCandidate()),
                        "currentFilteredCandidate", ChatClefDiagnostics.blockPos(checkpoint.currentFilteredCandidate()),
                        "currentPursuit", ChatClefDiagnostics.blockPos(checkpoint.currentPursuit()),
                        "childReplacementCountSinceLastCheckpoint", checkpoint.childReplacementCountSinceLastCheckpoint(),
                        "transferDecisionCountSinceLastCheckpoint", checkpoint.transferDecisionCountSinceLastCheckpoint(),
                        "candidateEvaluationCountSinceLastCheckpoint", checkpoint.candidateEvaluationCountSinceLastCheckpoint(),
                        "predicateAcceptedCountSinceLastCheckpoint", checkpoint.predicateAcceptedCountSinceLastCheckpoint(),
                        "predicateRejectedCountSinceLastCheckpoint", checkpoint.predicateRejectedCountSinceLastCheckpoint(),
                        "rejectionCountsSinceLastCheckpoint", checkpoint.rejectionCountsSinceLastCheckpoint(),
                        "notStoredStateHash", checkpoint.notStoredStateHash(),
                        "lastSuccessfulBoundary", checkpoint.lastSuccessfulBoundary(),
                        "firstExplicitFailureBoundary", checkpoint.firstExplicitFailureBoundary(),
                                "firstUnobservedBoundary", checkpoint.firstUnobservedBoundary()
                        }
                ),
                StoreContainerRangeEventFields.aggregateFields(
                        "sinceLastCheckpoint",
                        checkpoint.rangeAggregateSinceLastCheckpoint()
                )
        );
    }

    //20260902_kpopmodder: Preserve ordered route-summary fields through one immutable aggregate snapshot.
    public static Object[] routeSummaryFields(StoreDepositOperationState state) {
        if (state == null) {
            return new Object[]{"routeSummaryAvailable", false};
        }
        StoreContainerRouteSnapshot snapshot = state.routeState().snapshot();
        return StoreDepositEventFields.merge(
                new Object[]{
                        "routeSummaryAvailable", true,
                        "finalCandidateDecisionSequence", snapshot.candidateDecisionSequence(),
                        "finalBranchEpoch", snapshot.branchEpoch(),
                        "finalBranch", snapshot.currentBranch(),
                        "finalRawCandidate", ChatClefDiagnostics.blockPos(snapshot.currentRawCandidate()),
                        "finalFilteredCandidate", ChatClefDiagnostics.blockPos(snapshot.currentFilteredCandidate()),
                        "finalPursuit", ChatClefDiagnostics.blockPos(snapshot.currentPursuit()),
                        "branchCounts", snapshot.branchCounts(),
                        "branchTransitionCounts", snapshot.branchTransitionCounts(),
                        "childReplacementCount", snapshot.childReplacementCount(),
                        "rootRouteChildReplacementCount", snapshot.rootRouteChildReplacementCount(),
                        "childLifecycleSequence", snapshot.childLifecycleSequence(),
                        "finalRouteChildIdentity", snapshot.currentRouteChildIdentity(),
                        "finalRouteChildClass", snapshot.currentRouteChildClass(),
                        "routeTransferDecisionCount", snapshot.transferDecisionCount(),
                        "candidateEvaluationCount", snapshot.candidateEvaluationCount(),
                        "predicateAcceptedCount", snapshot.predicateAcceptedCount(),
                        "predicateRejectedCount", snapshot.predicateRejectedCount(),
                        "predicateRejectionCounts", snapshot.rejectionCounts(),
                        "checkpointCount", snapshot.checkpointCount(),
                        "lastSuccessfulBoundary", snapshot.lastSuccessfulBoundary(),
                        "firstExplicitFailureBoundary", snapshot.firstExplicitFailureBoundary(),
                        "firstUnobservedBoundary", snapshot.firstUnobservedBoundary()
                },
                StoreContainerRangeEventFields.aggregateFields("total", snapshot.rangeSummary())
        );
    }

    private static String filteredSearchId(StoreDepositOperationState state) {
        return StoreDepositEventFields.operationId(state)
                + "-filtered-"
                + (state == null ? 0 : state.filteredSearchResultCount());
    }

    private static String position(BlockPos position) {
        return position == null ? "none" : position.toShortString();
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }
}
