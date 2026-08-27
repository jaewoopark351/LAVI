package lavi.minecraft.task.container.home.execution;

//20260827_kpopmodder: Added this type file to preserve typed manual storage terminal meanings.
public enum StoreHomeResult {
    PENDING,
    COMPLETED,
    PARTIAL_TRUSTED_CAPACITY_EXHAUSTED,
    PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE,
    NO_USABLE_TRUSTED_DESTINATION,
    NO_TRUSTED_CAPACITY,
    CURSOR_NOT_EMPTY,
    MANIFEST_STALE,
    CONTEXT_CHANGED,
    TRANSFER_UNCONFIRMED,
    INTERRUPTED
}
