package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.catalogue.TaskCatalogue;
import adris.altoclef.tasks.interaction.InteractWithBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.construction.carryon.PlaceCarriedBlockTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.compat.CarryOnCompat;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;


/**
 * Interacts with a container, obtaining and placing one if none were found nearby.
 */
public abstract class DoStuffInContainerTask extends Task {

    private final ItemTarget containerTarget;
    private final Block[] containerBlocks;

    private final PlaceBlockNearbyTask placeTask;
    private PlaceCarriedBlockTask placeCarriedTask;
    // If we decided on placing, force place for at least 1 second
    // (originally 10)
    private final TimerGame placeForceTimer = new TimerGame(1);

    // If we just placed something, stop placing and try going to the nearest container.
    private final TimerGame justPlacedTimer = new TimerGame(3);
    private BlockPos cachedContainerPosition = null;
    //20260729_kpopmodder: Keep container fallback diagnostics visible before changing any crafting-table behavior.
    private final StateChangeLogger debugLogger = new StateChangeLogger("DoStuffInContainerTask");
    private String lastPlanEventKey = "";

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

        // Protect container since we might place it.
        mod.getBehaviour().addProtectedItems(ItemHelper.blocksToItems(containerBlocks));
        placeCarriedTask = null;
        lastPlanEventKey = "";
        debugLogger.event("start: containerTarget=" + containerTarget
                + ", carryOnSafeSneak=" + shouldUseCarryOnSafeInteraction());
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        Task carriedPlacement = getCarryOnCarriedContainerPlacementTask(mod);
        if (carriedPlacement != null) {
            return carriedPlacement;
        }

        // If we're placing, keep on placing.
        if (placeTask.isActive() && !placeTask.isFinished()) {
            if (mod.getItemStorage().hasItem(ItemHelper.blocksToItems(containerBlocks))) {
                setDebugState("Placing container");
                debugLogger.state("continue placing container: placed=" + describePos(placeTask.getPlaced()));
                return placeTask;
            }
            if (shouldUseCarryOnSafeInteraction() && !justPlacedTimer.elapsed()) {
                mod.getInputControls().release(Input.SNEAK);
                setDebugState("Waiting for placed container verification");
                debugLogger.state("wait for carry-on-safe placed container verification: placed=" + describePos(placeTask.getPlaced()));
                return null;
            }
        }

        if (isContainerOpen(mod)) {
            debugLogger.state("container open: targetPosition=" + describePos(cachedContainerPosition),
                    "container open: targetPosition=" + describePos(cachedContainerPosition)
                            + ", " + describeInteractionContext(mod));
            return containerSubTask(mod);
        }

        // infinity if such a container does not exist.
        double costToWalk = Double.POSITIVE_INFINITY;

        Optional<BlockPos> nearest;
        boolean usingPlacedContainer = false;

        Vec3d currentPos = mod.getPlayer().getPos();
        BlockPos override = overrideContainerPosition(mod);
        Optional<BlockPos> placedContainer = getPlacedContainerIfValid(mod);

        if (placedContainer.isPresent()) {
            nearest = placedContainer;
            usingPlacedContainer = true;
            debugLogger.state("prefer placed container:" + nearest.get().toShortString(),
                    "prefer placed container: targetPosition=" + nearest.get().toShortString());
        } else if (override != null && mod.getBlockScanner().isBlockAtPosition(override, containerBlocks)) {
            // We have an override so go there instead.
            nearest = Optional.of(override);
        } else {
            // Track nearest container
            nearest = mod.getBlockScanner().getNearestBlock(currentPos, blockPos -> WorldHelper.canReach(blockPos), containerBlocks);
        }
        if (nearest.isEmpty()) {
            // If all else fails, try using our placed task
            nearest = getPlacedContainerIfValid(mod);
        }
        if (nearest.isPresent()) {
            costToWalk = BaritoneHelper.calculateGenericHeuristic(currentPos, WorldHelper.toVec3d(nearest.get()));
        }
        double makeCost = getCostToMakeNew(mod);
        String actionReason = describeContainerActionReason(nearest, usingPlacedContainer, costToWalk, makeCost);
        logPlanEvent(actionReason + ":" + describeOptionalPos(nearest),
                "container plan transition: target=" + containerTarget
                        + ", action=" + actionReason
                        + ", nearest=" + describeOptionalPos(nearest)
                        + ", usingPlacedContainer=" + usingPlacedContainer
                        + ", override=" + describePos(override)
                        + ", placedTask=" + describePos(placeTask.getPlaced())
                        + ", carriedPlaced=" + describePos(placeCarriedTask == null ? null : placeCarriedTask.getPlaced())
                        + ", walkCost=" + formatDouble(costToWalk)
                        + ", makeCost=" + formatDouble(makeCost)
                        + ", placeForceElapsed=" + placeForceTimer.elapsed()
                        + ", justPlacedElapsed=" + justPlacedTimer.elapsed()
                        + ", hasContainerItem=" + mod.getItemStorage().hasItem(containerTarget)
                        + ", " + describeInteractionContext(mod));
        debugLogger.state("container decision:" + containerTarget
                        + ":nearest=" + describeOptionalPos(nearest)
                        + ":placed=" + usingPlacedContainer
                        + ":walk=" + formatDouble(costToWalk),
                "container decision: target=" + containerTarget
                        + ", nearest=" + describeOptionalPos(nearest)
                        + ", usingPlacedContainer=" + usingPlacedContainer
                        + ", override=" + describePos(override)
                        + ", placedTask=" + describePos(placeTask.getPlaced())
                        + ", carriedPlaced=" + describePos(placeCarriedTask == null ? null : placeCarriedTask.getPlaced())
                        + ", walkCost=" + formatDouble(costToWalk)
                        + ", makeCost=" + formatDouble(makeCost)
                        + ", placeForceElapsed=" + placeForceTimer.elapsed()
                        + ", justPlacedElapsed=" + justPlacedTimer.elapsed()
                        + ", hasContainerItem=" + mod.getItemStorage().hasItem(containerTarget)
                        + ", " + describeInteractionContext(mod));

        // Make a new container if going to the container is a pretty bad cost.
        // Also keep on making the container if we're stuck in some
        if (!usingPlacedContainer && costToWalk > makeCost) {
            placeForceTimer.reset();
        }
        if (nearest.isEmpty() || (!usingPlacedContainer && !placeForceTimer.elapsed() && justPlacedTimer.elapsed())) {
            // It's cheaper to make a new one, or our only option.

            // We're no longer going to our previous container.
            cachedContainerPosition = null;

            // Get if we don't have...
            if (!mod.getItemStorage().hasItem(containerTarget)) {
                setDebugState("Getting container item");
                debugLogger.state("get container item: target=" + containerTarget
                        + ", nearest=" + describeOptionalPos(nearest)
                        + ", walkCost=" + formatDouble(costToWalk)
                        + ", makeCost=" + formatDouble(makeCost)
                        + ", reason=" + actionReason
                        + ", " + describeInteractionContext(mod));
                return TaskCatalogue.getItemTask(containerTarget);
            }

            setDebugState("Placing container...");
            debugLogger.state("place new container: target=" + containerTarget
                    + ", nearest=" + describeOptionalPos(nearest)
                    + ", walkCost=" + formatDouble(costToWalk)
                    + ", makeCost=" + formatDouble(makeCost)
                    + ", previousPlaced=" + describePos(placeTask.getPlaced())
                    + ", reason=" + actionReason
                    + ", " + describeInteractionContext(mod));

            justPlacedTimer.reset();
            if (shouldUseCarryOnSafeInteraction()) {
                mod.getInputControls().release(Input.SNEAK);
            }
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
        debugLogger.state("walk/open container:" + nearest.get().toShortString(),
                "walk/open container: targetPosition=" + nearest.get().toShortString()
                + ", walkCost=" + formatDouble(costToWalk)
                + ", makeCost=" + formatDouble(makeCost)
                + ", usingPlacedContainer=" + usingPlacedContainer
                + ", cached=" + describePos(cachedContainerPosition)
                + ", " + describeInteractionContext(mod));

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
        if (shouldUseCarryOnSafeInteraction()) {
            mod.getInputControls().release(Input.SNEAK);
            debugLogger.state("carry-on-safe open container:" + cachedContainerPosition.toShortString(),
                    "carry-on-safe open container: release sneak before targetPosition=" + cachedContainerPosition.toShortString());
        }
        return new InteractWithBlockTask(cachedContainerPosition, false);
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

    private Optional<BlockPos> getPlacedContainerIfValid(AltoClef mod) {
        Optional<BlockPos> carriedPlaced = getPlacedContainerIfValid(mod,
                placeCarriedTask == null ? null : placeCarriedTask.getPlaced());
        if (carriedPlaced.isPresent()) {
            return carriedPlaced;
        }
        return getPlacedContainerIfValid(mod, placeTask.getPlaced());
    }

    private Optional<BlockPos> getPlacedContainerIfValid(AltoClef mod, BlockPos placed) {
        if (placed == null) {
            return Optional.empty();
        }
        if (!isPlacedContainerBlock(mod, placed)) {
            debugLogger.state("placed container rejected:" + placed.toShortString() + ":not-block",
                    "placed container rejected: pos=" + placed.toShortString()
                            + ", reason=not-container-block-or-unloaded");
            return Optional.empty();
        }
        if (!WorldHelper.canReach(placed)) {
            debugLogger.state("placed container rejected:" + placed.toShortString() + ":unreachable",
                    "placed container rejected: pos=" + placed.toShortString()
                            + ", reason=unreachable");
            return Optional.empty();
        }
        return Optional.of(placed);
    }

    private Task getCarryOnCarriedContainerPlacementTask(AltoClef mod) {
        if (!shouldUseCarryOnSafeInteraction()) {
            return null;
        }

        if (placeCarriedTask != null) {
            if (placeCarriedTask.isFinished()) {
                //20260729_kpopmodder: Only cache a Carry On placement after the task confirms the carried state cleared.
                Optional<BlockPos> placed = getPlacedContainerIfValid(mod, placeCarriedTask.getPlaced());
                if (placed.isPresent()) {
                    cachedContainerPosition = placed.get();
                    justPlacedTimer.reset();
                    debugLogger.state("carried container placed: targetPosition=" + cachedContainerPosition.toShortString());
                    placeCarriedTask = null;
                    return null;
                }
                debugLogger.state("carried container placement completed without reachable placed container");
                placeCarriedTask = null;
                return null;
            }
            if (placeCarriedTask.hasFailed()) {
                debugLogger.state("carried container placement failed, falling back to normal container flow");
                placeCarriedTask = null;
                return null;
            }
            if (placeCarriedTask.isActive() && !placeCarriedTask.isFinished()) {
                setDebugState("Placing carried container");
                return placeCarriedTask;
            }
        }

        Optional<BlockState> carriedContainer = getCarriedContainerState(mod);
        if (carriedContainer.isEmpty()) {
            return null;
        }

        cachedContainerPosition = null;
        justPlacedTimer.reset();
        placeCarriedTask = new PlaceCarriedBlockTask(containerBlocks);
        mod.getInputControls().release(Input.SNEAK);
        setDebugState("Placing carried container");
        debugLogger.state("detected carried container block: " + carriedContainer.get().getBlock().getTranslationKey());
        return placeCarriedTask;
    }

    private boolean isPlacedContainerBlock(AltoClef mod, BlockPos placed) {
        if (!mod.getChunkTracker().isChunkLoaded(placed)) {
            return false;
        }
        return isContainerBlock(mod.getWorld().getBlockState(placed).getBlock());
    }

    private Optional<BlockState> getCarriedContainerState(AltoClef mod) {
        return CarryOnCompat.getCarriedBlockState(mod.getPlayer())
                .filter(state -> isContainerBlock(state.getBlock()));
    }

    private boolean isContainerBlock(Block block) {
        for (Block containerBlock : containerBlocks) {
            if (block == containerBlock) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();
        debugLogger.event("stop diagnostics: interruptedBy=" + describeTask(interruptTask)
                + ", containerTarget=" + containerTarget
                + ", cached=" + describePos(cachedContainerPosition)
                + ", placedTask=" + describePos(placeTask.getPlaced())
                + ", carriedPlaced=" + describePos(placeCarriedTask == null ? null : placeCarriedTask.getPlaced())
                + ", placeTaskActive=" + placeTask.isActive()
                + ", placeTaskFinished=" + placeTask.isFinished()
                + ", carriedTaskActive=" + (placeCarriedTask != null && placeCarriedTask.isActive())
                + ", carriedTaskFinished=" + (placeCarriedTask != null && placeCarriedTask.isFinished())
                + ", " + describeInteractionContext(mod));
        mod.getBehaviour().pop();
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

    private boolean shouldUseCarryOnSafeInteraction() {
        return CarryOnCompat.shouldAvoidSneakRightClick(containerBlocks);
    }

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

    private String describeInteractionContext(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "context=missing-client";
        }
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return "screen=" + (screen == null ? "none" : screen.getClass().getSimpleName())
                + ", handler=" + (mod.getPlayer().currentScreenHandler == null
                ? "none"
                : mod.getPlayer().currentScreenHandler.getClass().getSimpleName())
                + ", cursor=" + describeStack(StorageHelper.getItemStackInCursorSlot())
                + ", pathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                + ", breaking=" + mod.getControllerExtras().isBreakingBlock();
    }

    private String describeContainerActionReason(Optional<BlockPos> nearest, boolean usingPlacedContainer, double costToWalk, double makeCost) {
        if (nearest.isEmpty()) {
            return "no-reachable-container";
        }
        if (!usingPlacedContainer && costToWalk > makeCost) {
            return "new-container-cheaper";
        }
        if (!usingPlacedContainer && !placeForceTimer.elapsed() && justPlacedTimer.elapsed()) {
            return "place-force-window-active";
        }
        return "use-existing-container";
    }

    private void logPlanEvent(String stateKey, String detail) {
        if (Objects.equals(lastPlanEventKey, stateKey)) {
            return;
        }
        lastPlanEventKey = stateKey;
        debugLogger.event(detail);
    }

    private String describeTask(Task task) {
        if (task == null) {
            return "none";
        }
        try {
            return task.getClass().getSimpleName() + "{" + task + "}";
        } catch (RuntimeException ex) {
            return task.getClass().getSimpleName() + "{debugString failed: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "}";
        }
    }

    private String formatDouble(double value) {
        if (Double.isInfinite(value)) {
            return "infinity";
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
