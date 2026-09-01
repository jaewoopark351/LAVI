package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

//20260831_kpopmodder: Expose whether cumulative terminal accounting can be reconciled exactly.
public enum StoreDepositTerminalReconciliationStatus {
    EXACT,
    COVERAGE_GAP,
    SATURATED
}
