package lavi.minecraft.diagnostics.session.admission;

import java.util.Objects;

public record DiagnosticAdmissionDecision(
        boolean admitted,
        DiagnosticAdmissionToken token,
        DiagnosticAdmissionToken canonicalCapToken,
        RejectionReason rejectionReason,
        DiagnosticCapTrigger newlyClaimedCapTrigger,
        DiagnosticSessionSnapshot snapshot
) {
    public DiagnosticAdmissionDecision {
        Objects.requireNonNull(rejectionReason, "rejectionReason");
        Objects.requireNonNull(newlyClaimedCapTrigger, "newlyClaimedCapTrigger");
        Objects.requireNonNull(snapshot, "snapshot");
        if (admitted != (token != null)) {
            throw new IllegalArgumentException("Only an admitted decision may carry the requested-event token.");
        }
        if ((canonicalCapToken == null) != (newlyClaimedCapTrigger == DiagnosticCapTrigger.NONE)) {
            throw new IllegalArgumentException("A newly claimed cap trigger and token must be returned together.");
        }
    }

    static DiagnosticAdmissionDecision granted(DiagnosticAdmissionToken token,
                                                DiagnosticSessionSnapshot snapshot) {
        return new DiagnosticAdmissionDecision(
                true,
                token,
                null,
                RejectionReason.NONE,
                DiagnosticCapTrigger.NONE,
                snapshot
        );
    }

    static DiagnosticAdmissionDecision rejected(RejectionReason reason,
                                                 DiagnosticAdmissionToken capToken,
                                                 DiagnosticCapTrigger capTrigger,
                                                 DiagnosticSessionSnapshot snapshot) {
        return new DiagnosticAdmissionDecision(false, null, capToken, reason, capTrigger, snapshot);
    }

    public boolean canonicalCapEventClaimedByThisDecision() {
        return canonicalCapToken != null;
    }

    public enum RejectionReason {
        NONE,
        MODE_INELIGIBLE,
        ORDINARY_CEILING_REACHED,
        FAMILY_QUOTA_EXHAUSTED,
        SHARED_HARD_CAP_REACHED,
        FINAL_SNAPSHOT_ALREADY_ADMITTED
    }
}
