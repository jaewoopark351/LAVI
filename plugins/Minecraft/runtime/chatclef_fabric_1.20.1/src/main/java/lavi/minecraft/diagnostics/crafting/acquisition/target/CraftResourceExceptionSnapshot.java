package lavi.minecraft.diagnostics.crafting.acquisition.target;

import java.util.Map;
import java.util.Objects;

//20260901_kpopmodder: Expose bounded exception and coverage aggregates for terminal evidence.
public final class CraftResourceExceptionSnapshot {
    private final long ownedExceptionOccurrenceCount;
    private final long ownedCoverageGapCount;
    private final long unownedObservationCount;
    private final long unknownAssociationCount;
    private final int globalRetainedSignatureCount;
    private final Map<String, Integer> retainedSignatureCounts;
    private final Map<String, CraftResourceExceptionOccurrenceWindow> occurrenceWindows;
    private final boolean counterSaturated;

    CraftResourceExceptionSnapshot(
            long ownedExceptionOccurrenceCount,
            long ownedCoverageGapCount,
            long unownedObservationCount,
            long unknownAssociationCount,
            int globalRetainedSignatureCount,
            Map<String, Integer> retainedSignatureCounts,
            Map<String, CraftResourceExceptionOccurrenceWindow> occurrenceWindows,
            boolean counterSaturated) {
        this.ownedExceptionOccurrenceCount = ownedExceptionOccurrenceCount;
        this.ownedCoverageGapCount = ownedCoverageGapCount;
        this.unownedObservationCount = unownedObservationCount;
        this.unknownAssociationCount = unknownAssociationCount;
        this.globalRetainedSignatureCount = globalRetainedSignatureCount;
        this.retainedSignatureCounts = Map.copyOf(Objects.requireNonNull(
                retainedSignatureCounts,
                "retainedSignatureCounts"
        ));
        this.occurrenceWindows = Map.copyOf(Objects.requireNonNull(
                occurrenceWindows,
                "occurrenceWindows"
        ));
        this.counterSaturated = counterSaturated;
    }

    public long ownedExceptionOccurrenceCount() {
        return ownedExceptionOccurrenceCount;
    }

    public long ownedCoverageGapCount() {
        return ownedCoverageGapCount;
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

    public int retainedSignatureCount(String correlationId) {
        return retainedSignatureCounts.getOrDefault(correlationId, 0);
    }

    public long occurrenceCount(String fingerprint) {
        CraftResourceExceptionOccurrenceWindow window = occurrenceWindows.get(fingerprint);
        return window == null ? 0L : window.occurrenceCount();
    }

    public long firstObservedTick(String fingerprint) {
        CraftResourceExceptionOccurrenceWindow window = occurrenceWindows.get(fingerprint);
        return window == null ? -1L : window.firstObservedTick();
    }

    public long lastObservedTick(String fingerprint) {
        CraftResourceExceptionOccurrenceWindow window = occurrenceWindows.get(fingerprint);
        return window == null ? -1L : window.lastObservedTick();
    }

    public boolean counterSaturated() {
        return counterSaturated;
    }
}
