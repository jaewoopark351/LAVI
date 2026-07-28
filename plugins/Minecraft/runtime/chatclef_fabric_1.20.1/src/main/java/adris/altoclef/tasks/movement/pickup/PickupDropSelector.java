package adris.altoclef.tasks.movement.pickup;

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

//20260729_kpopmodder: Added this selector to keep pickup drop candidate lookup out of task orchestration.
final class PickupDropSelector {
    private final ItemTarget[] itemTargets;
    private final Set<ItemEntity> blacklist;

    PickupDropSelector(ItemTarget[] itemTargets, Set<ItemEntity> blacklist) {
        this.itemTargets = itemTargets;
        this.blacklist = blacklist;
    }

    PickupDropSelection selectClosest(AltoClef mod, Vec3d pos) {
        return mod.getEntityTracker()
                .getClosestItemDrop(pos, drop -> isSelectable(mod, drop), itemTargets)
                .map(PickupDropSelection::selected)
                .orElseGet(PickupDropSelection::empty);
    }

    boolean isSelectable(AltoClef mod, ItemEntity drop) {
        return isUsable(drop)
                && matchesTargets(drop)
                && !blacklist.contains(drop)
                && mod.getEntityTracker().isEntityReachable(drop);
    }

    boolean matchesTargets(ItemEntity drop) {
        if (!isUsable(drop)) {
            return false;
        }
        Item item = drop.getStack().getItem();
        for (ItemTarget target : itemTargets) {
            if (target != null && target.matches(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsable(ItemEntity drop) {
        return drop != null
                && drop.isAlive()
                && !drop.getStack().isEmpty();
    }
}
