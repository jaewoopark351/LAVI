package adris.altoclef.tasks.container.smelt;

import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;

import java.util.Locale;

//20260729_kpopmodder: Added this snapshot to make furnace, smoker, and blast furnace GUI state observable before changing behavior.
public final class SmeltContainerSnapshot {
    private final boolean containerOpen;
    private final ItemStack materialSlot;
    private final ItemStack fuelSlot;
    private final ItemStack outputSlot;
    private final double burningFuelCount;
    private final double cookProgress;

    private SmeltContainerSnapshot(
            boolean containerOpen,
            ItemStack materialSlot,
            ItemStack fuelSlot,
            ItemStack outputSlot,
            double burningFuelCount,
            double cookProgress
    ) {
        this.containerOpen = containerOpen;
        this.materialSlot = normalize(materialSlot);
        this.fuelSlot = normalize(fuelSlot);
        this.outputSlot = normalize(outputSlot);
        this.burningFuelCount = burningFuelCount;
        this.cookProgress = cookProgress;
    }

    public static SmeltContainerSnapshot empty() {
        return new SmeltContainerSnapshot(false, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, 0, 0);
    }

    public static SmeltContainerSnapshot of(
            boolean containerOpen,
            ItemStack materialSlot,
            ItemStack fuelSlot,
            ItemStack outputSlot,
            double burningFuelCount,
            double cookProgress
    ) {
        return new SmeltContainerSnapshot(containerOpen, materialSlot, fuelSlot, outputSlot, burningFuelCount, cookProgress);
    }

    public static SmeltContainerSnapshot fromOpenContainer(
            AbstractFurnaceScreenHandler handler,
            Slot materialSlot,
            Slot fuelSlot,
            Slot outputSlot
    ) {
        return new SmeltContainerSnapshot(
                true,
                StorageHelper.getItemStackInSlot(materialSlot),
                StorageHelper.getItemStackInSlot(fuelSlot),
                StorageHelper.getItemStackInSlot(outputSlot),
                StorageHelper.getFurnaceFuel(handler),
                StorageHelper.getFurnaceCookPercent(handler)
        );
    }

    public boolean isContainerOpen() {
        return containerOpen;
    }

    public ItemStack getMaterialSlot() {
        return materialSlot;
    }

    public ItemStack getFuelSlot() {
        return fuelSlot;
    }

    public ItemStack getOutputSlot() {
        return outputSlot;
    }

    public double getBurningFuelCount() {
        return burningFuelCount;
    }

    public double getCookProgress() {
        return cookProgress;
    }

    public boolean hasActiveSmeltingState() {
        return burningFuelCount > 0
                || cookProgress > 0
                || isNotEmpty(fuelSlot)
                || isNotEmpty(materialSlot)
                || isNotEmpty(outputSlot);
    }

    public String describeWithPending(double pendingFuelAmount, int pendingFuelTicks, int pendingFuelMoveTicks) {
        return "snapshot[open=" + containerOpen
                + ", input=" + describeStack(materialSlot)
                + ", fuel=" + describeStack(fuelSlot)
                + ", output=" + describeStack(outputSlot)
                + ", burningFuel=" + formatDouble(burningFuelCount)
                + ", cook=" + formatDouble(cookProgress)
                + ", pendingFuel=" + formatDouble(pendingFuelAmount)
                + ", pendingFuelTicks=" + pendingFuelTicks
                + ", pendingFuelMoveTicks=" + pendingFuelMoveTicks
                + "]";
    }

    public static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem().getTranslationKey() + " x " + stack.getCount();
    }

    public static String formatDouble(double value) {
        if (Double.isInfinite(value)) {
            return "infinity";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static boolean isNotEmpty(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    private static ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copy();
    }
}
