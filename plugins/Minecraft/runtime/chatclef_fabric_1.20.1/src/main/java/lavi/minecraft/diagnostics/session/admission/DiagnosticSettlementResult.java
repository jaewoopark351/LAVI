package lavi.minecraft.diagnostics.session.admission;

import java.util.Objects;

public record DiagnosticSettlementResult(Status status, DiagnosticSessionSnapshot snapshot) {
    public DiagnosticSettlementResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(snapshot, "snapshot");
    }

    public boolean settled() {
        return status == Status.COMPLETED || status == Status.FAILED_AFTER_ADMISSION;
    }

    public enum Status {
        COMPLETED,
        FAILED_AFTER_ADMISSION,
        NOT_STARTED,
        ALREADY_SETTLED,
        FOREIGN_TOKEN
    }
}
