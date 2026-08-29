package lavi.minecraft.task.container.home.execution.timeout.decision;

//20260828_kpopmodder: Name the only three actions allowed after a STORE_HOME timeout match.
public enum StoreHomeTimeoutAction {
    FINISH_TRANSFER_UNCONFIRMED,
    FINISH_OPERATION,
    REJECT_CANDIDATE
}
