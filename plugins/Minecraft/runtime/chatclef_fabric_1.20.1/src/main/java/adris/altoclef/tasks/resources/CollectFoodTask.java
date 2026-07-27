package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.food.FoodCollectionBlacklist;
import adris.altoclef.chains.food.FoodCollectionTaskSelector;
import adris.altoclef.chains.food.FoodCollectionTargets;
import adris.altoclef.chains.food.FoodCookingTaskTracker;
import adris.altoclef.chains.food.FoodConversionPlanner;
import adris.altoclef.chains.food.FoodHuntTracker;
import adris.altoclef.chains.food.FoodPotentialCalculator;
import adris.altoclef.chains.food.FoodResourceSnapshot;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Locale;
import java.util.Optional;

public class CollectFoodTask extends Task {


    // Represents order of preferred mobs to least preferred
    public static final CookableFoodTarget[] COOKABLE_FOODS = new CookableFoodTarget[]{
            new CookableFoodTarget("beef", CowEntity.class),
            new CookableFoodTarget("porkchop", PigEntity.class),
            new CookableFoodTarget("chicken", ChickenEntity.class),
            new CookableFoodTarget("mutton", SheepEntity.class),
            new CookableFoodTarget("rabbit", RabbitEntity.class)
    };

    public static final Item[] ITEMS_TO_PICK_UP = new Item[]{
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_APPLE,
            Items.GOLDEN_CARROT,
            Items.BREAD,
            Items.BAKED_POTATO
    };

    public static final CropTarget[] CROPS = new CropTarget[]{
            new CropTarget(Items.WHEAT, Blocks.WHEAT),
            new CropTarget(Items.CARROT, Blocks.CARROTS)
    };

    private final double unitsNeeded;
    private final TimerGame checkNewOptionsTimer = new TimerGame(10);
    private final FoodHuntTracker huntTracker = new FoodHuntTracker();
    private final FoodCookingTaskTracker cookingTracker = new FoodCookingTaskTracker();
    private final StateChangeLogger debugLogger = new StateChangeLogger("CollectFoodTask");
    private Task currentResourceTask = null;

    public CollectFoodTask(double unitsNeeded) {
        this.unitsNeeded = unitsNeeded;
    }

    // Gets the units of food if we were to convert all of our raw resources to food.
    @SuppressWarnings("RedundantCast")
    public static double calculateFoodPotential(AltoClef mod) {
        return FoodPotentialCalculator.calculateFoodPotential(mod, COOKABLE_FOODS);
    }

    @Override
    protected void onStart() {
        AltoClef mod = AltoClef.getInstance();

        mod.getBehaviour().push();
        // Protect ALL food
        mod.getBehaviour().addProtectedItems(ITEMS_TO_PICK_UP);

        // Allow us to consume food.
        /*
        for (CookableFoodTarget food : COOKABLE_FOODS)
            mod.getBehaviour().addProtectedItems(food.getRaw(), food.getCooked());
            mod.getBehaviour().addProtectedItems(crop.cropItem);
        }
         */
        mod.getBehaviour().addProtectedItems(Items.HAY_BLOCK, Items.SWEET_BERRIES);
        debugLogger.event("start: targetUnits=" + formatDouble(unitsNeeded) + ", " + describeFoodResources(mod));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        FoodCollectionBlacklist.applyKnownFoodBlacklists(mod);

        // If we were previously smelting, keep on smelting.
        if (cookingTracker.hasSmeltingTask()) {
            if (cookingTracker.getSmeltTask().isFinished()) {
                debugLogger.state("smelting target complete: raw=" + describeItem(cookingTracker.getSmeltRawMaterial()));
                cookingTracker.clearSmelting();
                resetCookingFuelPreparationIfNoRawFood(mod);
            } else if (cookingTracker.getSmeltTask().thisOrChildAreTimedOut()) {
                huntTracker.clear();
                debugLogger.state("cancel smelting: timed out; raw=" + describeItem(cookingTracker.getSmeltRawMaterial())
                        + ", " + describeFoodResources(mod));
                cookingTracker.clearSmelting();
            } else {
                huntTracker.clear();
                setDebugState("Cooking...");
                debugLogger.state("continue smelting: raw=" + describeItem(cookingTracker.getSmeltRawMaterial()) + ", " + describeFoodResources(mod));
                return cookingTracker.getSmeltTask();
            }
        }

        if (currentResourceTask != null && currentResourceTask.isFinished()) {
            if (cookingTracker.isCookingFuelTask(currentResourceTask)) {
                int rawTotal = cookingTracker.getCookingFuelTaskRawTotal();
                cookingTracker.finishFuelPreparation();
                debugLogger.state("cooking fuel prepared",
                        "cooking fuel prepared: rawTotal=" + rawTotal
                                + ", preparedRawTotal=" + cookingTracker.getCookingFuelPreparedForRawTotal()
                                + ", inventoryFuel=" + formatDouble(StorageHelper.calculateInventoryFuelCount(mod))
                                + ", " + describeFoodResources(mod));
                currentResourceTask = null;
            }
            huntTracker.clear();
        }
        if (currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished() && currentResourceTask.thisOrChildAreTimedOut()) {
            debugLogger.state("subtask timed out", "subtask timed out: task="
                    + describeTask(currentResourceTask)
                    + ", " + describeFoodResources(mod));
            if (huntTracker.hasTarget()) {
                huntTracker.temporarilySkip(mod, debugLogger, "hunt task timed out");
            }
            if (cookingTracker.isCookingFuelTask(currentResourceTask)) {
                cookingTracker.clearFuelPreparationTask();
            }
            currentResourceTask = null;
        }
        if (cookingTracker.isActiveCookingFuelTask(currentResourceTask)) {
            huntTracker.clear();
            setDebugState("Preparing cooking fuel");
            debugLogger.state("continue cooking fuel prep",
                    "continue cooking fuel prep: task=" + describeTask(currentResourceTask)
                            + ", rawTotal=" + cookingTracker.getCookingFuelTaskRawTotal()
                            + ", preparedRawTotal=" + cookingTracker.getCookingFuelPreparedForRawTotal()
                            + ", inventoryFuel=" + formatDouble(StorageHelper.calculateInventoryFuelCount(mod))
                            + ", " + describeFoodResources(mod));
            return currentResourceTask;
        }
        if (huntTracker.hasTarget() && currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished()) {
            String skipReason = huntTracker.getSkipReason(mod, debugLogger);
            if (skipReason != null) {
                huntTracker.temporarilySkip(mod, debugLogger, skipReason);
                currentResourceTask = null;
                return null;
            }
            setDebugState("Killing " + huntTracker.getEntity().getType().getTranslationKey());
            debugLogger.state(huntTracker.getContinueStateKey(),
                    huntTracker.describeContinuation(mod, currentResourceTask)
                            + ", " + describeFoodResources(mod));
            return currentResourceTask;
        }

        if (isActiveFoodPickupTask(currentResourceTask)) {
            setDebugState("Picking up Food");
            debugLogger.state("continue food pickup",
                    "continue food pickup: task=" + describeTask(currentResourceTask)
                            + ", " + describeFoodResources(mod));
            return currentResourceTask;
        }

        if (checkNewOptionsTimer.elapsed()) {
            // Try a new resource task
            if (currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished()) {
                debugLogger.state("refresh subtask options", "refresh subtask options: previous="
                        + describeTask(currentResourceTask)
                        + ", timedOut=" + currentResourceTask.thisOrChildAreTimedOut()
                        + ", " + describeFoodResources(mod));
            }
            checkNewOptionsTimer.reset();
            currentResourceTask = null;
            huntTracker.clear();
        }

        if (currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished() && !currentResourceTask.thisOrChildAreTimedOut()) {
            debugLogger.state("continue subtask: " + describeTask(currentResourceTask) + ", " + describeFoodResources(mod));
            return currentResourceTask;
        }
        if (currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished() && currentResourceTask.thisOrChildAreTimedOut()) {
            debugLogger.state("subtask timed out", "subtask timed out: task="
                    + describeTask(currentResourceTask)
                    + ", " + describeFoodResources(mod));
            huntTracker.clear();
            currentResourceTask = null;
        }

        // Calculate potential
        double potentialFood = calculateFoodPotential(mod);
        if (potentialFood >= unitsNeeded) {
            debugLogger.state("potential enough", "potential enough: potential="
                    + formatDouble(potentialFood)
                    + ", target=" + formatDouble(unitsNeeded)
                    + ", readyScore=" + StorageHelper.calculateInventoryFoodScore()
                    + ", " + describeFoodResources(mod));
            Optional<FoodConversionPlanner.Plan> conversionPlan = FoodConversionPlanner.plan(mod, COOKABLE_FOODS);
            if (conversionPlan.isPresent()) {
                FoodConversionPlanner.Plan plan = conversionPlan.get();
                setDebugState(plan.getDebugState());
                debugLogger.state(plan.getStateKey(), plan.describeWithResources(describeFoodResources(mod)));
                if (plan.isSmelting()) {
                    if (plan.needsFuelPreparation(mod) && !cookingTracker.hasPreparedFuelFor(plan)) {
                        setDebugState("Preparing cooking fuel");
                        debugLogger.state("prepare cooking fuel", plan.describeFuelPreparation(mod, describeFoodResources(mod)));
                        return setCookingFuelTask(plan.createFuelPreparationTask(), plan.getTotalRawFoodCount());
                    }
                    cookingTracker.markFuelPreparedFor(plan);
                    cookingTracker.startSmelting(plan);
                    huntTracker.clear();
                    return cookingTracker.getSmeltTask();
                }
                return setCurrentResourceTask(plan.getTask());
            }
            if (FoodResourceSnapshot.getTotalRawFoodCount(mod, COOKABLE_FOODS) > 0) {
                debugLogger.state("raw food not selected for cooking", "raw food not selected for cooking: " + describeFoodResources(mod));
            } else {
                debugLogger.state("potential met but no conversion target", "potential met but no conversion target: "
                        + describeFoodResources(mod));
            }
        }
        
        cookingTracker.resetFuelPreparation();
        Optional<FoodCollectionTaskSelector.Selection> selection = FoodCollectionTaskSelector.select(
                mod,
                COOKABLE_FOODS,
                ITEMS_TO_PICK_UP,
                CROPS,
                debugLogger,
                describeFoodResources(mod)
        );
        if (selection.isPresent()) {
            FoodCollectionTaskSelector.Selection nextFoodTask = selection.get();
            setDebugState(nextFoodTask.getDebugState());
            debugLogger.state(nextFoodTask.getStateKey(), nextFoodTask.getDetail());
            if (nextFoodTask.isHunt()) {
                return setCurrentHuntTask(
                        mod,
                        nextFoodTask.getTask(),
                        nextFoodTask.getHuntEntity(),
                        nextFoodTask.getHuntRawFood()
                );
            }
            return setCurrentResourceTask(nextFoodTask.getTask());
        }

        // Look for food.
        huntTracker.clear();
        setDebugState("Searching...");
        debugLogger.state("searching for food: potential=" + formatDouble(potentialFood)
                + ", target=" + formatDouble(unitsNeeded)
                + ", " + describeFoodResources(mod));
        return new TimeoutWanderTask();
    }

    static void blackListChickenJockeys(AltoClef mod) {
        FoodCollectionBlacklist.blackListChickenJockeys(mod);
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.getInstance().getBehaviour().pop();
    }

    @Override
    public boolean isFinished() {
        if (cookingTracker.hasSmeltingTask()
                && !cookingTracker.getSmeltTask().isFinished()
                && !cookingTracker.getSmeltTask().thisOrChildAreTimedOut()) {
            return false;
        }
        return StorageHelper.calculateInventoryFoodScore() >= unitsNeeded;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CollectFoodTask) {
            CollectFoodTask task = (CollectFoodTask) other;
            return task.unitsNeeded == unitsNeeded;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Collect " + unitsNeeded + " units of food.";
    }

    private String describeTask(Task task) {
        return task == null ? "none" : task.getClass().getSimpleName() + "{" + task + "}";
    }

    private Task setCurrentResourceTask(Task task) {
        currentResourceTask = task;
        huntTracker.clear();
        return currentResourceTask;
    }

    private Task setCookingFuelTask(Task task, int rawFoodTotal) {
        currentResourceTask = cookingTracker.startFuelPreparation(task, rawFoodTotal);
        huntTracker.clear();
        return currentResourceTask;
    }

    private Task setCurrentHuntTask(AltoClef mod, Task task, Entity entity, Item rawFood) {
        currentResourceTask = task;
        huntTracker.start(mod, entity, rawFood);
        return currentResourceTask;
    }

    private boolean isActiveFoodPickupTask(Task task) {
        return task != null
                && task.isActive()
                && !task.isFinished()
                && !task.thisOrChildAreTimedOut()
                && task.thisOrChildSatisfies(child -> child instanceof PickupDroppedItemTask);
    }

    private void resetCookingFuelPreparationIfNoRawFood(AltoClef mod) {
        if (FoodResourceSnapshot.getTotalRawFoodCount(mod, COOKABLE_FOODS) <= 0) {
            cookingTracker.resetFuelPreparation();
        }
    }

    private String describeItem(Item item) {
        return item == null ? "null" : item.getTranslationKey();
    }

    private String describeFoodResources(AltoClef mod) {
        return FoodResourceSnapshot.capture(mod, COOKABLE_FOODS).describe();
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @SuppressWarnings("rawtypes")
    public static class CookableFoodTarget extends FoodCollectionTargets.CookableFoodTarget {
        public CookableFoodTarget(String rawFood, String cookedFood, Class mobToKill) {
            super(rawFood, cookedFood, mobToKill);
        }

        public CookableFoodTarget(String rawFood, Class mobToKill) {
            super(rawFood, mobToKill);
        }
    }

    @SuppressWarnings("rawtypes")
    private static class CookableFoodTargetFish extends CookableFoodTarget {

        public CookableFoodTargetFish(String rawFood, Class mobToKill) {
            super(rawFood, mobToKill);
        }

        @Override
        public boolean isFish() {
            return true;
        }
    }

    public static class CropTarget extends FoodCollectionTargets.CropTarget {
        public CropTarget(Item cropItem, Block cropBlock) {
            super(cropItem, cropBlock);
        }
    }
}
