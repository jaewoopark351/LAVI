package lavi.minecraft.diagnostics.inventory.registration;

import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

//20260805_kpopmodder: Format registerItem breadcrumb values separately from breadcrumb storage.
public final class InventoryRegisterBreadcrumbValues {
    private InventoryRegisterBreadcrumbValues() {
    }

    public static String itemId(Item item) {
        if (item == null) {
            return "unavailable";
        }
        return String.valueOf(Registries.ITEM.getId(item));
    }

    public static String slotClass(Slot slot) {
        return slot == null ? "unavailable" : slot.getClass().getSimpleName();
    }

    public static String identity(Object value) {
        return value == null ? "unavailable" : Integer.toHexString(System.identityHashCode(value));
    }

    public static String safeSlotValue(SlotValueSupplier supplier) {
        try {
            Integer value = supplier.get();
            return value == null ? "unavailable" : Integer.toString(value);
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    public static String inBounds(String slotValue, String slotCountValue) {
        try {
            int slot = Integer.parseInt(slotValue);
            int slotCount = Integer.parseInt(slotCountValue);
            return Boolean.toString(slot >= 0 && slot < slotCount);
        } catch (RuntimeException error) {
            return "unavailable";
        }
    }

    @FunctionalInterface
    public interface SlotValueSupplier {
        Integer get();
    }
}
