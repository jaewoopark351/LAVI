package lavi.minecraft.task.container.home.execution.session;

import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;

import java.util.Objects;

//20260828_kpopmodder: Carry both prepared sides of one no-yield confirmed-transfer commit.
public record HomeStorageConfirmedTransferCommit(
        StoreHomeOperationProgress operation,
        HomeStorageContainerSession session,
        boolean newlyCommitted) {

    public HomeStorageConfirmedTransferCommit {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(session, "session");
    }
}
