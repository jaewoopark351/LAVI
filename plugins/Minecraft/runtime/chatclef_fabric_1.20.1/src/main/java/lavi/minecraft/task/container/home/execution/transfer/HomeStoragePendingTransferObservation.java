package lavi.minecraft.task.container.home.execution.transfer;

//20260829_kpopmodder: Expose a neutral read-only view of behavior-owned pending transfer state.
public record HomeStoragePendingTransferObservation(
        String destinationKey,
        int logicalSlot,
        int sourceWindowSlot,
        int sourceCountBefore,
        int destinationCountBefore,
        int elapsedTicks) {
}
