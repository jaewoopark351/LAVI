package lavi.minecraft.task.container.deposit;

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Objects;

//20260826_kpopmodder: Added one shared bare deposit_all inventory-selection policy for manual and automatic execution.
public final class DepositAllInventoryTargetSelector {

    public ItemTarget[] select(AltoClef mod) {
        Objects.requireNonNull(mod, "mod");
        return StorageHelper.getAllInventoryItemsAsTargets(slot -> {
            if (ArrayUtils.contains(PlayerSlot.ARMOR_SLOTS, slot)) {
                return false;
            }
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            if (stack.isEmpty()) {
                return false;
            }
            Item item = stack.getItem();
            return !(item instanceof ToolItem);
        });
    }
}
