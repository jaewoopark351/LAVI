package lavi.minecraft.diagnostics.session.runtime;

import lavi.minecraft.diagnostics.session.admission.DiagnosticCapTrigger;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;

import java.util.Objects;

//20260831_kpopmodder: Carry the immutable first-rejection evidence for the canonical cap record.
public record DiagnosticCapEventContext(
        DiagnosticCapTrigger trigger,
        String firstSuppressedEvent,
        DiagnosticEventFamily rejectedFamily,
        DiagnosticSessionSnapshot snapshotAfterCapAdmission) {

    public DiagnosticCapEventContext {
        Objects.requireNonNull(trigger, "trigger");
        firstSuppressedEvent = bounded(firstSuppressedEvent);
        Objects.requireNonNull(rejectedFamily, "rejectedFamily");
        Objects.requireNonNull(snapshotAfterCapAdmission, "snapshotAfterCapAdmission");
    }

    private static String bounded(String value) {
        String normalized = value == null || value.isBlank() ? "UNAVAILABLE" : value;
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }
}
