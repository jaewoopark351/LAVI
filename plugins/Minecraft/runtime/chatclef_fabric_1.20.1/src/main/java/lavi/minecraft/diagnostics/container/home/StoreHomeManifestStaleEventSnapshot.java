package lavi.minecraft.diagnostics.container.home;

import java.util.Objects;

//20260828_kpopmodder: Bind one stale decision to immutable operation and failure context.
public record StoreHomeManifestStaleEventSnapshot(
        long operationId,
        int containerSessionOrdinal,
        long planRevision,
        long planCapturedClientTickId,
        long failureClientTickId,
        String phaseBeforeFailure,
        StoreHomeManifestFailureStage failureStage,
        String validationReason,
        int manifestStepCount,
        int currentManifestStepLogicalSlot,
        int confirmedItemCount,
        int touchedStackCount,
        StoreHomeManifestMismatchSnapshot mismatch,
        StoreHomePendingTransferSnapshot pending,
        StoreHomeHandlerSnapshot planHandler,
        StoreHomeHandlerSnapshot failureHandler,
        StoreHomeScreenSlotSnapshot mapping,
        String worldKey,
        String dimension,
        StoreHomeDestinationSnapshot destination) {

    public StoreHomeManifestStaleEventSnapshot {
        Objects.requireNonNull(phaseBeforeFailure, "phaseBeforeFailure");
        Objects.requireNonNull(failureStage, "failureStage");
        Objects.requireNonNull(validationReason, "validationReason");
        Objects.requireNonNull(mismatch, "mismatch");
        Objects.requireNonNull(pending, "pending");
        Objects.requireNonNull(planHandler, "planHandler");
        Objects.requireNonNull(failureHandler, "failureHandler");
        Objects.requireNonNull(mapping, "mapping");
        Objects.requireNonNull(worldKey, "worldKey");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(destination, "destination");
    }

    public long elapsedClientTicks() {
        return planCapturedClientTickId < 0L || failureClientTickId < 0L
                ? -1L
                : Math.max(0L, failureClientTickId - planCapturedClientTickId);
    }

    public String dedupeKey() {
        return operationId + "|" + containerSessionOrdinal + "|"
                + planRevision + "|" + failureStage + "|"
                + mismatch.logicalPlayerSlot() + "|" + validationReason;
    }
}
