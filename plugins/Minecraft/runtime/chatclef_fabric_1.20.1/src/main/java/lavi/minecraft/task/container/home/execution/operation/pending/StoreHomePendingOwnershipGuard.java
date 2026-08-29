package lavi.minecraft.task.container.home.execution.operation.pending;

import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to compare executor and session pending ownership.
public final class StoreHomePendingOwnershipGuard {
    private final StoreHomeExecutionState state;
    private final HomeStorageTransferExecutor transferExecutor;

    public StoreHomePendingOwnershipGuard(
            StoreHomeExecutionState state,
            HomeStorageTransferExecutor transferExecutor) {
        this.state = Objects.requireNonNull(state, "state");
        this.transferExecutor = Objects.requireNonNull(
                transferExecutor, "transferExecutor"
        );
    }

    public boolean hasAnyPending() {
        return transferExecutor.hasPending()
                || state.session().current() != null
                && state.session().current().pendingTransfer().isPresent();
    }

    public boolean ownershipMatches() {
        boolean executorPending = transferExecutor.hasPending();
        boolean sessionPending = state.session().current() != null
                && state.session().current().pendingTransfer().isPresent();
        if (executorPending != sessionPending) {
            return false;
        }
        if (!executorPending) {
            return true;
        }
        return transferExecutor.pendingLogicalSlot().isPresent()
                && state.session().current().pendingLogicalSlot().isPresent()
                && transferExecutor.pendingLogicalSlot().getAsInt()
                == state.session().current().pendingLogicalSlot().getAsInt()
                && state.session().current().pendingTransfer()
                .filter(identity -> state.session().current().ownsPendingTransfer(
                        identity, state.operation().current().operationId()
                ))
                .isPresent();
    }
}
