package lavi.minecraft.diagnostics.inventory.registration;

import lavi.minecraft.diagnostics.inventory.InventoryRegistrationOverlap;
import lavi.minecraft.diagnostics.inventory.InventoryRegistrationState;
import lavi.minecraft.diagnostics.inventory.InventoryScanContext;

//20260805_kpopmodder: Detect diagnostic-only registerItem overlap evidence.
public final class InventoryRegistrationOverlapDetector {
    public InventoryRegistrationOverlap detect(InventoryScanContext context,
                                               InventoryRegistrationState current,
                                               Iterable<InventoryScanContext> activeScans) {
        for (InventoryScanContext active : activeScans) {
            if (active.scanId() == context.scanId()) {
                continue;
            }
            InventoryRegistrationState other = active.activeRegistration();
            if (other == null) {
                continue;
            }
            boolean sameTargetMap = current.targetMap().equals(other.targetMap());
            boolean sameItem = sameTargetMap && current.itemId().equals(other.itemId());
            boolean sameList = sameTargetMap && current.listIdentity().equals(other.listIdentity());
            if (sameTargetMap || sameItem || sameList) {
                return new InventoryRegistrationOverlap(
                        sameItem,
                        sameTargetMap,
                        sameList,
                        Long.toString(other.scanId()),
                        Long.toString(other.threadId())
                );
            }
        }
        return InventoryRegistrationOverlap.none();
    }
}
