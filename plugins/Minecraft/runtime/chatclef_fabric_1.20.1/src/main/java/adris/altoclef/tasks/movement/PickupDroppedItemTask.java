package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.AbstractDoToClosestObjectTask;
import adris.altoclef.tasks.resources.SatisfyMiningRequirementTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.*;
import adris.altoclef.multiversion.versionedfields.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class PickupDroppedItemTask extends AbstractDoToClosestObjectTask<ItemEntity> implements ITaskRequiresGrounded {
    private static final Task getPickaxeFirstTask = new SatisfyMiningRequirementTask(MiningRequirement.STONE);
    private static final int CURRENT_DROP_RETRY_GRACE_TICKS = 20 * 4;
    //20260728_kpopmodder: Hold a newly selected drop briefly so mining/pickup decisions do not thrash.
    private static final int CURRENT_DROP_MIN_LOCK_TICKS = 20 * 3;
    private static final double CURRENT_DROP_MAX_RETAIN_DISTANCE = 64.0;
    // Not clean practice, but it helps keep things self contained I think.
    private static boolean isGettingPickaxeFirstFlag = false;
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(5, true);
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final ItemTarget[] itemTargets;
    private final StateChangeLogger pickupLogger = new StateChangeLogger("PickupDroppedItemTask");

    // This happens all the time in mineshafts and swamps/jungles
    private final Set<ItemEntity> _blacklist = new HashSet<>();
    private final boolean _freeInventoryIfFull;
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
            Blocks.SHORT_GRASS
    };
    private Task unstuckTask = null;
    // Am starting to regret not making this a singleton
    private AltoClef _mod;
    private boolean _collectingPickaxeForThisResource = false;
    private ItemEntity _currentDrop = null;
    private DropSnapshot currentDropSnapshot = null;
    //20260727_kpopmodder: Keep a short retry window so food drops are not abandoned after one pathing hiccup.
    private int currentDropLockStartTick = -1;
    private int currentDropFailureStartTick = -1;
    private int currentDropFailureLastTick = -1;
    private int currentDropFailureCount = 0;
    //20260728_kpopmodder: Summarize pickup stability decisions so latest.log can confirm reduced task switching.
    private int currentDropCandidateCount = 0;
    private int currentDropLockCount = 0;
    private int currentDropSwitchCount = 0;
    private int currentDropRetainCount = 0;
    private int currentDropMinLockRetainCount = 0;
    private int currentDropMovementStallCount = 0;
    private int currentDropRetryStartCount = 0;
    private int currentDropRetryContinueCount = 0;
    private int currentDropRetryRecoveredCount = 0;
    private int currentDropRetryExpiredCount = 0;
    private int currentDropAbandonCount = 0;
    private int currentDropBlacklistCount = 0;

    public PickupDroppedItemTask(ItemTarget[] itemTargets, boolean freeInventoryIfFull) {
        this.itemTargets = itemTargets;
        _freeInventoryIfFull = freeInventoryIfFull;
    }

    public PickupDroppedItemTask(ItemTarget target, boolean freeInventoryIfFull) {
        this(new ItemTarget[]{target}, freeInventoryIfFull);
    }

    public PickupDroppedItemTask(Item item, int targetCount, boolean freeInventoryIfFull) {
        this(new ItemTarget(item, targetCount), freeInventoryIfFull);
    }

    public PickupDroppedItemTask(Item item, int targetCount) {
        this(item, targetCount, true);
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

    public static boolean isIsGettingPickaxeFirst(AltoClef mod) {
        return isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst();
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

    public boolean isCollectingPickaxeForThis() {
        return _collectingPickaxeForThisResource;
    }

    @Override
    protected void onStart() {
        wanderTask.reset();
        progressChecker.reset();
        stuckCheck.reset();
        resetCurrentDropFailure();
        pickupLogger.reset();
        resetPickupDiagnostics();
        if (_currentDrop == null || (currentDropSnapshot != null && !currentDropSnapshot.matches(_currentDrop))) {
            currentDropSnapshot = null;
            resetCurrentDropLock();
        }
    }

    @Override
    protected void onStop(Task interruptTask) {
        if (hasPickupDiagnostics()) {
            pickupLogger.event("stop summary: interruptedBy=" + describeTask(interruptTask)
                    + ", candidates=" + currentDropCandidateCount
                    + ", locks=" + currentDropLockCount
                    + ", switches=" + currentDropSwitchCount
                    + ", retained=" + currentDropRetainCount
                    + ", minLockRetained=" + currentDropMinLockRetainCount
                    + ", movementStalls=" + currentDropMovementStallCount
                    + ", retryStarts=" + currentDropRetryStartCount
                    + ", retryTicks=" + currentDropRetryContinueCount
                    + ", retryRecoveries=" + currentDropRetryRecoveredCount
                    + ", retryExpired=" + currentDropRetryExpiredCount
                    + ", abandons=" + currentDropAbandonCount
                    + ", blacklisted=" + currentDropBlacklistCount
                    + ", currentDrop=" + describeCurrentDrop(AltoClef.getInstance()));
        }
        resetCurrentDropFailure();
    }

    @Override
    protected Task onTick() {
        if (wanderTask.isActive() && !wanderTask.isFinished()) {
            setDebugState("Wandering.");
            return wanderTask;
        }
        AltoClef mod = AltoClef.getInstance();

        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            progressChecker.reset();
        }
        if (unstuckTask != null && unstuckTask.isActive() && !unstuckTask.isFinished() && stuckInBlock(mod) != null) {
            setDebugState("Getting unstuck from block.");
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return unstuckTask;
        }
        boolean movementProgressing = progressChecker.check(mod);
        boolean stuckProgressing = stuckCheck.check(mod);
        if (!movementProgressing || !stuckProgressing) {
            BlockPos blockStuck = stuckInBlock(mod);
            if (blockStuck != null) {
                pickupLogger.state("unstuck block " + blockStuck.toShortString(),
                        "pickup approach stuck in block: block="
                                + mod.getWorld().getBlockState(blockStuck).getBlock().getTranslationKey()
                                + ", blockPos=" + blockStuck.toShortString()
                                + ", currentDrop=" + describeCurrentDrop(mod));
                unstuckTask = getFenceUnstuckTask();
                return unstuckTask;
            }
            stuckCheck.reset();
        }
        _mod = mod;
        resetCurrentDropFailureIfRecovered(mod, "movement progress resumed");

        // If we're getting a pickaxe for THIS resource...
        if (isIsGettingPickaxeFirst(mod) && _collectingPickaxeForThisResource && !StorageHelper.miningRequirementMetInventory(MiningRequirement.STONE)) {
            progressChecker.reset();
            setDebugState("Collecting pickaxe first");
            return getPickaxeFirstTask;
        } else {
            if (StorageHelper.miningRequirementMetInventory(MiningRequirement.STONE)) {
                isGettingPickaxeFirstFlag = false;
            }
            _collectingPickaxeForThisResource = false;
        }

        if (!movementProgressing) {
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            if (_currentDrop != null && !_currentDrop.getStack().isEmpty()) {
                currentDropMovementStallCount++;
                // We might want to get a pickaxe first.
                if (!isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst() && !StorageHelper.miningRequirementMetInventory(MiningRequirement.STONE)) {
                    Debug.logMessage("Failed to pick up drop, will try to collect a stone pickaxe first and try again!");
                    _collectingPickaxeForThisResource = true;
                    isGettingPickaxeFirstFlag = true;
                    return getPickaxeFirstTask;
                }
                if (isWithinCurrentDropMinLock()) {
                    currentDropMinLockRetainCount++;
                    setDebugState("Holding current drop.");
                    progressChecker.reset();
                    stuckCheck.reset();
                    pickupLogger.state("min lock retains stalled drop " + _currentDrop.getUuid(),
                            "minimum pickup lock retained stalled drop: ticksRemaining="
                                    + currentDropMinLockTicksRemaining()
                                    + ", " + describeDrop(mod, _currentDrop));
                    return super.onTick();
                }
                if (shouldRetryCurrentDrop(mod, "movement stalled")) {
                    setDebugState("Retrying current drop.");
                    progressChecker.reset();
                    stuckCheck.reset();
                    pickupLogger.state("retry current drop " + _currentDrop.getUuid(),
                            "retrying same drop during grace: failureCount=" + currentDropFailureCount
                                    + ", ticksRemaining=" + currentDropRetryTicksRemaining()
                                    + ", " + describeDrop(mod, _currentDrop));
                    return super.onTick();
                }
                abandonCurrentDrop(mod, "movement stalled past retry grace", true);
                return wanderTask;
            }
        }

        return super.onTick();
    }


    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof PickupDroppedItemTask task) {
            return Arrays.equals(task.itemTargets, itemTargets) && task._freeInventoryIfFull == _freeInventoryIfFull;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append("Pickup Dropped Items: [");
        int c = 0;
        for (ItemTarget target : itemTargets) {
            result.append(target.toString());
            if (++c != itemTargets.length) {
                result.append(", ");
            }
        }
        result.append("]");
        return result.toString();
    }

    @Override
    protected Vec3d getPos(AltoClef mod, ItemEntity obj) {
        if (!obj.isOnGround() && !obj.isTouchingWater()) {
            // Assume we'll land down one or two blocks from here. We could do this more advanced but whatever.
            BlockPos p = obj.getBlockPos();
            if (!WorldHelper.isSolidBlock(p.down(3))) {
                return obj.getPos().subtract(0, 2, 0);
            }
            return obj.getPos().subtract(0, 1, 0);
        }
        return obj.getPos();
    }

    @Override
    protected Optional<ItemEntity> getClosestTo(AltoClef mod, Vec3d pos) {
        if (_currentDrop != null) {
            String releaseReason = getCurrentDropReleaseReason(mod);
            if (releaseReason == null) {
                resetCurrentDropFailureIfRecovered(mod, "current drop reachable again");
                currentDropRetainCount++;
                pickupLogger.state("retain current drop " + _currentDrop.getUuid(),
                        "retained drop: reason=current target still valid, " + describeDrop(mod, _currentDrop));
                return Optional.of(_currentDrop);
            }
            abandonCurrentDrop(mod, releaseReason, "target unreachable after retry grace".equals(releaseReason));
        }
        Optional<ItemEntity> closest = mod.getEntityTracker().getClosestItemDrop(
                pos,
                itemTargets);
        closest.ifPresent(drop -> {
            currentDropCandidateCount++;
            updateCurrentDropSnapshotIfUseful(drop);
            pickupLogger.state("candidate drop " + drop.getUuid(),
                    "candidate drop selected by tracker: " + describeDrop(mod, drop));
        });
        return closest;
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        return mod.getPlayer().getPos();
    }

    @Override
    protected Task getGoalTask(ItemEntity itemEntity) {
        if (!itemEntity.equals(_currentDrop)) {
            ItemEntity previousDrop = _currentDrop;
            DropSnapshot previousDropSnapshot = currentDropSnapshot;
            _currentDrop = itemEntity;
            updateCurrentDropSnapshotForLockedDrop(itemEntity);
            armCurrentDropLock();
            resetCurrentDropFailure();
            progressChecker.reset();
            stuckCheck.reset();
            if (previousDrop == null) {
                currentDropLockCount++;
                pickupLogger.state("lock drop " + itemEntity.getUuid(),
                        "locked drop: " + describeDrop(_mod, itemEntity, currentDropSnapshot));
            } else {
                currentDropSwitchCount++;
                pickupLogger.state("switch drop " + previousDrop.getUuid() + " " + itemEntity.getUuid(),
                        "switched drop: old=" + describeDrop(_mod, previousDrop, previousDropSnapshot)
                                + ", new=" + describeDrop(_mod, itemEntity, currentDropSnapshot));
            }
            if (isGettingPickaxeFirstFlag && _collectingPickaxeForThisResource) {
                Debug.logMessage("New goal, no longer collecting a pickaxe.");
                _collectingPickaxeForThisResource = false;
                isGettingPickaxeFirstFlag = false;
            }
        }
        // Ensure our inventory is free if we're close
        boolean touching = _mod.getEntityTracker().isCollidingWithPlayer(itemEntity);
        if (touching) {
            if (_freeInventoryIfFull) {
                if (_mod.getItemStorage().getSlotsThatCanFitInPlayerInventory(itemEntity.getStack(), false).isEmpty()) {
                    return new EnsureFreeInventorySlotTask();
                }
            }
        }
        return new GetToEntityTask(itemEntity);
    }

    @Override
    protected boolean isValid(AltoClef mod, ItemEntity obj) {
        if (obj != null && obj.equals(_currentDrop)) {
            return getCurrentDropNonRetryReleaseReason(mod) == null;
        }
        return obj.isAlive()
                && !obj.getStack().isEmpty()
                && matchesTargets(obj)
                && !_blacklist.contains(obj)
                && mod.getEntityTracker().isEntityReachable(obj);
    }

    private boolean shouldRetryCurrentDrop(AltoClef mod, String reason) {
        if (_currentDrop == null) {
            return false;
        }
        String terminalReleaseReason = getCurrentDropNonRetryReleaseReason(mod);
        if (terminalReleaseReason != null) {
            pickupLogger.state("cannot retry current drop " + _currentDrop.getUuid() + " " + terminalReleaseReason,
                    "cannot retry current drop: reason=" + terminalReleaseReason
                            + ", " + describeDrop(mod, _currentDrop));
            return false;
        }
        int now = WorldHelper.getTicks();
        if (currentDropFailureStartTick < 0) {
            currentDropFailureStartTick = now;
            currentDropFailureLastTick = now;
            currentDropFailureCount = 1;
            currentDropRetryStartCount++;
            pickupLogger.event("pickup retry grace started: reason=" + reason
                    + ", graceTicks=" + CURRENT_DROP_RETRY_GRACE_TICKS
                    + ", " + describeDrop(mod, _currentDrop));
            pickupLogger.state("start retry grace " + _currentDrop.getUuid(),
                    "pickup retry grace started: reason=" + reason
                            + ", " + describeDrop(mod, _currentDrop));
            return true;
        }
        if (currentDropFailureLastTick != now) {
            currentDropFailureLastTick = now;
            currentDropFailureCount++;
        }
        boolean stillInGrace = now - currentDropFailureStartTick < CURRENT_DROP_RETRY_GRACE_TICKS;
        if (stillInGrace) {
            currentDropRetryContinueCount++;
        }
        if (!stillInGrace) {
            currentDropRetryExpiredCount++;
            pickupLogger.event("pickup retry grace expired: reason=" + reason
                    + ", failures=" + currentDropFailureCount
                    + ", " + describeDrop(mod, _currentDrop));
            pickupLogger.state("retry grace expired " + _currentDrop.getUuid(),
                    "pickup retry grace expired: reason=" + reason
                            + ", failures=" + currentDropFailureCount
                            + ", " + describeDrop(mod, _currentDrop));
        }
        return stillInGrace;
    }

    private int currentDropRetryTicksRemaining() {
        if (currentDropFailureStartTick < 0) {
            return CURRENT_DROP_RETRY_GRACE_TICKS;
        }
        return Math.max(0, CURRENT_DROP_RETRY_GRACE_TICKS - (WorldHelper.getTicks() - currentDropFailureStartTick));
    }

    private void resetCurrentDropFailure() {
        currentDropFailureStartTick = -1;
        currentDropFailureLastTick = -1;
        currentDropFailureCount = 0;
    }

    private String getCurrentDropReleaseReason(AltoClef mod) {
        String nonRetryReleaseReason = getCurrentDropNonRetryReleaseReason(mod);
        if (nonRetryReleaseReason != null) {
            return nonRetryReleaseReason;
        }
        if (!mod.getEntityTracker().isEntityReachable(_currentDrop)) {
            if (isWithinCurrentDropMinLock()) {
                currentDropMinLockRetainCount++;
                pickupLogger.state("min lock retains unreachable drop " + _currentDrop.getUuid(),
                        "minimum pickup lock retained temporarily unreachable drop: ticksRemaining="
                                + currentDropMinLockTicksRemaining()
                                + ", " + describeDrop(mod, _currentDrop));
                return null;
            }
            return shouldRetryCurrentDrop(mod, "entity tracker marked unreachable")
                    ? null
                    : "target unreachable after retry grace";
        }
        return null;
    }

    private String getCurrentDropNonRetryReleaseReason(AltoClef mod) {
        if (_currentDrop == null) {
            return "missing";
        }
        if (!_currentDrop.isAlive()) {
            return "target removed";
        }
        if (_currentDrop.getStack().isEmpty()) {
            return "stack empty";
        }
        if (!matchesTargets(_currentDrop)) {
            return "current item no longer matches target";
        }
        if (_blacklist.contains(_currentDrop)) {
            return "target already blacklisted";
        }
        if (!_currentDrop.isInRange(mod.getPlayer(), CURRENT_DROP_MAX_RETAIN_DISTANCE)) {
            return "target outside allowed range";
        }
        return null;
    }

    private void resetCurrentDropFailureIfRecovered(AltoClef mod, String reason) {
        if (_currentDrop == null || currentDropFailureStartTick < 0 || !mod.getEntityTracker().isEntityReachable(_currentDrop)) {
            return;
        }
        currentDropRetryRecoveredCount++;
        pickupLogger.event("pickup retry grace recovered: reason=" + reason
                + ", failures=" + currentDropFailureCount
                + ", " + describeDrop(mod, _currentDrop));
        pickupLogger.state("retry grace recovered " + _currentDrop.getUuid(),
                "pickup retry grace recovered: reason=" + reason
                        + ", failures=" + currentDropFailureCount
                        + ", " + describeDrop(mod, _currentDrop));
        resetCurrentDropFailure();
    }

    private void armCurrentDropLock() {
        currentDropLockStartTick = WorldHelper.getTicks();
    }

    private boolean isWithinCurrentDropMinLock() {
        return currentDropLockStartTick >= 0
                && WorldHelper.getTicks() - currentDropLockStartTick < CURRENT_DROP_MIN_LOCK_TICKS;
    }

    private int currentDropMinLockTicksRemaining() {
        if (currentDropLockStartTick < 0) {
            return 0;
        }
        return Math.max(0, CURRENT_DROP_MIN_LOCK_TICKS - (WorldHelper.getTicks() - currentDropLockStartTick));
    }

    private void resetCurrentDropLock() {
        currentDropLockStartTick = -1;
    }

    private void abandonCurrentDrop(AltoClef mod, String reason, boolean blacklistEntity) {
        if (_currentDrop == null) {
            resetCurrentDropFailure();
            return;
        }
        ItemEntity abandonedDrop = _currentDrop;
        DropSnapshot abandonedDropSnapshot = getSnapshotFor(abandonedDrop);
        if (abandonedDropSnapshot == null) {
            abandonedDropSnapshot = DropSnapshot.from(abandonedDrop);
        }
        if (blacklistEntity && currentDropFailureStartTick < 0) {
            pickupLogger.event("blacklist skipped because no retry grace was observed: reason=" + reason
                    + ", " + describeDrop(mod, abandonedDrop, abandonedDropSnapshot));
            blacklistEntity = false;
        }
        if (blacklistEntity) {
            _blacklist.add(abandonedDrop);
            mod.getEntityTracker().requestEntityUnreachable(abandonedDrop);
            currentDropBlacklistCount++;
        }
        currentDropAbandonCount++;
        pickupLogger.event("abandoned drop: reason=" + reason
                + ", blacklisted=" + blacklistEntity
                + ", failures=" + currentDropFailureCount
                + ", " + describeDrop(mod, abandonedDrop, abandonedDropSnapshot));
        pickupLogger.state("abandon current drop " + abandonedDrop.getUuid() + " " + reason,
                "abandoned drop: reason=" + reason
                        + ", blacklisted=" + blacklistEntity
                        + ", failures=" + currentDropFailureCount
                        + ", blacklist=" + StlHelper.toString(_blacklist, element -> element == null ? "(null)" : element.getStack().getItem().getTranslationKey())
                        + ", " + describeDrop(mod, abandonedDrop, abandonedDropSnapshot));
        _currentDrop = null;
        currentDropSnapshot = abandonedDropSnapshot;
        resetCurrentDropFailure();
        resetCurrentDropLock();
        resetSearch();
    }

    private boolean hasPickupDiagnostics() {
        return currentDropCandidateCount
                + currentDropLockCount
                + currentDropSwitchCount
                + currentDropRetainCount
                + currentDropMinLockRetainCount
                + currentDropMovementStallCount
                + currentDropRetryStartCount
                + currentDropRetryContinueCount
                + currentDropRetryRecoveredCount
                + currentDropRetryExpiredCount
                + currentDropAbandonCount
                + currentDropBlacklistCount > 0;
    }

    private void resetPickupDiagnostics() {
        currentDropCandidateCount = 0;
        currentDropLockCount = 0;
        currentDropSwitchCount = 0;
        currentDropRetainCount = 0;
        currentDropMinLockRetainCount = 0;
        currentDropMovementStallCount = 0;
        currentDropRetryStartCount = 0;
        currentDropRetryContinueCount = 0;
        currentDropRetryRecoveredCount = 0;
        currentDropRetryExpiredCount = 0;
        currentDropAbandonCount = 0;
        currentDropBlacklistCount = 0;
    }

    private String describeTask(Task task) {
        return task == null ? "none" : task.getClass().getSimpleName();
    }

    private String describeCurrentDrop(AltoClef mod) {
        return describeDrop(mod, _currentDrop, currentDropSnapshot);
    }

    private void updateCurrentDropSnapshotForLockedDrop(ItemEntity drop) {
        DropSnapshot nextSnapshot = DropSnapshot.from(drop);
        if (nextSnapshot != null) {
            currentDropSnapshot = nextSnapshot;
        } else if (currentDropSnapshot == null || !currentDropSnapshot.matches(drop)) {
            currentDropSnapshot = null;
        }
    }

    private void updateCurrentDropSnapshotIfUseful(ItemEntity drop) {
        DropSnapshot nextSnapshot = DropSnapshot.from(drop);
        if (nextSnapshot != null) {
            currentDropSnapshot = nextSnapshot;
        }
    }

    private boolean matchesTargets(ItemEntity drop) {
        if (drop == null || drop.getStack().isEmpty()) {
            return false;
        }
        Item item = drop.getStack().getItem();
        for (ItemTarget target : itemTargets) {
            if (target != null && target.matches(item)) {
                return true;
            }
        }
        return false;
    }

    private String describeDrop(AltoClef mod, ItemEntity drop) {
        return describeDrop(mod, drop, getSnapshotFor(drop));
    }

    private String describeDrop(AltoClef mod, ItemEntity drop, DropSnapshot snapshot) {
        if (drop == null) {
            return snapshot == null ? "drop=null" : "drop=null, " + snapshot.describeAsLastKnown();
        }
        String playerPos = mod == null || mod.getPlayer() == null ? "unknown" : mod.getPlayer().getBlockPos().toShortString();
        double distance = mod == null || mod.getPlayer() == null ? -1 : drop.distanceTo(mod.getPlayer());
        String description = "drop=" + drop.getUuid()
                + ", item=" + drop.getStack().getItem().getTranslationKey()
                + " x " + drop.getStack().getCount()
                + ", dropPos=" + drop.getBlockPos().toShortString()
                + ", playerPos=" + playerPos
                + ", distance=" + formatDouble(distance)
                + ", alive=" + drop.isAlive();
        if (snapshot != null && snapshot.shouldAnnotate(drop)) {
            description += ", " + snapshot.describeAsLastKnown();
        }
        return description;
    }

    private DropSnapshot getSnapshotFor(ItemEntity drop) {
        if (drop == null || currentDropSnapshot == null || !currentDropSnapshot.matches(drop)) {
            return null;
        }
        return currentDropSnapshot;
    }

    private String formatDouble(double value) {
        if (value < 0) {
            return "unknown";
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    //20260728_kpopmodder: Preserve the originally observed dropped item for logs after Minecraft clears the entity stack.
    private static class DropSnapshot {
        private final String uuid;
        private final String itemKey;
        private final int count;
        private final String blockPos;

        private DropSnapshot(String uuid, String itemKey, int count, String blockPos) {
            this.uuid = uuid;
            this.itemKey = itemKey;
            this.count = count;
            this.blockPos = blockPos;
        }

        private static DropSnapshot from(ItemEntity drop) {
            if (drop == null || drop.getStack().isEmpty()) {
                return null;
            }
            return new DropSnapshot(
                    drop.getUuid().toString(),
                    drop.getStack().getItem().getTranslationKey(),
                    drop.getStack().getCount(),
                    drop.getBlockPos().toShortString()
            );
        }

        private boolean matches(ItemEntity drop) {
            return drop != null && uuid.equals(drop.getUuid().toString());
        }

        private boolean shouldAnnotate(ItemEntity drop) {
            return drop == null
                    || !drop.isAlive()
                    || drop.getStack().isEmpty()
                    || !itemKey.equals(drop.getStack().getItem().getTranslationKey())
                    || count != drop.getStack().getCount()
                    || !blockPos.equals(drop.getBlockPos().toShortString());
        }

        private String describeAsLastKnown() {
            return "lastKnownItem=" + itemKey + " x " + count
                    + ", lastKnownDropPos=" + blockPos;
        }
    }

}
