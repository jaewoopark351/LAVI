package lavi.minecraft.diagnostics.crafting.acquisition.association;

import java.util.Objects;

//20260901_kpopmodder: Count only classified ownership observations and semantic transitions.
public final class CraftResourceAssociationLedger {
    private CraftResourceAssociationStatus currentStatus;
    private long chainOwnerTransitionCount;
    private long commandDescendantObservationCount;
    private long unownedObservationCount;
    private long unknownAssociationCount;
    private long observationGapCount;
    private String lastObservationGapBoundary = "UNAVAILABLE";
    private String lastObservationGapReason = "UNAVAILABLE";
    private boolean counterSaturated;

    public synchronized void observe(CraftResourceAssociationStatus status) {
        Objects.requireNonNull(status, "status");
        if (currentStatus != null && currentStatus != status) {
            chainOwnerTransitionCount = increment(chainOwnerTransitionCount);
        }
        currentStatus = status;
        switch (status) {
            case COMMAND_ROOT_DESCENDANT -> commandDescendantObservationCount = increment(
                    commandDescendantObservationCount
            );
            case CONCURRENT_CHAIN_UNOWNED -> unownedObservationCount = increment(
                    unownedObservationCount
            );
            case UNKNOWN -> unknownAssociationCount = increment(unknownAssociationCount);
        }
    }

    public synchronized CraftResourceAssociationLedgerSnapshot snapshot() {
        return new CraftResourceAssociationLedgerSnapshot(
                currentStatus == null ? CraftResourceAssociationStatus.UNKNOWN : currentStatus,
                chainOwnerTransitionCount,
                commandDescendantObservationCount,
                unownedObservationCount,
                unknownAssociationCount,
                observationGapCount,
                lastObservationGapBoundary,
                lastObservationGapReason,
                counterSaturated
        );
    }

    public synchronized void observeObservationGap(String boundary, String reason) {
        observationGapCount = increment(observationGapCount);
        lastObservationGapBoundary = CraftResourceAssociationTextBound.unavailableIfBlank(
                boundary
        );
        lastObservationGapReason = CraftResourceAssociationTextBound.unavailableIfBlank(reason);
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return value;
        }
        return value + 1L;
    }
}
