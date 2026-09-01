package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

//20260831_kpopmodder: Retain only a bounded recent sample of abnormal Store terminals.
public record StoreDepositAbnormalTerminalSample(
        long terminalSequence,
        String operationId,
        StoreDepositTerminalClassification classification,
        StoreDepositTerminalScope scope,
        long observedTick) {
}
