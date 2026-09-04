package lavi.minecraft.diagnostics.container.gui.slot;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

//20260904_kpopmodder: Read slot, cursor, player, and container counts without issuing an action.
public final class ContainerItemCountSnapshotCollector {
    public ContainerItemCountSnapshot capture(
            ScreenHandler handler,
            ClientPlayerEntity player,
            int windowSlot,
            Item preferredItem) {
        ItemStack clicked = stack(handler, windowSlot);
        ItemStack cursor = handler == null ? ItemStack.EMPTY : safeCursor(handler);
        Item focus = preferredItem;
        if (focus == null && !clicked.isEmpty()) {
            focus = clicked.getItem();
        }
        if (focus == null && !cursor.isEmpty()) {
            focus = cursor.getItem();
        }
        int playerCount = 0;
        int containerCount = 0;
        if (handler != null && focus != null) {
            for (Slot slot : handler.slots) {
                ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getStack();
                if (stack.isEmpty() || stack.getItem() != focus) {
                    continue;
                }
                if (player != null && slot.inventory == player.getInventory()) {
                    playerCount += stack.getCount();
                } else {
                    containerCount += stack.getCount();
                }
            }
        }
        return new ContainerItemCountSnapshot(
                focus,
                focus == null ? "unavailable" : Registries.ITEM.getId(focus).toString(),
                playerCount,
                containerCount,
                focus != null && !cursor.isEmpty() && cursor.getItem() == focus ? cursor.getCount() : 0,
                summary(cursor),
                summary(clicked),
                handler instanceof FurnaceScreenHandler ? summary(stack(handler, 0)) : "not_applicable",
                handler instanceof FurnaceScreenHandler ? summary(stack(handler, 1)) : "not_applicable",
                handler instanceof FurnaceScreenHandler ? summary(stack(handler, 2)) : "not_applicable"
        );
    }

    private static ItemStack stack(ScreenHandler handler, int windowSlot) {
        try {
            if (handler == null || windowSlot < 0 || windowSlot >= handler.slots.size()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = handler.slots.get(windowSlot).getStack();
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (RuntimeException | LinkageError error) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack safeCursor(ScreenHandler handler) {
        try {
            ItemStack stack = handler.getCursorStack();
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (RuntimeException | LinkageError error) {
            return ItemStack.EMPTY;
        }
    }

    private static String summary(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        try {
            return Registries.ITEM.getId(stack.getItem()) + "x" + stack.getCount();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }
}
