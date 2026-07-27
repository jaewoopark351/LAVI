package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasks.resources.CollectFuelTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Locale;
import java.util.Optional;

//20260727_kpopmodder: Extracted prepared-food conversion planning from CollectFoodTask.
public final class FoodConversionPlanner {
    private static final int EXTRA_COOKING_FUEL_ITEMS = 2;
    private static final double STANDARD_COOKING_FUEL_UNITS = ItemHelper.getFuelAmount(Items.COAL);

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

        int totalRawFoodCount = FoodResourceSnapshot.getTotalRawFoodCount(mod, cookableFoods);
        int estimatedFuelItems = estimateFuelItems(totalRawFoodCount);
        int requestedFuelItems = estimatedFuelItems <= 0 ? 0 : estimatedFuelItems + EXTRA_COOKING_FUEL_ITEMS;

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
                    totalRawFoodCount,
                    estimatedFuelItems,
                    requestedFuelItems,
                    "Cooking...",
                    "cook raw food: raw=" + cookable.getRaw().getTranslationKey(),
                    "cook raw food: raw=" + cookable.getRaw().getTranslationKey()
                            + ", cooked=" + cookable.getCooked().getTranslationKey()
                            + ", rawCount=" + rawCount
                            + ", totalRawCount=" + totalRawFoodCount
                            + ", fuelItems=" + requestedFuelItems
                            + ", targetCount=" + toSmelt,
                    ", "
            ));
        }

        return Optional.empty();
    }

    private static int estimateFuelItems(int rawFoodCount) {
        if (rawFoodCount <= 0 || STANDARD_COOKING_FUEL_UNITS <= 0) {
            return 0;
        }
        return (int) Math.ceil(rawFoodCount / STANDARD_COOKING_FUEL_UNITS);
    }

    public static final class Plan {
        private final Task task;
        private final SmeltInSmokerTask smeltTask;
        private final Item rawMaterial;
        private final int totalRawFoodCount;
        private final int estimatedFuelItems;
        private final int requestedFuelItems;
        private final String debugState;
        private final String stateKey;
        private final String detail;
        private final String resourceSeparator;

        private Plan(
                Task task,
                SmeltInSmokerTask smeltTask,
                Item rawMaterial,
                int totalRawFoodCount,
                int estimatedFuelItems,
                int requestedFuelItems,
                String debugState,
                String stateKey,
                String detail,
                String resourceSeparator
        ) {
            this.task = task;
            this.smeltTask = smeltTask;
            this.rawMaterial = rawMaterial;
            this.totalRawFoodCount = totalRawFoodCount;
            this.estimatedFuelItems = estimatedFuelItems;
            this.requestedFuelItems = requestedFuelItems;
            this.debugState = debugState;
            this.stateKey = stateKey;
            this.detail = detail;
            this.resourceSeparator = resourceSeparator;
        }

        private static Plan resource(Task task, String debugState, String stateKey, String detail, String resourceSeparator) {
            return new Plan(task, null, null, 0, 0, 0, debugState, stateKey, detail, resourceSeparator);
        }

        private static Plan smelting(
                SmeltInSmokerTask task,
                Item rawMaterial,
                int totalRawFoodCount,
                int estimatedFuelItems,
                int requestedFuelItems,
                String debugState,
                String stateKey,
                String detail,
                String resourceSeparator
        ) {
            return new Plan(task, task, rawMaterial, totalRawFoodCount, estimatedFuelItems, requestedFuelItems, debugState, stateKey, detail, resourceSeparator);
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

        public int getTotalRawFoodCount() {
            return totalRawFoodCount;
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

        public boolean needsFuelPreparation(AltoClef mod) {
            return isSmelting()
                    && getRequestedFuelUnits() > 0
                    && StorageHelper.calculateInventoryFuelCount(mod) < getRequestedFuelUnits();
        }

        public Task createFuelPreparationTask() {
            return new CollectFuelTask(getRequestedFuelUnits());
        }

        public String describeFuelPreparation(AltoClef mod, String resources) {
            return "prepare cooking fuel: rawTotal=" + totalRawFoodCount
                    + ", raw=" + describeItem(rawMaterial)
                    + ", estimatedFuelItems=" + estimatedFuelItems
                    + ", extraFuelItems=" + EXTRA_COOKING_FUEL_ITEMS
                    + ", requestedFuelItems=" + requestedFuelItems
                    + ", requestedFuel=" + formatDouble(getRequestedFuelUnits())
                    + ", inventoryFuel=" + formatDouble(StorageHelper.calculateInventoryFuelCount(mod))
                    + ", " + resources;
        }

        private double getRequestedFuelUnits() {
            return requestedFuelItems * STANDARD_COOKING_FUEL_UNITS;
        }

        private String describeItem(Item item) {
            return item == null ? "null" : item.getTranslationKey();
        }

        private String formatDouble(double value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
    }
}
