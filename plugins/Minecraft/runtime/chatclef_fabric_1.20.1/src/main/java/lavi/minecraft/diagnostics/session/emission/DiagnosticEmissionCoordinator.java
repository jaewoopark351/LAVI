package lavi.minecraft.diagnostics.session.emission;

import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionToken;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEmissionLease;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSettlementResult;

import java.util.Objects;

/**
 * Formats and emits one already-admitted record without ever requesting another shared slot.
 */
public final class DiagnosticEmissionCoordinator {
    private final DiagnosticSessionAdmissionAuthority authority;

    public DiagnosticEmissionCoordinator(DiagnosticSessionAdmissionAuthority authority) {
        this.authority = Objects.requireNonNull(authority, "authority");
    }

    public <T> DiagnosticEmissionOutcome emit(DiagnosticAdmissionToken token,
                                               T record,
                                               DiagnosticRecordFormatter<T> formatter,
                                               DiagnosticPhysicalSink sink) {
        Objects.requireNonNull(formatter, "formatter");
        Objects.requireNonNull(sink, "sink");

        DiagnosticEmissionLease lease = authority.beginEmission(token);
        if (!lease.started()) {
            return new DiagnosticEmissionOutcome(
                    DiagnosticEmissionOutcome.Status.TOKEN_UNAVAILABLE,
                    DiagnosticEmissionOutcome.FailureStage.TOKEN_LEASE,
                    lease.status().name(),
                    unavailableSettlement(lease)
            );
        }

        String encoded;
        try {
            encoded = Objects.requireNonNull(formatter.format(record), "formatted diagnostic record");
        } catch (RuntimeException | LinkageError failure) {
            return failed(token, DiagnosticEmissionOutcome.FailureStage.FORMATTER, failure);
        }

        try {
            sink.emit(encoded);
        } catch (RuntimeException | LinkageError failure) {
            return failed(token, DiagnosticEmissionOutcome.FailureStage.SINK, failure);
        }

        DiagnosticSettlementResult settlement = authority.completeEmission(token);
        return new DiagnosticEmissionOutcome(
                DiagnosticEmissionOutcome.Status.EMISSION_CALLS_RETURNED,
                DiagnosticEmissionOutcome.FailureStage.NONE,
                "",
                settlement
        );
    }

    private DiagnosticEmissionOutcome failed(DiagnosticAdmissionToken token,
                                             DiagnosticEmissionOutcome.FailureStage stage,
                                             Throwable failure) {
        DiagnosticSettlementResult settlement = authority.failEmission(token);
        return new DiagnosticEmissionOutcome(
                DiagnosticEmissionOutcome.Status.EMISSION_FAILED_AFTER_ADMISSION,
                stage,
                failure.getClass().getName(),
                settlement
        );
    }

    private static DiagnosticSettlementResult unavailableSettlement(DiagnosticEmissionLease lease) {
        DiagnosticSettlementResult.Status status = switch (lease.status()) {
            case ALREADY_SETTLED -> DiagnosticSettlementResult.Status.ALREADY_SETTLED;
            case STARTED, ALREADY_STARTED -> DiagnosticSettlementResult.Status.NOT_STARTED;
            case FOREIGN_TOKEN -> DiagnosticSettlementResult.Status.FOREIGN_TOKEN;
        };
        return new DiagnosticSettlementResult(status, lease.snapshot());
    }
}
