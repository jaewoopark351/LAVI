package lavi.minecraft.diagnostics.container.store.deposit.session.snapshot;

import lavi.minecraft.diagnostics.container.store.deposit.session.StoreDepositModeTransitionResult;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedger;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedgerEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedgerSnapshot;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.Objects;

//20260831_kpopmodder: Project only Store cumulative accounting into final session evidence.
public final class StoreDepositSessionSnapshotContributor
        implements DiagnosticSessionLifecycleObserver {
    private final StoreDepositSessionStateSnapshotReader stateReader;
    private final StoreDepositTerminalLedger terminalLedger;
    private long pendingSnapshotSequence;

    public StoreDepositSessionSnapshotContributor(
            StoreDepositSessionStateSnapshotReader stateReader,
            StoreDepositTerminalLedger terminalLedger) {
        this.stateReader = Objects.requireNonNull(stateReader, "stateReader");
        this.terminalLedger = Objects.requireNonNull(terminalLedger, "terminalLedger");
    }

    @Override
    public synchronized Object[] finalSnapshotFields() {
        StoreDepositModeTransitionResult active = stateReader.snapshot();
        StoreDepositTerminalLedgerSnapshot ledger = terminalLedger.captureSnapshot();
        pendingSnapshotSequence = ledger.snapshotSequence();
        return merge(
                StoreDepositTerminalLedgerEventFields.snapshotFields(
                        ledger,
                        "CLEAN_TEARDOWN"
                ),
                new Object[]{
                        "activeStoreDiagnosticOperationCount",
                        active.activeStoreOperationCount(),
                        "activeAutomaticDepositDiagnosticRunCount",
                        active.activeAutomaticRunCount(),
                        "storeThreadLocalCrossThreadCountComplete", false,
                        "storeThreadLocalInvalidation", "EPOCH_FAIL_CLOSED"
                }
        );
    }

    @Override
    public synchronized void afterCleanTeardownSnapshotAttempt(
            boolean emissionCallsReturned) {
        long emittedSnapshotSequence = pendingSnapshotSequence;
        pendingSnapshotSequence = 0L;
        if (emissionCallsReturned && emittedSnapshotSequence > 0L) {
            terminalLedger.recordSnapshotEmissionCallsReturned(
                    emittedSnapshotSequence
            );
        }
    }

    private static Object[] merge(Object[] first, Object[] second) {
        Object[] result = new Object[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
