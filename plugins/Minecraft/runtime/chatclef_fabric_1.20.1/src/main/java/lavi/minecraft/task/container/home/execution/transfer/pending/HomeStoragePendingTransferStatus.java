package lavi.minecraft.task.container.home.execution.transfer.pending;

//20260829_kpopmodder: Classify only the verification state of one pending exact transfer.
public enum HomeStoragePendingTransferStatus {
    CONTEXT_CHANGED,
    FINGERPRINT_CHANGED,
    CONFIRMED,
    DELTA_MISMATCH,
    DELTA_REVERSED,
    WAITING,
    TIMEOUT
}
