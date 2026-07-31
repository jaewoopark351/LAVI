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
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
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
    private static final int POST_PLACE_GUI_OPEN_TIMEOUT_TICKS = 10;

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
    private boolean waitingForPlacedContainerInteractionStability;
    private long postPlaceOperationId = -1;
    private BlockPos postPlaceContainerPosition = null;
    private int postPlaceStabilityWaitedTicks;
    private boolean postPlaceStabilityWaitLogged;
    private boolean postPlaceStabilityProceedLogged;
    private boolean postPlaceOpenIntentStarted;
    private boolean postPlaceGuiOpenedLogged;
    private boolean postPlaceGuiTimeoutLogged;

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
        resetPostPlaceDiagnostics();
        ChatClefDiagnostics.logEvent("CONTAINER_TASK", "ON_START_BEGIN", "do_stuff_in_container_start", this,
                "containerTarget", containerTarget,
                "containerBlocks", Arrays.toString(containerBlocks),
                "openTableTaskExists", openTableTask != null);
        mod.getBehaviour().push();
        if (openTableTask == null) {
            openTableTask = new DoToClosestBlockTask(InteractWithBlockTask::new, containerBlocks);
            ChatClefDiagnostics.logTaskTransition(this, null, openTableTask, "open_table_task_created",
                    "containerTarget", containerTarget,
                    "containerBlocks", Arrays.toString(containerBlocks));
        }

        // Protect container since we might place it.
        mod.getBehaviour().addProtectedItems(ItemHelper.blocksToItems(containerBlocks));
        ChatClefDiagnostics.logEvent("CONTAINER_TASK", "ON_START_END", "do_stuff_in_container_start", this,
                "containerTarget", containerTarget,
                "containerBlocks", Arrays.toString(containerBlocks));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        boolean hasContainerItem = mod.getItemStorage().hasItem(ItemHelper.blocksToItems(containerBlocks));
        boolean placeTaskActive = placeTask.isActive();
        boolean placeTaskFinished = placeTaskActive && placeTask.isFinished();
        ChatClefDiagnostics.logEvent("CONTAINER_TASK", "ON_TICK_BEGIN", "container_tick_begin", this,
                "containerTarget", containerTarget,
                "containerBlocks", Arrays.toString(containerBlocks),
                "cachedContainerPosition", cachedContainerPosition,
                "hasContainerItem", hasContainerItem,
                "containerItemCount", ChatClefDiagnostics.safeValue(() -> mod.getItemStorage().getItemCount(ItemHelper.blocksToItems(containerBlocks))),
                "placeTaskActive", placeTaskActive,
                "placeTaskFinished", placeTaskFinished,
                "placeTaskPlaced", placeTask.getPlaced(),
                "placeTaskPlacedBlockState", placeTask.getPlaced() == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(placeTask.getPlaced())));
        // If we're placing, keep on placing.
        if (hasContainerItem && placeTaskActive && !placeTaskFinished) {
            setDebugState("Placing container");
            ChatClefDiagnostics.logTaskTransition(this, null, placeTask, "return_active_place_task",
                    "containerTarget", containerTarget,
                    "placeTaskPlaced", placeTask.getPlaced());
            return placeTask;
        }

        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        // trace-191: split the post-placement child handoff so SNEAK release can settle before normal container open.
        if (placeTaskFinished) {
            waitingForPlacedContainerInteractionStability = true;
            postPlaceOperationId = ChatClefDiagnostics.nextOperationId();
            postPlaceContainerPosition = placeTask.getPlaced();
            postPlaceStabilityWaitedTicks = 0;
            postPlaceStabilityWaitLogged = false;
            postPlaceStabilityProceedLogged = false;
            postPlaceOpenIntentStarted = false;
            postPlaceGuiOpenedLogged = false;
            postPlaceGuiTimeoutLogged = false;
            setDebugState("Waiting for placed-container handoff");
            ChatClefDiagnostics.logBoundary("POST_PLACE_HANDOFF", "placed_container_handoff_deferred", this,
                    "operationId", postPlaceOperationId,
                    "containerType", containerTarget,
                    "placedPosition", postPlaceContainerPosition,
                    "placedBlockState", postPlaceContainerPosition == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(postPlaceContainerPosition)),
                    "playerSneaking", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isSneaking()),
                    "sneakHeld", ChatClefDiagnostics.inputHeldState(Input.SNEAK),
                    "clientTick", ChatClefDiagnostics.currentClientTickId());
            return null;
        }

        if (waitingForPlacedContainerInteractionStability) {
            boolean sneakHeld = mod.getInputControls().isHeldDown(Input.SNEAK);
            boolean playerSneaking = mod.getPlayer().isSneaking();
            postPlaceStabilityWaitedTicks++;
            if (sneakHeld || playerSneaking) {
                if (!postPlaceStabilityWaitLogged) {
                    postPlaceStabilityWaitLogged = true;
                    ChatClefDiagnostics.logBoundary("POST_PLACE_STABILITY", "placed_container_handoff_stability_check", this,
                            "operationId", postPlaceOperationId,
                            "decision", "WAIT",
                            "waitedTicks", postPlaceStabilityWaitedTicks,
                            "placedPosition", postPlaceContainerPosition,
                            "playerSneaking", playerSneaking,
                            "sneakHeld", sneakHeld);
                }
                setDebugState("Waiting for post-placement interaction stability");
                return null;
            }
            if (!postPlaceStabilityProceedLogged) {
                postPlaceStabilityProceedLogged = true;
                ChatClefDiagnostics.logBoundary("POST_PLACE_STABILITY", "placed_container_handoff_stability_check", this,
                        "operationId", postPlaceOperationId,
                        "decision", "PROCEED",
                        "waitedTicks", postPlaceStabilityWaitedTicks,
                        "placedPosition", postPlaceContainerPosition,
                        "playerSneaking", playerSneaking,
                        "sneakHeld", sneakHeld);
            }
            waitingForPlacedContainerInteractionStability = false;
        }

        ChatClefDiagnostics.logEvent("CONTAINER_TASK", "CONTAINER_OPEN_CHECK_BEFORE", "before_isContainerOpen", this,
                "containerTarget", containerTarget,
                "cachedContainerPosition", cachedContainerPosition);
        boolean containerOpen = isContainerOpen(mod);
        ChatClefDiagnostics.logEvent("CONTAINER_TASK", "CONTAINER_OPEN_CHECK_AFTER", "after_isContainerOpen", this,
                "containerTarget", containerTarget,
                "cachedContainerPosition", cachedContainerPosition,
                "containerOpen", containerOpen);
        if (containerOpen) {
            logPostPlaceGuiOpened(mod);
            Task containerTask = containerSubTask(mod);
            ChatClefDiagnostics.logTaskTransition(this, null, containerTask, "return_container_sub_task",
                    "containerTarget", containerTarget,
                    "containerOpen", true);
            return containerTask;
        }
        logPostPlaceGuiTimeoutIfNeeded(mod);

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
        double costToMakeNew = getCostToMakeNew(mod);
        boolean placeForceElapsed = placeForceTimer.elapsed();
        boolean justPlacedElapsed = justPlacedTimer.elapsed();
        ChatClefDiagnostics.logEvent("CONTAINER_TASK", "NEAREST_DECISION", "container_nearest_decision", this,
                "containerTarget", containerTarget,
                "overrideContainerPosition", override,
                "nearestPresent", nearest.isPresent(),
                "nearestPosition", nearest.map(Object::toString).orElse("unavailable"),
                "nearestBlockState", nearest.map(blockPos -> ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(blockPos))).orElse("unavailable"),
                "placeTaskPlaced", placeTask.getPlaced(),
                "placeTaskPlacedBlockState", placeTask.getPlaced() == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(placeTask.getPlaced())),
                "costToWalk", costToWalk,
                "costToMakeNew", costToMakeNew,
                "placeForceElapsed", placeForceElapsed,
                "justPlacedElapsed", justPlacedElapsed,
                "hasContainerItem", hasContainerItem);

        // Make a new container if going to the container is a pretty bad cost.
        // Also keep on making the container if we're stuck in some
        if (costToWalk > costToMakeNew) {
            ChatClefDiagnostics.logEvent("CONTAINER_TASK", "PLACE_FORCE_RESET", "cost_to_walk_exceeds_make_new", this,
                    "containerTarget", containerTarget,
                    "costToWalk", costToWalk,
                    "costToMakeNew", costToMakeNew,
                    "nearestPresent", nearest.isPresent());
            placeForceTimer.reset();
        }
        placeForceElapsed = placeForceTimer.elapsed();
        justPlacedElapsed = justPlacedTimer.elapsed();
        if (nearest.isEmpty() || (!placeForceElapsed && justPlacedElapsed)) {
            // It's cheaper to make a new one, or our only option.

            // We're no longer going to our previous container.
            cachedContainerPosition = null;
            ChatClefDiagnostics.logEvent("CONTAINER_TASK", "MAKE_OR_PLACE_CONTAINER", "container_make_or_place_branch", this,
                    "containerTarget", containerTarget,
                    "nearestPresent", nearest.isPresent(),
                    "placeForceElapsed", placeForceElapsed,
                    "justPlacedElapsed", justPlacedElapsed,
                    "hasContainerItem", hasContainerItem,
                    "placeTaskPlaced", placeTask.getPlaced());

            // Get if we don't have...
            if (!mod.getItemStorage().hasItem(containerTarget)) {
                setDebugState("Getting container item");
                ChatClefDiagnostics.logEvent("CONTAINER_TASK", "RETURN", "return_get_container_item_task", this,
                        "containerTarget", containerTarget,
                        "hasContainerItem", hasContainerItem,
                        "placeTaskPlaced", placeTask.getPlaced());
                return TaskCatalogue.getItemTask(containerTarget);
            }

            setDebugState("Placing container...");

            justPlacedTimer.reset();
            // Now place!
            ChatClefDiagnostics.logTaskTransition(this, null, placeTask, "return_place_task",
                    "containerTarget", containerTarget,
                    "nearestPresent", nearest.isPresent(),
                    "placeTaskPlaced", placeTask.getPlaced());
            return placeTask;
        }

        // This is insanely cursed.
        // TODO: Finish committing to optionals, this is ugly.
        cachedContainerPosition = nearest.get();

        // Walk to it and open it

        // Wait for food
        if (mod.getFoodChain().needsToEat()) {
            setDebugState("Waiting for eating...");
            ChatClefDiagnostics.logEvent("CONTAINER_TASK", "RETURN", "return_wait_for_food", this,
                    "containerTarget", containerTarget,
                    "cachedContainerPosition", cachedContainerPosition);
            return null;
        }
        setDebugState("Walking to container... " + nearest.get().toShortString());

        var cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            if (toMoveTo.isEmpty()) {
                ChatClefDiagnostics.logEvent("CONTAINER_TASK", "RETURN", "return_ensure_free_inventory_slot", this,
                        "containerTarget", containerTarget,
                        "cursorStack", cursorStack);
                return new EnsureFreeInventorySlotTask();
            }
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                ChatClefDiagnostics.logEvent("CONTAINER_TASK", "RETURN", "return_after_cursor_throwaway_click", this,
                        "containerTarget", containerTarget,
                        "cursorStack", cursorStack);
                return null;
            }
            mod.getSlotHandler().clickSlot(toMoveTo.get(), 0, SlotActionType.PICKUP);
            ChatClefDiagnostics.logEvent("CONTAINER_TASK", "RETURN", "return_after_cursor_move_click", this,
                    "containerTarget", containerTarget,
                    "cursorStack", cursorStack,
                    "slot", toMoveTo.get());
            return null;
        }
        ChatClefDiagnostics.startTrace("container_open_intent", this,
                "containerTarget", containerTarget,
                "cachedContainerPosition", cachedContainerPosition,
                "cachedContainerBlockState", cachedContainerPosition == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(cachedContainerPosition)),
                "openTableTaskClass", ChatClefDiagnostics.className(openTableTask));
        beginPostPlaceOpenIntentIfNeeded(mod);
        ChatClefDiagnostics.logTaskTransition(this, null, openTableTask, "return_open_table_task",
                "containerTarget", containerTarget,
                "cachedContainerPosition", cachedContainerPosition,
                "cachedContainerBlockState", cachedContainerPosition == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(cachedContainerPosition)));
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
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "container_task_onStop_begin",
                "containerTarget", containerTarget,
                "cachedContainerPosition", cachedContainerPosition);
        ChatClefDiagnostics.clearPostPlaceContainerOpenIntent(postPlaceOperationId);
        resetPostPlaceDiagnostics();
        AltoClef.getInstance().getBehaviour().pop();
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "container_task_onStop_end",
                "containerTarget", containerTarget,
                "cachedContainerPosition", cachedContainerPosition);
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

    private void beginPostPlaceOpenIntentIfNeeded(AltoClef mod) {
        if (postPlaceOperationId < 0
                || postPlaceContainerPosition == null
                || cachedContainerPosition == null
                || postPlaceOpenIntentStarted
                || !postPlaceContainerPosition.equals(cachedContainerPosition)) {
            return;
        }
        postPlaceOpenIntentStarted = true;
        ChatClefDiagnostics.beginPostPlaceContainerOpenIntent(
                postPlaceOperationId,
                containerTarget,
                cachedContainerPosition,
                ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(cachedContainerPosition))
        );
    }

    private void logPostPlaceGuiOpened(AltoClef mod) {
        if (postPlaceOperationId < 0 || !postPlaceOpenIntentStarted || postPlaceGuiOpenedLogged) {
            return;
        }
        long operationId = postPlaceOperationId;
        if (!ChatClefDiagnostics.markPostPlaceContainerGuiOpened(operationId)) {
            return;
        }
        postPlaceGuiOpenedLogged = true;
        ChatClefDiagnostics.logBoundary("CONTAINER_GUI_OPENED", "post_place_container_gui_opened", this,
                "operationId", operationId,
                "isContainerOpen", true,
                "screenHandler", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().currentScreenHandler.getClass().getSimpleName()),
                "syncId", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().currentScreenHandler.syncId),
                "elapsedTicks", ChatClefDiagnostics.postPlaceContainerElapsedTicks(operationId));
        ChatClefDiagnostics.clearPostPlaceContainerOpenIntent(operationId);
        resetPostPlaceDiagnostics();
    }

    private void logPostPlaceGuiTimeoutIfNeeded(AltoClef mod) {
        if (postPlaceOperationId < 0
                || !postPlaceOpenIntentStarted
                || postPlaceGuiOpenedLogged
                || postPlaceGuiTimeoutLogged) {
            return;
        }
        long elapsedTicks = ChatClefDiagnostics.postPlaceContainerElapsedTicks(postPlaceOperationId);
        if (elapsedTicks < POST_PLACE_GUI_OPEN_TIMEOUT_TICKS) {
            return;
        }
        if (!ChatClefDiagnostics.markPostPlaceContainerGuiTimeout(postPlaceOperationId)) {
            return;
        }
        postPlaceGuiTimeoutLogged = true;
        ChatClefDiagnostics.logWarningEvent("CONTAINER_GUI_OPEN_TIMEOUT", "post_place_container_gui_open_timeout", this,
                "operationId", postPlaceOperationId,
                "attempts", ChatClefDiagnostics.postPlaceContainerAttemptCount(postPlaceOperationId),
                "lastInteractResult", ChatClefDiagnostics.postPlaceContainerLastInteractResult(postPlaceOperationId),
                "actualScreenHandler", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().currentScreenHandler.getClass().getSimpleName()),
                "elapsedTicks", elapsedTicks);
    }

    private void resetPostPlaceDiagnostics() {
        postPlaceOperationId = -1;
        postPlaceContainerPosition = null;
        postPlaceStabilityWaitedTicks = 0;
        postPlaceStabilityWaitLogged = false;
        postPlaceStabilityProceedLogged = false;
        postPlaceOpenIntentStarted = false;
        postPlaceGuiOpenedLogged = false;
        postPlaceGuiTimeoutLogged = false;
    }
}
