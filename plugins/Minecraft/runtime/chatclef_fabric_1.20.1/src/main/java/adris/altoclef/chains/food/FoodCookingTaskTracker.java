package adris.altoclef.chains.food;

import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.item.Item;

// #20260727_kpopmodder: Keeps CollectFoodTask's cooking continuation state separate from food target selection.
public final class FoodCookingTaskTracker {
    private SmeltInSmokerTask smeltTask = null;
    private Item smeltRawMaterial = null;
    private Task cookingFuelTask = null;
    private int cookingFuelTaskRawTotal = 0;
    private int cookingFuelPreparedForRawTotal = 0;

    public boolean hasSmeltingTask() {
        return smeltTask != null;
    }

    public SmeltInSmokerTask getSmeltTask() {
        return smeltTask;
    }

    public Item getSmeltRawMaterial() {
        return smeltRawMaterial;
    }

    public void startSmelting(FoodConversionPlanner.Plan plan) {
        smeltTask = plan.getSmeltTask();
        smeltRawMaterial = plan.getRawMaterial();
    }

    public void clearSmelting() {
        smeltTask = null;
        smeltRawMaterial = null;
    }

    public Task startFuelPreparation(Task task, int rawFoodTotal) {
        cookingFuelTask = task;
        cookingFuelTaskRawTotal = rawFoodTotal;
        return cookingFuelTask;
    }

    public boolean isCookingFuelTask(Task task) {
        return task != null && task == cookingFuelTask;
    }

    public boolean isActiveCookingFuelTask(Task task) {
        return isCookingFuelTask(task)
                && task.isActive()
                && !task.isFinished()
                && !task.thisOrChildAreTimedOut();
    }

    public void finishFuelPreparation() {
        cookingFuelPreparedForRawTotal = Math.max(cookingFuelPreparedForRawTotal, cookingFuelTaskRawTotal);
        clearFuelPreparationTask();
    }

    public void clearFuelPreparationTask() {
        cookingFuelTask = null;
        cookingFuelTaskRawTotal = 0;
    }

    public boolean hasPreparedFuelFor(FoodConversionPlanner.Plan plan) {
        return cookingFuelPreparedForRawTotal >= plan.getTotalRawFoodCount();
    }

    public void markFuelPreparedFor(FoodConversionPlanner.Plan plan) {
        cookingFuelPreparedForRawTotal = Math.max(cookingFuelPreparedForRawTotal, plan.getTotalRawFoodCount());
    }

    public void resetFuelPreparation() {
        clearFuelPreparationTask();
        cookingFuelPreparedForRawTotal = 0;
    }

    public int getCookingFuelTaskRawTotal() {
        return cookingFuelTaskRawTotal;
    }

    public int getCookingFuelPreparedForRawTotal() {
        return cookingFuelPreparedForRawTotal;
    }
}
