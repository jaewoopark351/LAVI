package lavi.minecraft.diagnostics.session.admission;

import java.util.Objects;

public record DiagnosticEmissionLease(Status status, DiagnosticSessionSnapshot snapshot) {
    public DiagnosticEmissionLease {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(snapshot, "snapshot");
    }

    public boolean started() {
        return status == Status.STARTED;
    }

    public enum Status {
        STARTED,
        ALREADY_STARTED,
        ALREADY_SETTLED,
        FOREIGN_TOKEN
    }
}
