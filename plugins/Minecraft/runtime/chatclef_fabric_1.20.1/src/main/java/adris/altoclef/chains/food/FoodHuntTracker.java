package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;

import java.util.Locale;

//20260727_kpopmodder: Extracted food hunt progress tracking from CollectFoodTask.
public class FoodHuntTracker {
    private static final int HUNT_TARGET_NO_PROGRESS_TIMEOUT_TICKS = 20 * 30;
    private static final int HUNT_TARGET_HARD_TIMEOUT_TICKS = 20 * 60;
    private static final int HUNT_LOOT_GRACE_TICKS = 20 * 12;
    private static final double HUNT_DISTANCE_PROGRESS_BLOCKS = 2.0;

    private Entity currentHuntEntity = null;
    private Item currentHuntRawFood = null;
    private int currentHuntStartTick = -1;
    private int currentHuntLastProgressTick = -1;
    private int currentHuntRawFoodCount = 0;
    private double currentHuntBestDistanceSq = Double.POSITIVE_INFINITY;
    private boolean currentHuntTargetDown = false;

    public void start(AltoClef mod, Entity entity, Item rawFood) {
        currentHuntEntity = entity;
        currentHuntRawFood = rawFood;
        currentHuntStartTick = WorldHelper.getTicks();
        currentHuntLastProgressTick = currentHuntStartTick;
        currentHuntRawFoodCount = mod.getItemStorage().getItemCount(rawFood);
        currentHuntBestDistanceSq = entity.squaredDistanceTo(mod.getPlayer());
        currentHuntTargetDown = !entity.isAlive();
    }

    public void clear() {
        currentHuntEntity = null;
        currentHuntRawFood = null;
        currentHuntStartTick = -1;
        currentHuntLastProgressTick = -1;
        currentHuntRawFoodCount = 0;
        currentHuntBestDistanceSq = Double.POSITIVE_INFINITY;
        currentHuntTargetDown = false;
    }

    public boolean hasTarget() {
        return currentHuntEntity != null;
    }

    public Entity getEntity() {
        return currentHuntEntity;
    }

    public Item getRawFood() {
        return currentHuntRawFood;
    }

    public String getContinueStateKey() {
        return "continue hunt mob " + (currentHuntEntity == null ? "none" : currentHuntEntity.getUuid());
    }

    public String describeContinuation(AltoClef mod, Task currentResourceTask) {
        return "continue hunt mob: raw=" + describeItem(currentHuntRawFood)
                + ", task=" + describeTask(currentResourceTask)
                + ", " + describeEntity(mod, currentHuntEntity);
    }

    public void temporarilySkip(AltoClef mod, StateChangeLogger debugLogger, String reason) {
        if (currentHuntEntity == null) {
            return;
        }
        debugLogger.state("skip hunt mob " + currentHuntEntity.getUuid(),
                "skip hunt mob: reason=" + reason
                        + ", raw=" + describeItem(currentHuntRawFood)
                        + ", " + describeEntity(mod, currentHuntEntity));
        FoodCollectionBlacklist.temporarilySkipHuntTarget(mod, currentHuntEntity, reason);
        clear();
    }

    public String getSkipReason(AltoClef mod, StateChangeLogger debugLogger) {
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

    private String describeTask(Task task) {
        return task == null ? "none" : task.getClass().getSimpleName() + "{" + task + "}";
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

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private int ticksToSeconds(int ticks) {
        return Math.max(0, ticks / 20);
    }
}
