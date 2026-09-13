package lavi.minecraft.diagnostics.mining.gold;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.mining.operation.MiningOperationToolState;
import lavi.minecraft.integration.mining.operation.MiningToolCandidate;
import net.minecraft.item.ItemStack;

import java.util.Optional;
import java.util.StringJoiner;

//20260913_kpopmodder: Freeze read-only gold-tool fields without reevaluating behavior predicates.
public final class GoldToolSnapshot {
    private GoldToolSnapshot() { }

    public static Object[] preparation(AltoClef mod, MiningOperationToolState state) {
        return new Object[]{"targetCount", state.targetCount(), "rawGoldInventoryCount", state.targetInventoryCount(),
                "targetDurabilityReserve", state.targetDurabilityReserve(),
                "targetToolReady", state.targetToolReady(), "targetHotbarVisible", state.targetToolHotbarVisible(),
                "accessToolReady", state.accessToolReady(), "accessHotbarVisible", state.accessToolHotbarVisible(),
                "targetCandidate", candidate(state.targetToolCandidate()), "accessCandidate", candidate(state.accessToolCandidate()),
                "playerPosition", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getPos()),
                "inventorySlotCoordinateSystem", "PLAYER_INVENTORY_INDEX_HOTBAR_0_TO_8"};
    }

    public static String candidate(Optional<MiningToolCandidate> candidate) {
        return candidate.map(value -> "inventory=" + value.slot().getInventorySlot()
                + ",window=" + value.slot().getWindowSlot() + ",item=" + value.item()
                + ",remaining=" + value.remainingDurability() + ",hotbar=" + value.hotbarVisible()).orElse("NO_ACCEPTED_CANDIDATE");
    }

    public static String identity(Task task) {
        return task == null ? "NONE" : task.getClass().getSimpleName() + "#" + Integer.toHexString(System.identityHashCode(task));
    }

    public static String stack(ItemStack stack) {
        return stack == null ? "UNAVAILABLE" : stack.getItem() + ",count=" + stack.getCount()
                + ",damage=" + stack.getDamage() + ",maxDamage=" + stack.getMaxDamage();
    }

    public static String displacedItemLocations(AltoClef mod, ItemStack displaced) {
        if (displaced == null) return "UNAVAILABLE_PRE_EQUIP_STACK";
        if (displaced.isEmpty()) return "NO_DISPLACED_ITEM";
        return ChatClefDiagnostics.safeValue(() -> {
            StringJoiner slots = new StringJoiner(",");
            int count = 0;
            for (int index = 0; index < mod.getPlayer().getInventory().main.size(); index++) {
                ItemStack current = mod.getPlayer().getInventory().main.get(index);
                if (!current.isEmpty() && current.getItem() == displaced.getItem()) {
                    if (count < 8) slots.add(Integer.toString(index));
                    count++;
                }
            }
            return "item=" + displaced.getItem() + ",mainIndices=" + slots + ",matchingSlots=" + count
                    + ",truncated=" + (count > 8) + ",identity=ITEM_TYPE_ONLY";
        });
    }
}
