package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.item.ItemVer;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.container.CraftInTableTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.resources.KillAndLootTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Predicate;

//20260727_kpopmodder: Extracted CollectFoodTask subtask construction to keep collection flow focused.
public final class FoodCollectionTasks {
    private FoodCollectionTasks() {
    }

    public static Task createBreadCraftTask() {
        Item[] w = new Item[]{Items.WHEAT};
        Item[] o = null;
        return new CraftInTableTask(
                new RecipeTarget(Items.BREAD, 99999999, CraftingRecipe.newShapedRecipe("bread", new Item[][]{w, w, w, o, o, o, o, o, o}, 1)),
                false,
                false
        );
    }

    public static Task createWheatFromHayCraftTask() {
        Item[] o = null;
        return new CraftInInventoryTask(
                new RecipeTarget(Items.WHEAT, 99999999, CraftingRecipe.newShapedRecipe("wheat", new Item[][]{new Item[]{Items.HAY_BLOCK}, o, o, o}, 9)),
                false,
                false
        );
    }

    /**
     * Returns a task that mines a block and picks up its output.
     * Returns null if task cannot reasonably run.
     */
    public static Task pickupBlockTaskOrNull(AltoClef mod, Block blockToCheck, Item itemToGrab, Predicate<BlockPos> accept, double maxRange, StateChangeLogger debugLogger) {
        Predicate<BlockPos> acceptPlus = blockPos -> WorldHelper.canBreak(blockPos) && accept.test(blockPos);
        Optional<BlockPos> nearestBlock = mod.getBlockScanner().getNearestBlock(mod.getPlayer().getPos(), acceptPlus, blockToCheck);

        if (nearestBlock.isPresent() && !nearestBlock.get().isWithinDistance(mod.getPlayer().getPos(), maxRange)) {
            nearestBlock = Optional.empty();
        }

        Optional<ItemEntity> nearestDrop = Optional.empty();
        if (mod.getEntityTracker().itemDropped(itemToGrab)) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }

        if (nearestDrop.isPresent()) {
            return pickupTaskOrNull(mod, itemToGrab, debugLogger);
        }
        if (nearestBlock.isPresent()) {
            return new DoToClosestBlockTask(DestroyBlockTask::new, acceptPlus, blockToCheck);
        }

        return null;
    }

    public static Task pickupBlockTaskOrNull(AltoClef mod, Block blockToCheck, Item itemToGrab, double maxRange, StateChangeLogger debugLogger) {
        return pickupBlockTaskOrNull(mod, blockToCheck, itemToGrab, toAccept -> true, maxRange, debugLogger);
    }

    public static Task killAndLootTask(Entity entity, Predicate<Entity> entityPredicate, Item itemToGrab) {
        return new KillAndLootTask(entity.getClass(), entityPredicate, new ItemTarget(itemToGrab, 1));
    }

    /**
     * Returns a task that picks up a dropped item.
     * Returns null if task cannot reasonably run.
     */
    public static Task pickupTaskOrNull(AltoClef mod, Item itemToGrab, double maxRange, StateChangeLogger debugLogger) {
        Optional<ItemEntity> nearestDrop = Optional.empty();
        if (mod.getEntityTracker().itemDropped(itemToGrab)) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }
        if (nearestDrop.isPresent()) {
            if (nearestDrop.get().isInRange(mod.getPlayer(), maxRange)) {
                if (mod.getItemStorage().getSlotsThatCanFitInPlayerInventory(nearestDrop.get().getStack(), false).isEmpty()) {
                    Optional<Slot> slot = StorageHelper.getGarbageSlot(mod);

                    // tf am I supposed to do if its empty
                    if (slot.isPresent()) {
                        ItemStack stack = StorageHelper.getItemStackInSlot(slot.get());
                        if (ItemVer.isFood(stack.getItem())) {
                            // calculate priority, if the item laying on the ground has lower priority than the one we are gonna throw out because of it
                            // dont pick it up, otherwise we would get stuck in an infinite loop
                            int inventoryCost = ItemVer.getFoodComponent(stack.getItem()).getHunger() * stack.getCount();

                            double hunger = 0;
                            if (ItemVer.isFood(itemToGrab)) {
                                hunger = ItemVer.getFoodComponent(itemToGrab).getHunger();
                            } else if (itemToGrab.equals(Items.WHEAT)) {
                                hunger += ItemVer.getFoodComponent(Items.BREAD).getHunger() / 3d;
                            } else {
                                mod.log("unknown food item: " + itemToGrab);
                            }
                            int groundCost = (int) (hunger * nearestDrop.get().getStack().getCount());

                            if (inventoryCost > groundCost) {
                                debugLogger.state("skip pickup: inventory food is more valuable than ground item; item="
                                        + itemToGrab.getTranslationKey()
                                        + ", inventoryCost=" + inventoryCost
                                        + ", groundCost=" + groundCost);
                                return null;
                            }
                        }
                    }
                }
                return new PickupDroppedItemTask(new ItemTarget(itemToGrab), true);
            }
        }
        return null;
    }

    public static Task pickupTaskOrNull(AltoClef mod, Item itemToGrab, StateChangeLogger debugLogger) {
        return pickupTaskOrNull(mod, itemToGrab, Double.POSITIVE_INFINITY, debugLogger);
    }
}
