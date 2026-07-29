package adris.altoclef.tasks.container.smelt;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

//20260729_kpopmodder: Added this policy to isolate cursor compatibility checks before smelting slot clicks.
public final class SmeltTransferPolicy {
    public boolean ensureCursorCanStackWith(AltoClef mod, ItemStack targetStack) {
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (ItemHelper.canStackTogether(targetStack, cursor)) {
            return true;
        }
        Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
        if (toFit.isPresent()) {
            mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
            return false;
        }
        if (ItemHelper.canThrowAwayStack(mod, cursor)) {
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return false;
        }
        return true;
    }
}
