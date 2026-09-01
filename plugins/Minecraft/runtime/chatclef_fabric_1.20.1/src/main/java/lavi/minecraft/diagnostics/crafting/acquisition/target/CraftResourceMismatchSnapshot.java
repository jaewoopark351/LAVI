package lavi.minecraft.diagnostics.crafting.acquisition.target;

import java.util.Map;
import java.util.Objects;

//20260901_kpopmodder: Expose bounded mismatch aggregates without retaining raw world state.
public final class CraftResourceMismatchSnapshot {
    private final long ownedMismatchOccurrenceCount;
    private final long coverageGapCount;
    private final long duplicateSuppressedCount;
    private final long perCorrelationLimitSuppressedCount;
    private final long sessionLimitSuppressedCount;
    private final long mismatchAdmissionDeniedCount;
    private final long mismatchEmissionFailureCount;
    private final long unownedObservationCount;
    private final long unknownAssociationCount;
    private final int globalRetainedSignatureCount;
    private final int activeCorrelationCount;
    private final Map<String, Integer> retainedSignatureCounts;
    private final boolean counterSaturated;

    CraftResourceMismatchSnapshot(
            long ownedMismatchOccurrenceCount,
            long coverageGapCount,
            long duplicateSuppressedCount,
            long perCorrelationLimitSuppressedCount,
            long sessionLimitSuppressedCount,
            long mismatchAdmissionDeniedCount,
            long mismatchEmissionFailureCount,
            long unownedObservationCount,
            long unknownAssociationCount,
            int globalRetainedSignatureCount,
            int activeCorrelationCount,
            Map<String, Integer> retainedSignatureCounts,
            boolean counterSaturated) {
        this.ownedMismatchOccurrenceCount = ownedMismatchOccurrenceCount;
        this.coverageGapCount = coverageGapCount;
        this.duplicateSuppressedCount = duplicateSuppressedCount;
        this.perCorrelationLimitSuppressedCount = perCorrelationLimitSuppressedCount;
        this.sessionLimitSuppressedCount = sessionLimitSuppressedCount;
        this.mismatchAdmissionDeniedCount = mismatchAdmissionDeniedCount;
        this.mismatchEmissionFailureCount = mismatchEmissionFailureCount;
        this.unownedObservationCount = unownedObservationCount;
        this.unknownAssociationCount = unknownAssociationCount;
        this.globalRetainedSignatureCount = globalRetainedSignatureCount;
        this.activeCorrelationCount = activeCorrelationCount;
        this.retainedSignatureCounts = Map.copyOf(Objects.requireNonNull(
                retainedSignatureCounts,
                "retainedSignatureCounts"
        ));
        this.counterSaturated = counterSaturated;
    }

    public long ownedMismatchOccurrenceCount() {
        return ownedMismatchOccurrenceCount;
    }

    public long coverageGapCount() {
        return coverageGapCount;
    }

    public long duplicateSuppressedCount() {
        return duplicateSuppressedCount;
    }

    public long perCorrelationLimitSuppressedCount() {
        return perCorrelationLimitSuppressedCount;
    }

    public long sessionLimitSuppressedCount() {
        return sessionLimitSuppressedCount;
    }

    public long mismatchAdmissionDeniedCount() {
        return mismatchAdmissionDeniedCount;
    }

    public long mismatchEmissionFailureCount() {
        return mismatchEmissionFailureCount;
    }

    public long unownedObservationCount() {
        return unownedObservationCount;
    }

    public long unknownAssociationCount() {
        return unknownAssociationCount;
    }

    public int globalRetainedSignatureCount() {
        return globalRetainedSignatureCount;
    }

    public int activeCorrelationCount() {
        return activeCorrelationCount;
    }

    public int retainedSignatureCount(String correlationId) {
        return retainedSignatureCounts.getOrDefault(correlationId, 0);
    }

    public boolean counterSaturated() {
        return counterSaturated;
    }
}
