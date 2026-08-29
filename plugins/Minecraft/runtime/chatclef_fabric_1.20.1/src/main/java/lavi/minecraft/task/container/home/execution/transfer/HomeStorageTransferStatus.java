package lavi.minecraft.task.container.home.execution.transfer;

//20260829_kpopmodder: Expose STORE_HOME transfer outcomes without nesting public API in the executor.
public enum HomeStorageTransferStatus {
    WAITING,
    CLICK_REQUESTED,
    TRANSFERRED,
    CONTAINER_NOT_OPEN,
    NO_CAPACITY,
    NO_PROGRESS,
    MANIFEST_STALE,
    CURSOR_NOT_EMPTY,
    TRANSFER_UNCONFIRMED
}
