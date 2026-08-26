package lavi.minecraft.diagnostics.container.store.deposit.candidate;

import lavi.minecraft.diagnostics.container.store.deposit.candidate.range.StoreContainerRangeAggregate;
import net.minecraft.util.math.BlockPos;

public record StoreContainerRouteCheckpoint(long checkpointSequence,
                                            long gameTick,
                                            long elapsedTicks,
                                            long candidateDecisionSequence,
                                            long branchEpoch,
                                            String currentBranch,
                                            String branchCountsSinceLastCheckpoint,
                                            String branchTransitionCountsSinceLastCheckpoint,
                                            BlockPos currentRawCandidate,
                                            BlockPos currentFilteredCandidate,
                                            BlockPos currentPursuit,
                                            int childReplacementCountSinceLastCheckpoint,
                                            int transferDecisionCountSinceLastCheckpoint,
                                            int candidateEvaluationCountSinceLastCheckpoint,
                                            int predicateAcceptedCountSinceLastCheckpoint,
                                            int predicateRejectedCountSinceLastCheckpoint,
                                            String rejectionCountsSinceLastCheckpoint,
                                            StoreContainerRangeAggregate rangeAggregateSinceLastCheckpoint,
                                            String notStoredStateHash,
                                            String lastSuccessfulBoundary,
                                            String firstExplicitFailureBoundary,
                                            String firstUnobservedBoundary) {
}
