package adris.altoclef.tasks.movement.escape;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
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
    private final AnnoyingBlockDetector annoyingBlockDetector = new AnnoyingBlockDetector();
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

    public void resetWander() {
        _wanderDistanceExtension = 0;
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
        if (_unstuckTask != null
                && _unstuckTask.isActive()
                && !_unstuckTask.isFinished()
                && annoyingBlockDetector.findNearbyAnnoyingBlock(mod) != null) {
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
                clearBlockingEntityTask = new ClearBlockingEntityTask(
                        entity,
                        mod.getPlayer().getPos(),
                        BLOCKING_ENTITY_MAX_CHASE_RANGE,
                        BLOCKING_ENTITY_CLEAR_TIMEOUT_SECONDS
                );
                setDebugState("Clearing blocking entity.");
                debugLogger.event("clearing blocking entity: target=" + describeEntity(mod, entity)
                        + ", clearRange=" + BLOCKING_ENTITY_CLEAR_RANGE
                        + ", maxChaseRange=" + BLOCKING_ENTITY_MAX_CHASE_RANGE
                        + ", timeoutSeconds=" + BLOCKING_ENTITY_CLEAR_TIMEOUT_SECONDS);
                return clearBlockingEntityTask;
            }
            BlockPos blockStuck = annoyingBlockDetector.findNearbyAnnoyingBlock(mod);
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

}
