package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.block.BeetrootsBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarrotsBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.PotatoesBlock;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.Optional;

//20260727_kpopmodder: Extracted immediate food collection task selection from CollectFoodTask.
public final class FoodCollectionTaskSelector {
    private FoodCollectionTaskSelector() {
    }

    public static Optional<Selection> select(
            AltoClef mod,
            FoodCollectionTargets.CookableFoodTarget[] cookableFoods,
            Item[] itemsToPickUp,
            FoodCollectionTargets.CropTarget[] crops,
            StateChangeLogger debugLogger,
            String foodResources
    ) {
        for (Item item : itemsToPickUp) {
            Task task = FoodCollectionTasks.pickupTaskOrNull(mod, item, debugLogger);
            if (task != null) {
                return Optional.of(Selection.resource(
                        task,
                        "Picking up Food: " + item.getTranslationKey(),
                        "pickup ready food: item=" + item.getTranslationKey(),
                        "pickup ready food: item=" + item.getTranslationKey() + ", " + foodResources
                ));
            }
        }

        for (FoodCollectionTargets.CookableFoodTarget cookable : cookableFoods) {
            Task task = FoodCollectionTasks.pickupTaskOrNull(mod, cookable.getRaw(), 20, debugLogger);
            if (task == null) {
                task = FoodCollectionTasks.pickupTaskOrNull(mod, cookable.getCooked(), 40, debugLogger);
            }
            if (task != null) {
                return Optional.of(Selection.resource(
                        task,
                        "Picking up Cookable food",
                        "pickup cookable food: raw=" + cookable.getRaw().getTranslationKey(),
                        "pickup cookable food: raw=" + cookable.getRaw().getTranslationKey()
                                + ", cooked=" + cookable.getCooked().getTranslationKey()
                                + ", " + foodResources
                ));
            }
        }

        Task hayTask = FoodCollectionTasks.pickupBlockTaskOrNull(mod, Blocks.HAY_BLOCK, Items.HAY_BLOCK, 300, debugLogger);
        if (hayTask != null) {
            return Optional.of(Selection.resource(
                    hayTask,
                    "Collecting Hay",
                    "collect hay",
                    "collect hay: " + foodResources
            ));
        }

        for (FoodCollectionTargets.CropTarget crop : crops) {
            Task task = FoodCollectionTasks.pickupBlockTaskOrNull(mod, crop.cropBlock, crop.cropItem,
                    blockPos -> canHarvestCrop(mod, blockPos), 96, debugLogger);
            if (task != null) {
                return Optional.of(Selection.resource(
                        task,
                        "Harvesting " + crop.cropItem.getTranslationKey(),
                        "harvest crop: item=" + crop.cropItem.getTranslationKey(),
                        "harvest crop: item=" + crop.cropItem.getTranslationKey() + ", " + foodResources
                ));
            }
        }

        Optional<FoodMobHuntSelector.HuntTarget> huntTarget = FoodMobHuntSelector.selectBest(mod, cookableFoods);
        if (huntTarget.isPresent()) {
            Entity entity = huntTarget.get().getEntity();
            Item rawFood = huntTarget.get().getRawFood();
            return Optional.of(Selection.hunt(
                    FoodCollectionTasks.killAndLootTask(entity, huntTarget.get().getEntityPredicate(), rawFood),
                    entity,
                    rawFood,
                    "Killing " + entity.getType().getTranslationKey() + " ??? " + entity.isAlive(),
                    "hunt mob: entity=" + entity.getType().getTranslationKey(),
                    "hunt mob: entity=" + entity.getType().getTranslationKey()
                            + ", raw=" + rawFood.getTranslationKey()
                            + ", score=" + formatDouble(huntTarget.get().getScore())
                            + ", radius=" + huntTarget.get().getSearchRadius()
                            + ", distance=" + formatDouble(huntTarget.get().getDistance())
                            + ", foodUnits=" + huntTarget.get().getCookedUnits()
                            + ", " + describeEntity(mod, entity)
                            + ", " + foodResources
            ));
        }

        Task berryTask = FoodCollectionTasks.pickupBlockTaskOrNull(mod, Blocks.SWEET_BERRY_BUSH, Items.SWEET_BERRIES, 96, debugLogger);
        if (berryTask != null) {
            return Optional.of(Selection.resource(
                    berryTask,
                    "Getting sweet berries (no better foods are present)",
                    "collect sweet berries",
                    "collect sweet berries: " + foodResources
            ));
        }

        return Optional.empty();
    }

    private static boolean canHarvestCrop(AltoClef mod, BlockPos blockPos) {
        BlockState state = mod.getWorld().getBlockState(blockPos);
        Block block = state.getBlock();
        if (block instanceof CropBlock) {
            boolean isWheat = !(block instanceof PotatoesBlock || block instanceof CarrotsBlock || block instanceof BeetrootsBlock);
            if (isWheat) {
                if (!mod.getChunkTracker().isChunkLoaded(blockPos)) {
                    return false;
                }
                CropBlock crop = (CropBlock) block;
                return crop.isMature(state);
            }
        }
        return WorldHelper.canBreak(blockPos);
    }

    private static String describeEntity(AltoClef mod, Entity entity) {
        return "entityPos=" + entity.getBlockPos().toShortString()
                + ", playerPos=" + mod.getPlayer().getBlockPos().toShortString()
                + ", distance=" + formatDouble(entity.distanceTo(mod.getPlayer()))
                + ", alive=" + entity.isAlive();
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static final class Selection {
        private final Task task;
        private final Entity huntEntity;
        private final Item huntRawFood;
        private final String debugState;
        private final String stateKey;
        private final String detail;

        private Selection(Task task, Entity huntEntity, Item huntRawFood, String debugState, String stateKey, String detail) {
            this.task = task;
            this.huntEntity = huntEntity;
            this.huntRawFood = huntRawFood;
            this.debugState = debugState;
            this.stateKey = stateKey;
            this.detail = detail;
        }

        private static Selection resource(Task task, String debugState, String stateKey, String detail) {
            return new Selection(task, null, null, debugState, stateKey, detail);
        }

        private static Selection hunt(Task task, Entity entity, Item rawFood, String debugState, String stateKey, String detail) {
            return new Selection(task, entity, rawFood, debugState, stateKey, detail);
        }

        public Task getTask() {
            return task;
        }

        public boolean isHunt() {
            return huntEntity != null;
        }

        public Entity getHuntEntity() {
            return huntEntity;
        }

        public Item getHuntRawFood() {
            return huntRawFood;
        }

        public String getDebugState() {
            return debugState;
        }

        public String getStateKey() {
            return stateKey;
        }

        public String getDetail() {
            return detail;
        }
    }
}
