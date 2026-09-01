package lavi.minecraft.diagnostics.session.admission;

import java.util.Objects;

/**
 * Identity-bearing proof that a fixed number of shared session slots were committed.
 * Equality intentionally remains object identity so a saturated sequence is never reused as identity.
 */
public final class DiagnosticAdmissionToken {
    private final String diagnosticSessionId;
    private final long tokenSequence;
    private final boolean sequenceAvailable;
    private final DiagnosticEventFamily family;
    private final int admittedSlots;
    private final DiagnosticCapTrigger capTrigger;

    DiagnosticAdmissionToken(String diagnosticSessionId,
                             long tokenSequence,
                             boolean sequenceAvailable,
                             DiagnosticEventFamily family,
                             int admittedSlots,
                             DiagnosticCapTrigger capTrigger) {
        this.diagnosticSessionId = Objects.requireNonNull(diagnosticSessionId, "diagnosticSessionId");
        this.tokenSequence = tokenSequence;
        this.sequenceAvailable = sequenceAvailable;
        this.family = Objects.requireNonNull(family, "family");
        this.admittedSlots = admittedSlots;
        this.capTrigger = Objects.requireNonNull(capTrigger, "capTrigger");
    }

    public String diagnosticSessionId() {
        return diagnosticSessionId;
    }

    public long tokenSequence() {
        return tokenSequence;
    }

    public boolean sequenceAvailable() {
        return sequenceAvailable;
    }

    public DiagnosticEventFamily family() {
        return family;
    }

    public int admittedSlots() {
        return admittedSlots;
    }

    public DiagnosticCapTrigger capTrigger() {
        return capTrigger;
    }
}
