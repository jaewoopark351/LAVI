package lavi.minecraft.task.container.home.execution;

//20260827_kpopmodder: Added this type file to expose the owning StoreHomeTask phase.
public enum StoreHomePhase {
    ACCEPT_REQUEST,
    SNAPSHOT_CONTEXT,
    PLAN_LOADOUT,
    BUILD_DESTINATION_QUEUE,
    SELECT_DESTINATION,
    NAVIGATE_TO_CANDIDATE,
    OPEN_AND_BIND_CANDIDATE,
    VALIDATE_CONTAINER,
    TRANSFER_EXACT_SLOTS,
    VERIFY_TRANSFER,
    REVALIDATE_AFTER_RESUME,
    SUSPENDED,
    TERMINAL,
    PRECHECK_CURRENT_SURPLUS
}
