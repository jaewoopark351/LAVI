package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.baritone.GoalAnd;
import adris.altoclef.util.baritone.GoalBlockSide;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.*;
import adris.altoclef.multiversion.versionedfields.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.Objects;
import java.util.Optional;

/**
 * Left or Right click on a block on a particular (or any) side of the block.
 */
public class InteractWithBlockTask extends Task {
    private final MovementProgressChecker moveChecker = new MovementProgressChecker();
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final ItemTarget toUse;
    private final Direction direction;
    private final BlockPos target;
    private final boolean walkInto;
    private final Vec3i interactOffset;
    private final Input interactInput;
    private final boolean shiftClick;
    private final TimerGame clickTimer = new TimerGame(5);
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(5, true);
    Block[] annoyingBlocks = new Block[]{
            Blocks.VINE,
            Blocks.NETHER_SPROUTS,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.LADDER,
            Blocks.BIG_DRIPLEAF,
            Blocks.BIG_DRIPLEAF_STEM,
            Blocks.SMALL_DRIPLEAF,
            Blocks.TALL_GRASS,
            Blocks.SHORT_GRASS,
            Blocks.SWEET_BERRY_BUSH
    };
    private Task unstuckTask = null;
    private ClickResponse cachedClickStatus = ClickResponse.CANT_REACH;
    private int waitingForClickTicks = 0;

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, Vec3i interactOffset, boolean shiftClick) {
        this.toUse = toUse;
        this.direction = direction;
        this.target = target;
        this.interactInput = interactInput;
        this.walkInto = walkInto;
        this.interactOffset = interactOffset;
        this.shiftClick = shiftClick;
    }

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, boolean shiftClick) {
        this(toUse, direction, target, interactInput, walkInto, Vec3i.ZERO, shiftClick);
    }

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, boolean walkInto) {
        this(toUse, direction, target, Input.CLICK_RIGHT, walkInto, true);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target, boolean walkInto, Vec3i interactOffset) {
        // null means any side is OK
        this(toUse, null, target, Input.CLICK_RIGHT, walkInto, interactOffset, true);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target, boolean walkInto) {
        this(toUse, target, walkInto, Vec3i.ZERO);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target) {
        this(toUse, target, false);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, Vec3i interactOffset, boolean shiftClick) {
        this(new ItemTarget(toUse, 1), direction, target, interactInput, walkInto, interactOffset, shiftClick);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, boolean shiftClick) {
        this(new ItemTarget(toUse, 1), direction, target, interactInput, walkInto, shiftClick);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target, boolean walkInto) {
        this(new ItemTarget(toUse, 1), direction, target, walkInto);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target) {
        this(new ItemTarget(toUse, 1), direction, target, Input.CLICK_RIGHT, false, false);
    }

    public InteractWithBlockTask(Item toUse, BlockPos target, boolean walkInto, Vec3i interactOffset) {
        this(new ItemTarget(toUse, 1), target, walkInto, interactOffset);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target, Vec3i interactOffset) {
        this(new ItemTarget(toUse, 1), direction, target, Input.CLICK_RIGHT, false, interactOffset, false);
    }

    public InteractWithBlockTask(Item toUse, BlockPos target, Vec3i interactOffset) {
        this(new ItemTarget(toUse, 1), null, target, Input.CLICK_RIGHT, false, interactOffset, false);
    }

    public InteractWithBlockTask(Item toUse, BlockPos target, boolean walkInto) {
        this(new ItemTarget(toUse, 1), target, walkInto);
    }

    public InteractWithBlockTask(Item toUse, BlockPos target) {
        this(new ItemTarget(toUse, 1), target);
    }

    public InteractWithBlockTask(BlockPos target, boolean shiftClick) {
        this(ItemTarget.EMPTY, null, target, Input.CLICK_RIGHT, false, shiftClick);
    }

    public InteractWithBlockTask(BlockPos target) {
        this(ItemTarget.EMPTY, null, target, Input.CLICK_RIGHT, false, false);
    }

    private static BlockPos[] generateSides(BlockPos pos) {
        return new BlockPos[]{
                pos.add(1,0,0),
                pos.add(-1,0,0),
                pos.add(0,0,1),
                pos.add(0,0,-1),
                pos.add(1,0,-1),
                pos.add(1,0,1),
                pos.add(-1,0,-1),
                pos.add(-1,0,1)
        };
    }

    private static Goal createGoalForInteract(BlockPos target, int reachDistance, Direction interactSide, Vec3i interactOffset, boolean walkInto) {

        boolean sideMatters = interactSide != null;
        if (sideMatters) {
            Vec3i offs = interactSide.getVector();
            if (offs.getY() == -1) {
                // If we're below, place ourselves two blocks below.
                offs = offs.down();
            }
            target = target.add(offs);
        }

        if (walkInto) {
            return new GoalTwoBlocks(target);
        } else {
            if (sideMatters) {
                // Make sure we're on the right side of the block.
                Goal sideGoal = new GoalBlockSide(target, interactSide, 1);
                return new GoalAnd(sideGoal, new GoalNear(target.add(interactOffset), reachDistance));
            } else {
                // TODO: Cleaner method of picking which side to approach from. This is only here for the lava stuff.
                return new GoalTwoBlocks(target.up());
                //return new GoalNear(target.add(interactOffset), reachDistance);
            }
        }
    }

    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        if (annoyingBlocks != null) {
            for (Block AnnoyingBlocks : annoyingBlocks) {
                return mod.getWorld().getBlockState(pos).getBlock() == AnnoyingBlocks ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
            }
        }
        return false;
    }

    // This happens all the time in mineshafts and swamps/jungles
    private BlockPos stuckInBlock(AltoClef mod) {
        BlockPos p = mod.getPlayer().getBlockPos();
        if (isAnnoying(mod, p)) return p;
        if (isAnnoying(mod, p.up())) return p.up();
        BlockPos[] toCheck = generateSides(p);
        for (BlockPos check : toCheck) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        BlockPos[] toCheckHigh = generateSides(p.up());
        for (BlockPos check : toCheckHigh) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        return null;
    }

    private Task getFenceUnstuckTask() {
        return new SafeRandomShimmyTask();
    }

    @Override
    protected void onStart() {
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "ON_START_BEGIN", "interact_block_start", this,
                "targetPosition", target,
                "direction", direction,
                "interactInput", interactInput,
                "shiftClick", shiftClick,
                "walkInto", walkInto,
                "toUse", toUse);
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
        ChatClefDiagnostics.logEvent("BARITONE", "FORCE_CANCEL", "interact_block_onStart_existing_forceCancel", this,
                "targetPosition", target);

        moveChecker.reset();
        stuckCheck.reset();
        wanderTask.resetWander();
        clickTimer.reset();
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "ON_START_END", "interact_block_start", this,
                "targetPosition", target);
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "ON_TICK_BEGIN", "interact_block_tick_begin", this,
                "targetPosition", target,
                "cachedClickStatus", cachedClickStatus,
                "waitingForClickTicks", waitingForClickTicks);

        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            ChatClefDiagnostics.logEvent("BARITONE", "PATHING", "pathing_active_reset_move_checker", this,
                    "targetPosition", target);
            moveChecker.reset();
        }
        boolean inNetherPortal = WorldHelper.isInNetherPortal();
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "PORTAL_CHECK", "nether_portal_check", this,
                "targetPosition", target,
                "inNetherPortal", inNetherPortal,
                "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()));
        if (inNetherPortal) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                ChatClefDiagnostics.logInput("REQUEST", "nether_portal_hold_sneak", Input.SNEAK,
                        "inputRequested", true,
                        "targetPosition", target);
                mod.getInputControls().hold(Input.SNEAK);
                ChatClefDiagnostics.logInput("REQUEST", "nether_portal_hold_move_forward", Input.MOVE_FORWARD,
                        "inputRequested", true,
                        "targetPosition", target);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RETURN", "return_getting_out_of_nether_portal", this,
                        "targetPosition", target);
                return null;
            } else {
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "nether_portal_pathing_release_sneak", Input.SNEAK,
                        "inputReleased", true,
                        "targetPosition", target);
                mod.getInputControls().release(Input.SNEAK);
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "nether_portal_pathing_release_move_back", Input.MOVE_BACK,
                        "inputReleased", true,
                        "targetPosition", target);
                mod.getInputControls().release(Input.MOVE_BACK);
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "nether_portal_pathing_release_move_forward", Input.MOVE_FORWARD,
                        "inputReleased", true,
                        "targetPosition", target);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "pathing_release_sneak", Input.SNEAK,
                        "inputReleased", true,
                        "targetPosition", target);
                mod.getInputControls().release(Input.SNEAK);
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "pathing_release_move_back", Input.MOVE_BACK,
                        "inputReleased", true,
                        "targetPosition", target);
                mod.getInputControls().release(Input.MOVE_BACK);
                ChatClefDiagnostics.logInput("RELEASE_REQUEST", "pathing_release_move_forward", Input.MOVE_FORWARD,
                        "inputReleased", true,
                        "targetPosition", target);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        }
        if (unstuckTask != null && unstuckTask.isActive() && !unstuckTask.isFinished() && stuckInBlock(mod) != null) {
            setDebugState("Getting unstuck from block.");
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            ChatClefDiagnostics.logEvent("BARITONE", "ON_LOST_CONTROL", "unstuck_custom_goal_onLostControl", this,
                    "targetPosition", target,
                    "unstuckTask", unstuckTask);
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            ChatClefDiagnostics.logEvent("BARITONE", "ON_LOST_CONTROL", "unstuck_explore_onLostControl", this,
                    "targetPosition", target,
                    "unstuckTask", unstuckTask);
            mod.getClientBaritone().getExploreProcess().onLostControl();
            ChatClefDiagnostics.logTaskTransition(this, null, unstuckTask, "return_active_unstuck_task",
                    "targetPosition", target);
            return unstuckTask;
        }
        boolean moveCheckPassed = moveChecker.check(mod);
        boolean stuckCheckPassed = true;
        if (moveCheckPassed) {
            stuckCheckPassed = stuckCheck.check(mod);
        }
        if (!moveCheckPassed || !stuckCheckPassed) {
            ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "PROGRESS_CHECK_FAILED", "movement_or_stuck_check_failed", this,
                    "targetPosition", target,
                    "moveCheckPassed", moveCheckPassed,
                    "stuckCheckEvaluated", moveCheckPassed,
                    "stuckCheckPassed", stuckCheckPassed);
            BlockPos blockStuck = stuckInBlock(mod);
            if (blockStuck != null) {
                unstuckTask = getFenceUnstuckTask();
                ChatClefDiagnostics.logTaskTransition(this, null, unstuckTask, "return_new_unstuck_task",
                        "targetPosition", target,
                        "blockStuck", blockStuck);
                return unstuckTask;
            }
            stuckCheck.reset();
        }

        cachedClickStatus = ClickResponse.CANT_REACH;

        // Get our use item first
        if (!ItemTarget.nullOrEmpty(toUse) && !StorageHelper.itemTargetsMet(mod, toUse)) {
            moveChecker.reset();
            clickTimer.reset();
            ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RETURN", "return_get_required_item_task", this,
                    "targetPosition", target,
                    "toUse", toUse);
            return TaskCatalogue.getItemTask(toUse);
        }

        // Wander and check
        if (wanderTask.isActive() && !wanderTask.isFinished()) {
            moveChecker.reset();
            clickTimer.reset();
            ChatClefDiagnostics.logTaskTransition(this, null, wanderTask, "return_active_wander_task",
                    "targetPosition", target);
            return wanderTask;
        }
        if (!moveChecker.check(mod)) {
            Debug.logMessage("Failed, blacklisting and wandering.");
            ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "BLACKLIST", "move_checker_failed_request_unreachable", this,
                    "targetPosition", target);
            mod.getBlockScanner().requestBlockUnreachable(target);
            ChatClefDiagnostics.logTaskTransition(this, null, wanderTask, "return_wander_after_blacklist",
                    "targetPosition", target);
            return wanderTask;
        }

        int reachDistance = 0;
        Goal moveGoal = createGoalForInteract(target, reachDistance, direction, interactOffset, walkInto);
        ICustomGoalProcess proc = mod.getClientBaritone().getCustomGoalProcess();

        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_BEGIN", "before_rightClick", this,
                "targetPosition", target,
                "interactInput", interactInput,
                "shiftClick", shiftClick,
                "customGoalActive", ChatClefDiagnostics.safeValue(proc::isActive));
        cachedClickStatus = rightClick(mod);
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_END", "after_rightClick", this,
                "targetPosition", target,
                "cachedClickStatus", cachedClickStatus,
                "customGoalActive", ChatClefDiagnostics.safeValue(proc::isActive));
        switch (Objects.requireNonNull(cachedClickStatus)) {
            case CANT_REACH -> {
                setDebugState("Getting to our goal");
                // Get to our goal then
                if (!proc.isActive()) {
                    ChatClefDiagnostics.logEvent("BARITONE", "SET_GOAL_AND_PATH", "interact_cant_reach_setGoalAndPath", this,
                            "targetPosition", target,
                            "moveGoal", moveGoal);
                    proc.setGoalAndPath(moveGoal);
                }
                clickTimer.reset();
            }
            case WAIT_FOR_CLICK -> {
                setDebugState("Waiting for click");
                if (proc.isActive()) {
                    ChatClefDiagnostics.logEvent("BARITONE", "ON_LOST_CONTROL", "wait_for_click_custom_goal_onLostControl", this,
                            "targetPosition", target);
                    proc.onLostControl();
                }
                clickTimer.reset();

                // try to get unstuck by pressing shift
                waitingForClickTicks++;
                if (waitingForClickTicks % 25 == 0 && shiftClick) {
                    ChatClefDiagnostics.logInput("REQUEST", "wait_for_click_periodic_shift_hold", Input.SNEAK,
                            "inputRequested", true,
                            "targetPosition", target,
                            "waitingForClickTicks", waitingForClickTicks);
                    mod.getInputControls().hold(Input.SNEAK);
                    mod.log("trying to press shift");
                }

                if (waitingForClickTicks > 10*20) {
                    mod.log("trying to wander");
                    waitingForClickTicks = 0;
                    ChatClefDiagnostics.logTaskTransition(this, null, wanderTask, "return_wander_after_wait_for_click_limit",
                            "targetPosition", target);
                    return wanderTask;
                }
            }
            case CLICK_ATTEMPTED -> {
                setDebugState("Clicking.");
                if (proc.isActive()) {
                    ChatClefDiagnostics.logEvent("BARITONE", "ON_LOST_CONTROL", "click_attempted_custom_goal_onLostControl", this,
                            "targetPosition", target);
                    proc.onLostControl();
                }
                if (clickTimer.elapsed()) {
                    // We tried clicking but failed.
                    clickTimer.reset();
                    ChatClefDiagnostics.logTaskTransition(this, null, wanderTask, "return_wander_after_click_timer_elapsed",
                            "targetPosition", target,
                            "cachedClickStatus", cachedClickStatus);
                    return wanderTask;
                }
            }
        }

        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RETURN", "return_null_after_interact_tick", this,
                "targetPosition", target,
                "cachedClickStatus", cachedClickStatus);
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();

        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "interact_block_onStop_begin",
                "targetPosition", target,
                "cachedClickStatus", cachedClickStatus);
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        ChatClefDiagnostics.logEvent("BARITONE", "FORCE_CANCEL", "interact_block_onStop_existing_forceCancel", this,
                "targetPosition", target);
        mod.getInputControls().release(Input.SNEAK);
        ChatClefDiagnostics.logInput("RELEASED", "interact_block_onStop_existing_sneak_release", Input.SNEAK,
                "inputReleased", true,
                "targetPosition", target);
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "interact_block_onStop_end",
                "targetPosition", target,
                "cachedClickStatus", cachedClickStatus);
    }

    @Override
    public boolean isFinished() {
        return false;
        //return _trying && !proc(mod).isActive();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof InteractWithBlockTask task) {
            if ((task.direction == null) != (direction == null)) return false;
            if (task.direction != null && !task.direction.equals(direction)) return false;
            if ((task.toUse == null) != (toUse == null)) return false;
            if (task.toUse != null && !task.toUse.equals(toUse)) return false;
            if (!task.target.equals(target)) return false;
            if (!task.interactInput.equals(interactInput)) return false;
            return task.walkInto == walkInto;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Interact using " + toUse + " at " + target + " dir " + direction;
    }

    public ClickResponse getClickStatus() {
        return cachedClickStatus;
    }

    private ClickResponse rightClick(AltoClef mod) {
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_ENTRY", "rightClick_entry", this,
                "targetPosition", target,
                "interactInput", interactInput,
                "shiftClick", shiftClick);

        // Don't interact if baritone can't interact.
        if (mod.getExtraBaritoneSettings().isInteractionPaused()) {
            ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "interaction_paused", this,
                    "targetPosition", target,
                    "clickResponse", ClickResponse.WAIT_FOR_CLICK);
            return ClickResponse.WAIT_FOR_CLICK;
        }
        if (mod.getFoodChain().needsToEat()) {
            ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "needs_to_eat", this,
                    "targetPosition", target,
                    "clickResponse", ClickResponse.WAIT_FOR_CLICK);
            return ClickResponse.WAIT_FOR_CLICK;
        }
        if (mod.getPlayer().isBlocking()) {
            ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "player_blocking", this,
                    "targetPosition", target,
                    "clickResponse", ClickResponse.WAIT_FOR_CLICK);
            return ClickResponse.WAIT_FOR_CLICK;
        }

        // We can't interact while a screen is open.
        boolean playerInventoryOpen = StorageHelper.isPlayerInventoryOpen();
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "SCREEN_CHECK", "rightClick_screen_check", this,
                "targetPosition", target,
                "playerInventoryOpen", playerInventoryOpen);
        if (!playerInventoryOpen) {
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (!cursorStack.isEmpty()) {
                Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "cursor_stack_move_to_inventory", this,
                            "targetPosition", target,
                            "cursorStack", cursorStack,
                            "slot", moveTo.get(),
                            "clickResponse", ClickResponse.WAIT_FOR_CLICK);
                    mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                    return ClickResponse.WAIT_FOR_CLICK;
                }
                if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                    ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "cursor_stack_throwaway", this,
                            "targetPosition", target,
                            "cursorStack", cursorStack,
                            "clickResponse", ClickResponse.WAIT_FOR_CLICK);
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return ClickResponse.WAIT_FOR_CLICK;
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                // Try throwing away cursor slot if it's garbage
                if (garbage.isPresent()) {
                    ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "cursor_stack_to_garbage_slot", this,
                            "targetPosition", target,
                            "cursorStack", cursorStack,
                            "slot", garbage.get(),
                            "clickResponse", ClickResponse.WAIT_FOR_CLICK);
                    mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                    return ClickResponse.WAIT_FOR_CLICK;
                }
                ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "cursor_stack_pickup_undefined", this,
                        "targetPosition", target,
                        "cursorStack", cursorStack,
                        "clickResponse", ClickResponse.WAIT_FOR_CLICK);
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return ClickResponse.WAIT_FOR_CLICK;
            } else {
                ChatClefDiagnostics.logEvent("SCREEN", "CLOSE_REQUEST", "rightClick_close_open_screen", this,
                        "targetPosition", target);
                StorageHelper.closeScreen();
            }
        }

        Optional<Rotation> reachable = getCurrentReach();
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "REACH_CHECK", "rightClick_reach_check", this,
                "targetPosition", target,
                "reachable", reachable.isPresent(),
                "rotation", reachable.map(Object::toString).orElse("unavailable"));
        if (reachable.isPresent()) {
            if (LookHelper.isLookingAt(mod, target)) {
                ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "LOOK_CHECK", "rightClick_looking_at_target", this,
                        "targetPosition", target,
                        "lookingAtTarget", true);
                if (toUse != null) {
                    ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "EQUIP", "force_equip_item_before_click", this,
                            "targetPosition", target,
                            "toUse", toUse);
                    mod.getSlotHandler().forceEquipItem(toUse, false);
                } else {
                    ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "EQUIP", "force_deequip_right_clickable_before_click", this,
                            "targetPosition", target);
                    mod.getSlotHandler().forceDeequipRightClickableItem();
                }
                ChatClefDiagnostics.logInput("REQUEST", "rightClick_tryPress_before", interactInput,
                        "inputRequested", true,
                        "targetPosition", target,
                        "shiftClick", shiftClick);
                mod.getInputControls().tryPress(interactInput);
                boolean interactHeld = mod.getInputControls().isHeldDown(interactInput);
                ChatClefDiagnostics.logInput(interactHeld ? "HELD" : "NOT_HELD", "rightClick_tryPress_after", interactInput,
                        "inputRequested", true,
                        "inputAccepted", interactHeld,
                        "inputHeldAfter", interactHeld,
                        "targetPosition", target,
                        "shiftClick", shiftClick);
                if (interactHeld) {
                    if (shiftClick) {
                        ChatClefDiagnostics.logInput("REQUEST", "rightClick_shift_hold_before_click_attempted", Input.SNEAK,
                                "inputRequested", true,
                                "targetPosition", target);
                        mod.getInputControls().hold(Input.SNEAK);
                    }
                    ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "click_attempted", this,
                            "targetPosition", target,
                            "clickResponse", ClickResponse.CLICK_ATTEMPTED);
                    return ClickResponse.CLICK_ATTEMPTED;
                }
                //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(_interactInput, true);
            } else {
                ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "LOOK_CHECK", "rightClick_not_looking_at_target", this,
                        "targetPosition", target,
                        "lookingAtTarget", false,
                        "rotation", reachable.get());
                LookHelper.lookAt(reachable.get());
            }
            ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "wait_for_click_after_reach", this,
                    "targetPosition", target,
                    "clickResponse", ClickResponse.WAIT_FOR_CLICK);
            return ClickResponse.WAIT_FOR_CLICK;
        }
        if (shiftClick) {
            ChatClefDiagnostics.logInput("RELEASE_REQUEST", "rightClick_cant_reach_shift_release", Input.SNEAK,
                    "inputReleased", true,
                    "targetPosition", target);
            mod.getInputControls().release(Input.SNEAK);
        }
        ChatClefDiagnostics.logEvent("INTERACT_BLOCK", "RIGHT_CLICK_RETURN", "cant_reach", this,
                "targetPosition", target,
                "clickResponse", ClickResponse.CANT_REACH);
        return ClickResponse.CANT_REACH;
    }

    public Optional<Rotation> getCurrentReach() {
        return LookHelper.getReach(target, direction);
    }

    public enum ClickResponse {
        CANT_REACH,
        WAIT_FOR_CLICK,
        CLICK_ATTEMPTED
    }
}
