package adris.altoclef.lavibridge.state;

//20260725_kpopmodder: Added this reader for inventory count and slot snapshots.

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LaviInventoryReader {

    private final AltoClef mod;

    public LaviInventoryReader(AltoClef mod) {
        this.mod = mod;
    }

    public Map<String, Integer> inventoryCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (!AltoClef.inGame() || mod.getPlayer() == null) {
            return counts;
        }

        for (int slot = 0; slot < mod.getPlayer().getInventory().size(); slot++) {
            ItemStack stack = mod.getPlayer().getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                String itemName = ItemHelper.stripItemName(stack.getItem());
                counts.put(itemName, counts.getOrDefault(itemName, 0) + stack.getCount());
            }
        }
        return counts;
    }

    public List<Map<String, Object>> inventorySlots() {
        List<Map<String, Object>> slots = new ArrayList<>();
        if (!AltoClef.inGame() || mod.getPlayer() == null) {
            return slots;
        }

        for (int slot = 0; slot < mod.getPlayer().getInventory().size(); slot++) {
            ItemStack stack = mod.getPlayer().getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("slot", slot);
                item.put("item", ItemHelper.stripItemName(stack.getItem()));
                item.put("count", stack.getCount());
                slots.add(item);
            }
        }
        return slots;
    }
}
