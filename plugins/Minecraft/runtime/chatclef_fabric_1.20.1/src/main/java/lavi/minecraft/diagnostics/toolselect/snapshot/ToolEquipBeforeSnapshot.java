package lavi.minecraft.diagnostics.toolselect.snapshot;

import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;

//20260913_kpopmodder: Carry only captured diagnostic values across the unchanged equip action.
public record ToolEquipBeforeSnapshot(int selectedSlot, Slot hotbarSlot, ItemStack hotbarStack,
                                      ItemStack mainHand, ItemStack expectedSource) {
    public static final ToolEquipBeforeSnapshot UNAVAILABLE =
            new ToolEquipBeforeSnapshot(-1, null, null, null, null);
}
