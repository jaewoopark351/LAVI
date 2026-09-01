package lavi.minecraft.diagnostics.session.emission;

import lavi.minecraft.diagnostics.session.admission.DiagnosticSettlementResult;

import java.util.Objects;

public record DiagnosticEmissionOutcome(
        Status status,
        FailureStage failureStage,
        String failureType,
        DiagnosticSettlementResult settlement
) {
    public DiagnosticEmissionOutcome {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(failureStage, "failureStage");
        failureType = failureType == null ? "" : failureType;
        Objects.requireNonNull(settlement, "settlement");
    }

    public boolean completed() {
        return status == Status.EMISSION_CALLS_RETURNED;
    }

    public enum Status {
        EMISSION_CALLS_RETURNED,
        EMISSION_FAILED_AFTER_ADMISSION,
        TOKEN_UNAVAILABLE
    }

    public enum FailureStage {
        NONE,
        FORMATTER,
        SINK,
        TOKEN_LEASE
    }
}
