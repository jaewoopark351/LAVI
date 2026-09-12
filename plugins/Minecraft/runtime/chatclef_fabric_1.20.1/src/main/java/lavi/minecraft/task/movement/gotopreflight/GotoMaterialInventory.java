//#if MC == 12001
package lavi.minecraft.task.movement.gotopreflight;

import adris.altoclef.AltoClef;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;

import java.util.HashSet;
import java.util.Set;

import static lavi.minecraft.task.movement.gotopreflight.GotoMaterialPlan.*;

/** Read-only generic throwaway inventory view; never calls throwaway(false), which can swap slots. */
final class GotoMaterialInventory {
    private final Set<Item> accepted;
    private final boolean allowInventory;

    GotoMaterialInventory(AltoClef mod) {
        requirePlayerInventory(mod);
        accepted = Set.copyOf(mod.getClientBaritoneSettings().acceptableThrowawayItems.value);
        allowInventory = mod.getClientBaritoneSettings().allowInventory.value;
    }

    void validateSettings(AltoClef mod) {
        if (!mod.getClientBaritoneSettings().allowPlace.value) {
            throw new Failure(FailureReason.PLACING_DISABLED);
        }
        if (allowInventory != mod.getClientBaritoneSettings().allowInventory.value
                || !accepted.equals(new HashSet<>(mod.getClientBaritoneSettings().acceptableThrowawayItems.value))) {
            throw new Failure(FailureReason.SETTINGS_CHANGED);
        }
    }

    static void requirePlayerInventory(AltoClef mod) {
        if (mod.getPlayer() == null || mod.getWorld() == null
                || mod.getPlayer().currentScreenHandler != mod.getPlayer().playerScreenHandler
                || !mod.getPlayer().currentScreenHandler.getCursorStack().isEmpty()) {
            throw new Failure(FailureReason.INVENTORY_UNAVAILABLE,
                    "Close the container and empty the cursor stack; no items will be discarded.");
        }
    }

    boolean accepts(Item item) { return accepted.contains(item) && item instanceof BlockItem; }

    int count(AltoClef mod) {
        validateSettings(mod);
        requirePlayerInventory(mod);
        var inv = mod.getPlayer().getInventory();
        int result = 0;
        boolean offhandSelectable = false;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || stack.getItem() instanceof PickaxeItem) offhandSelectable = true;
        }
        for (int i = 0; i < (allowInventory ? 36 : 9); i++) {
            result += countStack(mod, inv.getStack(i));
        }
        if (offhandSelectable) result += countStack(mod, inv.getStack(40));
        return result;
    }

    private int countStack(AltoClef mod, ItemStack stack) {
        if (stack.isEmpty() || !accepted.contains(stack.getItem())) return 0;
        // Refuse rather than silently counting a protected/custom stack as expendable.
        if (!(stack.getItem() instanceof BlockItem) || stack.hasNbt()
                || mod.getBehaviour().isProtected(stack.getItem())) {
            throw new Failure(FailureReason.UNSAFE_ACCEPTED_STACK, stack.getItem().toString());
        }
        return stack.getCount();
    }

    int countItem(AltoClef mod, Item item) {
        requirePlayerInventory(mod);
        var inv = mod.getPlayer().getInventory();
        int result = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) result += stack.getCount();
        }
        if (inv.getStack(40).isOf(item)) result += inv.getStack(40).getCount();
        return result;
    }

    boolean hasCapacity(AltoClef mod, Item item) {
        requirePlayerInventory(mod);
        var inv = mod.getPlayer().getInventory();
        // One-item sources only. No clearing inventory and no container slots.
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || (stack.isOf(item) && !stack.hasNbt()
                    && stack.getCount() < stack.getMaxCount())) return true;
        }
        return false;
    }
}
//#endif
