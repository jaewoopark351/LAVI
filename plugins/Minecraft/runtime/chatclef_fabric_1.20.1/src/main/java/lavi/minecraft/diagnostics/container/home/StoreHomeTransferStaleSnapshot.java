package lavi.minecraft.diagnostics.container.home;

import java.util.Objects;

//20260828_kpopmodder: Carry exact executor stale evidence across pending cleanup.
public record StoreHomeTransferStaleSnapshot(
        StoreHomeManifestFailureStage failureStage,
        StoreHomeManifestMismatchSnapshot mismatch,
        StoreHomeScreenSlotSnapshot mapping,
        StoreHomePendingTransferSnapshot pending,
        StoreHomeHandlerSnapshot handler,
        StoreHomeDestinationSnapshot destination) {

    public StoreHomeTransferStaleSnapshot {
        Objects.requireNonNull(failureStage, "failureStage");
        Objects.requireNonNull(mismatch, "mismatch");
        Objects.requireNonNull(mapping, "mapping");
        Objects.requireNonNull(pending, "pending");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(destination, "destination");
    }
}
