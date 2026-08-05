package lavi.minecraft.diagnostics.inventory;

import lavi.minecraft.diagnostics.inventory.snapshot.InventoryScanEnvironmentSnapshot;

//20260805_kpopmodder: Capture one bounded inventory scan snapshot without calling ItemStorageTracker.
public final class InventoryScanSnapshot {
    private final InventoryScreenSnapshot screen;
    private final InventoryScanEnvironmentSnapshot environment;

    private InventoryScanSnapshot(InventoryScreenSnapshot screen,
                                  InventoryScanEnvironmentSnapshot environment) {
        this.screen = screen;
        this.environment = environment;
    }

    public static InventoryScanSnapshot capture() {
        return new InventoryScanSnapshot(
                InventoryScreenSnapshot.capture(),
                InventoryScanEnvironmentSnapshot.capture()
        );
    }

    public InventoryScreenSnapshot screen() {
        return screen;
    }

    public String stableKey() {
        return screen.stableKey() + "|" + environment.stableKeySegment();
    }

    public Object[] fields() {
        return InventoryDiagnosticFields.merge(
                screen.beginFields("AtBegin"),
                environment.fields()
        );
    }

    public String carryState() {
        return environment.carryState();
    }
}
