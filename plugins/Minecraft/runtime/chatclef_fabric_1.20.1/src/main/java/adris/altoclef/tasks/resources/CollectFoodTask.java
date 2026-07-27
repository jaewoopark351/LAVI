package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.food.FoodCollectionBlacklist;
import adris.altoclef.chains.food.FoodCollectionTargets;
import adris.altoclef.chains.food.FoodCollectionTasks;
import adris.altoclef.chains.food.FoodMobHuntSelector;
import adris.altoclef.chains.food.FoodPotentialCalculator;
import adris.altoclef.chains.food.FoodResourceSnapshot;
import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    private static final int HUNT_TARGET_NO_PROGRESS_TIMEOUT_TICKS = 20 * 30;
    private static final int HUNT_TARGET_HARD_TIMEOUT_TICKS = 20 * 60;
    private static final int HUNT_LOOT_GRACE_TICKS = 20 * 12;
    private static final double HUNT_DISTANCE_PROGRESS_BLOCKS = 2.0;

    private final double unitsNeeded;
    private final TimerGame checkNewOptionsTimer = new TimerGame(10);
    private SmeltInSmokerTask smeltTask = null;
    private Item smeltRawMaterial = null;
    private Task currentResourceTask = null;
    private Entity currentHuntEntity = null;
    private Item currentHuntRawFood = null;
    private int currentHuntStartTick = -1;
    private int currentHuntLastProgressTick = -1;
    private int currentHuntRawFoodCount = 0;
    private double currentHuntBestDistanceSq = Double.POSITIVE_INFINITY;
    private boolean currentHuntTargetDown = false;
    private final StateChangeLogger debugLogger = new StateChangeLogger("CollectFoodTask");

    public CollectFoodTask(double unitsNeeded) {
        this.unitsNeeded = unitsNeeded;
    }

    private static double getFoodPotential(ItemStack food) {
        return FoodPotentialCalculator.getFoodPotential(food, COOKABLE_FOODS);
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
        if (smeltTask != null) {
            if (smeltTask.isFinished()) {
                debugLogger.state("smelting target complete: raw=" + describeItem(smeltRawMaterial));
                smeltTask = null;
                smeltRawMaterial = null;
            } else if (smeltTask.thisOrChildAreTimedOut()) {
                clearCurrentHuntTarget();
                debugLogger.state("cancel smelting: timed out; raw=" + describeItem(smeltRawMaterial)
                        + ", " + describeFoodResources(mod));
                smeltTask = null;
                smeltRawMaterial = null;
            } else {
                clearCurrentHuntTarget();
                setDebugState("Cooking...");
                debugLogger.state("continue smelting: raw=" + describeItem(smeltRawMaterial) + ", " + describeFoodResources(mod));
                return smeltTask;
            }
        }

        if (currentResourceTask != null && currentResourceTask.isFinished()) {
            clearCurrentHuntTarget();
        }
        if (currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished() && currentResourceTask.thisOrChildAreTimedOut()) {
            debugLogger.state("subtask timed out", "subtask timed out: task="
                    + describeTask(currentResourceTask)
                    + ", " + describeFoodResources(mod));
            if (currentHuntEntity != null) {
                temporarilySkipCurrentHuntTarget(mod, "hunt task timed out");
            }
            currentResourceTask = null;
        }
        if (currentHuntEntity != null && currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished()) {
            String skipReason = getCurrentHuntSkipReason(mod);
            if (skipReason != null) {
                temporarilySkipCurrentHuntTarget(mod, skipReason);
                currentResourceTask = null;
                return null;
            }
            setDebugState("Killing " + currentHuntEntity.getType().getTranslationKey());
            debugLogger.state("continue hunt mob " + currentHuntEntity.getUuid(),
                    "continue hunt mob: raw=" + describeItem(currentHuntRawFood)
                    + ", task=" + describeTask(currentResourceTask)
                    + ", " + describeEntity(mod, currentHuntEntity)
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
            clearCurrentHuntTarget();
        }

        if (currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished() && !currentResourceTask.thisOrChildAreTimedOut()) {
            debugLogger.state("continue subtask: " + describeTask(currentResourceTask) + ", " + describeFoodResources(mod));
            return currentResourceTask;
        }
        if (currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished() && currentResourceTask.thisOrChildAreTimedOut()) {
            debugLogger.state("subtask timed out", "subtask timed out: task="
                    + describeTask(currentResourceTask)
                    + ", " + describeFoodResources(mod));
            clearCurrentHuntTarget();
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
            // Convert our raw foods
            // PLAN:
            // - If we have hay/wheat, make it into bread
            // - If we have raw foods, smelt all of them

            // Convert Hay+Wheat -> Bread
            if (mod.getItemStorage().getItemCount(Items.WHEAT) >= 3) {
                setDebugState("Crafting Bread");
                debugLogger.state("craft bread: " + describeFoodResources(mod));
                return setCurrentResourceTask(FoodCollectionTasks.createBreadCraftTask());
            }
            if (mod.getItemStorage().getItemCount(Items.HAY_BLOCK) >= 1) {
                setDebugState("Crafting Wheat");
                debugLogger.state("craft wheat from hay: " + describeFoodResources(mod));
                return setCurrentResourceTask(FoodCollectionTasks.createWheatFromHayCraftTask());
            }
            // Convert raw foods -> cooked foods

            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                int rawCount = mod.getItemStorage().getItemCount(cookable.getRaw());
                if (rawCount > 0) {
                    int toSmelt = rawCount + mod.getItemStorage().getItemCount(cookable.getCooked());
                    smeltTask = new SmeltInSmokerTask(new SmeltTarget(new ItemTarget(cookable.cookedFood, toSmelt), new ItemTarget(cookable.rawFood, rawCount)));
                    smeltTask.ignoreMaterials();
                    smeltRawMaterial = cookable.getRaw();
                    clearCurrentHuntTarget();
                    debugLogger.state("cook raw food: raw=" + cookable.getRaw().getTranslationKey()
                            + ", cooked=" + cookable.getCooked().getTranslationKey()
                            + ", rawCount=" + rawCount
                            + ", targetCount=" + toSmelt
                            + ", " + describeFoodResources(mod));
                    return smeltTask;
                }
            }
            if (FoodResourceSnapshot.getTotalRawFoodCount(mod, COOKABLE_FOODS) > 0) {
                debugLogger.state("raw food not selected for cooking", "raw food not selected for cooking: " + describeFoodResources(mod));
            } else {
                debugLogger.state("potential met but no conversion target", "potential met but no conversion target: "
                        + describeFoodResources(mod));
            }
        }
        
        {
            // Pick up food items from ground
            for (Item item : ITEMS_TO_PICK_UP) {
                Task t = FoodCollectionTasks.pickupTaskOrNull(mod, item, debugLogger);
                if (t != null) {
                    setDebugState("Picking up Food: " + item.getTranslationKey());
                    debugLogger.state("pickup ready food: item=" + item.getTranslationKey() + ", " + describeFoodResources(mod));
                    return setCurrentResourceTask(t);
                }
            }
            // Pick up raw/cooked foods on ground
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                Task t = FoodCollectionTasks.pickupTaskOrNull(mod, cookable.getRaw(), 20, debugLogger);
                if (t == null) t = FoodCollectionTasks.pickupTaskOrNull(mod, cookable.getCooked(), 40, debugLogger);
                if (t != null) {
                    setDebugState("Picking up Cookable food");
                    debugLogger.state("pickup cookable food: raw=" + cookable.getRaw().getTranslationKey()
                            + ", cooked=" + cookable.getCooked().getTranslationKey()
                            + ", " + describeFoodResources(mod));
                    return setCurrentResourceTask(t);
                }
            }
            // Hay blocks
            Task hayTaskBlock = FoodCollectionTasks.pickupBlockTaskOrNull(mod, Blocks.HAY_BLOCK, Items.HAY_BLOCK, 300, debugLogger);
            if (hayTaskBlock != null) {
                setDebugState("Collecting Hay");
                debugLogger.state("collect hay: " + describeFoodResources(mod));
                return setCurrentResourceTask(hayTaskBlock);
            }
            // Crops
            for (CropTarget target : CROPS) {
                // If crops are nearby. Do not replant cause we don't care.
                Task t = FoodCollectionTasks.pickupBlockTaskOrNull(mod, target.cropBlock, target.cropItem, (blockPos -> {
                    BlockState s = mod.getWorld().getBlockState(blockPos);
                    Block b = s.getBlock();
                    if (b instanceof CropBlock) {
                        boolean isWheat = !(b instanceof PotatoesBlock || b instanceof CarrotsBlock || b instanceof BeetrootsBlock);
                        if (isWheat) {
                            // Chunk needs to be loaded for wheat maturity to be checked.
                            if (!mod.getChunkTracker().isChunkLoaded(blockPos)) {
                                return false;
                            }
                            // Prune if we're not mature/fully grown wheat.
                            CropBlock crop = (CropBlock) b;
                            return crop.isMature(s);
                        }
                    }
                    // Unbreakable.
                    return WorldHelper.canBreak(blockPos);
                    // We're not wheat so do NOT reject.
                }), 96, debugLogger);
                if (t != null) {
                    setDebugState("Harvesting " + target.cropItem.getTranslationKey());
                    debugLogger.state("harvest crop: item=" + target.cropItem.getTranslationKey() + ", " + describeFoodResources(mod));
                    return setCurrentResourceTask(t);
                }
            }
            // Cooked foods
            Optional<FoodMobHuntSelector.HuntTarget> huntTarget = FoodMobHuntSelector.selectBest(mod, COOKABLE_FOODS);
            if (huntTarget.isPresent()) {
                Entity entity = huntTarget.get().getEntity();
                setDebugState("Killing " + entity.getType().getTranslationKey() + " ??? " + entity.isAlive());
                debugLogger.state("hunt mob: entity=" + entity.getType().getTranslationKey()
                        + ", raw=" + huntTarget.get().getRawFood().getTranslationKey()
                        + ", score=" + formatDouble(huntTarget.get().getScore())
                        + ", " + describeEntity(mod, entity)
                        + ", " + describeFoodResources(mod));
                return setCurrentHuntTask(
                        mod,
                        FoodCollectionTasks.killAndLootTask(entity, huntTarget.get().getEntityPredicate(), huntTarget.get().getRawFood()),
                        entity,
                        huntTarget.get().getRawFood()
                );
            }

            // Sweet berries (separate from crops because they should have a lower priority than everything else cause they suck)
            Task berryPickup = FoodCollectionTasks.pickupBlockTaskOrNull(mod, Blocks.SWEET_BERRY_BUSH, Items.SWEET_BERRIES, 96, debugLogger);
            if (berryPickup != null) {
                setDebugState("Getting sweet berries (no better foods are present)");
                debugLogger.state("collect sweet berries: " + describeFoodResources(mod));
                return setCurrentResourceTask(berryPickup);
            }
        }

        // Look for food.
        clearCurrentHuntTarget();
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
        if (smeltTask != null && !smeltTask.isFinished() && !smeltTask.thisOrChildAreTimedOut()) {
            return false;
        }
        return StorageHelper.calculateInventoryFoodScore() >= unitsNeeded;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CollectFoodTask task) {
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
        clearCurrentHuntTarget();
        return currentResourceTask;
    }

    private Task setCurrentHuntTask(AltoClef mod, Task task, Entity entity, Item rawFood) {
        currentResourceTask = task;
        currentHuntEntity = entity;
        currentHuntRawFood = rawFood;
        currentHuntStartTick = WorldHelper.getTicks();
        currentHuntLastProgressTick = currentHuntStartTick;
        currentHuntRawFoodCount = mod.getItemStorage().getItemCount(rawFood);
        currentHuntBestDistanceSq = entity.squaredDistanceTo(mod.getPlayer());
        currentHuntTargetDown = !entity.isAlive();
        return currentResourceTask;
    }

    private void clearCurrentHuntTarget() {
        currentHuntEntity = null;
        currentHuntRawFood = null;
        currentHuntStartTick = -1;
        currentHuntLastProgressTick = -1;
        currentHuntRawFoodCount = 0;
        currentHuntBestDistanceSq = Double.POSITIVE_INFINITY;
        currentHuntTargetDown = false;
    }

    private void temporarilySkipCurrentHuntTarget(AltoClef mod, String reason) {
        debugLogger.state("skip hunt mob " + currentHuntEntity.getUuid(),
                "skip hunt mob: reason=" + reason
                + ", raw=" + describeItem(currentHuntRawFood)
                + ", " + describeEntity(mod, currentHuntEntity));
        FoodCollectionBlacklist.temporarilySkipHuntTarget(mod, currentHuntEntity, reason);
        clearCurrentHuntTarget();
    }

    private String getCurrentHuntSkipReason(AltoClef mod) {
        if (currentHuntEntity == null || currentHuntRawFood == null) {
            return null;
        }

        int now = WorldHelper.getTicks();
        int currentRawFoodCount = mod.getItemStorage().getItemCount(currentHuntRawFood);
        if (currentRawFoodCount > currentHuntRawFoodCount) {
            currentHuntRawFoodCount = currentRawFoodCount;
            currentHuntLastProgressTick = now;
            currentHuntTargetDown = true;
            debugLogger.state("hunt loot collected " + currentHuntEntity.getUuid(),
                    "hunt loot collected: raw=" + describeItem(currentHuntRawFood)
                            + ", count=" + currentRawFoodCount
                            + ", " + describeEntity(mod, currentHuntEntity));
            return null;
        }

        if (!currentHuntEntity.isAlive()) {
            if (!currentHuntTargetDown) {
                currentHuntTargetDown = true;
                currentHuntLastProgressTick = now;
                debugLogger.state("hunt target down " + currentHuntEntity.getUuid(),
                        "hunt target down: raw=" + describeItem(currentHuntRawFood)
                                + ", " + describeEntity(mod, currentHuntEntity));
            }
            if (now - currentHuntLastProgressTick >= HUNT_LOOT_GRACE_TICKS) {
                return "hunt target died but loot was not collected for " + ticksToSeconds(HUNT_LOOT_GRACE_TICKS) + "s";
            }
            return null;
        }

        double currentDistanceSq = currentHuntEntity.squaredDistanceTo(mod.getPlayer());
        double progressThresholdSq = HUNT_DISTANCE_PROGRESS_BLOCKS * HUNT_DISTANCE_PROGRESS_BLOCKS;
        if (Double.isInfinite(currentHuntBestDistanceSq) || currentHuntBestDistanceSq - currentDistanceSq >= progressThresholdSq) {
            currentHuntBestDistanceSq = currentDistanceSq;
            currentHuntLastProgressTick = now;
            debugLogger.state("hunt approach progress " + currentHuntEntity.getUuid() + " " + (int) Math.sqrt(currentDistanceSq),
                    "hunt approach progress: raw=" + describeItem(currentHuntRawFood)
                            + ", bestDistance=" + formatDouble(Math.sqrt(currentHuntBestDistanceSq))
                            + ", " + describeEntity(mod, currentHuntEntity));
        }

        int ticksSinceProgress = now - currentHuntLastProgressTick;
        if (ticksSinceProgress >= HUNT_TARGET_NO_PROGRESS_TIMEOUT_TICKS) {
            return "no hunt approach progress for " + ticksToSeconds(ticksSinceProgress) + "s";
        }

        int ticksSinceStart = now - currentHuntStartTick;
        if (ticksSinceStart >= HUNT_TARGET_HARD_TIMEOUT_TICKS) {
            return "hunt target took longer than " + ticksToSeconds(HUNT_TARGET_HARD_TIMEOUT_TICKS) + "s";
        }

        return null;
    }

    private boolean isActiveFoodPickupTask(Task task) {
        return task != null
                && task.isActive()
                && !task.isFinished()
                && !task.thisOrChildAreTimedOut()
                && task.thisOrChildSatisfies(child -> child instanceof PickupDroppedItemTask);
    }

    private String describeItem(Item item) {
        return item == null ? "null" : item.getTranslationKey();
    }

    private String describeEntity(AltoClef mod, Entity entity) {
        if (entity == null) {
            return "entity=null";
        }
        return "entityPos=" + entity.getBlockPos().toShortString()
                + ", playerPos=" + mod.getPlayer().getBlockPos().toShortString()
                + ", distance=" + formatDouble(entity.distanceTo(mod.getPlayer()))
                + ", alive=" + entity.isAlive();
    }

    private String describeFoodResources(AltoClef mod) {
        return FoodResourceSnapshot.capture(mod, COOKABLE_FOODS).describe();
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private int ticksToSeconds(int ticks) {
        return Math.max(0, ticks / 20);
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
