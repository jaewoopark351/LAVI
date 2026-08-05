package lavi.minecraft.diagnostics.inventory.snapshot;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;

//20260805_kpopmodder: Capture furnace slot details for inventory diagnostics separately.
public final class InventoryFurnaceSlotsSnapshot {
    private final String inputItemId;
    private final String inputCount;
    private final String fuelItemId;
    private final String fuelCount;
    private final String outputItemId;
    private final String outputCount;

    private InventoryFurnaceSlotsSnapshot(String inputItemId,
                                          String inputCount,
                                          String fuelItemId,
                                          String fuelCount,
                                          String outputItemId,
                                          String outputCount) {
        this.inputItemId = inputItemId;
        this.inputCount = inputCount;
        this.fuelItemId = fuelItemId;
        this.fuelCount = fuelCount;
        this.outputItemId = outputItemId;
        this.outputCount = outputCount;
    }

    public static InventoryFurnaceSlotsSnapshot capture(ScreenHandler handler) {
        return new InventoryFurnaceSlotsSnapshot(
                furnaceSlotItem(handler, 0),
                furnaceSlotCount(handler, 0),
                furnaceSlotItem(handler, 1),
                furnaceSlotCount(handler, 1),
                furnaceSlotItem(handler, 2),
                furnaceSlotCount(handler, 2)
        );
    }

    public static InventoryFurnaceSlotsSnapshot unavailable(String value) {
        return new InventoryFurnaceSlotsSnapshot(value, value, value, value, value, value);
    }

    public Object[] fields(String suffix) {
        return new Object[]{
                "furnaceInputItemId" + suffix, inputItemId,
                "furnaceInputCount" + suffix, inputCount,
                "furnaceFuelItemId" + suffix, fuelItemId,
                "furnaceFuelCount" + suffix, fuelCount,
                "furnaceOutputItemId" + suffix, outputItemId,
                "furnaceOutputCount" + suffix, outputCount
        };
    }

    private static String furnaceSlotItem(ScreenHandler handler, int slot) {
        ItemStack stack = furnaceSlotStack(handler, slot);
        return stack == null ? "not_applicable" : InventorySnapshotValues.itemId(stack);
    }

    private static String furnaceSlotCount(ScreenHandler handler, int slot) {
        ItemStack stack = furnaceSlotStack(handler, slot);
        return stack == null ? "not_applicable" : InventorySnapshotValues.count(stack);
    }

    private static ItemStack furnaceSlotStack(ScreenHandler handler, int slot) {
        if (!(handler instanceof AbstractFurnaceScreenHandler) || handler.slots.size() <= slot) {
            return null;
        }
        return handler.getSlot(slot).getStack();
    }
}
