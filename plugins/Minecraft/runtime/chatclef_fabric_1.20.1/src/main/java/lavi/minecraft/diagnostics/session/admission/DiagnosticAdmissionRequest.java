package lavi.minecraft.diagnostics.session.admission;

import java.util.Objects;

public record DiagnosticAdmissionRequest(DiagnosticEventFamily family, boolean modeEligible) {
    public DiagnosticAdmissionRequest {
        Objects.requireNonNull(family, "family");
    }

    public static DiagnosticAdmissionRequest eligible(DiagnosticEventFamily family) {
        return new DiagnosticAdmissionRequest(family, true);
    }

    public static DiagnosticAdmissionRequest modeIneligible(DiagnosticEventFamily family) {
        return new DiagnosticAdmissionRequest(family, false);
    }
}
