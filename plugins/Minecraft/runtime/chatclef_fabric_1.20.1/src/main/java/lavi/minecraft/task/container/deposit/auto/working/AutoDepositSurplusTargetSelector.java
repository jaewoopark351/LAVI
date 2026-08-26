package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ToolItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AutoDepositSurplusTargetSelector {

    public ItemTarget[] select(WorkingSetSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<ItemTarget> targets = new ArrayList<>();
        snapshot.mainInventoryCounts().forEach((item, count) -> addSurplus(snapshot, targets, item, count));
        return targets.toArray(ItemTarget[]::new);
    }

    private static void addSurplus(WorkingSetSnapshot snapshot,
                                   List<ItemTarget> targets,
                                   Item item,
                                   int count) {
        if (item instanceof ToolItem) {
            return;
        }
        int surplus = Math.max(0, count - snapshot.reservedCount(item));
        if (surplus > 0) {
            targets.add(new ItemTarget(item, surplus));
        }
    }
}
