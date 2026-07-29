package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.control.InputControls;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasks.movement.escape.EscapePlan;
import adris.altoclef.tasks.movement.escape.EscapePlanSearchResult;
import adris.altoclef.tasks.movement.escape.EscapeRetryCooldowns;
import adris.altoclef.tasks.movement.escape.LocalTerrainEscapeTask;
import adris.altoclef.tasks.movement.escape.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.time.TimerGame;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.input.Input;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a baritone goal into a task.
 */
public abstract class CustomBaritoneGoalTask extends Task implements ITaskRequiresGrounded {
    private static final double BLOCKING_ENTITY_CLEAR_RANGE = 1.45;
    private static final double BLOCKING_ENTITY_MAX_CHASE_RANGE = 2.75;
    private static final double BLOCKING_ENTITY_CLEAR_TIMEOUT_SECONDS = 2.0;
    private static final double BLOCKING_ENTITY_MIN_PROGRESS_SQ = 0.08 * 0.08;
    private static final int BLOCKING_ENTITY_STUCK_TICKS = 20 * 2;
    private static final int BLOCKING_ENTITY_RETRY_COOLDOWN_TICKS = 20 * 6;
    private static final int TERRAIN_ESCAPE_RETRY_COOLDOWN_TICKS = 20 * 8;

    private final Task wanderTask = new TimeoutWanderTask(5, true);
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final StateChangeLogger debugLogger = new StateChangeLogger("CustomBaritoneGoalTask");
    private final Map<UUID, Integer> blockingEntityRetryCooldowns = new HashMap<>();
    private final EscapeRetryCooldowns terrainEscapeRetryCooldowns = new EscapeRetryCooldowns(TERRAIN_ESCAPE_RETRY_COOLDOWN_TICKS);
    private final boolean wander;
    protected MovementProgressChecker checker = new MovementProgressChecker();
    protected Goal cachedGoal = null;
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
    private ClearBlockingEntityTask clearBlockingEntityTask = null;
    private LocalTerrainEscapeTask localTerrainEscapeTask = null;
    private Vec3d lastBlockingProgressPos = null;
    private int lastBlockingProgressTick = 0;
    private String lastTerrainEscapeDiagnosticKey = "";

    // This happens all the time in mineshafts and swamps/jungles

    public CustomBaritoneGoalTask(boolean wander) {
        this.wander = wander;
    }

    public CustomBaritoneGoalTask() {
        this(true);
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
        checker.reset();
        stuckCheck.reset();
        clearBlockingEntityTask = null;
        localTerrainEscapeTask = null;
        lastTerrainEscapeDiagnosticKey = "";
        AltoClef mod = AltoClef.getInstance();
        if (mod.getPlayer() != null) {
            lastBlockingProgressPos = mod.getPlayer().getPos();
            lastBlockingProgressTick = WorldHelper.getTicks();
        }
        pruneBlockingEntityRetryCooldowns();
        terrainEscapeRetryCooldowns.pruneExpired();
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        InputControls controls = mod.getInputControls();
        
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            checker.reset();
        }
        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                controls.hold(Input.SNEAK);
                controls.hold(Input.MOVE_FORWARD);
                return null;
            } else {
                controls.release(Input.SNEAK);
                controls.release(Input.MOVE_BACK);
                controls.release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                controls.release(Input.SNEAK);
                controls.release(Input.MOVE_BACK);
                controls.release(Input.MOVE_FORWARD);
            }
        }
        if (clearBlockingEntityTask != null) {
            if (!clearBlockingEntityTask.isFinished()) {
                setDebugState("Clearing blocking entity.");
                mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                mod.getClientBaritone().getExploreProcess().onLostControl();
                return clearBlockingEntityTask;
            }
            if (clearBlockingEntityTask.didTimeOut()) {
                cooldownBlockingEntity(clearBlockingEntityTask.getTargetUuid());
                debugLogger.event("blocking entity clear timed out: target="
                        + clearBlockingEntityTask.describeTarget(mod)
                        + ", task=" + toDebugString()
                        + ", cooldownTicks=" + BLOCKING_ENTITY_RETRY_COOLDOWN_TICKS);
            } else {
                debugLogger.event("blocking entity clear finished: target="
                        + clearBlockingEntityTask.describeTarget(mod)
                        + ", task=" + toDebugString());
            }
            clearBlockingEntityTask = null;
            resetBlockingEntityProgress(mod);
            checker.reset();
            stuckCheck.reset();
        }
        Optional<Entity> blockingEntity = getBlockingEntityToClear(mod);
        if (blockingEntity.isPresent()) {
            Entity entity = blockingEntity.get();
            clearBlockingEntityTask = new ClearBlockingEntityTask(entity, mod.getPlayer().getPos());
            setDebugState("Clearing blocking entity.");
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            debugLogger.event("clearing blocking entity during goal: target=" + describeEntity(mod, entity)
                    + ", task=" + toDebugString()
                    + ", clearRange=" + BLOCKING_ENTITY_CLEAR_RANGE
                    + ", maxChaseRange=" + BLOCKING_ENTITY_MAX_CHASE_RANGE
                    + ", timeoutSeconds=" + BLOCKING_ENTITY_CLEAR_TIMEOUT_SECONDS);
            return clearBlockingEntityTask;
        }
        if (localTerrainEscapeTask != null) {
            if (!localTerrainEscapeTask.isFinished()) {
                setDebugState("Clearing local terrain escape route.");
                mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                mod.getClientBaritone().getExploreProcess().onLostControl();
                return localTerrainEscapeTask;
            }
            if (localTerrainEscapeTask.didFail()) {
                terrainEscapeRetryCooldowns.rememberFailure(localTerrainEscapeTask.getPlan());
                debugLogger.event("local terrain escape failed: reason="
                        + localTerrainEscapeTask.describeFailureReason()
                        + ", timedOut=" + localTerrainEscapeTask.didTimeOut()
                        + ", "
                        + localTerrainEscapeTask.describePlan()
                        + ", task=" + toDebugString()
                        + ", candidateCooldownTicks=" + TERRAIN_ESCAPE_RETRY_COOLDOWN_TICKS
                        + ", cooldowns=" + terrainEscapeRetryCooldowns.describe());
            } else {
                debugLogger.event("local terrain escape finished: "
                        + localTerrainEscapeTask.describePlan()
                        + ", task=" + toDebugString());
            }
            localTerrainEscapeTask = null;
            lastTerrainEscapeDiagnosticKey = "";
            resetBlockingEntityProgress(mod);
            checker.reset();
            stuckCheck.reset();
        }
        Optional<EscapePlan> terrainEscapePlan = getTerrainEscapePlan(mod);
        if (terrainEscapePlan.isPresent()) {
            localTerrainEscapeTask = new LocalTerrainEscapeTask(terrainEscapePlan.get(), toDebugString());
            setDebugState("Clearing local terrain escape route.");
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            debugLogger.event("starting local terrain escape during goal: "
                    + terrainEscapePlan.get().describe()
                    + ", task=" + toDebugString());
            return localTerrainEscapeTask;
        }
        if (unstuckTask != null && unstuckTask.isActive() && !unstuckTask.isFinished() && stuckInBlock(mod) != null) {
            setDebugState("Getting unstuck from block.");
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return unstuckTask;
        }
        if (!checker.check(mod) || !stuckCheck.check(mod)) {
            BlockPos blockStuck = stuckInBlock(mod);
            if (blockStuck != null) {
                unstuckTask = getFenceUnstuckTask();
                return unstuckTask;
            }
            stuckCheck.reset();
        }
        if (cachedGoal == null) {
            cachedGoal = newGoal(mod);
        }

        if (wander) {
            if (isFinished()) {
                // Don't wander if we've reached our goal.
                checker.reset();
            } else {
                if (wanderTask.isActive() && !wanderTask.isFinished()) {
                    setDebugState("Wandering...");
                    checker.reset();
                    return wanderTask;
                }
                if (!checker.check(mod)) {
                    Debug.logMessage("Failed to make progress on goal, wandering.");
                    onWander(mod);
                    return wanderTask;
                }
            }
        }
        if (!mod.getClientBaritone().getCustomGoalProcess().isActive()
                && mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(cachedGoal);
        }
        setDebugState("Completing goal.");
        return null;
    }

    @Override
    public boolean isFinished() {
        if (cachedGoal == null) {
            cachedGoal = newGoal(AltoClef.getInstance());
        }
        return cachedGoal != null && cachedGoal.isInGoal(AltoClef.getInstance().getPlayer().getBlockPos());
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();
    }

    protected abstract Goal newGoal(AltoClef mod);

    protected void onWander(AltoClef mod) {
    }

    private Optional<Entity> getBlockingEntityToClear(AltoClef mod) {
        pruneBlockingEntityRetryCooldowns();
        if (!isLocallyStalled(mod)) {
            return Optional.empty();
        }

        List<Entity> closeEntities = mod.getEntityTracker().getCloseEntities();
        for (Entity entity : closeEntities) {
            if (isClearableBlockingEntity(mod, entity)) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    private Optional<EscapePlan> getTerrainEscapePlan(AltoClef mod) {
        terrainEscapeRetryCooldowns.pruneExpired();
        String skipReason = getLocalRecoverySkipReason(mod);
        if (skipReason != null) {
            logTerrainEscapeDiagnostic("skip:" + skipReasonCategory(skipReason),
                    "terrain escape not eligible: " + skipReason);
            return Optional.empty();
        }
        BlockPos origin = mod.getPlayer().getBlockPos();
        logTerrainEscapeDiagnostic("eligible:" + origin.toShortString(),
                "terrain escape eligible: local stall detected at origin=" + origin.toShortString()
                        + ", cooldowns=" + terrainEscapeRetryCooldowns.describe());
        EscapePlanSearchResult planSearch = LocalTerrainEscapeTask.searchPlan(mod,
                terrainEscapeRetryCooldowns.activeOrigins(),
                terrainEscapeRetryCooldowns.activeCandidates(),
                debugLogger);
        Optional<EscapePlan> plan = planSearch.getPlan();
        if (plan.isEmpty()) {
            logTerrainEscapeDiagnostic("no-plan:" + origin.toShortString(),
                    "terrain escape plan unavailable after local stall: origin=" + origin.toShortString()
                            + ", cooldowns=" + terrainEscapeRetryCooldowns.describe()
                            + ", reason=" + planSearch.describeFailure());
        }
        return plan;
    }

    private boolean isLocallyStalled(AltoClef mod) {
        return getLocalRecoverySkipReason(mod) == null;
    }

    private String getLocalRecoverySkipReason(AltoClef mod) {
        if (mod.getPlayer() == null || mod.getPlayer().getPos() == null) {
            return "player unavailable";
        }
        if (isFinished() || mod.getFoodChain().needsToEat() || mod.getControllerExtras().isBreakingBlock()) {
            String reason;
            if (isFinished()) {
                reason = "goal already reached";
            } else if (mod.getFoodChain().needsToEat()) {
                reason = "food chain needs to eat";
            } else {
                reason = "already breaking block";
            }
            resetBlockingEntityProgress(mod);
            return reason;
        }

        Vec3d currentPos = mod.getPlayer().getPos();
        int now = WorldHelper.getTicks();
        if (lastBlockingProgressPos == null) {
            lastBlockingProgressPos = currentPos;
            lastBlockingProgressTick = now;
            return "local recovery progress tracker initialized at player=" + formatVec(currentPos);
        }
        double movementSq = currentPos.squaredDistanceTo(lastBlockingProgressPos);
        if (movementSq > BLOCKING_ENTITY_MIN_PROGRESS_SQ) {
            lastBlockingProgressPos = currentPos;
            lastBlockingProgressTick = now;
            return "movement progress observed: moved=" + formatDouble(Math.sqrt(movementSq))
                    + ", threshold=" + formatDouble(Math.sqrt(BLOCKING_ENTITY_MIN_PROGRESS_SQ))
                    + ", player=" + formatVec(currentPos);
        }
        int stationaryTicks = now - lastBlockingProgressTick;
        if (stationaryTicks < BLOCKING_ENTITY_STUCK_TICKS) {
            return "waiting for local stall: stationaryTicks=" + stationaryTicks
                    + "/" + BLOCKING_ENTITY_STUCK_TICKS
                    + ", player=" + formatVec(currentPos);
        }
        return null;
    }

    private void resetBlockingEntityProgress(AltoClef mod) {
        if (mod.getPlayer() != null) {
            lastBlockingProgressPos = mod.getPlayer().getPos();
            lastBlockingProgressTick = WorldHelper.getTicks();
        } else {
            lastBlockingProgressPos = null;
            lastBlockingProgressTick = 0;
        }
    }

    private boolean isClearableBlockingEntity(AltoClef mod, Entity entity) {
        if (!(entity instanceof MobEntity)
                || entity == null
                || !entity.isAlive()
                || isBlockingEntityOnCooldown(entity)
                || !isSafeToClear(entity)) {
            return false;
        }
        return isEntityBlockingPlayerSpace(mod, entity);
    }

    private boolean isSafeToClear(Entity entity) {
        if (entity instanceof VillagerEntity
                || entity instanceof WanderingTraderEntity
                || entity instanceof IronGolemEntity) {
            return false;
        }
        return !(entity instanceof TameableEntity tameable) || !tameable.isTamed();
    }

    private boolean isEntityBlockingPlayerSpace(AltoClef mod, Entity entity) {
        if (mod.getPlayer() == null) {
            return false;
        }
        if (mod.getPlayer().getBoundingBox().expand(0.35, 0.15, 0.35)
                .intersects(entity.getBoundingBox().expand(0.05))) {
            return true;
        }
        return entity.getPos().isInRange(mod.getPlayer().getPos(), BLOCKING_ENTITY_CLEAR_RANGE)
                && Math.abs(entity.getY() - mod.getPlayer().getY()) <= 1.5;
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

    private void logTerrainEscapeDiagnostic(String stateKey, String detail) {
        if (stateKey.equals(lastTerrainEscapeDiagnosticKey)) {
            return;
        }
        lastTerrainEscapeDiagnosticKey = stateKey;
        debugLogger.event(detail + ", task=" + toDebugString());
    }

    private String skipReasonCategory(String skipReason) {
        int separatorIndex = skipReason.indexOf(':');
        if (separatorIndex >= 0) {
            return skipReason.substring(0, separatorIndex);
        }
        return skipReason;
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatVec(Vec3d vec) {
        if (vec == null) {
            return "none";
        }
        return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
    }

    private String describeEntity(AltoClef mod, Entity entity) {
        if (entity == null) {
            return "none";
        }
        return entity.getType().getTranslationKey()
                + " uuid=" + entity.getUuid()
                + ", entityPos=" + entity.getBlockPos().toShortString()
                + ", playerPos=" + mod.getPlayer().getBlockPos().toShortString()
                + ", distance=" + String.format(Locale.ROOT, "%.2f", entity.distanceTo(mod.getPlayer()));
    }

    //20260728_kpopmodder: Bounded local recovery for Baritone goals blocked by nearby entities.
    private static class ClearBlockingEntityTask extends Task {
        private final Entity target;
        private final Vec3d origin;
        private final TimerGame timeout = new TimerGame(BLOCKING_ENTITY_CLEAR_TIMEOUT_SECONDS);
        private final StateChangeLogger debugLogger = new StateChangeLogger("GoalClearBlockingEntityTask");
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
            return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
        }
    }
}
