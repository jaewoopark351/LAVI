package lavi.minecraft.diagnostics.container.store.deposit.candidate;

import net.minecraft.util.math.BlockPos;

public record StoreContainerParentDecision(long sequence,
                                           long branchEpoch,
                                           String previousBranch,
                                           String selectedBranch,
                                           boolean branchChanged,
                                           boolean closestEvaluated,
                                           BlockPos rawClosest,
                                           boolean closestWithinRangeEvaluated,
                                           boolean closestWithinRange,
                                           boolean currentTryWithinExtraRangeEvaluated,
                                           boolean currentTryWithinExtraRange,
                                           BlockPos currentChestTry,
                                           String rangeDecisionOutcome,
                                           String notStoredStateHash) {
    static StoreContainerParentDecision unavailable() {
        return new StoreContainerParentDecision(
                0,
                0,
                "NONE",
                "NONE",
                false,
                false,
                null,
                false,
                false,
                false,
                false,
                null,
                "UNAVAILABLE",
                "unavailable"
        );
    }
}
