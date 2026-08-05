package lavi.minecraft.diagnostics.inventory;

//20260805_kpopmodder: Describe overlapping registerItem breadcrumbs without changing inventory state.
public record InventoryRegistrationOverlap(boolean sameItemActiveInOtherScan,
                                           boolean sameTargetMapActiveInOtherScan,
                                           boolean sameListIdentityActiveInOtherScan,
                                           String otherActiveScanId,
                                           String otherActiveThreadId) {
    public static InventoryRegistrationOverlap none() {
        return new InventoryRegistrationOverlap(false, false, false, "unavailable", "unavailable");
    }

    public boolean any() {
        return sameItemActiveInOtherScan || sameTargetMapActiveInOtherScan || sameListIdentityActiveInOtherScan;
    }
}
