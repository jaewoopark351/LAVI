package lavi.minecraft.task.container.home.execution.transfer.delta;

//20260829_kpopmodder: Carry one immutable paired-delta verification independently of its verifier.
public record HomeStorageTransferDeltaVerification(
        HomeStorageTransferDeltaStatus status,
        int sourceDelta,
        int destinationDelta) {
}
