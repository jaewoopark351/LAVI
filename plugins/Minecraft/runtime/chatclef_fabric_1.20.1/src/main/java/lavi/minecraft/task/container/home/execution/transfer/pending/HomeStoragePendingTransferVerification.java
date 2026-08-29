package lavi.minecraft.task.container.home.execution.transfer.pending;

//20260829_kpopmodder: Carry one immutable pending-transfer verification result.
public record HomeStoragePendingTransferVerification(
        HomeStoragePendingTransferStatus status,
        int transferredCount,
        int sourceCountAfter,
        String reason) {
}
