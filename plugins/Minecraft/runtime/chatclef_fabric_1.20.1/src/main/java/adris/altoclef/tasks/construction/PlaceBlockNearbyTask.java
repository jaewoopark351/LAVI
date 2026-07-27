package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.compat.CarryOnCompat;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Place a type of block nearby, anywhere.
 * <p>
 * Also known as the "bear strats" task.
 */
public class PlaceBlockNearbyTask extends Task {

    private static final int MAX_DIRECT_PLACE_ATTEMPTS_PER_TARGET = 20;
    private static final int PLANNED_PLACE_GRACE_TICKS = 20 * 8;
    private static final int MAX_CLEAR_ATTEMPTS_PER_TARGET = 2;
    private static final double DIRECT_PLACE_TARGET_SKIP_SECONDS = 6.0;

    private final Block[] toPlace;

    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final TimeoutWanderTask wander = new TimeoutWanderTask(5);

    private final TimerGame _randomlookTimer = new TimerGame(0.25);
    private final Predicate<BlockPos> _canPlaceHere;
    private BlockPos justPlaced; // Where we JUST placed a block.
    private BlockPos tryPlace;   // Where we should TRY placing a block.
    // Oof, necesarry for the onBlockPlaced action.
    private Subscription<BlockPlaceEvent> _onBlockPlaced;
    private final StateChangeLogger debugLogger = new StateChangeLogger("PlaceBlockNearbyTask");
    private BlockPos directPlaceAttemptTarget;
    private int directPlaceAttempts;
    private BlockPos skippedDirectPlaceTarget;
    private final TimerGame skippedDirectPlaceTimer = new TimerGame(DIRECT_PLACE_TARGET_SKIP_SECONDS);
    private PlaceBlockTask plannedPlaceTask;
    private BlockPos plannedPlaceTarget;
    private int plannedPlaceTicks;
    private DestroyBlockTask clearNearbyTask;
    private BlockPos clearNearbyTarget;
    private BlockPos clearNearbyForPlaceTarget;
    private BlockPos lastClearAttemptPlaceTarget;
    private int clearAttemptsForTarget;

    public PlaceBlockNearbyTask(Predicate<BlockPos> canPlaceHere, Block... toPlace) {
        this.toPlace = toPlace;
        _canPlaceHere = canPlaceHere;
    }

    public PlaceBlockNearbyTask(Block... toPlace) {
        this(blockPos -> true, toPlace);
    }

    @Override
    protected void onStart() {
        progressChecker.reset();
        resetDirectPlaceAttempts();
        resetPlannedPlaceTask();
        resetClearNearbyTask(false);
        skippedDirectPlaceTarget = null;
        lastClearAttemptPlaceTarget = null;
        clearAttemptsForTarget = 0;
        AltoClef.getInstance().getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
        if (shouldAvoidSneakRightClick()) {
            AltoClef.getInstance().getInputControls().release(Input.SNEAK);
        }
        debugLogger.event("start: blocks=" + Arrays.toString(toPlace)
                + ", carryOnSafeSneak=" + shouldAvoidSneakRightClick());

        // Check for blocks being placed
        _onBlockPlaced = EventBus.subscribe(BlockPlaceEvent.class, evt -> {
            if (ArrayUtils.contains(toPlace, evt.blockState.getBlock())) {
                justPlaced = evt.blockPos;
                resetDirectPlaceAttempts();
                resetPlannedPlaceTask();
                resetClearNearbyTask(false);
                debugLogger.event("block placed event: block=" + evt.blockState.getBlock().getTranslationKey()
                        + ", pos=" + evt.blockPos.toShortString());
                stopPlacing();
            }
        });
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            progressChecker.reset();
        }
        // Method:
        // - If looking at placable block
        //      Place immediately
        // Find a spot to place
        // - Prefer flat areas (open space, block below) closest to player
        // -

        //20260727_kpopmodder: Do not send world placement clicks while an inventory/container screen is still open.
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            setDebugState("Waiting for cursor slot to clear before placing.");
            debugLogger.state("cursor occupied before placing",
                    "cursor occupied while placing: cursor=" + describeStack(cursorStack));
            return null;
           /* Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            if (moveTo.isPresent()) {
                mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            if (garbage.isPresent()) {
                mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);*/
        }
        if (waitForScreenToClose()) {
            setDebugState("Closing screen before placing.");
            return null;
        }

        Task clearTask = continueClearNearbyTask(mod);
        if (clearTask != null) {
            return clearTask;
        }

        // Try placing where we're looking right now.
        BlockPos current = getCurrentlyLookingBlockPlace(mod);
        if (current != null && isTemporarilySkippingDirectPlace(current)) {
            debugLogger.state("skip repeated direct place target: " + current.toShortString(),
                    "skip repeated direct place target: target=" + current.toShortString()
                            + ", blockAtTarget=" + describeBlockAt(current));
        } else if (current != null && isPlacementCollisionBlocked(mod, current)) {
            debugLogger.state("skip direct place target blocked by collision: " + current.toShortString(),
                    "skip direct place target blocked by collision: target=" + current.toShortString()
                            + ", entity=" + describeNearbyBlockingEntity(mod, current)
                            + ", " + describePlacementContext(mod, current));
            markDirectPlaceTargetSkipped(current);
        } else if (current != null && _canPlaceHere.test(current)) {
            setDebugState("Placing since we can...");
            if (mod.getSlotHandler().forceEquipItem(ItemHelper.blocksToItems(toPlace))) {
                if (place(mod, current)) {
                    return null;
                }
            } else {
                debugLogger.state("place blocked: could not equip block; target=" + current.toShortString());
            }
        }

        // Wander while we can.
        if (wander.isActive() && !wander.isFinished()) {
            setDebugState("Wandering, will try to place again later.");
            debugLogger.state("wander before retrying place: tryPlace=" + describePos(tryPlace)
                    + ", justPlaced=" + describePos(justPlaced));
            progressChecker.reset();
            return wander;
        }
        // Fail check
        if (!progressChecker.check(mod)) {
            Debug.logMessage("Failed placing, wandering and trying again.");
            debugLogger.state("placing progress failed: tryPlace=" + describePos(tryPlace)
                    + ", justPlaced=" + describePos(justPlaced));
            LookHelper.randomOrientation();
            if (tryPlace != null) {
                mod.getBlockScanner().requestBlockUnreachable(tryPlace);
                tryPlace = null;
            }
            return wander;
        }

        // Try to place at a particular spot.
        if (tryPlace == null || !WorldHelper.canReach(tryPlace)) {
            tryPlace = locateClosePlacePos(mod);
        }
        if (tryPlace != null) {
            BlockPos plannedTarget = tryPlace;
            Task clearBeforePlacing = getClearTaskBeforePlacing(mod, plannedTarget);
            if (clearBeforePlacing != null) {
                return clearBeforePlacing;
            }
            if (tryPlace == null || !plannedTarget.equals(tryPlace)) {
                // The pre-place check skipped this target.
            } else if (isPlacementCollisionBlocked(mod, plannedTarget)) {
                debugLogger.event("planned place target blocked by collision: target=" + plannedTarget.toShortString()
                        + ", entity=" + describeNearbyBlockingEntity(mod, plannedTarget)
                        + ", " + describePlacementContext(mod, plannedTarget));
                markDirectPlaceTargetSkipped(plannedTarget);
            } else {
                setDebugState("Trying to place at " + plannedTarget);
                justPlaced = plannedTarget;
                Task plannedTask = getPlannedPlaceTaskOrRecover(mod, plannedTarget);
                if (plannedTask != null) {
                    return plannedTask;
                }
            }
        }

        // Look in random places to maybe get a random hit
        if (_randomlookTimer.elapsed()) {
            _randomlookTimer.reset();
            LookHelper.randomOrientation();
        }

        setDebugState("Wandering until we randomly place or find a good place spot.");
        debugLogger.state("no valid place position found; random looking");
        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(Task interruptTask) {
        stopPlacing();
        resetPlannedPlaceTask();
        resetClearNearbyTask(false);
        EventBus.unsubscribe(_onBlockPlaced);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PlaceBlockNearbyTask task) {
            return Arrays.equals(task.toPlace, toPlace);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Place " + Arrays.toString(toPlace) + " nearby";
    }

    @Override
    public boolean isFinished() {
        return justPlaced != null && ArrayUtils.contains(toPlace, AltoClef.getInstance().getWorld().getBlockState(justPlaced).getBlock());
    }

    public BlockPos getPlaced() {
        return justPlaced;
    }

    private BlockPos getCurrentlyLookingBlockPlace(AltoClef mod) {
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if (hit instanceof BlockHitResult bhit) {
            BlockPos bpos = bhit.getBlockPos();//.subtract(bhit.getSide().getVector());
            //Debug.logMessage("TEMP: A: " + bpos);
            IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
            if (MovementHelper.canPlaceAgainst(ctx, bpos)) {
                BlockPos placePos = bhit.getBlockPos().add(bhit.getSide().getVector());
                // Don't place inside the player.
                if (WorldHelper.isInsidePlayer(placePos)) {
                    return null;
                }
                //Debug.logMessage("TEMP: B (actual): " + placePos);
                if (WorldHelper.canPlace(placePos)) {
                    return placePos;
                }
            }
        }
        return null;
    }

    private boolean blockEquipped() {
        return StorageHelper.isEquipped(ItemHelper.blocksToItems(toPlace));
    }

    private boolean place(AltoClef mod, BlockPos targetPlace) {
        if (waitForScreenToClose()) {
            return false;
        }
        if (!mod.getExtraBaritoneSettings().isInteractionPaused() && blockEquipped()) {
            recordDirectPlaceAttempt(targetPlace);
            if (directPlaceAttempts > MAX_DIRECT_PLACE_ATTEMPTS_PER_TARGET) {
                markDirectPlaceTargetSkipped(targetPlace);
                return false;
            }

            boolean avoidSneakRightClick = shouldAvoidSneakRightClick();
            //20260727_kpopmodder: Carry On uses sneak-right-click for carrying blocks, so avoid it for container placement.
            if (avoidSneakRightClick) {
                mod.getInputControls().release(Input.SNEAK);
            } else {
                // Shift click just for 100% container security.
                mod.getInputControls().hold(Input.SNEAK);
            }

            //mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            // This appears to work on servers...
            // TODO: Helper lol
            HitResult mouseOver = MinecraftClient.getInstance().crosshairTarget;
            if (mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK) {
                debugLogger.state("place aborted: crosshair target is not a block; mouseOver=" + mouseOver);
                return false;
            }
            Hand hand = Hand.MAIN_HAND;
            assert MinecraftClient.getInstance().interactionManager != null;
            ActionResult result = MinecraftClient.getInstance().interactionManager.interactBlock(mod.getPlayer(),hand, (BlockHitResult) mouseOver);
            debugLogger.state("place click attempted: " + targetPlace.toShortString(),
                    "place click attempted: target=" + targetPlace.toShortString()
                            + ", attempt=" + directPlaceAttempts
                            + ", result=" + result
                            + ", sneaking=" + mod.getPlayer().isSneaking()
                            + ", carryOnSafeSneak=" + avoidSneakRightClick);
            if (result == ActionResult.SUCCESS && (avoidSneakRightClick || mod.getPlayer().isSneaking())) {
                mod.getPlayer().swingHand(hand);
                justPlaced = targetPlace;
                if (isPlacedAt(targetPlace)) {
                    resetDirectPlaceAttempts();
                    debugLogger.event("place confirmed immediately: target=" + targetPlace.toShortString());
                } else {
                    debugLogger.state("place click accepted waiting for block update: " + targetPlace.toShortString(),
                            "place click accepted, waiting for block update: target=" + targetPlace.toShortString()
                                    + ", blockAtTarget=" + describeBlockAt(targetPlace));
                }
                return true;
            }

            //mod.getControllerExtras().mouseClickOverride(1, true);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            return true;
        }
        debugLogger.state("place blocked: interactionPaused=" + mod.getExtraBaritoneSettings().isInteractionPaused()
                + ", equipped=" + blockEquipped());
        return false;
    }

    private boolean waitForScreenToClose() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null) {
            return false;
        }

        if (isClosableScreen(screen)) {
            debugLogger.state("screen open before placing: " + describeScreen(screen),
                    "screen open before placing; closing screen=" + describeScreen(screen));
            StorageHelper.closeScreen();
        } else {
            debugLogger.state("protected screen open before placing: " + describeScreen(screen),
                    "protected screen open before placing; waiting screen=" + describeScreen(screen));
        }
        return true;
    }

    private boolean isClosableScreen(Screen screen) {
        return !(screen instanceof GameMenuScreen)
                && !(screen instanceof GameOptionsScreen)
                && !(screen instanceof ChatScreen);
    }

    private void recordDirectPlaceAttempt(BlockPos targetPlace) {
        if (!targetPlace.equals(directPlaceAttemptTarget)) {
            directPlaceAttemptTarget = targetPlace;
            directPlaceAttempts = 0;
        }
        directPlaceAttempts++;
    }

    private void resetDirectPlaceAttempts() {
        directPlaceAttemptTarget = null;
        directPlaceAttempts = 0;
    }

    private void markDirectPlaceTargetSkipped(BlockPos targetPlace) {
        skippedDirectPlaceTarget = new BlockPos(targetPlace.getX(), targetPlace.getY(), targetPlace.getZ());
        skippedDirectPlaceTimer.reset();
        if (targetPlace.equals(tryPlace)) {
            tryPlace = null;
        }
        if (targetPlace.equals(justPlaced) && !isPlacedAt(justPlaced)) {
            justPlaced = null;
        }
        resetDirectPlaceAttempts();
        debugLogger.event("temporarily skipping place target: target=" + targetPlace.toShortString()
                + ", seconds=" + DIRECT_PLACE_TARGET_SKIP_SECONDS
                + ", blockAtTarget=" + describeBlockAt(targetPlace));
        LookHelper.randomOrientation();
    }

    private boolean isTemporarilySkippingDirectPlace(BlockPos targetPlace) {
        if (skippedDirectPlaceTarget == null) {
            return false;
        }
        if (skippedDirectPlaceTimer.elapsed()) {
            skippedDirectPlaceTarget = null;
            return false;
        }
        return skippedDirectPlaceTarget.equals(targetPlace);
    }

    private boolean isPlacedAt(BlockPos targetPlace) {
        return targetPlace != null
                && ArrayUtils.contains(toPlace, AltoClef.getInstance().getWorld().getBlockState(targetPlace).getBlock());
    }

    private Task getPlannedPlaceTaskOrRecover(AltoClef mod, BlockPos targetPlace) {
        if (isPlacedAt(targetPlace)) {
            justPlaced = targetPlace;
            resetPlannedPlaceTask();
            return null;
        }

        if (plannedPlaceTask != null && targetPlace.equals(plannedPlaceTarget)) {
            if (plannedPlaceTask.isFinished()) {
                justPlaced = targetPlace;
                resetPlannedPlaceTask();
                return null;
            }

            plannedPlaceTicks++;
            if (plannedPlaceTicks > PLANNED_PLACE_GRACE_TICKS) {
                return recoverFromBlockedPlannedPlace(mod, targetPlace);
            }

            debugLogger.state("continue planned place: " + targetPlace.toShortString(),
                    "continue planned place: target=" + targetPlace.toShortString()
                            + ", ticks=" + plannedPlaceTicks
                            + ", graceTicks=" + PLANNED_PLACE_GRACE_TICKS
                            + ", " + describePlacementContext(mod, targetPlace));
            return plannedPlaceTask;
        }

        plannedPlaceTarget = copyPos(targetPlace);
        plannedPlaceTicks = 0;
        plannedPlaceTask = new PlaceBlockTask(targetPlace, toPlace);
        debugLogger.state("try planned place: target=" + targetPlace.toShortString(),
                "try planned place: target=" + targetPlace.toShortString()
                        + ", graceTicks=" + PLANNED_PLACE_GRACE_TICKS
                        + ", " + describePlacementContext(mod, targetPlace));
        return plannedPlaceTask;
    }

    private Task recoverFromBlockedPlannedPlace(AltoClef mod, BlockPos targetPlace) {
        debugLogger.event("planned place grace expired: target=" + targetPlace.toShortString()
                + ", ticks=" + plannedPlaceTicks
                + ", " + describePlacementContext(mod, targetPlace));
        resetPlannedPlaceTask();

        BlockPos clearTarget = findClearableBlockNearPlacement(mod, targetPlace);
        Task clearTask = startClearNearbyTask(mod, targetPlace, clearTarget, "planned place stuck");
        if (clearTask != null) {
            return clearTask;
        }

        debugLogger.event("planned place skipped after failed recovery: target=" + targetPlace.toShortString()
                + ", " + describePlacementContext(mod, targetPlace));
        markDirectPlaceTargetSkipped(targetPlace);
        return null;
    }

    private Task getClearTaskBeforePlacing(AltoClef mod, BlockPos targetPlace) {
        BlockState state = mod.getWorld().getBlockState(targetPlace);
        if (state.isAir() || state.isReplaceable()) {
            return null;
        }

        if (!isSafeToClearForPlacement(mod, targetPlace)) {
            debugLogger.event("planned place target occupied but unsafe to clear: target=" + targetPlace.toShortString()
                    + ", block=" + describeBlockAt(targetPlace)
                    + ", " + describePlacementContext(mod, targetPlace));
            markDirectPlaceTargetSkipped(targetPlace);
            return null;
        }

        return startClearNearbyTask(mod, targetPlace, targetPlace, "placement target occupied");
    }

    private Task continueClearNearbyTask(AltoClef mod) {
        if (clearNearbyTask == null) {
            return null;
        }

        if (clearNearbyTask.isFinished()) {
            debugLogger.event("clear nearby block complete: target=" + describePos(clearNearbyForPlaceTarget)
                    + ", cleared=" + describePos(clearNearbyTarget)
                    + ", blockNow=" + describeBlockAt(clearNearbyTarget));
            resetClearNearbyTask(false);
            resetPlannedPlaceTask();
            progressChecker.reset();
            return null;
        }

        if (clearNearbyTask.stopped()) {
            debugLogger.event("clear nearby block stopped before completion: target=" + describePos(clearNearbyForPlaceTarget)
                    + ", clear=" + describePos(clearNearbyTarget)
                    + ", block=" + describeBlockAt(clearNearbyTarget));
            BlockPos skippedTarget = clearNearbyForPlaceTarget;
            resetClearNearbyTask(false);
            if (skippedTarget != null) {
                markDirectPlaceTargetSkipped(skippedTarget);
            }
            return null;
        }

        setDebugState("Clearing nearby block before placing.");
        debugLogger.state("continue clearing nearby block: " + describePos(clearNearbyTarget),
                "continue clearing nearby block: target=" + describePos(clearNearbyForPlaceTarget)
                        + ", clear=" + describePos(clearNearbyTarget)
                        + ", block=" + describeBlockAt(clearNearbyTarget));
        return clearNearbyTask;
    }

    private Task startClearNearbyTask(AltoClef mod, BlockPos placeTarget, BlockPos blockToClear, String reason) {
        if (blockToClear == null) {
            return null;
        }
        if (!recordClearAttempt(placeTarget)) {
            debugLogger.event("clear nearby block limit reached: target=" + placeTarget.toShortString()
                    + ", maxAttempts=" + MAX_CLEAR_ATTEMPTS_PER_TARGET
                    + ", reason=" + reason);
            return null;
        }

        resetPlannedPlaceTask();
        clearNearbyTask = new DestroyBlockTask(blockToClear);
        clearNearbyTarget = copyPos(blockToClear);
        clearNearbyForPlaceTarget = copyPos(placeTarget);
        progressChecker.reset();
        debugLogger.event("clear nearby block before place retry: reason=" + reason
                + ", target=" + placeTarget.toShortString()
                + ", clear=" + blockToClear.toShortString()
                + ", block=" + describeBlockAt(blockToClear)
                + ", attempt=" + clearAttemptsForTarget + "/" + MAX_CLEAR_ATTEMPTS_PER_TARGET);
        return clearNearbyTask;
    }

    private boolean recordClearAttempt(BlockPos placeTarget) {
        if (lastClearAttemptPlaceTarget == null || !lastClearAttemptPlaceTarget.equals(placeTarget)) {
            lastClearAttemptPlaceTarget = copyPos(placeTarget);
            clearAttemptsForTarget = 0;
        }
        if (clearAttemptsForTarget >= MAX_CLEAR_ATTEMPTS_PER_TARGET) {
            return false;
        }
        clearAttemptsForTarget++;
        return true;
    }

    private void resetPlannedPlaceTask() {
        plannedPlaceTask = null;
        plannedPlaceTarget = null;
        plannedPlaceTicks = 0;
    }

    private void resetClearNearbyTask(boolean resetAttemptCounter) {
        clearNearbyTask = null;
        clearNearbyTarget = null;
        clearNearbyForPlaceTarget = null;
        if (resetAttemptCounter) {
            lastClearAttemptPlaceTarget = null;
            clearAttemptsForTarget = 0;
        }
    }

    private BlockPos findClearableBlockNearPlacement(AltoClef mod, BlockPos targetPlace) {
        BlockPos best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        BlockPos start = targetPlace.add(-1, 0, -1);
        BlockPos end = targetPlace.add(1, 1, 1);
        for (BlockPos blockPos : WorldHelper.scanRegion(start, end)) {
            if (blockPos.equals(targetPlace) || blockPos.equals(targetPlace.down())) {
                continue;
            }
            if (!isSafeToClearForPlacement(mod, blockPos)) {
                continue;
            }
            double score = BlockPosVer.getSquaredDistance(blockPos, mod.getPlayer().getPos())
                    + (blockPos.getY() > targetPlace.getY() ? 1.0 : 0.0)
                    + (WorldHelper.isInsidePlayer(blockPos) ? 5.0 : 0.0);
            if (score < bestScore) {
                best = copyPos(blockPos);
                bestScore = score;
            }
        }
        return best;
    }

    private boolean isSafeToClearForPlacement(AltoClef mod, BlockPos blockPos) {
        if (blockPos == null || mod == null || mod.getWorld() == null) {
            return false;
        }
        BlockState state = mod.getWorld().getBlockState(blockPos);
        if (state.isAir() || state.isReplaceable()) {
            return false;
        }
        if (!WorldHelper.canBreak(blockPos)) {
            return false;
        }
        if (WorldHelper.isInsidePlayer(blockPos)) {
            return false;
        }
        if (mod.getWorld().getBlockEntity(blockPos) != null || state.hasBlockEntity()) {
            return false;
        }

        Block block = state.getBlock();
        if (ArrayUtils.contains(toPlace, block) || isProtectedPlacementClearBlock(block)) {
            return false;
        }
        String translationKey = block.getTranslationKey();
        return !translationKey.contains("_ore")
                && !translationKey.contains("ancient_debris")
                && !translationKey.contains("spawner");
    }

    private boolean isProtectedPlacementClearBlock(Block block) {
        return block == Blocks.BEDROCK
                || block == Blocks.OBSIDIAN
                || block == Blocks.CHEST
                || block == Blocks.TRAPPED_CHEST
                || block == Blocks.BARREL
                || block == Blocks.HOPPER
                || block == Blocks.FURNACE
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER
                || block == Blocks.CRAFTING_TABLE
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL;
    }

    private boolean isPlacementCollisionBlocked(AltoClef mod, BlockPos targetPlace) {
        if (mod == null || mod.getWorld() == null || targetPlace == null) {
            return false;
        }
        BlockState currentState = mod.getWorld().getBlockState(targetPlace);
        if (!currentState.isAir() && !currentState.isReplaceable()) {
            return false;
        }
        return !mod.getWorld().canPlace(getFallbackPlaceState(), targetPlace, ShapeContext.absent());
    }

    private BlockState getFallbackPlaceState() {
        for (Block block : toPlace) {
            if (block != null) {
                return block.getDefaultState();
            }
        }
        return Blocks.COBBLESTONE.getDefaultState();
    }

    private boolean shouldAvoidSneakRightClick() {
        return CarryOnCompat.shouldAvoidSneakRightClick(toPlace);
    }

    private void stopPlacing() {
        AltoClef.getInstance().getInputControls().release(Input.SNEAK);
        //mod.getControllerExtras().mouseClickOverride(1, false);
        // Oof, these sometimes cause issues so this is a bit of a duct tape fix.
        AltoClef.getInstance().getClientBaritone().getBuilderProcess().onLostControl();
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

    private String describeScreen(Screen screen) {
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    private String describeBlockAt(BlockPos pos) {
        if (pos == null) {
            return "none";
        }
        Block block = AltoClef.getInstance().getWorld().getBlockState(pos).getBlock();
        return block.getTranslationKey();
    }

    private String describePlacementContext(AltoClef mod, BlockPos targetPlace) {
        if (mod == null || mod.getPlayer() == null || mod.getWorld() == null) {
            return "context=missing-client";
        }
        return "player=" + mod.getPlayer().getBlockPos().toShortString()
                + ", blockAtTarget=" + describeBlockAt(targetPlace)
                + ", collisionBlocked=" + isPlacementCollisionBlocked(mod, targetPlace)
                + ", nearbyEntity=" + describeNearbyBlockingEntity(mod, targetPlace)
                + ", screen=" + describeScreen(MinecraftClient.getInstance().currentScreen)
                + ", screenHandler=" + describeScreenHandler(mod)
                + ", cursor=" + describeStack(StorageHelper.getItemStackInCursorSlot())
                + ", pathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                + ", builderActive=" + mod.getClientBaritone().getBuilderProcess().isActive()
                + ", tryPlace=" + describePos(tryPlace)
                + ", justPlaced=" + describePos(justPlaced);
    }

    private String describeScreenHandler(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null || mod.getPlayer().currentScreenHandler == null) {
            return "none";
        }
        return mod.getPlayer().currentScreenHandler.getClass().getSimpleName();
    }

    private String describeNearbyBlockingEntity(AltoClef mod, BlockPos targetPlace) {
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null || targetPlace == null) {
            return "none";
        }

        Entity closest = null;
        double closestDistance = 2.25;
        for (Entity entity : mod.getWorld().getEntities()) {
            if (entity == mod.getPlayer() || !entity.isAlive()) {
                continue;
            }
            double distance = BlockPosVer.getSquaredDistance(targetPlace, entity.getPos());
            if (distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }
        if (closest == null) {
            return "none";
        }
        return closest.getType().getTranslationKey() + "@" + closest.getBlockPos().toShortString();
    }

    private BlockPos copyPos(BlockPos pos) {
        if (pos == null) {
            return null;
        }
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private BlockPos locateClosePlacePos(AltoClef mod) {
        int range = 7;
        BlockPos best = null;
        double smallestScore = Double.POSITIVE_INFINITY;
        BlockPos start = mod.getPlayer().getBlockPos().add(-range,-range,-range);
        BlockPos end = mod.getPlayer().getBlockPos().add(range,range,range);
        for (BlockPos blockPos : WorldHelper.scanRegion(start, end)) {
            BlockState state = mod.getWorld().getBlockState(blockPos);
            boolean occupied = !state.isAir() && !state.isReplaceable();
            boolean solid = WorldHelper.isSolidBlock(blockPos);
            boolean inside = WorldHelper.isInsidePlayer(blockPos);
            // We can't break this block.
            if (occupied && !isSafeToClearForPlacement(mod, blockPos)) {
                continue;
            }
            if (isTemporarilySkippingDirectPlace(blockPos)) {
                continue;
            }
            // We can't place here as defined by user.
            if (!_canPlaceHere.test(blockPos)) {
                continue;
            }
            // We can't place here.
            if (!WorldHelper.canReach(blockPos) || !WorldHelper.canPlace(blockPos)) {
                continue;
            }
            if (isPlacementCollisionBlocked(mod, blockPos)) {
                continue;
            }
            boolean hasBelow = WorldHelper.isSolidBlock(blockPos.down());
            double distSq = BlockPosVer.getSquaredDistance(blockPos,mod.getPlayer().getPos());

            double score = distSq + (solid ? 4 : 0) + (hasBelow ? 0 : 10) + (inside ? 3 : 0);

            if (score < smallestScore) {
                best = blockPos;
                smallestScore = score;
            }
        }

        return best;
    }
}
