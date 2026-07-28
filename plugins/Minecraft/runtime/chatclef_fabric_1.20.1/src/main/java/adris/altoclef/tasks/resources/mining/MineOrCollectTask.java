package adris.altoclef.tasks.resources.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.AbstractDoToClosestObjectTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
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

    private final Block[] _blocks;
    private final ItemTarget[] _targets;
    private final LocalMiningSessionPolicy localMiningSessionPolicy;
    private final MiningTargetTracker miningTargetTracker;
    private final MineOrCollectDiagnostics diagnostics = new MineOrCollectDiagnostics();
    private final StateChangeLogger debugLogger = new StateChangeLogger("MineOrCollectTask");
    private final MiningPickupCoordinator pickupCoordinator;

    public MineOrCollectTask(Block[] blocks, ItemTarget[] targets) {
        _blocks = blocks;
        _targets = targets;
        pickupCoordinator = new MiningPickupCoordinator(_targets, diagnostics, debugLogger, this::describeDrop);
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
        if (pickupCoordinator.isManagedGoal(obj)) {
            return pickupCoordinator.getGoalPos(mod, obj);
        }
        throw new UnsupportedOperationException("Shouldn't try to get the position of object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
    }

    @Override
    protected Optional<Object> getClosestTo(AltoClef mod, Vec3d pos) {
        miningTargetTracker.pruneExpired();
        pickupCoordinator.pruneExpired();
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

        Optional<Object> pickupTarget = pickupCoordinator.getPreferredTarget(
                closestDrop.getRight(),
                closestBlock.getRight(),
                isPickupTaskContinuing(),
                pos
        );
        if (pickupTarget.isPresent()) {
            return pickupTarget;
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
            pickupCoordinator.armAfterMining(newPos);
            return new DestroyBlockTask(miningTargetTracker.miningPos());
        }
        Optional<Task> pickupTask = pickupCoordinator.getGoalTask(obj);
        if (pickupTask.isPresent()) {
            miningTargetTracker.clear();
            return pickupTask.get();
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
            return pickupCoordinator.shouldContinueActivePickup(isPickupTaskContinuing());
        }
        if (obj instanceof PostMiningSweepGoal) {
            return pickupCoordinator.isWaitingForPotentialDrops();
        }
        return false;
    }

    @Override
    protected void onStart() {
        miningTargetTracker.reset();
        pickupCoordinator.reset();
        localMiningSessionPolicy.reset();
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
        pickupCoordinator.reset();
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
        boolean shouldDelay = pickupCoordinator.shouldDelayFinish(closestDrop.getRight(), isPickupTaskContinuing());
        if (shouldDelay) {
            diagnostics.recordPostMiningSweepFinishDelay();
            debugLogger.state("post mining sweep delays finish",
                    "post mining sweep delays finish: closest="
                            + closestDrop.getRight().map(this::describeDrop).orElse("none")
                            + ", pickupTaskContinuing=" + isPickupTaskContinuing()
                            + ", sweepTicksRemaining=" + pickupCoordinator.sweepTicksRemaining());
        }
        return shouldDelay;
    }

    private boolean isPickupTaskContinuing() {
        return pickupCoordinator.isPickupTaskContinuing();
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
            pickupCoordinator.armAfterBlockBreak(release.pos());
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
