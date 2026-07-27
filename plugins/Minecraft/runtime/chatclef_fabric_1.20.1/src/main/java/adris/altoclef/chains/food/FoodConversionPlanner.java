package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Optional;

//20260727_kpopmodder: Extracted prepared-food conversion planning from CollectFoodTask.
public final class FoodConversionPlanner {
    private FoodConversionPlanner() {
    }

    public static Optional<Plan> plan(AltoClef mod, FoodCollectionTargets.CookableFoodTarget[] cookableFoods) {
        if (mod.getItemStorage().getItemCount(Items.WHEAT) >= 3) {
            return Optional.of(Plan.resource(
                    FoodCollectionTasks.createBreadCraftTask(),
                    "Crafting Bread",
                    "craft bread",
                    "craft bread",
                    ": "
            ));
        }

        if (mod.getItemStorage().getItemCount(Items.HAY_BLOCK) >= 1) {
            return Optional.of(Plan.resource(
                    FoodCollectionTasks.createWheatFromHayCraftTask(),
                    "Crafting Wheat",
                    "craft wheat from hay",
                    "craft wheat from hay",
                    ": "
            ));
        }

        for (FoodCollectionTargets.CookableFoodTarget cookable : cookableFoods) {
            int rawCount = mod.getItemStorage().getItemCount(cookable.getRaw());
            if (rawCount <= 0) {
                continue;
            }

            int toSmelt = rawCount + mod.getItemStorage().getItemCount(cookable.getCooked());
            SmeltInSmokerTask smeltTask = new SmeltInSmokerTask(
                    new SmeltTarget(new ItemTarget(cookable.cookedFood, toSmelt), new ItemTarget(cookable.rawFood, rawCount))
            );
            smeltTask.ignoreMaterials();
            return Optional.of(Plan.smelting(
                    smeltTask,
                    cookable.getRaw(),
                    "Cooking...",
                    "cook raw food: raw=" + cookable.getRaw().getTranslationKey(),
                    "cook raw food: raw=" + cookable.getRaw().getTranslationKey()
                            + ", cooked=" + cookable.getCooked().getTranslationKey()
                            + ", rawCount=" + rawCount
                            + ", targetCount=" + toSmelt,
                    ", "
            ));
        }

        return Optional.empty();
    }

    public static final class Plan {
        private final Task task;
        private final SmeltInSmokerTask smeltTask;
        private final Item rawMaterial;
        private final String debugState;
        private final String stateKey;
        private final String detail;
        private final String resourceSeparator;

        private Plan(Task task, SmeltInSmokerTask smeltTask, Item rawMaterial, String debugState, String stateKey, String detail, String resourceSeparator) {
            this.task = task;
            this.smeltTask = smeltTask;
            this.rawMaterial = rawMaterial;
            this.debugState = debugState;
            this.stateKey = stateKey;
            this.detail = detail;
            this.resourceSeparator = resourceSeparator;
        }

        private static Plan resource(Task task, String debugState, String stateKey, String detail, String resourceSeparator) {
            return new Plan(task, null, null, debugState, stateKey, detail, resourceSeparator);
        }

        private static Plan smelting(SmeltInSmokerTask task, Item rawMaterial, String debugState, String stateKey, String detail, String resourceSeparator) {
            return new Plan(task, task, rawMaterial, debugState, stateKey, detail, resourceSeparator);
        }

        public Task getTask() {
            return task;
        }

        public boolean isSmelting() {
            return smeltTask != null;
        }

        public SmeltInSmokerTask getSmeltTask() {
            return smeltTask;
        }

        public Item getRawMaterial() {
            return rawMaterial;
        }

        public String getDebugState() {
            return debugState;
        }

        public String getStateKey() {
            return stateKey;
        }

        public String describeWithResources(String resources) {
            return detail + resourceSeparator + resources;
        }
    }
}
