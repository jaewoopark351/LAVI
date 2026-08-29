package lavi.minecraft.task.container.home.execution.timeout.decision;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

import java.util.Optional;

//20260828_kpopmodder: Resolve pending precedence without owning clocks or executing Task side effects.
public final class StoreHomeTimeoutResolver {
    private StoreHomeTimeoutResolver() {
    }

    public static Optional<StoreHomeTimeoutDecision> resolveOperation(
            boolean pendingTransfer,
            Optional<StoreHomeTimeoutReason> matchedReason) {
        return matchedReason.map(reason -> {
            if (reason.candidateScoped()) {
                throw new IllegalArgumentException(
                        "candidate timeout cannot be resolved as operation timeout"
                );
            }
            return decision(
                    pendingTransfer,
                    reason,
                    StoreHomeTimeoutAction.FINISH_OPERATION
            );
        });
    }

    public static Optional<StoreHomeTimeoutDecision> resolveCandidate(
            boolean pendingTransfer,
            Optional<StoreHomeTimeoutReason> matchedReason) {
        return matchedReason.map(reason -> {
            if (!reason.candidateScoped()) {
                throw new IllegalArgumentException(
                        "operation timeout cannot be resolved as candidate timeout"
                );
            }
            return decision(
                    pendingTransfer,
                    reason,
                    StoreHomeTimeoutAction.REJECT_CANDIDATE
            );
        });
    }

    private static StoreHomeTimeoutDecision decision(
            boolean pendingTransfer,
            StoreHomeTimeoutReason reason,
            StoreHomeTimeoutAction noPendingAction) {
        return new StoreHomeTimeoutDecision(
                reason,
                pendingTransfer
                        ? StoreHomeTimeoutAction.FINISH_TRANSFER_UNCONFIRMED
                        : noPendingAction
        );
    }
}
