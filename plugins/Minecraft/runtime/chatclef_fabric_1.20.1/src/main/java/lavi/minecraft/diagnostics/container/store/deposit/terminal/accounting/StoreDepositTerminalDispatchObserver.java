package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionOutcome;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchObserver;

import java.util.Objects;

//20260831_kpopmodder: Reconcile one Store terminal ledger token with shared group admission and emission.
public final class StoreDepositTerminalDispatchObserver implements DiagnosticDispatchObserver {
    private final StoreDepositTerminalLedger ledger;
    private final StoreDepositTerminalAccountingToken token;

    public StoreDepositTerminalDispatchObserver(StoreDepositTerminalLedger ledger,
                                                StoreDepositTerminalAccountingToken token) {
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.token = Objects.requireNonNull(token, "token");
    }

    @Override
    public void admissionDecided(DiagnosticAdmissionDecision decision) {
        if (decision.admitted()) {
            ledger.recordAdmissionGranted(token);
        } else {
            ledger.recordSuppressedBeforeAdmission(token);
        }
    }

    @Override
    public void emissionSettled(DiagnosticEmissionOutcome outcome) {
        if (outcome == null) {
            return;
        }
        if (outcome.completed()) {
            ledger.recordEmissionCompleted(token);
        } else {
            ledger.recordEmissionFailedAfterAdmission(token);
        }
    }
}
