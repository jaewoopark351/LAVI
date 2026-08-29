package lavi.minecraft.task.container.home.execution.transfer;

import lavi.minecraft.task.container.home.execution.transfer.failure.HomeStorageTransferFailureObservation;

//20260829_kpopmodder: Carry one immutable transfer outcome independently of executor lifecycle state.
public record HomeStorageTransferResult(
        HomeStorageTransferStatus status,
        int transferredCount,
        int sourceCountAfter,
        String reason,
        HomeStorageTransferFailureObservation failureObservation) {

    public static HomeStorageTransferResult of(
            HomeStorageTransferStatus status,
            String reason) {
        return new HomeStorageTransferResult(status, 0, -1, reason, null);
    }

    public static HomeStorageTransferResult stale(
            String reason,
            HomeStorageTransferFailureObservation observation) {
        return new HomeStorageTransferResult(
                HomeStorageTransferStatus.MANIFEST_STALE,
                0,
                -1,
                reason,
                observation
        );
    }
}
