package lavi.minecraft.diagnostics.crafting.acquisition.scope;

import java.util.Set;

//20260901_kpopmodder: Expose a bounded immutable view of diagnostic scope accounting.
public record IronPickaxeAcquisitionScopeSnapshot(
        int activeCount,
        int tombstoneCount,
        Set<IronPickaxeAcquisitionScopeKey> activeKeys,
        Set<IronPickaxeAcquisitionScopeKey> tombstoneKeys,
        long activationRefusalCount,
        long replacedTombstoneCount,
        long lateEventCount,
        boolean counterSaturated
) {
    public IronPickaxeAcquisitionScopeSnapshot {
        activeKeys = Set.copyOf(activeKeys);
        tombstoneKeys = Set.copyOf(tombstoneKeys);
    }
}
