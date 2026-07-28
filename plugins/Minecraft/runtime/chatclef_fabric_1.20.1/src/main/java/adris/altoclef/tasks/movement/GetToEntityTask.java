package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.escape.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalFollowEntity;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.input.Input;
import net.minecraft.block.*;
import adris.altoclef.multiversion.versionedfields.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

public class GetToEntityTask extends Task implements ITaskRequiresGrounded {
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker _progress = new MovementProgressChecker();
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(5);
    private final Entity _entity;
    private final double _closeEnoughDistance;
    private final StateChangeLogger movementLogger = new StateChangeLogger("GetToEntityTask");
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
        Block block = mod.getWorld().getBlockState(pos).getBlock();
        if (block instanceof DoorBlock ||
                block instanceof FenceBlock ||
                block instanceof FenceGateBlock ||
                block instanceof FlowerBlock) {
            return true;
        }
        if (annoyingBlocks != null) {
            for (Block annoyingBlock : annoyingBlocks) {
                if (block == annoyingBlock) {
                    return true;
                }
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
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
        _progress.reset();
        stuckCheck.reset();
        _wanderTask.resetWander();
        movementLogger.debugEvent("start: " + describeTarget(AltoClef.getInstance()));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            _progress.reset();
            movementLogger.state("baritone pathing", "baritone pathing: " + describeTarget(mod));
        }
        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                movementLogger.state("nether portal escape", "nether portal escape: " + describeTarget(mod));
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        }
        BlockPos currentStuckBlock = stuckInBlock(mod);
        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished() && currentStuckBlock != null) {
            setDebugState("Getting unstuck from block.");
            movementLogger.state("continue unstuck " + currentStuckBlock.toShortString(), "continue unstuck: "
                    + describeBlock(mod, currentStuckBlock)
                    + ", task=" + _unstuckTask
                    + ", " + describeTarget(mod));
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return _unstuckTask;
        }
        if (!_progress.check(mod) || !stuckCheck.check(mod)) {
            BlockPos blockStuck = currentStuckBlock != null ? currentStuckBlock : stuckInBlock(mod);
            if (blockStuck != null) {
                movementLogger.state("annoying block " + blockStuck.toShortString(), "annoying block while approaching: "
                        + describeBlock(mod, blockStuck)
                        + ", " + describeTarget(mod));
                _unstuckTask = getFenceUnstuckTask();
                return _unstuckTask;
            }
            movementLogger.state("progress stalled", "progress stalled without annoying block: "
                    + "baritonePathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                    + ", goalActive=" + mod.getClientBaritone().getCustomGoalProcess().isActive()
                    + ", " + describeTarget(mod));
            stuckCheck.reset();
        }
        if (_wanderTask.isActive() && !_wanderTask.isFinished()) {
            _progress.reset();
            setDebugState("Failed to get to target, wandering for a bit.");
            movementLogger.state("wander after approach fail", "wander after approach fail: "
                    + "wanderTask=" + _wanderTask
                    + ", " + describeTarget(mod));
            return _wanderTask;
        }

        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
            movementLogger.state("set follow goal", "set follow goal: closeEnough="
                    + formatDouble(_closeEnoughDistance)
                    + ", " + describeTarget(mod));
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalFollowEntity(_entity, _closeEnoughDistance));
        }

        if (mod.getPlayer().isInRange(_entity, _closeEnoughDistance)) {
            movementLogger.state("close enough", "close enough to entity: " + describeTarget(mod));
            _progress.reset();
        }

        if (!_progress.check(mod)) {
            movementLogger.state("progress failed switch wander", "progress failed, switching to wander: " + describeTarget(mod));
            return _wanderTask;
        }

        setDebugState("Going to entity");
        movementLogger.state("going to entity", "going to entity: " + describeTarget(mod));
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        movementLogger.debugEvent("stop: interruptedBy="
                + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName() + "{" + interruptTask + "}")
                + ", " + describeTarget(AltoClef.getInstance()));
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
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

    private String describeTarget(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null || _entity == null) {
            return "target=missing";
        }
        return "target=" + _entity.getType().getTranslationKey()
                + ", targetPos=" + _entity.getBlockPos().toShortString()
                + ", playerPos=" + mod.getPlayer().getBlockPos().toShortString()
                + ", distance=" + formatDouble(_entity.distanceTo(mod.getPlayer()))
                + ", targetAlive=" + _entity.isAlive()
                + ", closeEnough=" + formatDouble(_closeEnoughDistance);
    }

    private String describeBlock(AltoClef mod, BlockPos pos) {
        Block block = mod.getWorld().getBlockState(pos).getBlock();
        return "block=" + block.getTranslationKey()
                + ", blockPos=" + pos.toShortString();
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
