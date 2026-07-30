package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

// TODO improve wandering
/**
 * Call this when the place you're currently at is bad for some reason and you just wanna get away.
 */
public class TimeoutWanderTask extends Task implements ITaskRequiresGrounded {
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final float distanceToWander;
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final boolean increaseRange;
    private final TimerGame timer = new TimerGame(60);
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
    private Vec3d origin;
    //private DistanceProgressChecker _distanceProgressChecker = new DistanceProgressChecker(10, 0.1f);
    private boolean _forceExplore;
    private Task _unstuckTask = null;
    private int failCounter;
    private double _wanderDistanceExtension;

    public TimeoutWanderTask(float distanceToWander, boolean increaseRange) {
        this.distanceToWander = distanceToWander;
        this.increaseRange = increaseRange;
        _forceExplore = false;
    }

    public TimeoutWanderTask(float distanceToWander) {
        this(distanceToWander, false);
    }

    public TimeoutWanderTask() {
        this(Float.POSITIVE_INFINITY, false);
    }

    public TimeoutWanderTask(boolean forceExplore) {
        this();
        _forceExplore = forceExplore;
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

    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        for (Block AnnoyingBlocks : annoyingBlocks) {
            return mod.getWorld().getBlockState(pos).getBlock() == AnnoyingBlocks ||
                    mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock ||
                    mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock ||
                    mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock ||
                    mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
        }
        return false;
    }

    public void resetWander() {
        _wanderDistanceExtension = 0;
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
        AltoClef mod = AltoClef.getInstance();

        //20260730_kpopmodder: Diagnostics-only LAVI log for wander fallback loop investigation; no behavior change.
        ChatClefDiagnostics.logEvent("WANDER", "ON_START", "timeout_wander_start", this,
                "distanceToWander", distanceToWander,
                "increaseRange", increaseRange,
                "forceExplore", _forceExplore,
                "wanderDistanceExtension", _wanderDistanceExtension,
                "pathingBeforeForceCancel", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()));
        timer.reset();
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        origin = mod.getPlayer().getPos();
        progressChecker.reset();
        stuckCheck.reset();
        failCounter = 0;
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }
        ChatClefDiagnostics.logEvent("WANDER", "ON_START", "timeout_wander_start_after_setup", this,
                "origin", ChatClefDiagnostics.vec3d(origin),
                "cursorStack", ChatClefDiagnostics.itemStackSummary(cursorStack),
                "pathingAfterForceCancel", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();


        boolean pathing = mod.getClientBaritone().getPathingBehavior().isPathing();
        ChatClefDiagnostics.logEvent("WANDER", "ON_TICK", "timeout_wander_tick_begin", this,
                "origin", ChatClefDiagnostics.vec3d(origin),
                "playerPosition", ChatClefDiagnostics.playerPosition(mod),
                "distanceToWander", distanceToWander,
                "wanderDistanceExtension", _wanderDistanceExtension,
                "failCounter", failCounter,
                "pathing", pathing,
                "exploreActive", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getExploreProcess().isActive()),
                "unstuckTask", ChatClefDiagnostics.taskSummary(_unstuckTask));

        if (pathing) {
            ChatClefDiagnostics.logEvent("WANDER", "OBSERVE", "pathing_active_resets_progress", this);
            progressChecker.reset();
        }
        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                ChatClefDiagnostics.logEvent("WANDER", "DECISION", "nether_portal_manual_move", this);
                setDebugState("Getting out from nether portal");
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                ChatClefDiagnostics.logEvent("WANDER", "DECISION", "nether_portal_release_manual_move", this);
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                ChatClefDiagnostics.logEvent("WANDER", "DECISION", "pathing_release_manual_move", this);
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        }
        BlockPos activeUnstuckBlock = null;
        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished()) {
            activeUnstuckBlock = stuckInBlock(mod);
        }
        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished() && activeUnstuckBlock != null) {
            setDebugState("Getting unstuck from block.");
            ChatClefDiagnostics.logEvent("WANDER", "DECISION", "continue_unstuck_task", this,
                    "stuckBlock", ChatClefDiagnostics.blockPos(activeUnstuckBlock),
                    "unstuckTask", ChatClefDiagnostics.taskSummary(_unstuckTask));
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return _unstuckTask;
        }
        boolean progressOk = progressChecker.check(mod);
        boolean stuckOk = true;
        if (progressOk) {
            stuckOk = stuckCheck.check(mod);
        }
        if (!progressOk || !stuckOk) {
            List<Entity> closeEntities = mod.getEntityTracker().getCloseEntities();
            ChatClefDiagnostics.logEvent("WANDER", "OBSERVE", "wander_progress_failed", this,
                    "progressOk", progressOk,
                    "stuckOk", stuckOk,
                    "closeEntityCount", closeEntities.size(),
                    "playerPosition", ChatClefDiagnostics.playerPosition(mod));
            for (Entity CloseEntities : closeEntities) {
                if (CloseEntities instanceof MobEntity &&
                        CloseEntities.getPos().isInRange(mod.getPlayer().getPos(), 1)) {
                    setDebugState("Killing annoying entity.");
                    ChatClefDiagnostics.logEvent("WANDER", "DECISION", "kill_close_annoying_entity", this,
                            "entity", ChatClefDiagnostics.entitySummary(CloseEntities));
                    return new KillEntitiesTask(CloseEntities.getClass());
                }
            }
            BlockPos blockStuck = stuckInBlock(mod);
            if (blockStuck != null) {
                failCounter++;
                _unstuckTask = getFenceUnstuckTask();
                ChatClefDiagnostics.logEvent("WANDER", "DECISION", "start_unstuck_task", this,
                        "stuckBlock", ChatClefDiagnostics.blockPos(blockStuck),
                        "failCounter", failCounter,
                        "unstuckTask", ChatClefDiagnostics.taskSummary(_unstuckTask));
                return _unstuckTask;
            }
            stuckCheck.reset();
        }
        setDebugState("Exploring.");
        ChatClefDiagnostics.logEvent("WANDER", "DECISION", "explore_tick", this,
                "dimension", ChatClefDiagnostics.safeValue(WorldHelper::getCurrentDimension),
                "exploreActive", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getExploreProcess().isActive()));
        switch (WorldHelper.getCurrentDimension()) {
            case END -> {
                double timerDuration = timer.getDuration();
                if (timerDuration >= 30) {
                    ChatClefDiagnostics.logEvent("WANDER", "DECISION", "reset_timer_end_dimension", this,
                            "timerDuration", timerDuration);
                    timer.reset();
                }
            }
            case OVERWORLD, NETHER -> {
                double timerDuration = timer.getDuration();
                if (timerDuration >= 30) {
                }
                if (timer.elapsed()) {
                    ChatClefDiagnostics.logEvent("WANDER", "DECISION", "reset_elapsed_timer", this,
                            "dimension", WorldHelper.getCurrentDimension(),
                            "timerDuration", timerDuration);
                    timer.reset();
                }
            }
        }
        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
            ChatClefDiagnostics.logEvent("WANDER", "DECISION", "start_explore_process", this,
                    "origin", ChatClefDiagnostics.vec3d(origin));
            mod.getClientBaritone().getExploreProcess().explore((int) origin.getX(), (int) origin.getZ());
        }
        boolean progressOkAfterExplore = progressChecker.check(mod);
        ChatClefDiagnostics.logEvent("WANDER", "OBSERVE", "post_explore_progress_check", this,
                "progressOk", progressOkAfterExplore,
                "forceExplore", _forceExplore,
                "failCounter", failCounter);
        if (!progressOkAfterExplore) {
            progressChecker.reset();
            if (!_forceExplore) {
                failCounter++;
                Debug.logMessage("Failed exploring.");
                ChatClefDiagnostics.logEvent("WANDER", "DECISION", "failed_exploring_increment", this,
                        "failCounter", failCounter);
            }
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logEvent("WANDER", "ON_STOP", "timeout_wander_stop_begin", this,
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask),
                "failCounter", failCounter,
                "pathingBeforeForceCancel", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getClientBaritone().getPathingBehavior().isPathing()));
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
        boolean finished = isFinished();
        ChatClefDiagnostics.logEvent("WANDER", "ON_STOP", "timeout_wander_stop_after_force_cancel", this,
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask),
                "finished", finished,
                "failCounter", failCounter,
                "pathingAfterForceCancel", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getClientBaritone().getPathingBehavior().isPathing()));
        if (finished) {
            if (increaseRange) {
                _wanderDistanceExtension += distanceToWander;
                Debug.logMessage("Increased wander range");
                ChatClefDiagnostics.logEvent("WANDER", "DECISION", "increase_wander_range", this,
                        "distanceToWander", distanceToWander,
                        "wanderDistanceExtension", _wanderDistanceExtension);
            }
        }
    }

    @Override
    public boolean isFinished() {
        // Why the heck did I add this in?
        //if (_origin == null) return true;

        if (Float.isInfinite(distanceToWander)) return false;

        // If we fail 10 times or more, we may as well try the previous task again.
        if (failCounter > 10) {
            return true;
        }

        ClientPlayerEntity player = AltoClef.getInstance().getPlayer();

        if (player != null && player.getPos() != null && (player.isOnGround() ||
                player.isTouchingWater())) {
            double sqDist = player.getPos().squaredDistanceTo(origin);
            double toWander = distanceToWander + _wanderDistanceExtension;
            return sqDist > toWander * toWander;
        } else {
            return false;
        }
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof TimeoutWanderTask task) {
            if (Float.isInfinite(task.distanceToWander) || Float.isInfinite(distanceToWander)) {
                return Float.isInfinite(task.distanceToWander) == Float.isInfinite(distanceToWander);
            }
            return Math.abs(task.distanceToWander - distanceToWander) < 0.5f;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Wander for " + (distanceToWander + _wanderDistanceExtension) + " blocks";
    }
}
