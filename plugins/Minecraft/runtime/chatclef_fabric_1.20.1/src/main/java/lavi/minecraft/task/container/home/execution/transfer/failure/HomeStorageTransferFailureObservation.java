package lavi.minecraft.task.container.home.execution.transfer.failure;

import lavi.minecraft.task.container.home.execution.transfer.HomeStoragePendingTransferObservation;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Carry neutral same-tick facts from transfer behavior to optional diagnostics.
public record HomeStorageTransferFailureObservation(
        HomeStorageTransferFailureStage stage,
        int expectedSourceCount,
        int manifestStepIndex,
        ItemStack actualStack,
        boolean fingerprintMatched,
        String observationSource,
        Optional<HomeStoragePendingTransferObservation> pendingTransfer) {

    public HomeStorageTransferFailureObservation {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(observationSource, "observationSource");
        Objects.requireNonNull(pendingTransfer, "pendingTransfer");
    }
}
