package adris.altoclef.tasks.resources.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.pickup.PickupDroppedItemTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.function.Function;

//20260728_kpopmodder: Added this class to keep post-mining pickup coordination out of MineOrCollectTask.
final class MiningPickupCoordinator {

    private static final int DROPPED_ITEM_PICKUP_GRACE_TICKS = 20 * 6;
    private static final int ACTIVE_PICKUP_CONTINUATION_TICKS = 20 * 4;
    private static final double DROPPED_ITEM_PICKUP_GRACE_RANGE = 16;
    private static final int POST_MINING_SWEEP_TICKS = 20 * 5;
    private static final int POST_MINING_SETTLE_TICKS = 6;
    private static final double POST_MINING_SWEEP_RANGE = 12;

    private static final PickupContinuationGoal PICKUP_CONTINUATION_GOAL = new PickupContinuationGoal();
    private static final PostMiningSweepGoal POST_MINING_SWEEP_GOAL = new PostMiningSweepGoal();

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
    private final Task pickupTask;
    private final Task postMiningSweepWaitTask = new PostMiningSweepWaitTask(postMiningSweepPolicy);
    private final MineOrCollectDiagnostics diagnostics;
    private final StateChangeLogger debugLogger;
    private final Function<ItemEntity, String> dropFormatter;

    MiningPickupCoordinator(ItemTarget[] targets,
                            MineOrCollectDiagnostics diagnostics,
                            StateChangeLogger debugLogger,
                            Function<ItemEntity, String> dropFormatter) {
        pickupTask = new PickupDroppedItemTask(targets, true);
        this.diagnostics = diagnostics;
        this.debugLogger = debugLogger;
        this.dropFormatter = dropFormatter;
    }

    void pruneExpired() {
        pickupContinuationPolicy.pruneExpired();
        postMiningSweepPolicy.pruneExpired();
    }

    Optional<Object> getPreferredTarget(Optional<ItemEntity> closestDrop,
                                        Optional<BlockPos> closestBlock,
                                        boolean pickupTaskContinuing,
                                        Vec3d playerPos) {
        Optional<ItemEntity> sweepDrop = postMiningSweepPolicy.getPreferredSweepDrop(closestDrop);
        if (sweepDrop.isPresent()) {
            diagnostics.recordPostMiningSweepPreferred();
            debugLogger.state("post mining sweep prefers dropped item",
                    "post mining sweep prefers dropped item: drop=" + describeDrop(sweepDrop.get())
                            + ", block=" + describeBlock(closestBlock)
                            + ", sweepTicksRemaining=" + postMiningSweepPolicy.sweepTicksRemaining());
            return sweepDrop.map(Object.class::cast);
        }

        if (postMiningSweepPolicy.shouldWaitForPotentialDrops(closestDrop)) {
            diagnostics.recordPostMiningSweepWait();
            debugLogger.state("post mining settle wait",
                    "post mining settle wait: waiting briefly for newly mined drops"
                            + ", block=" + describeBlock(closestBlock)
                            + ", settleTicksRemaining=" + postMiningSweepPolicy.settleTicksRemaining()
                            + ", sweepTicksRemaining=" + postMiningSweepPolicy.sweepTicksRemaining());
            return Optional.of((Object) POST_MINING_SWEEP_GOAL);
        }

        Optional<ItemEntity> graceDrop = pickupContinuationPolicy.getPreferredMinedDrop(closestDrop);
        if (graceDrop.isPresent()) {
            diagnostics.recordPickupGracePreferred();
            debugLogger.state("pickup grace prefers dropped item",
                    "pickup grace prefers dropped item: drop=" + describeDrop(graceDrop.get())
                            + ", block=" + describeBlock(closestBlock)
                            + ", ticksRemaining=" + pickupContinuationPolicy.miningGraceTicksRemaining());
            return graceDrop.map(Object.class::cast);
        }

        Optional<ItemEntity> continuationDrop = pickupContinuationPolicy.getPreferredActivePickupDrop(
                closestDrop,
                pickupTaskContinuing,
                playerPos
        );
        if (continuationDrop.isPresent()) {
            diagnostics.recordPickupContinuationPreferred();
            debugLogger.state("active pickup prefers dropped item",
                    "active pickup prefers dropped item: drop=" + describeDrop(continuationDrop.get())
                            + ", block=" + describeBlock(closestBlock)
                            + ", ticksRemaining=" + pickupContinuationPolicy.activePickupTicksRemaining());
            return continuationDrop.map(Object.class::cast);
        }

        if (pickupContinuationPolicy.shouldContinueActivePickup(pickupTaskContinuing)) {
            diagnostics.recordPickupContinuationPreferred();
            debugLogger.state("active pickup settling",
                    "active pickup settling: keeping pickup task alive"
                            + ", block=" + describeBlock(closestBlock)
                            + ", drop=" + closestDrop.map(this::describeDrop).orElse("none")
                            + ", ticksRemaining=" + pickupContinuationPolicy.activePickupTicksRemaining());
            return Optional.of((Object) PICKUP_CONTINUATION_GOAL);
        }

        return Optional.empty();
    }

    Vec3d getGoalPos(AltoClef mod, Object obj) {
        if (obj instanceof PickupContinuationGoal) {
            return pickupContinuationPolicy.activePickupPos(mod.getPlayer().getPos());
        }
        if (obj instanceof PostMiningSweepGoal) {
            return postMiningSweepPolicy.activePos(mod.getPlayer().getPos());
        }
        throw new UnsupportedOperationException("Object is not a mining pickup goal: " + obj);
    }

    Optional<Task> getGoalTask(Object obj) {
        if (obj instanceof ItemEntity drop) {
            diagnostics.recordGoalSelection("drop:" + drop.getUuid(), false);
            debugLogger.state("pickup target selected: " + describeDrop(drop));
            pickupContinuationPolicy.armActivePickup(drop);
            postMiningSweepPolicy.armAfterPickup(drop);
            return Optional.of(pickupTask);
        }
        if (obj instanceof PickupContinuationGoal) {
            diagnostics.recordGoalSelection("drop:active-pickup-continuation", false);
            debugLogger.state("continue active pickup while drop settles",
                    "continue active pickup while drop settles: ticksRemaining="
                            + pickupContinuationPolicy.activePickupTicksRemaining());
            return Optional.of(pickupTask);
        }
        if (obj instanceof PostMiningSweepGoal) {
            diagnostics.recordGoalSelection("drop:post-mining-settle", false);
            debugLogger.state("wait for post mining settle",
                    "wait for post mining settle: settleTicksRemaining="
                            + postMiningSweepPolicy.settleTicksRemaining()
                            + ", sweepTicksRemaining=" + postMiningSweepPolicy.sweepTicksRemaining());
            return Optional.of(postMiningSweepWaitTask);
        }
        return Optional.empty();
    }

    boolean isManagedGoal(Object obj) {
        return obj instanceof PickupContinuationGoal || obj instanceof PostMiningSweepGoal;
    }

    boolean shouldContinueActivePickup(boolean pickupTaskContinuing) {
        return pickupContinuationPolicy.shouldContinueActivePickup(pickupTaskContinuing);
    }

    boolean isWaitingForPotentialDrops() {
        return postMiningSweepPolicy.isWaitingForPotentialDrops();
    }

    boolean shouldDelayFinish(Optional<ItemEntity> closestDrop, boolean pickupTaskContinuing) {
        return postMiningSweepPolicy.shouldDelayFinish(closestDrop, pickupTaskContinuing);
    }

    boolean isPickupTaskContinuing() {
        return pickupTask.isActive()
                && !pickupTask.isFinished()
                && !pickupTask.thisOrChildAreTimedOut();
    }

    int sweepTicksRemaining() {
        return postMiningSweepPolicy.sweepTicksRemaining();
    }

    void armAfterMining(BlockPos origin) {
        pickupContinuationPolicy.armAfterMining(origin);
    }

    void armAfterBlockBreak(BlockPos origin) {
        postMiningSweepPolicy.armAfterBlockBreak(origin);
    }

    void reset() {
        pickupContinuationPolicy.reset();
        postMiningSweepPolicy.reset();
        postMiningSweepWaitTask.reset();
    }

    private String describeDrop(ItemEntity drop) {
        return dropFormatter.apply(drop);
    }

    private String describeBlock(Optional<BlockPos> block) {
        return block.map(BlockPos::toShortString).orElse("none");
    }
}
