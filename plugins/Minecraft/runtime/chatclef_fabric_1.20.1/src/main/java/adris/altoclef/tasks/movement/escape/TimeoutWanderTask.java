package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// TODO improve wandering
/**
 * Call this when the place you're currently at is bad for some reason and you just wanna get away.
 */
public class TimeoutWanderTask extends Task implements ITaskRequiresGrounded {
    private static final double BLOCKING_ENTITY_CLEAR_RANGE = 1.25;
    private static final double BLOCKING_ENTITY_MAX_CHASE_RANGE = 2.75;
    private static final double BLOCKING_ENTITY_CLEAR_TIMEOUT_SECONDS = 2.0;
    private static final int BLOCKING_ENTITY_RETRY_COOLDOWN_TICKS = 20 * 6;

    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final float distanceToWander;
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final boolean increaseRange;
    private final TimerGame timer = new TimerGame(60);
    private final StateChangeLogger debugLogger = new StateChangeLogger("TimeoutWanderTask");
    private final Map<UUID, Integer> blockingEntityRetryCooldowns = new HashMap<>();
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
    private ClearBlockingEntityTask clearBlockingEntityTask = null;
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

        timer.reset();
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        origin = mod.getPlayer().getPos();
        progressChecker.reset();
        stuckCheck.reset();
        failCounter = 0;
        clearBlockingEntityTask = null;
        pruneBlockingEntityRetryCooldowns();
        debugLogger.event("start: distance=" + distanceToWander
                + ", increaseRange=" + increaseRange
                + ", forceExplore=" + _forceExplore
                + ", origin=" + formatVec(origin));
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
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();


        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            progressChecker.reset();
        }
        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
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
        if (clearBlockingEntityTask != null) {
            if (!clearBlockingEntityTask.isFinished()) {
                setDebugState("Clearing blocking entity.");
                return clearBlockingEntityTask;
            }
            if (clearBlockingEntityTask.didTimeOut()) {
                cooldownBlockingEntity(clearBlockingEntityTask.getTargetUuid());
                debugLogger.event("blocking entity clear timed out: target="
                        + clearBlockingEntityTask.describeTarget(mod)
                        + ", cooldownTicks=" + BLOCKING_ENTITY_RETRY_COOLDOWN_TICKS);
            } else {
                debugLogger.event("blocking entity clear finished: target="
                        + clearBlockingEntityTask.describeTarget(mod));
            }
            clearBlockingEntityTask = null;
            progressChecker.reset();
            stuckCheck.reset();
        }
        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished() && stuckInBlock(mod) != null) {
            setDebugState("Getting unstuck from block.");
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return _unstuckTask;
        }
        if (!progressChecker.check(mod) || !stuckCheck.check(mod)) {
            Optional<Entity> blockingEntity = getBlockingEntityToClear(mod);
            if (blockingEntity.isPresent()) {
                Entity entity = blockingEntity.get();
                clearBlockingEntityTask = new ClearBlockingEntityTask(entity, mod.getPlayer().getPos());
                setDebugState("Clearing blocking entity.");
                debugLogger.event("clearing blocking entity: target=" + describeEntity(mod, entity)
                        + ", clearRange=" + BLOCKING_ENTITY_CLEAR_RANGE
                        + ", maxChaseRange=" + BLOCKING_ENTITY_MAX_CHASE_RANGE
                        + ", timeoutSeconds=" + BLOCKING_ENTITY_CLEAR_TIMEOUT_SECONDS);
                return clearBlockingEntityTask;
            }
            BlockPos blockStuck = stuckInBlock(mod);
            if (blockStuck != null) {
                failCounter++;
                _unstuckTask = getFenceUnstuckTask();
                debugLogger.event("stuck in annoying block; shimmy: block=" + blockStuck.toShortString()
                        + ", failCounter=" + failCounter);
                return _unstuckTask;
            }
            stuckCheck.reset();
        }
        setDebugState("Exploring.");
        switch (WorldHelper.getCurrentDimension()) {
            case END -> {
                if (timer.getDuration() >= 30) {
                    timer.reset();
                }
            }
            case OVERWORLD, NETHER -> {
                if (timer.getDuration() >= 30) {
                }
                if (timer.elapsed()) {
                    timer.reset();
                }
            }
        }
        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
            mod.getClientBaritone().getExploreProcess().explore((int) origin.getX(), (int) origin.getZ());
        }
        if (!progressChecker.check(mod)) {
            progressChecker.reset();
            if (!_forceExplore) {
                failCounter++;
                Debug.logMessage("Failed exploring.");
                debugLogger.event("explore progress failed: failCounter=" + failCounter
                        + ", origin=" + formatVec(origin)
                        + ", player=" + formatVec(mod.getPlayer().getPos()));
            }
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
        clearBlockingEntityTask = null;
        if (isFinished()) {
            if (increaseRange) {
                _wanderDistanceExtension += distanceToWander;
                Debug.logMessage("Increased wander range");
                debugLogger.event("increased wander range: extension=" + _wanderDistanceExtension
                        + ", distance=" + distanceToWander);
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

    private Optional<Entity> getBlockingEntityToClear(AltoClef mod) {
        pruneBlockingEntityRetryCooldowns();
        List<Entity> closeEntities = mod.getEntityTracker().getCloseEntities();
        for (Entity entity : closeEntities) {
            if (entity instanceof MobEntity
                    && entity.isAlive()
                    && !isBlockingEntityOnCooldown(entity)
                    && entity.getPos().isInRange(mod.getPlayer().getPos(), BLOCKING_ENTITY_CLEAR_RANGE)) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    private boolean isBlockingEntityOnCooldown(Entity entity) {
        return blockingEntityRetryCooldowns.containsKey(entity.getUuid());
    }

    private void cooldownBlockingEntity(UUID uuid) {
        if (uuid != null) {
            blockingEntityRetryCooldowns.put(uuid, WorldHelper.getTicks() + BLOCKING_ENTITY_RETRY_COOLDOWN_TICKS);
        }
    }

    private void pruneBlockingEntityRetryCooldowns() {
        int now = WorldHelper.getTicks();
        blockingEntityRetryCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private String describeEntity(AltoClef mod, Entity entity) {
        if (entity == null) {
            return "none";
        }
        return entity.getType().getTranslationKey()
                + " uuid=" + entity.getUuid()
                + ", entityPos=" + entity.getBlockPos().toShortString()
                + ", playerPos=" + mod.getPlayer().getBlockPos().toShortString()
                + ", distance=" + String.format(java.util.Locale.ROOT, "%.2f", entity.distanceTo(mod.getPlayer()));
    }

    private String formatVec(Vec3d vec) {
        if (vec == null) {
            return "none";
        }
        return String.format(java.util.Locale.ROOT, "%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
    }

    //20260728_kpopmodder: Keep local obstacle removal bounded so recovery does not turn into animal hunting.
    private static class ClearBlockingEntityTask extends Task {
        private final Entity target;
        private final Vec3d origin;
        private final TimerGame timeout = new TimerGame(BLOCKING_ENTITY_CLEAR_TIMEOUT_SECONDS);
        private final StateChangeLogger debugLogger = new StateChangeLogger("ClearBlockingEntityTask");
        private boolean finished;
        private boolean timedOut;

        private ClearBlockingEntityTask(Entity target, Vec3d origin) {
            this.target = target;
            this.origin = origin;
        }

        @Override
        protected void onStart() {
            timeout.reset();
            finished = false;
            timedOut = false;
            debugLogger.event("start: target=" + describeTarget(AltoClef.getInstance())
                    + ", origin=" + formatVec(origin));
        }

        @Override
        protected Task onTick() {
            AltoClef mod = AltoClef.getInstance();
            if (target == null || !target.isAlive()) {
                finished = true;
                debugLogger.event("finished: target gone");
                return null;
            }
            if (!target.getPos().isInRange(mod.getPlayer().getPos(), BLOCKING_ENTITY_MAX_CHASE_RANGE)) {
                finished = true;
                debugLogger.event("finished: target no longer blocking: " + describeTarget(mod));
                return null;
            }
            if (!target.getPos().isInRange(origin, BLOCKING_ENTITY_MAX_CHASE_RANGE + 1.0)) {
                finished = true;
                debugLogger.event("finished: target left local recovery area: " + describeTarget(mod));
                return null;
            }
            if (timeout.elapsed()) {
                timedOut = true;
                finished = true;
                debugLogger.event("timed out: " + describeTarget(mod));
                return null;
            }

            setDebugState("Clearing " + target.getType().getTranslationKey());
            return new KillEntityTask(target, 0, BLOCKING_ENTITY_MAX_CHASE_RANGE, 0);
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        private boolean didTimeOut() {
            return timedOut;
        }

        private UUID getTargetUuid() {
            return target == null ? null : target.getUuid();
        }

        private String describeTarget(AltoClef mod) {
            if (target == null) {
                return "none";
            }
            String playerPos = mod == null || mod.getPlayer() == null
                    ? "unknown"
                    : mod.getPlayer().getBlockPos().toShortString();
            return target.getType().getTranslationKey()
                    + " uuid=" + target.getUuid()
                    + ", entityPos=" + target.getBlockPos().toShortString()
                    + ", playerPos=" + playerPos
                    + ", alive=" + target.isAlive();
        }

        @Override
        protected boolean isEqual(Task other) {
            if (other instanceof ClearBlockingEntityTask task) {
                return target != null && target.equals(task.target);
            }
            return false;
        }

        @Override
        protected String toDebugString() {
            return "Clear blocking entity " + (target == null ? "none" : target.getType().getTranslationKey());
        }

        private String formatVec(Vec3d vec) {
            if (vec == null) {
                return "none";
            }
            return String.format(java.util.Locale.ROOT, "%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
        }
    }
}
