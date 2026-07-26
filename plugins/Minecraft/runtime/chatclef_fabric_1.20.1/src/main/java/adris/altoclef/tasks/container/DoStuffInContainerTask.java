package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Optional;


/**
 * Interacts with a container, obtaining and placing one if none were found nearby.
 */
public abstract class DoStuffInContainerTask extends Task {

    private final ItemTarget containerTarget;
    private final Block[] containerBlocks;

    private final PlaceBlockNearbyTask placeTask;
    // If we decided on placing, force place for at least 1 second
    // (originally 10)
    private final TimerGame placeForceTimer = new TimerGame(1);

    // If we just placed something, stop placing and try going to the nearest container.
    private final TimerGame justPlacedTimer = new TimerGame(3);
    private BlockPos cachedContainerPosition = null;
    private Task openTableTask;
    private final StateChangeLogger debugLogger = new StateChangeLogger("DoStuffInContainerTask");

    public DoStuffInContainerTask(Block[] containerBlocks, ItemTarget containerTarget) {
        this.containerBlocks = containerBlocks;
        this.containerTarget = containerTarget;

        placeTask = new PlaceBlockNearbyTask(this.containerBlocks);
    }

    public DoStuffInContainerTask(Block containerBlock, ItemTarget containerTarget) {
        this(new Block[]{containerBlock}, containerTarget);
    }

    @Override
    protected void onStart() {
        AltoClef mod = AltoClef.getInstance();
        mod.getBehaviour().push();
        if (openTableTask == null) {
            openTableTask = new DoToClosestBlockTask(InteractWithBlockTask::new, containerBlocks);
        }

        // Protect container since we might place it.
        mod.getBehaviour().addProtectedItems(ItemHelper.blocksToItems(containerBlocks));
        debugLogger.event("start: containerTarget=" + containerTarget);
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        // If we're placing, keep on placing.
        if (mod.getItemStorage().hasItem(ItemHelper.blocksToItems(containerBlocks)) && placeTask.isActive() && !placeTask.isFinished()) {
            setDebugState("Placing container");
            debugLogger.state("continue placing container: placed=" + describePos(placeTask.getPlaced()));
            return placeTask;
        }

        if (isContainerOpen(mod)) {
            debugLogger.state("container open: targetPosition=" + describePos(cachedContainerPosition));
            return containerSubTask(mod);
        }

        // infinity if such a container does not exist.
        double costToWalk = Double.POSITIVE_INFINITY;

        Optional<BlockPos> nearest;

        Vec3d currentPos = mod.getPlayer().getPos();
        BlockPos override = overrideContainerPosition(mod);

        if (override != null && mod.getBlockScanner().isBlockAtPosition(override, containerBlocks)) {
            // We have an override so go there instead.
            nearest = Optional.of(override);
        } else {
            // Track nearest container
            nearest = mod.getBlockScanner().getNearestBlock(currentPos, blockPos -> WorldHelper.canReach(blockPos), containerBlocks);
        }
        if (nearest.isEmpty()) {
            // If all else fails, try using our placed task
            nearest = Optional.ofNullable(placeTask.getPlaced());
            if (nearest.isPresent() && !mod.getBlockScanner().isBlockAtPosition(nearest.get(), containerBlocks)) {
                nearest = Optional.empty();
            }
        }
        if (nearest.isPresent()) {
            costToWalk = BaritoneHelper.calculateGenericHeuristic(currentPos, WorldHelper.toVec3d(nearest.get()));
        }

        // Make a new container if going to the container is a pretty bad cost.
        // Also keep on making the container if we're stuck in some
        if (costToWalk > getCostToMakeNew(mod)) {
            placeForceTimer.reset();
        }
        if (nearest.isEmpty() || (!placeForceTimer.elapsed() && justPlacedTimer.elapsed())) {
            // It's cheaper to make a new one, or our only option.

            // We're no longer going to our previous container.
            cachedContainerPosition = null;

            // Get if we don't have...
            if (!mod.getItemStorage().hasItem(containerTarget)) {
                setDebugState("Getting container item");
                debugLogger.state("get container item: target=" + containerTarget
                        + ", nearest=" + describeOptionalPos(nearest)
                        + ", walkCost=" + formatDouble(costToWalk)
                        + ", makeCost=" + formatDouble(getCostToMakeNew(mod)));
                return TaskCatalogue.getItemTask(containerTarget);
            }

            setDebugState("Placing container...");
            debugLogger.state("place new container: target=" + containerTarget
                    + ", nearest=" + describeOptionalPos(nearest)
                    + ", walkCost=" + formatDouble(costToWalk)
                    + ", makeCost=" + formatDouble(getCostToMakeNew(mod))
                    + ", previousPlaced=" + describePos(placeTask.getPlaced()));

            justPlacedTimer.reset();
            // Now place!
            return placeTask;
        }

        // This is insanely cursed.
        // TODO: Finish committing to optionals, this is ugly.
        cachedContainerPosition = nearest.get();

        // Walk to it and open it

        // Wait for food
        if (mod.getFoodChain().needsToEat()) {
            setDebugState("Waiting for eating...");
            debugLogger.state("wait for eating before opening container: targetPosition=" + describePos(cachedContainerPosition));
            return null;
        }
        setDebugState("Walking to container... " + nearest.get().toShortString());
        debugLogger.state("walk/open container: targetPosition=" + nearest.get().toShortString()
                + ", walkCost=" + formatDouble(costToWalk)
                + ", makeCost=" + formatDouble(getCostToMakeNew(mod)));

        if (!StorageHelper.getItemStackInCursorSlot().isEmpty()) {
            debugLogger.state("clear cursor before opening container: cursor=" + describeStack(StorageHelper.getItemStackInCursorSlot()));
            Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(StorageHelper.getItemStackInCursorSlot(), false);
            if (toMoveTo.isEmpty()) {
                return new EnsureFreeInventorySlotTask();
            }
            if (ItemHelper.canThrowAwayStack(mod, StorageHelper.getItemStackInCursorSlot())) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            mod.getSlotHandler().clickSlot(toMoveTo.get(), 0, SlotActionType.PICKUP);
            return null;
        }
        return openTableTask;
        //return new GetToBlockTask(nearest, true);
    }

    public ItemTarget getContainerTarget() {
        return containerTarget;
    }

    // Virtual
    protected BlockPos overrideContainerPosition(AltoClef mod) {
        return null;
    }

    protected BlockPos getTargetContainerPosition() {
        return cachedContainerPosition;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.getInstance().getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DoStuffInContainerTask task) {
            if (!Arrays.equals(task.containerBlocks, containerBlocks)) return false;
            if (!task.containerTarget.equals(containerTarget)) return false;
            return isSubTaskEqual(task);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing stuff in " + containerTarget + " container";
    }

    protected abstract boolean isSubTaskEqual(DoStuffInContainerTask other);

    protected abstract boolean isContainerOpen(AltoClef mod);

    protected abstract Task containerSubTask(AltoClef mod);

    protected abstract double getCostToMakeNew(AltoClef mod);

    private String describeOptionalPos(Optional<BlockPos> pos) {
        return pos.map(BlockPos::toShortString).orElse("none");
    }

    private String describePos(BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }

    private String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem().getTranslationKey() + " x " + stack.getCount();
    }

    private String formatDouble(double value) {
        if (Double.isInfinite(value)) {
            return "infinity";
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
