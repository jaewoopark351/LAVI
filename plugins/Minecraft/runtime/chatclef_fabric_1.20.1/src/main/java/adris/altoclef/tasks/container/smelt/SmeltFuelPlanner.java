package adris.altoclef.tasks.container.smelt;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;

//20260729_kpopmodder: Added this planner to keep fuel amount and best-fuel selection separate from smelting task control flow.
public final class SmeltFuelPlanner {
    public double calculateTotalPlannedFuelInContainer(
            SmeltContainerSnapshot snapshot,
            double pendingInsertedFuelAmount,
            double cursorFuelAmount
    ) {
        return ItemHelper.getFuelAmount(snapshot.getFuelSlot())
                + snapshot.getBurningFuelCount()
                + snapshot.getCookProgress()
                + pendingInsertedFuelAmount
                + cursorFuelAmount;
    }

    public double getCurrentFuelAndCookProgress(AltoClef mod) {
        if (mod.getPlayer().currentScreenHandler instanceof AbstractFurnaceScreenHandler handler) {
            return StorageHelper.getFurnaceFuel(handler) + StorageHelper.getFurnaceCookPercent(handler);
        }
        return 0;
    }

    public double getCursorFuelAmount(AltoClef mod) {
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (cursor.isEmpty() || !mod.getModSettings().isSupportedFuel(cursor.getItem())) {
            return 0;
        }
        return ItemHelper.getFuelAmount(cursor);
    }

    public ItemStack getBestFuelStack(AltoClef mod, double needs) {
        double closestDelta = Double.NEGATIVE_INFINITY;
        ItemStack bestStack = null;
        for (ItemStack stack : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
            if (mod.getModSettings().isSupportedFuel(stack.getItem())) {
                double fuelAmount = ItemHelper.getFuelAmount(stack.getItem()) * stack.getCount();
                double delta = needs - fuelAmount;
                if (
                        (bestStack == null) ||
                                (closestDelta > 0 && delta < closestDelta) ||
                                (delta < 0 && delta > closestDelta)
                ) {
                    bestStack = stack;
                    closestDelta = delta;
                }
            }
        }
        return bestStack;
    }
}
