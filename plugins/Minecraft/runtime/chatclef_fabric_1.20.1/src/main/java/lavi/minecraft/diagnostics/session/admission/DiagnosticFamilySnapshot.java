package lavi.minecraft.diagnostics.session.admission;

public record DiagnosticFamilySnapshot(
        long admittedRequests,
        long admittedSlots,
        long suppressedRequests,
        long emissionPending,
        long emissionInProgress,
        long emissionCompleted,
        long emissionFailedAfterAdmission
) {
}
