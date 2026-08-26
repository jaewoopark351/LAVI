package lavi.minecraft.diagnostics.container.store.deposit.candidate.range;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;

public final class StoreContainerRangeEventFields {
    private StoreContainerRangeEventFields() {
    }

    public static Object[] parentDecisionFields(StoreContainerRangeTransition transition,
                                                Object fallbackContainerItemPresent) {
        StoreContainerRangeTransition resolved = transition == null
                ? StoreContainerRangeTransition.unavailable()
                : transition;
        StoreContainerRangeSnapshot snapshot = resolved.current();
        return new Object[]{
                "playerPositionAtDecision", ChatClefDiagnostics.vec3d(snapshot.playerPosition()),
                "rawDistanceSquared", value(snapshot.rawDistanceSquared()),
                "rawRangeThresholdSquared", StoreContainerRangeSnapshot.RAW_RANGE_THRESHOLD_SQUARED,
                "rawWithin50Before", resolved.rawPreviousAvailable()
                        ? resolved.rawWithin50Before()
                        : "unavailable",
                "rawWithin50After", snapshot.rawRangeEvaluated()
                        ? snapshot.rawWithin50()
                        : "unavailable",
                "rawRangeCrossing", resolved.rawRangeCrossing(),
                "currentTryDistanceSquared", value(snapshot.currentTryDistanceSquared()),
                "currentTryRangeThresholdSquared", StoreContainerRangeSnapshot.CURRENT_TRY_RANGE_THRESHOLD_SQUARED,
                "currentTryWithin70Before", resolved.currentTryPreviousAvailable()
                        ? resolved.currentTryWithin70Before()
                        : "unavailable",
                "currentTryWithin70After", snapshot.currentTryRangeEvaluated()
                        ? snapshot.currentTryWithin70()
                        : "unavailable",
                "currentTryRangeCrossing", resolved.currentTryRangeCrossing(),
                "fallbackContainerItemPresent", fallbackContainerItemPresent,
                "triggeringBranchEpoch", resolved.branchEpoch()
        };
    }

    public static Object[] childReconciliationFields(StoreContainerRangeTransition transition,
                                                     boolean previousRouteChildStopObserved,
                                                     boolean resourceAcquisitionInterruptedByBranchChange) {
        StoreContainerRangeTransition resolved = transition == null
                ? StoreContainerRangeTransition.unavailable()
                : transition;
        return new Object[]{
                "rawRangeCrossingAtChildReconciliation", resolved.rawRangeCrossing(),
                "currentTryRangeCrossingAtChildReconciliation", resolved.currentTryRangeCrossing(),
                "rangeCrossingAtChildReconciliation", resolved.hasCrossing(),
                "rangeCrossingBranch", resolved.selectedBranch(),
                "rangeCrossingBranchEpoch", resolved.branchEpoch(),
                "previousRouteChildStopObserved", previousRouteChildStopObserved,
                "resourceAcquisitionInterruptedByBranchChange",
                resourceAcquisitionInterruptedByBranchChange
        };
    }

    public static Object[] aggregateFields(String prefix, StoreContainerRangeAggregate aggregate) {
        StoreContainerRangeAggregate resolved = aggregate == null
                ? StoreContainerRangeAggregate.empty()
                : aggregate;
        return new Object[]{
                prefix + "Raw50CrossingCount", resolved.raw50CrossingCount(),
                prefix + "CurrentTry70CrossingCount", resolved.currentTry70CrossingCount(),
                prefix + "BranchChangeAtRangeCrossingCount", resolved.branchChangeAtRangeCrossingCount(),
                prefix + "ResourceChildInterruptedAtRangeCrossingCount",
                resolved.resourceChildInterruptedAtRangeCrossingCount(),
                prefix + "FirstRangeCrossingSample", resolved.firstRangeCrossingSample(),
                prefix + "LastRangeCrossingSample", resolved.lastRangeCrossingSample()
        };
    }

    private static Object value(Double number) {
        return number == null ? "unavailable" : number;
    }
}
