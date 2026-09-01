package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

//20260831_kpopmodder: Distinguish an available terminal sequence from saturation without reusing MAX_VALUE.
public enum StoreDepositSequenceUnavailableReason {
    NONE,
    UNAVAILABLE_SATURATED
}
