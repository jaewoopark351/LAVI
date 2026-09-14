package lavi.minecraft.task.container.deposit.auto.maintenance;

public enum AutoDepositMaintenanceOutcome {
    PENDING,
    FULL_RELIEF,
    PARTIAL_RELIEF,
    NO_SLOT_RELIEF,
    CANCELLED,
    //20260914_kpopmodder: Missing pressure evidence is not measured zero relief.
    UNAVAILABLE
}
