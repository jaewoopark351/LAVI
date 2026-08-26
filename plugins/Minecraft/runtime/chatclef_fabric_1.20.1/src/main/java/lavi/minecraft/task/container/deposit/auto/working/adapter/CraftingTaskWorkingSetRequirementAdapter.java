package lavi.minecraft.task.container.deposit.auto.working.adapter;

import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.container.CraftInTableTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetRequirementAccumulator;

public final class CraftingTaskWorkingSetRequirementAdapter implements TaskWorkingSetRequirementAdapter {
    @Override
    public boolean supports(Task task) {
        return task instanceof CraftInInventoryTask || task instanceof CraftInTableTask;
    }

    @Override
    public boolean collect(Task task, WorkingSetRequirementAccumulator accumulator) {
        if (task instanceof CraftInInventoryTask inventoryTask) {
            return collectRecipe(inventoryTask.getRecipeTarget(), accumulator);
        } else if (task instanceof CraftInTableTask tableTask) {
            RecipeTarget[] targets = tableTask.getRecipeTargets();
            if (targets == null) {
                return false;
            }
            for (RecipeTarget target : targets) {
                if (!collectRecipe(target, accumulator)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean collectRecipe(RecipeTarget target,
                                         WorkingSetRequirementAccumulator accumulator) {
        if (target == null || target.getRecipe() == null) {
            return false;
        }
        int outputHeld = accumulator.available(target.getOutputItem());
        int remainingOutput = Math.max(0, target.getTargetCount() - outputHeld);
        CraftingRecipe recipe = target.getRecipe();
        int outputPerCraft = Math.max(1, recipe.outputCount());
        int crafts = (remainingOutput + outputPerCraft - 1) / outputPerCraft;
        if (crafts == 0) {
            return true;
        }
        for (int index = 0; index < recipe.getSlotCount(); index++) {
            ItemTarget ingredient = recipe.getSlot(index);
            if (ItemTarget.nullOrEmpty(ingredient)) {
                continue;
            }
            long required = (long) ingredient.getTargetCount() * crafts;
            accumulator.reserveTarget(ingredient, required > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) required);
        }
        return true;
    }
}
