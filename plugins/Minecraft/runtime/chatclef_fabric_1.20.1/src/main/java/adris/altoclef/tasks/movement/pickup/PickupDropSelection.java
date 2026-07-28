package adris.altoclef.tasks.movement.pickup;

import net.minecraft.entity.ItemEntity;

import java.util.Optional;

//20260729_kpopmodder: Added this type file to isolate dropped-item candidate selection results.
final class PickupDropSelection {
    private static final PickupDropSelection EMPTY = new PickupDropSelection(null);
    private final ItemEntity drop;

    private PickupDropSelection(ItemEntity drop) {
        this.drop = drop;
    }

    static PickupDropSelection selected(ItemEntity drop) {
        return new PickupDropSelection(drop);
    }

    static PickupDropSelection empty() {
        return EMPTY;
    }

    Optional<ItemEntity> drop() {
        return Optional.ofNullable(drop);
    }
}
