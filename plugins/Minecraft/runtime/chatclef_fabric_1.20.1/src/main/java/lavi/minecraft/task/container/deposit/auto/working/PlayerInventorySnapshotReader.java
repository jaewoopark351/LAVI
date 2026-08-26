package lavi.minecraft.task.container.deposit.auto.working;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PlayerInventorySnapshotReader {

    public Map<Item, Integer> readMain(AltoClef mod) {
        Objects.requireNonNull(mod, "mod");
        Map<Item, Integer> result = new LinkedHashMap<>();
        if (mod.getPlayer() == null || mod.getPlayer().getInventory() == null) {
            return result;
        }
        for (ItemStack stack : mod.getPlayer().getInventory().main) {
            add(result, stack);
        }
        return result;
    }

    public Map<Item, Integer> readMainAndCursor(AltoClef mod) {
        Map<Item, Integer> result = new LinkedHashMap<>(readMain(mod));
        add(result, StorageHelper.getItemStackInCursorSlot());
        return result;
    }

    private static void add(Map<Item, Integer> result, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        result.merge(stack.getItem(), stack.getCount(), Integer::sum);
    }
}
