package lavi.minecraft.diagnostics.container.home;

//20260828_kpopmodder: Preserve pending QUICK_MOVE context before executor cleanup.
public record StoreHomePendingTransferSnapshot(
        boolean pending,
        String destinationKey,
        int logicalSlot,
        int sourceWindowSlot,
        int sourceCountBefore,
        int destinationCountBefore,
        int elapsedTicks) {

    public static StoreHomePendingTransferSnapshot none() {
        return new StoreHomePendingTransferSnapshot(
                false,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                -1,
                -1,
                0,
                0,
                0
        );
    }

    public Object logicalSlotValue() {
        return pending ? logicalSlot : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object sourceWindowSlotValue() {
        return pending ? sourceWindowSlot : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object sourceCountBeforeValue() {
        return pending ? sourceCountBefore : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object destinationCountBeforeValue() {
        return pending ? destinationCountBefore : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object elapsedTicksValue() {
        return pending ? elapsedTicks : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object destinationKeyValue() {
        return pending ? destinationKey : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }
}
