package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

//20260831_kpopmodder: Track one terminal group's accounting transition without retaining operation IDs.
enum StoreDepositTerminalAccountingStage {
    ADMISSION_PENDING,
    EMISSION_PENDING,
    SUPPRESSED_BEFORE_ADMISSION,
    EMISSION_COMPLETED,
    EMISSION_FAILED_AFTER_ADMISSION
}
