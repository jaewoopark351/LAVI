package adris.altoclef.tasks.movement.pickup;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.AbstractDoToClosestObjectTask;
import adris.altoclef.tasks.movement.GetToEntityTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
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
    private final PickupUnstuckHelper unstuckHelper = new PickupUnstuckHelper();

    // This happens all the time in mineshafts and swamps/jungles
    private final Set<ItemEntity> _blacklist = new HashSet<>();
    private final boolean _freeInventoryIfFull;
    private Task unstuckTask = null;
    // Am starting to regret not making this a singleton
    private AltoClef _mod;
    private boolean _collectingPickaxeForThisResource = false;
    private ItemEntity _currentDrop = null;
    private DropSnapshot currentDropSnapshot = null;
    //20260727_kpopmodder: Keep a short retry window so food drops are not abandoned after one pathing hiccup.
    private final CurrentDropRetentionPolicy currentDropRetention = new CurrentDropRetentionPolicy(
            CURRENT_DROP_RETRY_GRACE_TICKS,
            CURRENT_DROP_MIN_LOCK_TICKS
    );
    //20260728_kpopmodder: Summarize pickup stability decisions so latest.log can confirm reduced task switching.
    private final PickupDropDiagnostics pickupDiagnostics = new PickupDropDiagnostics();

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

    public static boolean isIsGettingPickaxeFirst(AltoClef mod) {
        return isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst();
    }

    public boolean isCollectingPickaxeForThis() {
        return _collectingPickaxeForThisResource;
    }

    @Override
    protected void onStart() {
        wanderTask.reset();
        progressChecker.reset();
        stuckCheck.reset();
        currentDropRetention.resetFailure();
        pickupLogger.reset();
        resetPickupDiagnostics();
        if (_currentDrop == null || (currentDropSnapshot != null && !currentDropSnapshot.matches(_currentDrop))) {
            currentDropSnapshot = null;
            currentDropRetention.resetLock();
        }
    }

    @Override
    protected void onStop(Task interruptTask) {
        if (pickupDiagnostics.hasEvents()) {
            pickupLogger.event(pickupDiagnostics.describeStopSummary(
                    describeTask(interruptTask),
                    describeCurrentDrop(AltoClef.getInstance())
            ));
        }
        currentDropRetention.resetFailure();
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
        if (unstuckTask != null && unstuckTask.isActive() && !unstuckTask.isFinished() && unstuckHelper.stuckInBlock(mod) != null) {
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
            BlockPos blockStuck = unstuckHelper.stuckInBlock(mod);
            if (blockStuck != null) {
                pickupLogger.state("unstuck block " + blockStuck.toShortString(),
                        "pickup approach stuck in block: block="
                                + mod.getWorld().getBlockState(blockStuck).getBlock().getTranslationKey()
                                + ", blockPos=" + blockStuck.toShortString()
                                + ", currentDrop=" + describeCurrentDrop(mod));
                unstuckTask = unstuckHelper.createUnstuckTask();
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
                pickupDiagnostics.movementStallCount++;
                // We might want to get a pickaxe first.
                if (!isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst() && !StorageHelper.miningRequirementMetInventory(MiningRequirement.STONE)) {
                    Debug.logMessage("Failed to pick up drop, will try to collect a stone pickaxe first and try again!");
                    _collectingPickaxeForThisResource = true;
                    isGettingPickaxeFirstFlag = true;
                    return getPickaxeFirstTask;
                }
                if (currentDropRetention.isWithinMinLock()) {
                    pickupDiagnostics.minLockRetainCount++;
                    setDebugState("Holding current drop.");
                    progressChecker.reset();
                    stuckCheck.reset();
                    pickupLogger.state("min lock retains stalled drop " + _currentDrop.getUuid(),
                            "minimum pickup lock retained stalled drop: ticksRemaining="
                                    + currentDropRetention.minLockTicksRemaining()
                                    + ", " + describeDrop(mod, _currentDrop));
                    return super.onTick();
                }
                if (shouldRetryCurrentDrop(mod, "movement stalled")) {
                    setDebugState("Retrying current drop.");
                    progressChecker.reset();
                    stuckCheck.reset();
                    pickupLogger.state("retry current drop " + _currentDrop.getUuid(),
                            "retrying same drop during grace: failureCount=" + currentDropRetention.failureCount()
                                    + ", ticksRemaining=" + currentDropRetention.retryTicksRemaining()
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
                pickupDiagnostics.retainCount++;
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
            pickupDiagnostics.candidateCount++;
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
            currentDropRetention.armLock();
            currentDropRetention.resetFailure();
            progressChecker.reset();
            stuckCheck.reset();
            if (previousDrop == null) {
                pickupDiagnostics.lockCount++;
                pickupLogger.state("lock drop " + itemEntity.getUuid(),
                        "locked drop: " + describeDrop(_mod, itemEntity, currentDropSnapshot));
            } else {
                pickupDiagnostics.switchCount++;
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
        boolean firstFailure = !currentDropRetention.hasFailure();
        boolean stillInGrace = currentDropRetention.recordFailureAndIsWithinGrace();
        if (firstFailure) {
            pickupDiagnostics.retryStartCount++;
            pickupLogger.event("pickup retry grace started: reason=" + reason
                    + ", graceTicks=" + currentDropRetention.retryGraceTicks()
                    + ", " + describeDrop(mod, _currentDrop));
            pickupLogger.state("start retry grace " + _currentDrop.getUuid(),
                    "pickup retry grace started: reason=" + reason
                            + ", " + describeDrop(mod, _currentDrop));
            return true;
        }
        if (stillInGrace) {
            pickupDiagnostics.retryContinueCount++;
        }
        if (!stillInGrace) {
            pickupDiagnostics.retryExpiredCount++;
            pickupLogger.event("pickup retry grace expired: reason=" + reason
                    + ", failures=" + currentDropRetention.failureCount()
                    + ", " + describeDrop(mod, _currentDrop));
            pickupLogger.state("retry grace expired " + _currentDrop.getUuid(),
                    "pickup retry grace expired: reason=" + reason
                            + ", failures=" + currentDropRetention.failureCount()
                            + ", " + describeDrop(mod, _currentDrop));
        }
        return stillInGrace;
    }

    private String getCurrentDropReleaseReason(AltoClef mod) {
        String nonRetryReleaseReason = getCurrentDropNonRetryReleaseReason(mod);
        if (nonRetryReleaseReason != null) {
            return nonRetryReleaseReason;
        }
        if (!mod.getEntityTracker().isEntityReachable(_currentDrop)) {
            if (currentDropRetention.isWithinMinLock()) {
                pickupDiagnostics.minLockRetainCount++;
                pickupLogger.state("min lock retains unreachable drop " + _currentDrop.getUuid(),
                        "minimum pickup lock retained temporarily unreachable drop: ticksRemaining="
                                + currentDropRetention.minLockTicksRemaining()
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
        if (_currentDrop == null || !currentDropRetention.hasFailure() || !mod.getEntityTracker().isEntityReachable(_currentDrop)) {
            return;
        }
        pickupDiagnostics.retryRecoveredCount++;
        pickupLogger.event("pickup retry grace recovered: reason=" + reason
                + ", failures=" + currentDropRetention.failureCount()
                + ", " + describeDrop(mod, _currentDrop));
        pickupLogger.state("retry grace recovered " + _currentDrop.getUuid(),
                "pickup retry grace recovered: reason=" + reason
                        + ", failures=" + currentDropRetention.failureCount()
                        + ", " + describeDrop(mod, _currentDrop));
        currentDropRetention.resetFailure();
    }

    private void abandonCurrentDrop(AltoClef mod, String reason, boolean blacklistEntity) {
        if (_currentDrop == null) {
            currentDropRetention.resetFailure();
            return;
        }
        ItemEntity abandonedDrop = _currentDrop;
        DropSnapshot abandonedDropSnapshot = getSnapshotFor(abandonedDrop);
        if (abandonedDropSnapshot == null) {
            abandonedDropSnapshot = DropSnapshot.from(abandonedDrop);
        }
        if (blacklistEntity && !currentDropRetention.hasFailure()) {
            pickupLogger.event("blacklist skipped because no retry grace was observed: reason=" + reason
                    + ", " + describeDrop(mod, abandonedDrop, abandonedDropSnapshot));
            blacklistEntity = false;
        }
        if (blacklistEntity) {
            _blacklist.add(abandonedDrop);
            mod.getEntityTracker().requestEntityUnreachable(abandonedDrop);
            pickupDiagnostics.blacklistCount++;
        }
        pickupDiagnostics.abandonCount++;
        pickupLogger.event("abandoned drop: reason=" + reason
                + ", blacklisted=" + blacklistEntity
                + ", failures=" + currentDropRetention.failureCount()
                + ", " + describeDrop(mod, abandonedDrop, abandonedDropSnapshot));
        pickupLogger.state("abandon current drop " + abandonedDrop.getUuid() + " " + reason,
                "abandoned drop: reason=" + reason
                        + ", blacklisted=" + blacklistEntity
                        + ", failures=" + currentDropRetention.failureCount()
                        + ", blacklist=" + StlHelper.toString(_blacklist, element -> element == null ? "(null)" : element.getStack().getItem().getTranslationKey())
                        + ", " + describeDrop(mod, abandonedDrop, abandonedDropSnapshot));
        _currentDrop = null;
        currentDropSnapshot = abandonedDropSnapshot;
        currentDropRetention.resetFailure();
        currentDropRetention.resetLock();
        resetSearch();
    }

    private void resetPickupDiagnostics() {
        pickupDiagnostics.reset();
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

}
