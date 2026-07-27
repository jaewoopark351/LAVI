package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.BotBehaviour;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.container.PickupFromContainerTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasks.slot.EnsureFreePlayerCraftingGridTask;
import adris.altoclef.tasks.slot.MoveInaccessibleItemToInventoryTask;
import adris.altoclef.tasksystem.ITaskCanForce;
import adris.altoclef.tasksystem.ITaskUsesCraftingGrid;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The parent for all "collect an item" tasks.
 * <p>
 * If the target item is on the ground or in a chest, will grab from those sources first.
 */
public abstract class ResourceTask extends Task implements ITaskCanForce {

    private static final int DROPPED_ITEM_PICKUP_CONTINUATION_TICKS = 20 * 4;

    protected final ItemTarget[] itemTargets;

    private final PickupDroppedItemTask pickupTask;
    private final DroppedItemPickupContinuation pickupContinuation = new DroppedItemPickupContinuation(DROPPED_ITEM_PICKUP_CONTINUATION_TICKS);
    private final EnsureFreePlayerCraftingGridTask ensureFreeCraftingGridTask = new EnsureFreePlayerCraftingGridTask();
    private ContainerCache currentContainer;
    // Extra resource parameters
    private Block[] mineIfPresent = null;
    private boolean forceDimension = false;
    private boolean allowContainers = false;
    private Dimension targetDimension;
    private BlockPos mineLastClosest = null;
    private final StateChangeLogger resourceLogger = new StateChangeLogger("ResourceTask." + getClass().getSimpleName());
    //20260728_kpopmodder: Summarize pickup continuation behavior so task-transition changes are visible in latest.log.
    private int pickupVisibleReturnCount = 0;
    private int pickupStartReturnCount = 0;
    private int pickupContinuationReturnCount = 0;
    private int pickupPickaxeFirstReturnCount = 0;

    public ResourceTask(ItemTarget[] itemTargets) {
        this.itemTargets = itemTargets;
        pickupTask = new PickupDroppedItemTask(this.itemTargets, true);
    }

    public ResourceTask(ItemTarget target) {
        this(new ItemTarget[]{target});
    }

    public ResourceTask(Item item, int targetCount) {
        this(new ItemTarget(item, targetCount));
    }

    @Override
    public boolean isFinished() {
        return StorageHelper.itemTargetsMetInventoryNoCursor(itemTargets);
    }

    @Override
    public boolean shouldForce(Task interruptingCandidate) {
        // We have an important item target in our cursor.
        return StorageHelper.itemTargetsMetInventory(itemTargets) && !isFinished()
                // This _should_ be redundant, but it'll be a guard just to make 100% sure.
                && Arrays.stream(itemTargets).anyMatch(target -> target.matches(StorageHelper.getItemStackInCursorSlot().getItem()));
    }

    @Override
    protected void onStart() {
        BotBehaviour botBehaviour = AltoClef.getInstance().getBehaviour();

        botBehaviour.push();
        //removeThrowawayItems(_itemTargets);
        botBehaviour.addProtectedItems(ItemTarget.getMatches(itemTargets));

        pickupContinuation.reset();
        resetPickupDiagnostics();
        onResourceStart(AltoClef.getInstance());
        resourceLogger.event("start: targets=" + describeTargets());
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        // If we have an item in an INACCESSIBLE inventory slot
        if (!(thisOrChildSatisfies(task -> task instanceof ITaskUsesCraftingGrid)) || ensureFreeCraftingGridTask.isActive()) {
            for (ItemTarget target : itemTargets) {
                if (StorageHelper.isItemInaccessibleToContainer(mod, target)) {
                    setDebugState("Moving from SPECIAL inventory slot");
                    resourceLogger.state("moving inaccessible item: target=" + target);
                    return new MoveInaccessibleItemToInventoryTask(target);
                }
            }
        }
        // We have enough items COUNTING the cursor slot, we just need to move an item from our cursor.
        if (StorageHelper.itemTargetsMetInventory(itemTargets) && Arrays.stream(itemTargets).anyMatch(target -> target.matches(StorageHelper.getItemStackInCursorSlot().getItem()))) {
            setDebugState("Moving from cursor");
            resourceLogger.state("moving target item from cursor: cursor=" + StorageHelper.getItemStackInCursorSlot()
                    + ", targets=" + describeTargets());
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(StorageHelper.getItemStackInCursorSlot(), false);
            if (moveTo.isPresent()) {
                mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            if (ItemHelper.canThrowAwayStack(mod, StorageHelper.getItemStackInCursorSlot())) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            if (garbage.isPresent()) {
                mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return null;
        }

        if (!shouldAvoidPickingUp(mod)) {
            // Check if items are on the floor. If so, pick em up.
            boolean pickupActive = pickupTask.isActive() && !pickupTask.isFinished();
            boolean droppedItemVisible = mod.getEntityTracker().itemDropped(itemTargets);
            Optional<ItemEntity> closest = droppedItemVisible || pickupActive
                    ? mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemTargets)
                    : Optional.empty();
            if (droppedItemVisible) {

                // If we're picking up a pickaxe (we can't go far underground or mine much)
                if (PickupDroppedItemTask.isIsGettingPickaxeFirst(mod)) {
                    if (pickupTask.isCollectingPickaxeForThis()) {
                        setDebugState("Picking up (pickaxe first!)");
                        pickupPickaxeFirstReturnCount++;
                        resourceLogger.state("pickup delayed by pickaxe-first flow; continuing pickaxe pickup: targets=" + describeTargets());
                        // Our pickup task is the one collecting the pickaxe, keep it going.
                        return pickupTask;
                    }
                    // Only get items that are CLOSE to us.
                    if (closest.isPresent() && !closest.get().isInRange(mod.getPlayer(), 10)) {
                        resourceLogger.state("pickup skipped during pickaxe-first flow: closest=" + describeDrop(closest.get())
                                + ", targets=" + describeTargets());
                        return onResourceTick(mod);
                    }
                }
            }

            double range = getPickupRange(mod);
            if (droppedItemVisible && (range < 0 || (closest.isPresent() && closest.get().isInRange(mod.getPlayer(), range)) || pickupActive)) {
                setDebugState("Picking up");
                pickupVisibleReturnCount++;
                if (!pickupActive) {
                    pickupStartReturnCount++;
                }
                pickupContinuation.arm();
                resourceLogger.state("pickup dropped item: closest=" + closest.map(this::describeDrop).orElse("unknown")
                        + ", range=" + range
                        + ", targets=" + describeTargets());
                return pickupTask;
            }
            if (pickupContinuation.shouldContinue(pickupActive)) {
                setDebugState("Picking up");
                pickupContinuationReturnCount++;
                resourceLogger.state("pickup continuation grace",
                        "pickup continuation grace: visible=" + droppedItemVisible
                                + ", closest=" + closest.map(this::describeDrop).orElse("none")
                                + ", ticksRemaining=" + pickupContinuation.ticksRemaining());
                return pickupTask;
            }
            pickupContinuation.clearIfIdle(pickupActive);
        }

        // Check for chests and grab resources from them.
        if (currentContainer == null && allowContainers) {
            List<ContainerCache> containersWithItem = mod.getItemStorage().getContainersWithItem(Arrays.stream(itemTargets).reduce(new Item[0], (items, target) -> ArrayUtils.addAll(items, target.getMatches()), ArrayUtils::addAll));
            if (!containersWithItem.isEmpty()) {
                ContainerCache closest = containersWithItem.stream().min(StlHelper.compareValues(container -> BlockPosVer.getSquaredDistance(container.getBlockPos(),mod.getPlayer().getPos()))).get();
                if (closest.getBlockPos().isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceChestLocateRange())) {
                    currentContainer = closest;
                    resourceLogger.state("found container with target: pos=" + currentContainer.getBlockPos().toShortString()
                            + ", targets=" + describeTargets());
                }
            }
        }
        if (currentContainer != null) {
            Optional<ContainerCache> container = mod.getItemStorage().getContainerAtPosition(currentContainer.getBlockPos());
            if (container.isPresent()) {
                if (Arrays.stream(itemTargets).noneMatch(target -> container.get().hasItem(target.getMatches()))) {
                    resourceLogger.state("container no longer has target: pos=" + currentContainer.getBlockPos().toShortString()
                            + ", targets=" + describeTargets());
                    currentContainer = null;
                } else {
                    // We have a current chest, grab from it.
                    setDebugState("Picking up from container");
                    resourceLogger.state("pickup from container: pos=" + currentContainer.getBlockPos().toShortString()
                            + ", targets=" + describeTargets());
                    return new PickupFromContainerTask(currentContainer.getBlockPos(), itemTargets);
                }
            } else {
                resourceLogger.state("container cache missing at pos=" + currentContainer.getBlockPos().toShortString());
                currentContainer = null;
            }
        }

        // We may just mine if a block is found.
        if (mineIfPresent != null) {
            ArrayList<Block> satisfiedReqs = new ArrayList<>(Arrays.asList(mineIfPresent));
            satisfiedReqs.removeIf(block -> !StorageHelper.miningRequirementMet(MiningRequirement.getMinimumRequirementForBlock(block)));
            if (!satisfiedReqs.isEmpty()) {
                if (mod.getBlockScanner().anyFound(satisfiedReqs.toArray(Block[]::new))) {
                    Optional<BlockPos> closest = mod.getBlockScanner().getNearestBlock(mineIfPresent);
                    if (closest.isPresent() && closest.get().isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceMineRange())) {
                        mineLastClosest = closest.get();
                    }
                    if (mineLastClosest != null) {
                        if (mineLastClosest.isWithinDistance(mod.getPlayer().getPos(), mod.getModSettings().getResourceMineRange() * 1.5 + 20)) {
                            resourceLogger.state("mine nearby resource: closest=" + mineLastClosest.toShortString()
                                    + ", targets=" + describeTargets());
                            return new MineAndCollectTask(itemTargets, mineIfPresent, MiningRequirement.HAND);
                        }
                    }
                }
            }
        }
        // Make sure that items don't get stuck in the player crafting grid. May be an issue if a future task isn't a resource task.
        if (StorageHelper.isPlayerInventoryOpen()) {
            if (!(thisOrChildSatisfies(task -> task instanceof ITaskUsesCraftingGrid)) || ensureFreeCraftingGridTask.isActive()) {
                for (Slot slot : PlayerSlot.CRAFT_INPUT_SLOTS) {
                    if (!StorageHelper.getItemStackInSlot(slot).isEmpty()) {
                        resourceLogger.state("free player crafting grid before resource task: slot=" + slot);
                        return ensureFreeCraftingGridTask;
                    }
                }
            }
        }
        resourceLogger.state("delegate to resource-specific tick: targets=" + describeTargets());
        return onResourceTick(mod);
    }

    protected double getPickupRange(AltoClef mod) {
        return mod.getModSettings().getResourcePickupRange();
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.getInstance().getBehaviour().pop();
        if (hasPickupDiagnostics()) {
            resourceLogger.event("pickup summary: interruptedBy=" + describeTask(interruptTask)
                    + ", visibleReturns=" + pickupVisibleReturnCount
                    + ", pickupStarts=" + pickupStartReturnCount
                    + ", continuationReturns=" + pickupContinuationReturnCount
                    + ", pickaxeFirstReturns=" + pickupPickaxeFirstReturnCount
                    + ", targets=" + describeTargets());
        }
        pickupContinuation.reset();
        onResourceStop(AltoClef.getInstance(), interruptTask);
    }

    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof ResourceTask t) {
            if (!isEqualResource(t)) return false;
            return Arrays.equals(t.itemTargets, itemTargets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append(toDebugStringName()).append(": [");
        int c = 0;
        if (itemTargets != null) {
            for (ItemTarget target : itemTargets) {
                result.append(target != null ? target.toString() : "(null)");
                if (++c != itemTargets.length) {
                    result.append(", ");
                }
            }
        }
        result.append("]");
        return result.toString();
    }

    protected boolean isInWrongDimension(AltoClef mod) {
        if (forceDimension) {
            return WorldHelper.getCurrentDimension() != targetDimension;
        }
        return false;
    }

    protected Task getToCorrectDimensionTask(AltoClef mod) {
        return new DefaultGoToDimensionTask(targetDimension);
    }

    public ResourceTask mineIfPresent(Block[] toMine) {
        mineIfPresent = toMine;
        return this;
    }

    public ResourceTask forceDimension(Dimension dimension) {
        forceDimension = true;
        targetDimension = dimension;
        return this;
    }

    public void setAllowContainers(boolean value) {
        this.allowContainers = value;
    }

    public boolean getAllowContainers() {
        return allowContainers;
    }

    private String describeTargets() {
        return Arrays.toString(itemTargets);
    }

    private String describeDrop(ItemEntity drop) {
        return drop.getStack().getItem().getTranslationKey()
                + " x " + drop.getStack().getCount()
                + " at " + drop.getBlockPos().toShortString();
    }

    private boolean hasPickupDiagnostics() {
        return pickupVisibleReturnCount
                + pickupStartReturnCount
                + pickupContinuationReturnCount
                + pickupPickaxeFirstReturnCount > 0;
    }

    private void resetPickupDiagnostics() {
        pickupVisibleReturnCount = 0;
        pickupStartReturnCount = 0;
        pickupContinuationReturnCount = 0;
        pickupPickaxeFirstReturnCount = 0;
    }

    private String describeTask(Task task) {
        return task == null ? "none" : task.getClass().getSimpleName();
    }

    protected abstract boolean shouldAvoidPickingUp(AltoClef mod);

    protected abstract void onResourceStart(AltoClef mod);

    protected abstract Task onResourceTick(AltoClef mod);

    protected abstract void onResourceStop(AltoClef mod, Task interruptTask);

    protected abstract boolean isEqualResource(ResourceTask other);

    protected abstract String toDebugStringName();

    public ItemTarget[] getItemTargets() {
        return itemTargets;
    }

    //20260727_kpopmodder: Keeps dropped-item pickup decisions from flickering back to mining while entity tracking settles.
    private static class DroppedItemPickupContinuation {
        private final int continuationTicks;
        private int continueUntilTick;

        private DroppedItemPickupContinuation(int continuationTicks) {
            this.continuationTicks = continuationTicks;
        }

        private void arm() {
            continueUntilTick = WorldHelper.getTicks() + continuationTicks;
        }

        private boolean shouldContinue(boolean pickupActive) {
            return pickupActive && WorldHelper.getTicks() <= continueUntilTick;
        }

        private int ticksRemaining() {
            return Math.max(0, continueUntilTick - WorldHelper.getTicks());
        }

        private void clearIfIdle(boolean pickupActive) {
            if (!pickupActive) {
                reset();
            }
        }

        private void reset() {
            continueUntilTick = 0;
        }
    }
}
