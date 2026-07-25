package adris.altoclef.lavibridge.actionstate;

//20260725_kpopmodder: Added action status enum outside the registry so state transitions stay explicit.

public enum LaviActionStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
