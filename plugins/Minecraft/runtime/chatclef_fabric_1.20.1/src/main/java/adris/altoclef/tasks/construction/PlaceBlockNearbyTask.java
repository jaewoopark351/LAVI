package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
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

    private final Block[] toPlace;

    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final TimeoutWanderTask wander = new TimeoutWanderTask(5);

    private final TimerGame _randomlookTimer = new TimerGame(0.25);
    private final Predicate<BlockPos> _canPlaceHere;
    private BlockPos justPlaced; // Where we JUST placed a block.
    private BlockPos tryPlace;   // Where we should TRY placing a block.
    // Oof, necesarry for the onBlockPlaced action.
    private Subscription<BlockPlaceEvent> _onBlockPlaced;

    public PlaceBlockNearbyTask(Predicate<BlockPos> canPlaceHere, Block... toPlace) {
        this.toPlace = toPlace;
        _canPlaceHere = canPlaceHere;
    }

    public PlaceBlockNearbyTask(Block... toPlace) {
        this(blockPos -> true, toPlace);
    }

    @Override
    protected void onStart() {
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "ON_START_BEGIN", "place_block_nearby_start", this,
                "toPlace", Arrays.toString(toPlace),
                "justPlaced", justPlaced,
                "tryPlace", tryPlace);
        progressChecker.reset();
        AltoClef.getInstance().getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
        ChatClefDiagnostics.logInput("RELEASED", "place_block_nearby_existing_right_click_force_false", Input.CLICK_RIGHT,
                "inputReleased", true,
                "toPlace", Arrays.toString(toPlace));

        // Check for blocks being placed
        _onBlockPlaced = EventBus.subscribe(BlockPlaceEvent.class, evt -> {
            if (ArrayUtils.contains(toPlace, evt.blockState.getBlock())) {
                ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "BLOCK_PLACE_EVENT", "matching_block_place_event", this,
                        "toPlace", Arrays.toString(toPlace),
                        "eventBlockPos", evt.blockPos,
                        "eventBlockState", evt.blockState,
                        "worldBlockStateAtEventPos", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getWorld().getBlockState(evt.blockPos)),
                        "justPlacedBefore", justPlaced,
                        "tryPlace", tryPlace);
                stopPlacing();
            }
        });
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "ON_START_END", "place_block_nearby_start", this,
                "toPlace", Arrays.toString(toPlace));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "ON_TICK_BEGIN", "place_block_nearby_tick_begin", this,
                "toPlace", Arrays.toString(toPlace),
                "justPlaced", justPlaced,
                "tryPlace", tryPlace,
                "blockEquipped", blockEquipped(),
                "pathing", mod.getClientBaritone().getPathingBehavior().isPathing(),
                "builderActive", mod.getClientBaritone().getBuilderProcess().isActive(),
                "crosshairTargetClass", ChatClefDiagnostics.className(MinecraftClient.getInstance().crosshairTarget),
                "mainHandItem", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getMainHandStack()));

        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "PATHING", "pathing_active_reset_progress_checker", this,
                    "toPlace", Arrays.toString(toPlace),
                    "justPlaced", justPlaced,
                    "tryPlace", tryPlace);
            progressChecker.reset();
        }
        // Method:
        // - If looking at placable block
        //      Place immediately
        // Find a spot to place
        // - Prefer flat areas (open space, block below) closest to player
        // -

        // Close screen first
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "CURSOR_CHECK", "before_close_screen_for_place", this,
                "cursorStack", cursorStack,
                "cursorEmpty", cursorStack.isEmpty());
        if (!cursorStack.isEmpty()) {
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
        } else {
            StorageHelper.closeScreen();
        }

        // Try placing where we're looking right now.
        BlockPos current = getCurrentlyLookingBlockPlace(mod);
        boolean canPlaceHere = current != null && _canPlaceHere.test(current);
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "CURRENT_LOOK_PLACE_CHECK", "current_look_place_candidate", this,
                "currentPlace", current,
                "canPlaceHere", canPlaceHere,
                "blockEquipped", blockEquipped(),
                "toPlace", Arrays.toString(toPlace));
        if (current != null && canPlaceHere) {
            setDebugState("Placing since we can...");
            boolean equipped = mod.getSlotHandler().forceEquipItem(ItemHelper.blocksToItems(toPlace));
            ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "EQUIP", "force_equip_before_direct_place", this,
                    "currentPlace", current,
                    "equipAccepted", equipped,
                    "toPlace", Arrays.toString(toPlace),
                    "mainHandItem", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getMainHandStack()));
            if (equipped) {
                boolean placed = place(mod, current);
                ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "DIRECT_PLACE_RETURN", "direct_place_result", this,
                        "currentPlace", current,
                        "placedReturn", placed,
                        "justPlaced", justPlaced,
                        "toPlace", Arrays.toString(toPlace));
                if (placed) {
                    return null;
                }
            }
        }

        // Wander while we can.
        if (wander.isActive() && !wander.isFinished()) {
            setDebugState("Wandering, will try to place again later.");
            progressChecker.reset();
            ChatClefDiagnostics.logTaskTransition(this, null, wander, "return_active_wander",
                    "toPlace", Arrays.toString(toPlace),
                    "justPlaced", justPlaced,
                    "tryPlace", tryPlace);
            return wander;
        }
        // Fail check
        if (!progressChecker.check(mod)) {
            Debug.logMessage("Failed placing, wandering and trying again.");
            ChatClefDiagnostics.logTaskTransition(this, null, wander, "return_wander_after_progress_failed",
                    "toPlace", Arrays.toString(toPlace),
                    "justPlaced", justPlaced,
                    "tryPlace", tryPlace);
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
            ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "LOCATE_CLOSE_PLACE", "locate_close_place_position", this,
                    "tryPlace", tryPlace,
                    "toPlace", Arrays.toString(toPlace));
        }
        if (tryPlace != null) {
            setDebugState("Trying to place at " + tryPlace);
            justPlaced = tryPlace;
            Task placeBlockTask = new PlaceBlockTask(tryPlace, toPlace);
            ChatClefDiagnostics.logTaskTransition(this, null, placeBlockTask, "return_place_block_task_for_try_place",
                    "tryPlace", tryPlace,
                    "justPlaced", justPlaced,
                    "toPlace", Arrays.toString(toPlace));
            return placeBlockTask;
        }

        // Look in random places to maybe get a random hit
        if (_randomlookTimer.elapsed()) {
            _randomlookTimer.reset();
            LookHelper.randomOrientation();
        }

        setDebugState("Wandering until we randomly place or find a good place spot.");
        Task timeoutWander = new TimeoutWanderTask();
        ChatClefDiagnostics.logTaskTransition(this, null, timeoutWander, "return_new_timeout_wander_no_place_found",
                "toPlace", Arrays.toString(toPlace),
                "justPlaced", justPlaced,
                "tryPlace", tryPlace);
        return timeoutWander;
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "place_block_nearby_onStop_begin",
                "toPlace", Arrays.toString(toPlace),
                "justPlaced", justPlaced,
                "tryPlace", tryPlace);
        stopPlacing();
        EventBus.unsubscribe(_onBlockPlaced);
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "place_block_nearby_onStop_end",
                "toPlace", Arrays.toString(toPlace),
                "justPlaced", justPlaced,
                "tryPlace", tryPlace);
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
        boolean finished = justPlaced != null && ArrayUtils.contains(toPlace, AltoClef.getInstance().getWorld().getBlockState(justPlaced).getBlock());
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "IS_FINISHED", "place_block_nearby_isFinished_check", this,
                "justPlaced", justPlaced,
                "justPlacedBlockState", justPlaced == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getWorld().getBlockState(justPlaced)),
                "toPlace", Arrays.toString(toPlace),
                "finished", finished);
        return finished;
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
            boolean canPlaceAgainst = MovementHelper.canPlaceAgainst(ctx, bpos);
            ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "LOOK_HIT_BLOCK", "looking_block_place_against_check", this,
                    "hitBlockPos", bpos,
                    "hitSide", bhit.getSide(),
                    "hitBlockState", ChatClefDiagnostics.safeValue(() -> MinecraftClient.getInstance().world.getBlockState(bpos)),
                    "canPlaceAgainst", canPlaceAgainst,
                    "toPlace", Arrays.toString(toPlace));
            if (canPlaceAgainst) {
                BlockPos placePos = bhit.getBlockPos().add(bhit.getSide().getVector());
                // Don't place inside the player.
                boolean insidePlayer = WorldHelper.isInsidePlayer(placePos);
                boolean canPlace = !insidePlayer && WorldHelper.canPlace(placePos);
                ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "LOOK_PLACE_POSITION", "looking_block_place_position_check", this,
                        "hitBlockPos", bpos,
                        "hitSide", bhit.getSide(),
                        "candidatePlacePos", placePos,
                        "insidePlayer", insidePlayer,
                        "canPlace", canPlace,
                        "candidateBlockState", ChatClefDiagnostics.safeValue(() -> MinecraftClient.getInstance().world.getBlockState(placePos)),
                        "toPlace", Arrays.toString(toPlace));
                if (insidePlayer) {
                    return null;
                }
                //Debug.logMessage("TEMP: B (actual): " + placePos);
                if (canPlace) {
                    return placePos;
                }
            }
        } else {
            ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "LOOK_HIT_NOT_BLOCK", "looking_target_not_block", this,
                    "hitClass", ChatClefDiagnostics.className(hit),
                    "hitType", hit == null ? "unavailable" : hit.getType(),
                    "toPlace", Arrays.toString(toPlace));
        }
        return null;
    }

    private boolean blockEquipped() {
        return StorageHelper.isEquipped(ItemHelper.blocksToItems(toPlace));
    }

    private boolean place(AltoClef mod, BlockPos targetPlace) {
        boolean interactionPaused = mod.getExtraBaritoneSettings().isInteractionPaused();
        boolean equipped = blockEquipped();
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "DIRECT_PLACE_ENTRY", "place_method_entry", this,
                "targetPlace", targetPlace,
                "interactionPaused", interactionPaused,
                "blockEquipped", equipped,
                "playerSneakingBefore", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isSneaking()),
                "mainHandItem", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getMainHandStack()),
                "toPlace", Arrays.toString(toPlace));
        if (!interactionPaused && equipped) {
            // Shift click just for 100% container security.
            ChatClefDiagnostics.logInput("REQUEST", "place_block_nearby_hold_sneak_before_direct_interact", Input.SNEAK,
                    "inputRequested", true,
                    "targetPlace", targetPlace,
                    "toPlace", Arrays.toString(toPlace),
                    "playerSneakingBefore", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isSneaking()));
            mod.getInputControls().hold(Input.SNEAK);
            ChatClefDiagnostics.logInput("HELD", "place_block_nearby_hold_sneak_after_direct_interact_prepare", Input.SNEAK,
                    "inputRequested", true,
                    "targetPlace", targetPlace,
                    "toPlace", Arrays.toString(toPlace),
                    "playerSneakingAfter", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isSneaking()));

            //mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            // This appears to work on servers...
            // TODO: Helper lol
            HitResult mouseOver = MinecraftClient.getInstance().crosshairTarget;
            if (mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK) {
                ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "DIRECT_PLACE_RETURN", "mouse_over_not_block_before_interact", this,
                        "targetPlace", targetPlace,
                        "mouseOverClass", ChatClefDiagnostics.className(mouseOver),
                        "mouseOverType", mouseOver == null ? "unavailable" : mouseOver.getType(),
                        "returnValue", false,
                        "toPlace", Arrays.toString(toPlace));
                return false;
            }
            Hand hand = Hand.MAIN_HAND;
            assert MinecraftClient.getInstance().interactionManager != null;
            BlockHitResult blockHit = (BlockHitResult) mouseOver;
            ChatClefDiagnostics.logInteractBlock("DIRECT_CALL_BEFORE", "place_block_nearby_direct_interact_before", hand, blockHit, "unavailable");
            ActionResult result = MinecraftClient.getInstance().interactionManager.interactBlock(mod.getPlayer(), hand, blockHit);
            boolean playerSneakingAfter = mod.getPlayer().isSneaking();
            ChatClefDiagnostics.logInteractBlock("DIRECT_CALL_AFTER", "place_block_nearby_direct_interact_after", hand, blockHit, result);
            ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "DIRECT_INTERACT_RESULT", "place_block_nearby_direct_interact_result", this,
                    "targetPlace", targetPlace,
                    "hitBlockPos", blockHit.getBlockPos(),
                    "hitSide", blockHit.getSide(),
                    "hitBlockState", ChatClefDiagnostics.safeValue(() -> MinecraftClient.getInstance().world.getBlockState(blockHit.getBlockPos())),
                    "interactionResult", result,
                    "playerSneakingAfter", playerSneakingAfter,
                    "toPlace", Arrays.toString(toPlace));
            if (result == ActionResult.SUCCESS &&
                    playerSneakingAfter) {
                mod.getPlayer().swingHand(hand);
                justPlaced = targetPlace;
                Debug.logMessage("PRESSED");
                ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "DIRECT_PLACE_SUCCESS", "direct_place_success_set_justPlaced", this,
                        "targetPlace", targetPlace,
                        "justPlaced", justPlaced,
                        "interactionResult", result,
                        "playerSneakingAfter", playerSneakingAfter,
                        "toPlace", Arrays.toString(toPlace));
                return true;
            }

            //mod.getControllerExtras().mouseClickOverride(1, true);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "DIRECT_PLACE_RETURN", "direct_interact_not_success_or_not_sneaking", this,
                    "targetPlace", targetPlace,
                    "interactionResult", result,
                    "playerSneakingAfter", playerSneakingAfter,
                    "returnValue", true,
                    "toPlace", Arrays.toString(toPlace));
            return true;
        }
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "DIRECT_PLACE_RETURN", "interaction_paused_or_block_not_equipped", this,
                "targetPlace", targetPlace,
                "interactionPaused", interactionPaused,
                "blockEquipped", equipped,
                "returnValue", false,
                "toPlace", Arrays.toString(toPlace));
        return false;
    }

    private void stopPlacing() {
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "STOP_PLACING_BEGIN", "stop_placing_begin", this,
                "toPlace", Arrays.toString(toPlace),
                "justPlaced", justPlaced,
                "tryPlace", tryPlace);
        AltoClef.getInstance().getInputControls().release(Input.SNEAK);
        ChatClefDiagnostics.logInput("RELEASED", "place_block_nearby_stop_placing_sneak_release", Input.SNEAK,
                "inputReleased", true,
                "toPlace", Arrays.toString(toPlace),
                "justPlaced", justPlaced,
                "tryPlace", tryPlace);
        //mod.getControllerExtras().mouseClickOverride(1, false);
        // Oof, these sometimes cause issues so this is a bit of a duct tape fix.
        AltoClef.getInstance().getClientBaritone().getBuilderProcess().onLostControl();
        ChatClefDiagnostics.logEvent("PLACE_BLOCK_NEARBY", "STOP_PLACING_END", "stop_placing_end", this,
                "toPlace", Arrays.toString(toPlace),
                "justPlaced", justPlaced,
                "tryPlace", tryPlace);
    }

    private BlockPos locateClosePlacePos(AltoClef mod) {
        int range = 7;
        BlockPos best = null;
        double smallestScore = Double.POSITIVE_INFINITY;
        BlockPos start = mod.getPlayer().getBlockPos().add(-range,-range,-range);
        BlockPos end = mod.getPlayer().getBlockPos().add(range,range,range);
        for (BlockPos blockPos : WorldHelper.scanRegion(start, end)) {
            boolean solid = WorldHelper.isSolidBlock(blockPos);
            boolean inside = WorldHelper.isInsidePlayer(blockPos);
            // We can't break this block.
            if (solid && !WorldHelper.canBreak(blockPos)) {
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
