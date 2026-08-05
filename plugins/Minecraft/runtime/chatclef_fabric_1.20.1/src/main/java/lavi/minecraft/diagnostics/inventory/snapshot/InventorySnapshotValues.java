package lavi.minecraft.diagnostics.inventory.snapshot;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;

//20260805_kpopmodder: Format small inventory snapshot values without invoking trackers.
public final class InventorySnapshotValues {
    private InventorySnapshotValues() {
    }

    public static String className(Object value, String emptyValue) {
        return value == null ? emptyValue : value.getClass().getSimpleName();
    }

    public static String identity(Object value) {
        return value == null ? "unavailable" : Integer.toHexString(System.identityHashCode(value));
    }

    public static String syncId(ScreenHandler handler) {
        return handler == null ? "unavailable" : Integer.toString(handler.syncId);
    }

    public static String slotCount(ScreenHandler handler) {
        return handler == null ? "unavailable" : Integer.toString(handler.slots.size());
    }

    public static String itemId(ItemStack stack) {
        if (stack == null) {
            return "unavailable";
        }
        return String.valueOf(Registries.ITEM.getId(stack.getItem()));
    }

    public static String count(ItemStack stack) {
        return stack == null ? "unavailable" : Integer.toString(stack.getCount());
    }
}
