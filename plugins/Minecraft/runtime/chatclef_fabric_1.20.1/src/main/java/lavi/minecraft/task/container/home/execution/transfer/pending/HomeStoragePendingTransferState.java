package lavi.minecraft.task.container.home.execution.transfer.pending;

//20260829_kpopmodder: Store only the behavior-owned facts for one pending exact transfer.
public record HomeStoragePendingTransferState(
        String destinationKey,
        int logicalSlot,
        int sourceWindowSlot,
        int sourceCountBefore,
        int destinationCountBefore,
        int elapsedTicks) {
}
