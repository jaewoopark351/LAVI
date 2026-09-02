package lavi.minecraft.diagnostics.container.store.deposit.candidate.snapshot;

import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeAggregate;
import net.minecraft.util.math.BlockPos;

//20260902_kpopmodder: Capture the final store-container route summary as one immutable diagnostic value.
public record StoreContainerRouteSnapshot(long candidateDecisionSequence,
                                          long branchEpoch,
                                          String currentBranch,
                                          BlockPos currentRawCandidate,
                                          BlockPos currentFilteredCandidate,
                                          BlockPos currentPursuit,
                                          String branchCounts,
                                          String branchTransitionCounts,
                                          int childReplacementCount,
                                          int rootRouteChildReplacementCount,
                                          long childLifecycleSequence,
                                          String currentRouteChildIdentity,
                                          String currentRouteChildClass,
                                          int transferDecisionCount,
                                          int candidateEvaluationCount,
                                          int predicateAcceptedCount,
                                          int predicateRejectedCount,
                                          String rejectionCounts,
                                          int checkpointCount,
                                          String lastSuccessfulBoundary,
                                          String firstExplicitFailureBoundary,
                                          String firstUnobservedBoundary,
                                          StoreContainerRangeAggregate rangeSummary) {
    public StoreContainerRouteSnapshot {
        currentRawCandidate = immutable(currentRawCandidate);
        currentFilteredCandidate = immutable(currentFilteredCandidate);
        currentPursuit = immutable(currentPursuit);
        rangeSummary = rangeSummary == null ? StoreContainerRangeAggregate.empty() : rangeSummary;
    }

    private static BlockPos immutable(BlockPos position) {
        return position == null ? null : position.toImmutable();
    }
}
