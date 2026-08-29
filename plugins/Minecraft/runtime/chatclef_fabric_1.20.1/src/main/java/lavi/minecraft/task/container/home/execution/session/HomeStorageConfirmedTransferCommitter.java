package lavi.minecraft.task.container.home.execution.session;

import lavi.minecraft.task.container.home.execution.HomeStorageManifestProgress;
import lavi.minecraft.task.container.home.execution.operation.HomeStorageTransferIdentity;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;

import java.util.Objects;

//20260828_kpopmodder: Prepare session overlay and operation totals from one paired-delta confirmation.
public final class HomeStorageConfirmedTransferCommitter {
    public HomeStorageConfirmedTransferCommit prepare(
            StoreHomeOperationProgress operation,
            HomeStorageContainerSession session,
            HomeStorageManifestStep step,
            int transferredCount,
            int sourceCountAfter) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(step, "step");
        HomeStorageTransferIdentity identity = session.pendingTransfer()
                .orElseThrow(() -> new IllegalStateException(
                        "confirmed executor result has no task-owned transfer identity"
                ));
        if (!session.ownsPendingTransfer(identity, operation.operationId())
                || identity.logicalPlayerSlot() != step.logicalPlayerInventorySlot()) {
            throw new IllegalStateException("confirmed step differs from pending identity");
        }
        HomeStorageManifestProgress confirmedProgress = session.progress().confirmed(
                step,
                transferredCount,
                sourceCountAfter
        );
        StoreHomeOperationProgress confirmedOperation = operation.confirmed(
                identity,
                transferredCount,
                sourceCountAfter,
                confirmedProgress.remainingStackCount()
        );
        return new HomeStorageConfirmedTransferCommit(
                confirmedOperation,
                session.committed(confirmedProgress),
                confirmedOperation != operation
        );
    }
}
