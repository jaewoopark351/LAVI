package lavi.minecraft.task.container.home.execution.session;

//20260828_kpopmodder: Name each fail-closed exact-GUI activation outcome.
public enum HomeStorageContainerActivationStatus {
    READY,
    PENDING_TRANSFER,
    CONTEXT_CHANGED,
    TRUST_LOST,
    EXACT_BINDING_MISSING,
    CONTAINER_UNSUPPORTED,
    HANDLER_UNAVAILABLE,
    CURSOR_NOT_EMPTY
}
