package lavi.minecraft.diagnostics.session.admission;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record DiagnosticSessionSnapshot(
        String diagnosticSessionId,
        long admittedRequests,
        long admittedSlots,
        long ordinarySlotsUsed,
        long criticalSlotsUsed,
        long suppressedRequests,
        long emissionPending,
        long emissionInProgress,
        long emissionCompleted,
        long emissionFailedAfterAdmission,
        boolean capEventClaimed,
        boolean finalSnapshotAdmitted,
        DiagnosticCapTrigger capTrigger,
        long lastTokenSequence,
        boolean tokenSequenceAvailable,
        boolean counterSaturated,
        Map<DiagnosticEventFamily, DiagnosticFamilySnapshot> familyCounters
) {
    public DiagnosticSessionSnapshot {
        Objects.requireNonNull(diagnosticSessionId, "diagnosticSessionId");
        Objects.requireNonNull(capTrigger, "capTrigger");
        Objects.requireNonNull(familyCounters, "familyCounters");
        familyCounters = Collections.unmodifiableMap(new EnumMap<>(familyCounters));
    }

    public long criticalReserveRemaining() {
        return Math.max(0L, DiagnosticSessionLimits.CRITICAL_RESERVE - criticalSlotsUsed);
    }

    public DiagnosticFamilySnapshot family(DiagnosticEventFamily family) {
        return familyCounters.get(Objects.requireNonNull(family, "family"));
    }
}
