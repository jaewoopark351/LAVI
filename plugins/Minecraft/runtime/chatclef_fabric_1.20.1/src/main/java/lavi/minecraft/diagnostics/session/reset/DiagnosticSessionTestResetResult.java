package lavi.minecraft.diagnostics.session.reset;

import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionOutcome;

import java.util.Objects;

public record DiagnosticSessionTestResetResult(
        Status status,
        DiagnosticSessionSnapshot disposedSessionSnapshot,
        DiagnosticEmissionOutcome finalSnapshotEmission,
        DiagnosticSessionAdmissionAuthority freshSession
) {
    public DiagnosticSessionTestResetResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(disposedSessionSnapshot, "disposedSessionSnapshot");
        Objects.requireNonNull(freshSession, "freshSession");
        if (status == Status.SKIPPED_MODE_OFF && finalSnapshotEmission != null) {
            throw new IllegalArgumentException("OFF reset must not attempt a final snapshot emission.");
        }
        if (status != Status.SKIPPED_MODE_OFF && finalSnapshotEmission == null) {
            throw new IllegalArgumentException("Eligible reset must report its final snapshot emission.");
        }
    }

    public enum Status {
        RESET_COMPLETED,
        RESET_COMPLETED_WITH_FAILED_SNAPSHOT,
        SKIPPED_MODE_OFF
    }
}
