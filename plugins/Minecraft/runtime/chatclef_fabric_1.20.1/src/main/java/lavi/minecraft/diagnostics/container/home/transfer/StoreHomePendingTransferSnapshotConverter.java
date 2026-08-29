package lavi.minecraft.diagnostics.container.home.transfer;

import lavi.minecraft.diagnostics.container.home.StoreHomePendingTransferSnapshot;
import lavi.minecraft.task.container.home.execution.transfer.HomeStoragePendingTransferObservation;

import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Convert neutral pending behavior state into diagnostics-only snapshot data.
public final class StoreHomePendingTransferSnapshotConverter {
    public StoreHomePendingTransferSnapshot convert(
            Optional<HomeStoragePendingTransferObservation> observation) {
        Objects.requireNonNull(observation, "observation");
        if (observation.isEmpty()) {
            return StoreHomePendingTransferSnapshot.none();
        }
        HomeStoragePendingTransferObservation current = observation.orElseThrow();
        return new StoreHomePendingTransferSnapshot(
                true,
                current.destinationKey(),
                current.logicalSlot(),
                current.sourceWindowSlot(),
                current.sourceCountBefore(),
                current.destinationCountBefore(),
                current.elapsedTicks()
        );
    }
}
