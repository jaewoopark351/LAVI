package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionOutcome;

//20260831_kpopmodder: Reconcile domain accounting at the shared admission and settlement boundaries.
public interface DiagnosticDispatchObserver {
    DiagnosticDispatchObserver NONE = new DiagnosticDispatchObserver() {
        @Override
        public void admissionDecided(DiagnosticAdmissionDecision decision) {
        }

        @Override
        public void emissionSettled(DiagnosticEmissionOutcome outcome) {
        }
    };

    void admissionDecided(DiagnosticAdmissionDecision decision);

    void emissionSettled(DiagnosticEmissionOutcome outcome);
}
