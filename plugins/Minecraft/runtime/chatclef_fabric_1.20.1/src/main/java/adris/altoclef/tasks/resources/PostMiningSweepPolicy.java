package adris.altoclef.tasks.resources;

import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

//20260728_kpopmodder: Keeps a short local pickup sweep after a mined block produces item drops.
final class PostMiningSweepPolicy {
    private final int sweepTicks;
    private final int settleTicks;
    private final double maxDistanceSq;
    private int sweepUntilTick;
    private int settleUntilTick;
    private BlockPos sweepOrigin;
    private Vec3d lastDropPos;

    PostMiningSweepPolicy(int sweepTicks, int settleTicks, double maxDistance) {
        this.sweepTicks = sweepTicks;
        this.settleTicks = settleTicks;
        maxDistanceSq = maxDistance * maxDistance;
    }

    void armAfterBlockBreak(BlockPos origin) {
        if (origin == null) {
            return;
        }
        int now = WorldHelper.getTicks();
        sweepOrigin = origin;
        sweepUntilTick = Math.max(sweepUntilTick, now + sweepTicks);
        settleUntilTick = Math.max(settleUntilTick, now + settleTicks);
    }

    void armAfterPickup(ItemEntity drop) {
        if (!isUsableDrop(drop)) {
            return;
        }
        int now = WorldHelper.getTicks();
        lastDropPos = drop.getPos();
        if (sweepOrigin == null) {
            sweepOrigin = drop.getBlockPos();
        }
        sweepUntilTick = Math.max(sweepUntilTick, now + sweepTicks);
        settleUntilTick = Math.max(settleUntilTick, now + settleTicks);
    }

    Optional<ItemEntity> getPreferredSweepDrop(Optional<ItemEntity> closestDrop) {
        pruneExpired();
        if (!isActive() || closestDrop.isEmpty()) {
            return Optional.empty();
        }
        ItemEntity drop = closestDrop.get();
        if (!isUsableDrop(drop) || !isWithinSweepRange(drop)) {
            return Optional.empty();
        }
        return Optional.of(drop);
    }

    boolean shouldWaitForPotentialDrops(Optional<ItemEntity> closestDrop) {
        pruneExpired();
        if (!isWaitingForPotentialDrops()) {
            return false;
        }
        if (closestDrop.isPresent() && isUsableDrop(closestDrop.get()) && isWithinSweepRange(closestDrop.get())) {
            return false;
        }
        return closestDrop.isEmpty();
    }

    boolean shouldDelayFinish(Optional<ItemEntity> closestDrop, boolean pickupTaskContinuing) {
        pruneExpired();
        if (!isActive()) {
            return false;
        }
        if (pickupTaskContinuing) {
            return true;
        }
        return getPreferredSweepDrop(closestDrop).isPresent();
    }

    boolean isWaitingForPotentialDrops() {
        pruneExpired();
        return sweepOrigin != null && settleUntilTick > WorldHelper.getTicks();
    }

    Vec3d activePos(Vec3d fallback) {
        if (lastDropPos != null) {
            return lastDropPos;
        }
        if (sweepOrigin != null) {
            return WorldHelper.toVec3d(sweepOrigin);
        }
        return fallback;
    }

    int sweepTicksRemaining() {
        return Math.max(0, sweepUntilTick - WorldHelper.getTicks());
    }

    int settleTicksRemaining() {
        return Math.max(0, settleUntilTick - WorldHelper.getTicks());
    }

    void pruneExpired() {
        int currentTick = WorldHelper.getTicks();
        if (sweepOrigin != null && currentTick > sweepUntilTick) {
            reset();
            return;
        }
        if (settleUntilTick > 0 && currentTick > settleUntilTick) {
            settleUntilTick = 0;
        }
    }

    void reset() {
        sweepOrigin = null;
        lastDropPos = null;
        sweepUntilTick = 0;
        settleUntilTick = 0;
    }

    private boolean isActive() {
        return sweepOrigin != null && sweepUntilTick > WorldHelper.getTicks();
    }

    private boolean isWithinSweepRange(ItemEntity drop) {
        Vec3d origin = sweepOrigin == null ? lastDropPos : WorldHelper.toVec3d(sweepOrigin);
        return origin != null && drop.getPos().squaredDistanceTo(origin) <= maxDistanceSq;
    }

    private boolean isUsableDrop(ItemEntity drop) {
        return drop != null && drop.isAlive() && !drop.getStack().isEmpty();
    }
}
