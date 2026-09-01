package lavi.minecraft.diagnostics.crafting.acquisition.target.mismatch.command;

//20260901_kpopmodder: Expose mismatch outcomes for one exact immutable command scope.
public record CraftResourceCommandMismatchSnapshot(
        long ownedMismatchOccurrenceCount,
        long coverageGapCount,
        long duplicateSuppressedCount,
        long perCorrelationLimitSuppressedCount,
        long sessionLimitSuppressedCount,
        long admissionDeniedCount,
        long emissionFailureCount,
        long unownedObservationCount,
        long unknownAssociationCount,
        int retainedSignatureCount,
        boolean counterSaturated) {
}
