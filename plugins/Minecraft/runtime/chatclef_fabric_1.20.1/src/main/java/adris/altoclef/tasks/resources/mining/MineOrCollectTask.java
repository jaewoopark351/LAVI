package adris.altoclef.tasks.resources.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.AbstractDoToClosestObjectTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.pickup.PickupDroppedItemTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

//20260728_kpopmodder: Added this type file to keep the mine/drop task separate from MineAndCollectTask orchestration.
public class MineOrCollectTask extends AbstractDoToClosestObjectTask<Object> {

    //20260728_kpopmodder: Give drops a clearer ownership window before mining can take over again.
    private static final int DROPPED_ITEM_PICKUP_GRACE_TICKS = 20 * 6;
    private static final int ACTIVE_PICKUP_CONTINUATION_TICKS = 20 * 4;
    private static final double DROPPED_ITEM_PICKUP_GRACE_RANGE = 16;
    //20260728_kpopmodder: Sweep nearby drops briefly after a mined block disappears before chasing more blocks.
    private static final int POST_MINING_SWEEP_TICKS = 20 * 5;
    private static final int POST_MINING_SETTLE_TICKS = 15;
    private static final double POST_MINING_SWEEP_RANGE = 12;

    private static final PickupContinuationGoal PICKUP_CONTINUATION_GOAL = new PickupContinuationGoal();
    private static final PostMiningSweepGoal POST_MINING_SWEEP_GOAL = new PostMiningSweepGoal();

    private final Block[] _blocks;
    private final ItemTarget[] _targets;
    private final PickupContinuationPolicy pickupContinuationPolicy = new PickupContinuationPolicy(
            DROPPED_ITEM_PICKUP_GRACE_TICKS,
            ACTIVE_PICKUP_CONTINUATION_TICKS,
            DROPPED_ITEM_PICKUP_GRACE_RANGE
    );
    private final PostMiningSweepPolicy postMiningSweepPolicy = new PostMiningSweepPolicy(
            POST_MINING_SWEEP_TICKS,
            POST_MINING_SETTLE_TICKS,
            POST_MINING_SWEEP_RANGE
    );
    private final LocalMiningSessionPolicy localMiningSessionPolicy;
    private final MiningTargetTracker miningTargetTracker;
    private final MineOrCollectDiagnostics diagnostics = new MineOrCollectDiagnostics();
    private final Task _pickupTask;
    private final Task postMiningSweepWaitTask = new PostMiningSweepWaitTask(postMiningSweepPolicy);
    private final StateChangeLogger debugLogger = new StateChangeLogger("MineOrCollectTask");

    public MineOrCollectTask(Block[] blocks, ItemTarget[] targets) {
        _blocks = blocks;
        _targets = targets;
        _pickupTask = new PickupDroppedItemTask(_targets, true);
        miningTargetTracker = new MiningTargetTracker(blocks);
        localMiningSessionPolicy = new LocalMiningSessionPolicy(blocks);
    }

    @Override
    protected Vec3d getPos(AltoClef mod, Object obj) {
        if (obj instanceof BlockPos b) {
            return WorldHelper.toVec3d(b);
        }
        if (obj instanceof ItemEntity item) {
            return item.getPos();
        }
        if (obj instanceof PickupContinuationGoal) {
            return pickupContinuationPolicy.activePickupPos(mod.getPlayer().getPos());
        }
        if (obj instanceof PostMiningSweepGoal) {
            return postMiningSweepPolicy.activePos(mod.getPlayer().getPos());
        }
        throw new UnsupportedOperationException("Shouldn't try to get the position of object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
    }

    @Override
    protected Optional<Object> getClosestTo(AltoClef mod, Vec3d pos) {
        miningTargetTracker.pruneExpired();
        pickupContinuationPolicy.pruneExpired();
        postMiningSweepPolicy.pruneExpired();
        localMiningSessionPolicy.pruneExpired(mod);
        miningTargetTracker.releaseCompleted(mod, this::handleMiningTargetRelease);

        Pair<Double, Optional<BlockPos>> closestBlock = getClosestBlock(mod, pos, miningTargetTracker::isAllowedCandidate, _blocks);
        Pair<Double, Optional<ItemEntity>> closestDrop = getClosestItemDrop(mod, pos, _targets);

        double blockSq = closestBlock.getLeft();
        double dropSq = closestDrop.getLeft();

        // We can't mine right now.
        if (mod.getExtraBaritoneSettings().isInteractionPaused()) {
            diagnostics.recordInteractionPausedDropPreferred();
            debugLogger.state("interaction paused; prefer dropped item: drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                    + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                    + ", skippedBlocks=" + miningTargetTracker.skippedBlockCount());
            return closestDrop.getRight().map(Object.class::cast);
        }

        Optional<ItemEntity> sweepDrop = postMiningSweepPolicy.getPreferredSweepDrop(closestDrop.getRight());
        if (sweepDrop.isPresent()) {
            diagnostics.recordPostMiningSweepPreferred();
            debugLogger.state("post mining sweep prefers dropped item",
                    "post mining sweep prefers dropped item: drop=" + describeDrop(sweepDrop.get())
                            + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                            + ", sweepTicksRemaining=" + postMiningSweepPolicy.sweepTicksRemaining());
            return sweepDrop.map(Object.class::cast);
        }

        if (postMiningSweepPolicy.shouldWaitForPotentialDrops(closestDrop.getRight())) {
            diagnostics.recordPostMiningSweepWait();
            debugLogger.state("post mining sweep settling",
                    "post mining sweep settling: waiting for nearby drops"
                            + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                            + ", settleTicksRemaining=" + postMiningSweepPolicy.settleTicksRemaining()
                            + ", sweepTicksRemaining=" + postMiningSweepPolicy.sweepTicksRemaining());
            return Optional.of((Object) POST_MINING_SWEEP_GOAL);
        }

        Optional<ItemEntity> graceDrop = pickupContinuationPolicy.getPreferredMinedDrop(closestDrop.getRight());
        if (graceDrop.isPresent()) {
            diagnostics.recordPickupGracePreferred();
            debugLogger.state("pickup grace prefers dropped item",
                    "pickup grace prefers dropped item: drop=" + describeDrop(graceDrop.get())
                            + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                            + ", ticksRemaining=" + pickupContinuationPolicy.miningGraceTicksRemaining());
            return graceDrop.map(Object.class::cast);
        }

        Optional<ItemEntity> continuationDrop = pickupContinuationPolicy.getPreferredActivePickupDrop(
                closestDrop.getRight(),
                isPickupTaskContinuing(),
                pos
        );
        if (continuationDrop.isPresent()) {
            diagnostics.recordPickupContinuationPreferred();
            debugLogger.state("active pickup prefers dropped item",
                    "active pickup prefers dropped item: drop=" + describeDrop(continuationDrop.get())
                            + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                            + ", ticksRemaining=" + pickupContinuationPolicy.activePickupTicksRemaining());
            return continuationDrop.map(Object.class::cast);
        }

        if (pickupContinuationPolicy.shouldContinueActivePickup(isPickupTaskContinuing())) {
            diagnostics.recordPickupContinuationPreferred();
            debugLogger.state("active pickup settling",
                    "active pickup settling: keeping pickup task alive"
                            + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                            + ", drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                            + ", ticksRemaining=" + pickupContinuationPolicy.activePickupTicksRemaining());
            return Optional.of((Object) PICKUP_CONTINUATION_GOAL);
        }

        Optional<ItemEntity> currentMiningInterruptDrop = miningTargetTracker.getDropCloserThanCurrentTarget(mod, pos, closestDrop.getRight(), dropSq);
        if (currentMiningInterruptDrop.isPresent()) {
            diagnostics.recordDropPreferred();
            debugLogger.state("closest drop interrupts mining target " + currentMiningInterruptDrop.get().getUuid(),
                    "closest dropped item interrupts retained mining target: drop="
                            + describeDrop(currentMiningInterruptDrop.get())
                            + ", miningTarget=" + describePos(miningTargetTracker.miningPos())
                            + ", miningSq=" + formatDouble(miningTargetTracker.currentTargetDistanceSq(pos))
                            + ", dropSq=" + formatDouble(dropSq));
            return currentMiningInterruptDrop.map(Object.class::cast);
        }

        Optional<BlockPos> retainedMiningTarget = miningTargetTracker.retainOrRelease(mod, this::handleMiningTargetRelease);
        if (retainedMiningTarget.isPresent()) {
            diagnostics.recordBlockPreferred();
            diagnostics.recordMiningTargetRetained();
            BlockPos retained = retainedMiningTarget.get();
            debugLogger.state("retain mining target " + retained.toShortString(),
                    "retained mining target: current=" + retained.toShortString()
                            + ", currentSq=" + formatDouble(BlockPosVer.getSquaredDistance(retained, pos))
                            + ", nearestBlock=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                            + ", nearestBlockSq=" + formatDouble(blockSq)
                            + ", drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                            + ", dropSq=" + formatDouble(dropSq));
            return retainedMiningTarget.map(Object.class::cast);
        }

        Optional<BlockPos> localMiningTarget = localMiningSessionPolicy.getPreferredLocalBlock(mod, pos, miningTargetTracker::isAllowedCandidate, _blocks);
        if (localMiningTarget.isPresent()) {
            BlockPos local = localMiningTarget.get();
            diagnostics.recordBlockPreferred();
            diagnostics.recordLocalMiningPreferred();
            debugLogger.state("local mining session prefers block " + local.toShortString(),
                    "local mining session prefers block: block=" + local.toShortString()
                            + ", anchor=" + localMiningSessionPolicy.describeAnchor()
                            + ", ticksRemaining=" + localMiningSessionPolicy.ticksRemaining()
                            + ", drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                            + ", dropSq=" + formatDouble(dropSq));
            return localMiningTarget.map(Object.class::cast);
        }

        if (dropSq <= blockSq) {
            diagnostics.recordDropPreferred();
            debugLogger.state("closest target is dropped item: drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                    + ", dropSq=" + formatDouble(dropSq)
                    + ", blockSq=" + formatDouble(blockSq)
                    + ", skippedBlocks=" + miningTargetTracker.skippedBlockCount());
            return closestDrop.getRight().map(Object.class::cast);
        } else {
            diagnostics.recordBlockPreferred();
            debugLogger.state("closest target is block: block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                    + ", blockSq=" + formatDouble(blockSq)
                    + ", dropSq=" + formatDouble(dropSq)
                    + ", skippedBlocks=" + miningTargetTracker.skippedBlockCount());
            return closestBlock.getRight().map(Object.class::cast);
        }
    }

    public static Pair<Double, Optional<ItemEntity>> getClosestItemDrop(AltoClef mod, Vec3d pos, ItemTarget... items) {
        return MiningTargetScanner.getClosestItemDrop(mod, pos, items);
    }

    public static Pair<Double, Optional<BlockPos>> getClosestBlock(AltoClef mod, Vec3d pos, Block... blocks) {
        return MiningTargetScanner.getClosestBlock(mod, pos, blocks);
    }

    public static Pair<Double, Optional<BlockPos>> getClosestBlock(AltoClef mod, Vec3d pos, Predicate<BlockPos> isValidTest, Block... blocks) {
        return MiningTargetScanner.getClosestBlock(mod, pos, isValidTest, blocks);
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        return mod.getPlayer().getPos();
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        miningTargetTracker.resetProgressIfPathing(mod);
        Optional<MiningTargetTracker.Skip> skip = miningTargetTracker.skipIfTimedOutOrStuck(mod);
        if (skip.isPresent()) {
            handleMiningTargetSkip(skip.get());
        }
        return super.onTick();
    }

    @Override
    protected Task getGoalTask(Object obj) {
        if (obj instanceof BlockPos newPos) {
            if (miningTargetTracker.selectMiningTarget(newPos)) {
                diagnostics.recordGoalSelection("block:" + newPos.toShortString(), true);
                debugLogger.state("new mining target: pos=" + newPos.toShortString()
                        + ", timeoutTicks=" + miningTargetTracker.targetTimeoutTicks());
            }
            localMiningSessionPolicy.armAfterMiningTarget(newPos, AltoClef.getInstance());
            pickupContinuationPolicy.armAfterMining(newPos);
            return new DestroyBlockTask(miningTargetTracker.miningPos());
        }
        if (obj instanceof ItemEntity drop) {
            diagnostics.recordGoalSelection("drop:" + drop.getUuid(), false);
            debugLogger.state("pickup target selected: " + describeDrop(drop));
            miningTargetTracker.clear();
            pickupContinuationPolicy.armActivePickup(drop);
            postMiningSweepPolicy.armAfterPickup(drop);
            return _pickupTask;
        }
        if (obj instanceof PickupContinuationGoal) {
            diagnostics.recordGoalSelection("drop:active-pickup-continuation", false);
            debugLogger.state("continue active pickup while drop settles",
                    "continue active pickup while drop settles: ticksRemaining="
                            + pickupContinuationPolicy.activePickupTicksRemaining());
            miningTargetTracker.clear();
            return _pickupTask;
        }
        if (obj instanceof PostMiningSweepGoal) {
            diagnostics.recordGoalSelection("drop:post-mining-sweep", false);
            debugLogger.state("wait for post mining sweep",
                    "wait for post mining sweep: settleTicksRemaining="
                            + postMiningSweepPolicy.settleTicksRemaining()
                            + ", sweepTicksRemaining=" + postMiningSweepPolicy.sweepTicksRemaining());
            miningTargetTracker.clear();
            return postMiningSweepWaitTask;
        }
        throw new UnsupportedOperationException("Shouldn't try to get the goal from object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
    }

    @Override
    protected boolean isValid(AltoClef mod, Object obj) {
        if (obj instanceof BlockPos b) {
            return mod.getBlockScanner().isBlockAtPosition(b, _blocks)
                    && miningTargetTracker.isAllowedCandidate(b)
                    && !mod.getBlockScanner().isUnreachable(b)
                    && WorldHelper.canBreak(b);
        }
        if (obj instanceof ItemEntity drop) {
            Item item = drop.getStack().getItem();
            if (_targets != null) {
                for (ItemTarget target : _targets) {
                    if (target.matches(item)) return true;
                }
            }
            return false;
        }
        if (obj instanceof PickupContinuationGoal) {
            return pickupContinuationPolicy.shouldContinueActivePickup(isPickupTaskContinuing());
        }
        if (obj instanceof PostMiningSweepGoal) {
            return postMiningSweepPolicy.isWaitingForPotentialDrops();
        }
        return false;
    }

    @Override
    protected void onStart() {
        miningTargetTracker.reset();
        pickupContinuationPolicy.reset();
        postMiningSweepPolicy.reset();
        localMiningSessionPolicy.reset();
        postMiningSweepWaitTask.reset();
        diagnostics.reset();
        debugLogger.event("start: blocks=" + Arrays.toString(_blocks)
                + ", targets=" + Arrays.toString(_targets)
                + ", localMiningSession=" + localMiningSessionPolicy.isEnabled());
    }

    @Override
    protected void onStop(Task interruptTask) {
        debugLogger.event("stop: interruptedBy=" + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName())
                + ", miningPos=" + describePos(miningTargetTracker.miningPos()));
        if (diagnostics.hasEvents()) {
            debugLogger.event(diagnostics.summary(interruptTask));
        }
        pickupContinuationPolicy.reset();
        postMiningSweepPolicy.reset();
        localMiningSessionPolicy.reset();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof MineOrCollectTask task) {
            return Arrays.equals(task._blocks, _blocks) && Arrays.equals(task._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Mining or Collecting";
    }

    public boolean isMining() {
        return miningTargetTracker.isMining();
    }

    public BlockPos miningPos() {
        return miningTargetTracker.miningPos();
    }

    public boolean shouldDelayResourceFinish(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return false;
        }
        Pair<Double, Optional<ItemEntity>> closestDrop = getClosestItemDrop(mod, mod.getPlayer().getPos(), _targets);
        boolean shouldDelay = postMiningSweepPolicy.shouldDelayFinish(closestDrop.getRight(), isPickupTaskContinuing());
        if (shouldDelay) {
            diagnostics.recordPostMiningSweepFinishDelay();
            debugLogger.state("post mining sweep delays finish",
                    "post mining sweep delays finish: closest="
                            + closestDrop.getRight().map(this::describeDrop).orElse("none")
                            + ", pickupTaskContinuing=" + isPickupTaskContinuing()
                            + ", sweepTicksRemaining=" + postMiningSweepPolicy.sweepTicksRemaining());
        }
        return shouldDelay;
    }

    private boolean isPickupTaskContinuing() {
        return _pickupTask.isActive()
                && !_pickupTask.isFinished()
                && !_pickupTask.thisOrChildAreTimedOut();
    }

    private void handleMiningTargetSkip(MiningTargetTracker.Skip skip) {
        diagnostics.recordTemporaryMiningSkip();
        Debug.logMessage("Temporarily skipping mining target " + skip.pos().toShortString() + " (" + skip.reason() + ").");
        debugLogger.state("temporarily skip mining target: pos=" + skip.pos().toShortString()
                + ", reason=" + skip.reason()
                + ", skipTicks=" + skip.skipTicks()
                + ", skippedBlocks=" + skip.skippedBlocks());
        resetSearch();
    }

    private void handleMiningTargetRelease(MiningTargetTracker.Release release) {
        diagnostics.recordMiningTargetRelease();
        if (release.blockChanged()) {
            postMiningSweepPolicy.armAfterBlockBreak(release.pos());
            localMiningSessionPolicy.armAfterBlockBreak(release.pos());
        }
        debugLogger.state("release mining target " + release.pos().toShortString() + " " + release.reason(),
                "released mining target: pos=" + release.pos().toShortString()
                        + ", reason=" + release.reason());
    }

    private String describeDrop(ItemEntity drop) {
        return drop.getStack().getItem().getTranslationKey()
                + " x " + drop.getStack().getCount()
                + " at " + drop.getBlockPos().toShortString();
    }

    private String describePos(BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }

    private String formatDouble(double value) {
        if (Double.isInfinite(value)) {
            return "infinity";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
