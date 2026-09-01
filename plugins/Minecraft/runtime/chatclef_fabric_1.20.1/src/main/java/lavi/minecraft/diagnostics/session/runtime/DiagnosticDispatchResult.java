package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionOutcome;

import java.util.Objects;

//20260831_kpopmodder: Return admission and physical outcomes without changing producer behavior.
public record DiagnosticDispatchResult(
        DiagnosticAdmissionDecision admission,
        DiagnosticEmissionOutcome requestedEmission,
        DiagnosticEmissionOutcome canonicalCapEmission) {

    public DiagnosticDispatchResult {
        Objects.requireNonNull(admission, "admission");
    }

    public boolean admitted() {
        return admission.admitted();
    }

    public boolean emissionCompleted() {
        return requestedEmission != null && requestedEmission.completed();
    }
}
