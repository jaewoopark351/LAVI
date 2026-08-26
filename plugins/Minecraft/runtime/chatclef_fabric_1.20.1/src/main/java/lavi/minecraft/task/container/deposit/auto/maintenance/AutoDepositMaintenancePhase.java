package lavi.minecraft.task.container.deposit.auto.maintenance;

public enum AutoDepositMaintenancePhase {
    DEPOSIT_TRUSTED,
    DEPOSIT_GENERAL,
    VERIFY_WORKING_SET,
    RECOVER,
    VERIFY_FREE_SLOTS,
    DONE,
    CANCELLED
}
