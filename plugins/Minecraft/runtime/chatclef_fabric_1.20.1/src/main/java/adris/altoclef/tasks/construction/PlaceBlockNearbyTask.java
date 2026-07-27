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
    private final StateChangeLogger debugLogger = new StateChangeLogger("PlaceBlockNearbyTask");

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

        // Close screen first
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            debugLogger.state("cursor occupied while placing: cursor=" + describeStack(cursorStack));
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
        if (current != null && _canPlaceHere.test(current)) {
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
            setDebugState("Trying to place at " + tryPlace);
            debugLogger.state("try planned place: target=" + tryPlace.toShortString());
            justPlaced = tryPlace;
            return new PlaceBlockTask(tryPlace, toPlace);
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
        if (!mod.getExtraBaritoneSettings().isInteractionPaused() && blockEquipped()) {
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
            debugLogger.state("place click attempted: target=" + targetPlace.toShortString()
                    + ", result=" + result
                    + ", sneaking=" + mod.getPlayer().isSneaking()
                    + ", carryOnSafeSneak=" + avoidSneakRightClick);
            if (result == ActionResult.SUCCESS && (avoidSneakRightClick || mod.getPlayer().isSneaking())) {
                mod.getPlayer().swingHand(hand);
                justPlaced = targetPlace;
                Debug.logMessage("PRESSED");
                debugLogger.event("place click succeeded: target=" + targetPlace.toShortString());
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
