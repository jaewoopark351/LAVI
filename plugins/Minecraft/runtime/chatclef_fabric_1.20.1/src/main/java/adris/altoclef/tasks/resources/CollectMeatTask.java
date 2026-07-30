package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.multiversion.item.ItemVer;
import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.SmokerSlot;
import adris.altoclef.util.time.TimerGame;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmokerScreenHandler;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class CollectMeatTask extends Task {
    public static final CookableFoodTarget[] COOKABLE_FOODS = new CookableFoodTarget[]{
            new CookableFoodTarget("beef", CowEntity.class),
            new CookableFoodTarget("porkchop", PigEntity.class),
            new CookableFoodTarget("chicken", ChickenEntity.class),
            new CookableFoodTarget("mutton", SheepEntity.class),
            new CookableFoodTarget("rabbit", RabbitEntity.class)
    };
    private final double unitsNeeded;
    private final TimerGame checkNewOptionsTimer = new TimerGame(10);
    private SmeltInSmokerTask smeltTask = null;
    private Task currentResourceTask = null;

    public CollectMeatTask(double unitsNeeded) {
        this.unitsNeeded = unitsNeeded;
    }

    private static double getFoodPotential(ItemStack food) {
        if (food == null) return 0;
        int count = food.getCount();
        if (count <= 0) return 0;
        for (CookableFoodTarget cookable : COOKABLE_FOODS) {
            if (food.getItem() == cookable.getRaw()) {
                assert ItemVer.getFoodComponent(cookable.getCooked()) != null;
                return count * ItemVer.getFoodComponent(cookable.getCooked()).getHunger();
            }
        }
        return 0;
    }

    private static double calculateFoodPotential(AltoClef mod) {
        double potentialFood = 0;
        for (ItemStack food : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
            potentialFood += getFoodPotential(food);
        }
        // Check smelting
        ScreenHandler screen = mod.getPlayer().currentScreenHandler;
        if (screen instanceof SmokerScreenHandler) {
            potentialFood += getFoodPotential(StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_MATERIALS));
            potentialFood += getFoodPotential(StorageHelper.getItemStackInSlot(SmokerSlot.OUTPUT_SLOT));
        }
        return potentialFood;
    }

    @Override
    protected void onStart() {
        //20260730_kpopmodder: Diagnostics-only LAVI log for entity resource loop investigation; no behavior change.
        ChatClefDiagnostics.logEvent("RESOURCE", "ON_START", "collect_meat_start", this,
                "unitsNeeded", unitsNeeded);
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        ChatClefDiagnostics.logEvent("RESOURCE", "ON_TICK", "collect_meat_tick_begin", this,
                "unitsNeeded", unitsNeeded,
                "playerPosition", ChatClefDiagnostics.playerPosition(mod),
                "smeltTask", ChatClefDiagnostics.taskSummary(smeltTask),
                "currentResourceTask", ChatClefDiagnostics.taskSummary(currentResourceTask),
                "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()));

        CollectFoodTask.blackListChickenJockeys(mod);
        // If we were previously smelting, keep on smelting.
        if (smeltTask != null && smeltTask.isActive() && !smeltTask.isFinished()) {
            ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "resume_smelt_task", this,
                    "smeltTask", ChatClefDiagnostics.taskSummary(smeltTask));
            setDebugState("Cooking...");
            return smeltTask;
        } else {
            smeltTask = null;
        }
        if (checkNewOptionsTimer.elapsed()) {
            // Try a new resource task
            ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "refresh_resource_options", this,
                    "previousResourceTask", ChatClefDiagnostics.taskSummary(currentResourceTask));
            checkNewOptionsTimer.reset();
            currentResourceTask = null;
        }
        if (currentResourceTask != null && currentResourceTask.isActive() && !currentResourceTask.isFinished() && !currentResourceTask.thisOrChildAreTimedOut()) {
            ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "reuse_current_resource_task", this,
                    "currentResourceTask", ChatClefDiagnostics.taskSummary(currentResourceTask));
            return currentResourceTask;
        }
        // Calculate potential
        double potentialFood = calculateFoodPotential(mod);
        ChatClefDiagnostics.logEvent("RESOURCE", "OBSERVE", "calculated_food_potential", this,
                "potentialFood", potentialFood,
                "unitsNeeded", unitsNeeded);
        if (potentialFood >= unitsNeeded) {
            // Convert our raw foods
            // PLAN:
            // - If we have raw foods, smelt all of them
            // Convert raw foods -> cooked foods
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                int rawCount = mod.getItemStorage().getItemCount(cookable.getRaw());
                if (rawCount > 0) {
                    //Debug.logMessage("STARTING COOK OF " + cookable.getRaw().getTranslationKey());
                    int toSmelt = rawCount + mod.getItemStorage().getItemCount(cookable.getCooked());
                    ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "start_smelt_raw_food", this,
                            "rawFood", cookable.rawFood,
                            "cookedFood", cookable.cookedFood,
                            "rawCount", rawCount,
                            "toSmelt", toSmelt);
                    smeltTask = new SmeltInSmokerTask(new SmeltTarget(new ItemTarget(cookable.cookedFood, toSmelt), new ItemTarget(cookable.rawFood, rawCount)));
                    smeltTask.ignoreMaterials();
                    return smeltTask;
                }
            }
        } else {
            // Pick up raw/cooked foods on ground
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                Task t = this.pickupTaskOrNull(mod, cookable.getRaw(), 20);
                if (t == null) t = this.pickupTaskOrNull(mod, cookable.getCooked(), 40);
                if (t != null) {
                    ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "pickup_cookable_food", this,
                            "rawFood", cookable.rawFood,
                            "cookedFood", cookable.cookedFood,
                            "pickupTask", ChatClefDiagnostics.taskSummary(t));
                    setDebugState("Picking up Cookable food");
                    currentResourceTask = t;
                    return currentResourceTask;
                }
            }
            // Cooked foods
            double bestScore = 0;
            Entity bestEntity = null;
            Item bestRawFood = null;
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                boolean entityFound = mod.getEntityTracker().entityFound(cookable.mobToKill);
                ChatClefDiagnostics.logEvent("RESOURCE", "OBSERVE", "cookable_mob_found_check", this,
                        "rawFood", cookable.rawFood,
                        "cookedFood", cookable.cookedFood,
                        "mobClass", ChatClefDiagnostics.classList(new Class<?>[]{cookable.mobToKill}),
                        "entityFound", entityFound);
                if (!entityFound) continue;
                Optional<Entity> nearest = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), cookable.mobToKill);
                ChatClefDiagnostics.logEvent("RESOURCE", "OBSERVE", "cookable_mob_nearest_check", this,
                        "rawFood", cookable.rawFood,
                        "nearestEntityPresent", nearest.isPresent(),
                        "nearestEntity", nearest.map(ChatClefDiagnostics::entitySummary).orElse("none"),
                        "nearestDistanceSqr", nearest.map(entity -> ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, entity)).orElse("unavailable"));
                if (nearest.isEmpty()) continue; // ?? This crashed once?
                int hungerPerformance = cookable.getCookedUnits();
                double sqDistance = nearest.get().squaredDistanceTo(mod.getPlayer());
                double score = (double) 100 * hungerPerformance / (sqDistance);
                ChatClefDiagnostics.logEvent("RESOURCE", "OBSERVE", "cookable_mob_candidate_score", this,
                        "rawFood", cookable.rawFood,
                        "candidateEntity", ChatClefDiagnostics.entitySummary(nearest.get()),
                        "hungerPerformance", hungerPerformance,
                        "squaredDistance", sqDistance,
                        "score", score,
                        "previousBestScore", bestScore);
                if (score > bestScore) {
                    bestScore = score;
                    bestEntity = nearest.get();
                    bestRawFood = cookable.getRaw();
                }
            }
            if (bestEntity != null) {
                String bestRawFoodName = bestRawFood == null ? "none" : bestRawFood.getTranslationKey();
                ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "kill_best_food_mob", this,
                        "bestEntity", ChatClefDiagnostics.entitySummary(bestEntity),
                        "bestRawFood", bestRawFoodName,
                        "bestScore", bestScore);
                setDebugState("Killing " + bestEntity.getType().getTranslationKey());
                Predicate<Entity> notBaby = entity -> entity instanceof LivingEntity livingEntity && !livingEntity.isBaby();
                currentResourceTask = killTaskOrNull(bestEntity, notBaby, bestRawFood);
                return currentResourceTask;
            }
        }
        for (Item raw : ItemHelper.RAW_FOODS) {
            if (mod.getItemStorage().hasItem(raw)) {
                Optional<Item> cooked = ItemHelper.getCookedFood(raw);
                if (cooked.isPresent()) {
                    int targetCount = mod.getItemStorage().getItemCount(cooked.get()) + mod.getItemStorage().getItemCount(raw);
                    ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "smelt_existing_raw_food", this,
                            "rawFood", ChatClefDiagnostics.safeValue(raw::getTranslationKey),
                            "cookedFood", ChatClefDiagnostics.safeValue(() -> cooked.get().getTranslationKey()),
                            "targetCount", targetCount);
                    smeltTask = new SmeltInSmokerTask(new SmeltTarget(new ItemTarget(cooked.get(), targetCount), new ItemTarget(raw, targetCount)));
                    return smeltTask;
                }
            }
        }
        // Look for food.
        ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "search_food_with_wander", this,
                "potentialFood", potentialFood,
                "unitsNeeded", unitsNeeded);
        setDebugState("Searching...");
        return new TimeoutWanderTask();
    }

    private Task killTaskOrNull(Entity entity, Predicate<Entity> entityPredicate, Item itemToGrab) {
        return new KillAndLootTask(entity.getClass(), entityPredicate, new ItemTarget(itemToGrab, 1));
    }

    private Task pickupTaskOrNull(AltoClef mod, Item itemToGrab, double maxRange) {
        Optional<ItemEntity> nearestDrop = Optional.empty();
        boolean itemDropped = mod.getEntityTracker().itemDropped(itemToGrab);
        ChatClefDiagnostics.logEvent("RESOURCE", "OBSERVE", "pickup_drop_presence_check", this,
                "item", ChatClefDiagnostics.safeValue(itemToGrab::getTranslationKey),
                "maxRange", maxRange,
                "itemDropped", itemDropped);
        if (itemDropped) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }
        if (nearestDrop.isPresent()) {
            boolean inRange = nearestDrop.get().isInRange(mod.getPlayer(), maxRange);
            ChatClefDiagnostics.logEvent("RESOURCE", "OBSERVE", "pickup_drop_candidate", this,
                    "item", ChatClefDiagnostics.safeValue(itemToGrab::getTranslationKey),
                    "dropEntity", ChatClefDiagnostics.entitySummary(nearestDrop.get()),
                    "dropDistanceSqr", ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, nearestDrop.get()),
                    "maxRange", maxRange,
                    "inRange", inRange);
            if (inRange) {
                ChatClefDiagnostics.logEvent("RESOURCE", "DECISION", "create_pickup_drop_task", this,
                        "item", ChatClefDiagnostics.safeValue(itemToGrab::getTranslationKey),
                        "dropEntity", ChatClefDiagnostics.entitySummary(nearestDrop.get()));
                return new PickupDroppedItemTask(new ItemTarget(itemToGrab), true);
            }
            //return new GetToBlockTask(nearestDrop.getBlockPos(), false);
        }
        return null;
    }

    private Task pickupTaskOrNull(AltoClef mod, Item itemToGrab) {
        return pickupTaskOrNull(mod, itemToGrab, Double.POSITIVE_INFINITY);
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logEvent("RESOURCE", "ON_STOP", "collect_meat_stop", this,
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask),
                "smeltTask", ChatClefDiagnostics.taskSummary(smeltTask),
                "currentResourceTask", ChatClefDiagnostics.taskSummary(currentResourceTask));
    }

    @Override
    public boolean isFinished() {
        return StorageHelper.calculateInventoryFoodScore() >= unitsNeeded && smeltTask == null;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CollectMeatTask task) {
            return task.unitsNeeded == unitsNeeded;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Collect " + unitsNeeded + " units of meat.";
    }

    public static class CookableFoodTarget {
        public String rawFood;
        public String cookedFood;
        public Class<?> mobToKill;

        public CookableFoodTarget(String rawFood, String cookedFood, Class<?> mobToKill) {
            this.rawFood = rawFood;
            this.cookedFood = cookedFood;
            this.mobToKill = mobToKill;
        }

        public CookableFoodTarget(String rawFood, Class<?> mobToKill) {
            this(rawFood, "cooked_" + rawFood, mobToKill);
        }

        public Item getRaw() {
            return Objects.requireNonNull(TaskCatalogue.getItemMatches(rawFood))[0];
        }

        public Item getCooked() {
            return Objects.requireNonNull(TaskCatalogue.getItemMatches(cookedFood))[0];
        }

        public int getCookedUnits() {
            assert ItemVer.getFoodComponent(getCooked()) != null;
            return ItemVer.getFoodComponent(getCooked()).getHunger();
        }
    }
}
