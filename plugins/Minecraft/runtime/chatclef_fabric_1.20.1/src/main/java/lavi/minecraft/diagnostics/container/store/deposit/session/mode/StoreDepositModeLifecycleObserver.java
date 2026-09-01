package lavi.minecraft.diagnostics.container.store.deposit.session.mode;

import lavi.minecraft.diagnostics.container.store.deposit.session.StoreDepositModeStateInvalidator;
import lavi.minecraft.diagnostics.container.store.deposit.session.StoreDepositModeTransitionResult;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting.StoreDepositTerminalLedger;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;

import java.util.Objects;

//20260831_kpopmodder: Own only Store diagnostic invalidation at OFF/teardown boundaries.
public final class StoreDepositModeLifecycleObserver
        implements DiagnosticSessionLifecycleObserver {
    private final StoreDepositModeStateInvalidator invalidator;
    private final StoreDepositTerminalLedger terminalLedger;

    public StoreDepositModeLifecycleObserver(
            StoreDepositModeStateInvalidator invalidator,
            StoreDepositTerminalLedger terminalLedger) {
        this.invalidator = Objects.requireNonNull(invalidator, "invalidator");
        this.terminalLedger = Objects.requireNonNull(terminalLedger, "terminalLedger");
    }

    @Override
    public void beforeModeOff() {
        StoreDepositModeTransitionResult result = invalidator.clear();
        if (result.invalidatedAnything()) {
            terminalLedger.recordPartialModeDisabledCoverageGap(
                    result.invalidatedDiagnosticEntryCount()
            );
        }
    }

    @Override
    public void afterCleanTeardownSnapshotAttempt(boolean emissionCallsReturned) {
        invalidator.clear();
    }
}
