package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalFollowEntity;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.input.Input;
import net.minecraft.block.*;
import adris.altoclef.multiversion.versionedfields.Blocks;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class GetToEntityTask extends Task implements ITaskRequiresGrounded {
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker _progress = new MovementProgressChecker();
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(5);
    private final Entity _entity;
    private final double _closeEnoughDistance;
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
    private Task _unstuckTask = null;

    public GetToEntityTask(Entity entity, double closeEnoughDistance) {
        _entity = entity;
        _closeEnoughDistance = closeEnoughDistance;
    }

    public GetToEntityTask(Entity entity) {
        this(entity, 1);
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
        //20260730_kpopmodder: Diagnostics-only LAVI log for entity pathing loop investigation; no behavior change.
        ChatClefDiagnostics.logEvent("ENTITY_PATH", "ON_START", "get_to_entity_start", this,
                "entity", ChatClefDiagnostics.entitySummary(_entity),
                "closeEnoughDistance", _closeEnoughDistance,
                "playerPosition", ChatClefDiagnostics.playerPosition(AltoClef.getInstance()),
                "pathingBeforeForceCancel", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getClientBaritone().getPathingBehavior().isPathing()),
                "customGoalActiveBefore", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getClientBaritone().getCustomGoalProcess().isActive()));
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
        _progress.reset();
        stuckCheck.reset();
        _wanderTask.resetWander();
        ChatClefDiagnostics.logEvent("ENTITY_PATH", "ON_START", "get_to_entity_start_after_reset", this,
                "entity", ChatClefDiagnostics.entitySummary(_entity),
                "pathingAfterForceCancel", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getClientBaritone().getPathingBehavior().isPathing()));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        boolean pathing = mod.getClientBaritone().getPathingBehavior().isPathing();
        ChatClefDiagnostics.logEvent("ENTITY_PATH", "ON_TICK", "get_to_entity_tick_begin", this,
                "entity", ChatClefDiagnostics.entitySummary(_entity),
                "entityDistanceSqr", ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, _entity),
                "closeEnoughDistance", _closeEnoughDistance,
                "playerPosition", ChatClefDiagnostics.playerPosition(mod),
                "pathing", pathing,
                "customGoalActive", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getCustomGoalProcess().isActive()),
                "wanderTask", ChatClefDiagnostics.taskSummary(_wanderTask),
                "unstuckTask", ChatClefDiagnostics.taskSummary(_unstuckTask));

        if (pathing) {
            ChatClefDiagnostics.logEvent("ENTITY_PATH", "OBSERVE", "pathing_active_resets_progress", this,
                    "entity", ChatClefDiagnostics.entitySummary(_entity));
            _progress.reset();
        }
        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "nether_portal_manual_move", this,
                        "entity", ChatClefDiagnostics.entitySummary(_entity));
                setDebugState("Getting out from nether portal");
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "nether_portal_release_manual_move", this,
                        "entity", ChatClefDiagnostics.entitySummary(_entity));
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "pathing_release_manual_move", this,
                        "entity", ChatClefDiagnostics.entitySummary(_entity));
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
            ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "continue_unstuck_task", this,
                    "entity", ChatClefDiagnostics.entitySummary(_entity),
                    "stuckBlock", ChatClefDiagnostics.blockPos(activeUnstuckBlock),
                    "unstuckTask", ChatClefDiagnostics.taskSummary(_unstuckTask));
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return _unstuckTask;
        }
        boolean progressOk = _progress.check(mod);
        boolean stuckOk = true;
        if (progressOk) {
            stuckOk = stuckCheck.check(mod);
        }
        if (!progressOk || !stuckOk) {
            BlockPos blockStuck = stuckInBlock(mod);
            ChatClefDiagnostics.logEvent("ENTITY_PATH", "OBSERVE", "movement_progress_failed", this,
                    "entity", ChatClefDiagnostics.entitySummary(_entity),
                    "progressOk", progressOk,
                    "stuckOk", stuckOk,
                    "stuckBlock", ChatClefDiagnostics.blockPos(blockStuck),
                    "pathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()));
            if (blockStuck != null) {
                _unstuckTask = getFenceUnstuckTask();
                ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "start_unstuck_task", this,
                        "entity", ChatClefDiagnostics.entitySummary(_entity),
                        "stuckBlock", ChatClefDiagnostics.blockPos(blockStuck),
                        "unstuckTask", ChatClefDiagnostics.taskSummary(_unstuckTask));
                return _unstuckTask;
            }
            stuckCheck.reset();
        }
        if (_wanderTask.isActive() && !_wanderTask.isFinished()) {
            _progress.reset();
            setDebugState("Failed to get to target, wandering for a bit.");
            ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "continue_wander_after_entity_path_failure", this,
                    "entity", ChatClefDiagnostics.entitySummary(_entity),
                    "wanderTask", ChatClefDiagnostics.taskSummary(_wanderTask));
            return _wanderTask;
        }

        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "set_goal_follow_entity", this,
                    "entity", ChatClefDiagnostics.entitySummary(_entity),
                    "closeEnoughDistance", _closeEnoughDistance);
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalFollowEntity(_entity, _closeEnoughDistance));
        }

        if (mod.getPlayer().isInRange(_entity, _closeEnoughDistance)) {
            ChatClefDiagnostics.logEvent("ENTITY_PATH", "OBSERVE", "player_in_entity_range", this,
                    "entity", ChatClefDiagnostics.entitySummary(_entity),
                    "closeEnoughDistance", _closeEnoughDistance);
            _progress.reset();
        }

        boolean progressOkAfterGoal = _progress.check(mod);
        ChatClefDiagnostics.logEvent("ENTITY_PATH", "OBSERVE", "post_goal_progress_check", this,
                "entity", ChatClefDiagnostics.entitySummary(_entity),
                "progressOk", progressOkAfterGoal,
                "pathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()),
                "customGoalActive", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getCustomGoalProcess().isActive()));
        if (!progressOkAfterGoal) {
            ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "return_wander_task_after_progress_failure", this,
                    "entity", ChatClefDiagnostics.entitySummary(_entity),
                    "wanderTask", ChatClefDiagnostics.taskSummary(_wanderTask));
            return _wanderTask;
        }

        setDebugState("Going to entity");
        ChatClefDiagnostics.logEvent("ENTITY_PATH", "DECISION", "get_to_entity_continue_pathing", this,
                "entity", ChatClefDiagnostics.entitySummary(_entity),
                "entityDistanceSqr", ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, _entity));
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logEvent("ENTITY_PATH", "ON_STOP", "get_to_entity_stop", this,
                "entity", ChatClefDiagnostics.entitySummary(_entity),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask),
                "pathingBeforeForceCancel", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getClientBaritone().getPathingBehavior().isPathing()));
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
        ChatClefDiagnostics.logEvent("ENTITY_PATH", "ON_STOP", "get_to_entity_stop_after_force_cancel", this,
                "entity", ChatClefDiagnostics.entitySummary(_entity),
                "pathingAfterForceCancel", ChatClefDiagnostics.safeValue(() -> AltoClef.getInstance().getClientBaritone().getPathingBehavior().isPathing()));
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GetToEntityTask task) {
            return task._entity.equals(_entity) && Math.abs(task._closeEnoughDistance - _closeEnoughDistance) < 0.1;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Approach entity " + _entity.getType().getTranslationKey();
    }
}
