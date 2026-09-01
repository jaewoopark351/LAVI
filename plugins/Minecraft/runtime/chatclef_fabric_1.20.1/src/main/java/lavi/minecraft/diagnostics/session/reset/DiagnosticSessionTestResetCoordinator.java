package lavi.minecraft.diagnostics.session.reset;

import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionCoordinator;
import lavi.minecraft.diagnostics.session.emission.DiagnosticEmissionOutcome;
import lavi.minecraft.diagnostics.session.emission.DiagnosticPhysicalSink;
import lavi.minecraft.diagnostics.session.emission.DiagnosticRecordFormatter;

import java.util.Objects;

/**
 * Deterministic test-only replacement flow. The composition owner must exclude new admissions
 * while this method replaces the old authority with the returned fresh authority.
 */
public final class DiagnosticSessionTestResetCoordinator {
    public DiagnosticSessionTestResetResult reset(
            DiagnosticSessionAdmissionAuthority currentSession,
            String freshSessionId,
            boolean modeEligible,
            DiagnosticRecordFormatter<DiagnosticSessionSnapshot> formatter,
            DiagnosticPhysicalSink sink) {
        Objects.requireNonNull(currentSession, "currentSession");
        Objects.requireNonNull(formatter, "formatter");
        Objects.requireNonNull(sink, "sink");

        DiagnosticSessionSnapshot beforeReset = currentSession.snapshot();
        if (beforeReset.emissionPending() != 0L || beforeReset.emissionInProgress() != 0L) {
            throw new IllegalStateException(
                    "Deterministic reset requires all previously admitted emissions to be settled.");
        }

        if (!modeEligible) {
            return new DiagnosticSessionTestResetResult(
                    DiagnosticSessionTestResetResult.Status.SKIPPED_MODE_OFF,
                    beforeReset,
                    null,
                    new DiagnosticSessionAdmissionAuthority(freshSessionId)
            );
        }

        DiagnosticAdmissionDecision finalSnapshot = currentSession.admitFinalSnapshot(true);
        if (!finalSnapshot.admitted()) {
            throw new IllegalStateException(
                    "Eligible deterministic reset could not reserve its final snapshot: "
                            + finalSnapshot.rejectionReason());
        }
        DiagnosticSessionSnapshot payload = finalSnapshot.snapshot();
        DiagnosticEmissionOutcome outcome = new DiagnosticEmissionCoordinator(currentSession).emit(
                finalSnapshot.token(),
                payload,
                formatter,
                sink
        );
        DiagnosticSessionTestResetResult.Status status = outcome.completed()
                ? DiagnosticSessionTestResetResult.Status.RESET_COMPLETED
                : DiagnosticSessionTestResetResult.Status.RESET_COMPLETED_WITH_FAILED_SNAPSHOT;
        return new DiagnosticSessionTestResetResult(
                status,
                currentSession.snapshot(),
                outcome,
                new DiagnosticSessionAdmissionAuthority(freshSessionId)
        );
    }
}
