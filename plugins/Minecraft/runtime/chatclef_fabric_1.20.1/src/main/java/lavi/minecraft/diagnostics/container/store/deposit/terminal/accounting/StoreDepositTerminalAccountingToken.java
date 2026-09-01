package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

//20260831_kpopmodder: Carry an ephemeral exact-once terminal accounting transition outside the ledger lock.
public final class StoreDepositTerminalAccountingToken {
    private final Object ownerIdentity;
    private final long terminalSequence;
    private StoreDepositTerminalAccountingStage stage = StoreDepositTerminalAccountingStage.ADMISSION_PENDING;

    StoreDepositTerminalAccountingToken(Object ownerIdentity, long terminalSequence) {
        this.ownerIdentity = ownerIdentity;
        this.terminalSequence = terminalSequence;
    }

    public long terminalSequence() {
        return terminalSequence;
    }

    boolean ownedBy(Object expectedOwnerIdentity) {
        return ownerIdentity == expectedOwnerIdentity;
    }

    synchronized boolean transition(StoreDepositTerminalAccountingStage expected,
                                    StoreDepositTerminalAccountingStage next) {
        if (stage != expected) {
            return false;
        }
        stage = next;
        return true;
    }

    public synchronized String stageName() {
        return stage.name();
    }
}
