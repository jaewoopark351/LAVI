package lavi.minecraft.diagnostics.container.gui.slot;

import net.minecraft.item.Item;

//20260904_kpopmodder: Carry bounded item counts for one observed slot action.
public record ContainerItemCountSnapshot(
        Item focusItem,
        String focusItemId,
        int playerItemCount,
        int containerItemCount,
        int cursorItemCount,
        String cursorStack,
        String clickedSlotStack,
        String furnaceMaterialSlot,
        String furnaceFuelSlot,
        String furnaceOutputSlot) {
}
