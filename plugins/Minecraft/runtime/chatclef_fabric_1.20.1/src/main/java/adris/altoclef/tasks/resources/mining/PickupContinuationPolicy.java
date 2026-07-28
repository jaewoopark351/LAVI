package adris.altoclef.tasks.resources.mining;

import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

//20260728_kpopmodder: Keeps mine/drop switching policy separate from target scoring.
final class PickupContinuationPolicy {
    private final int miningGraceTicks;
    private final int activePickupTicks;
    private final double maxDistanceSq;
    private int miningGraceUntilTick;
    private int activePickupUntilTick;
    private BlockPos miningOrigin;
    private Vec3d activePickupPos;

    PickupContinuationPolicy(int miningGraceTicks, int activePickupTicks, double maxDistance) {
        this.miningGraceTicks = miningGraceTicks;
        this.activePickupTicks = activePickupTicks;
        maxDistanceSq = maxDistance * maxDistance;
    }

    void armAfterMining(BlockPos origin) {
        miningOrigin = origin;
        miningGraceUntilTick = WorldHelper.getTicks() + miningGraceTicks;
        activePickupUntilTick = 0;
    }

    void armActivePickup(ItemEntity drop) {
        if (isUsableDrop(drop)) {
            activePickupUntilTick = WorldHelper.getTicks() + activePickupTicks;
            activePickupPos = drop.getPos();
        }
    }

    Optional<ItemEntity> getPreferredMinedDrop(Optional<ItemEntity> closestDrop) {
        pruneExpired();
        if (miningOrigin == null || closestDrop.isEmpty()) {
            return Optional.empty();
        }

        ItemEntity drop = closestDrop.get();
        if (!isUsableDrop(drop)) {
            return Optional.empty();
        }

        double distanceSq = drop.getPos().squaredDistanceTo(WorldHelper.toVec3d(miningOrigin));
        if (distanceSq > maxDistanceSq) {
            return Optional.empty();
        }
        return Optional.of(drop);
    }

    Optional<ItemEntity> getPreferredActivePickupDrop(Optional<ItemEntity> closestDrop, boolean pickupTaskContinuing, Vec3d playerPos) {
        pruneExpired();
        if (!pickupTaskContinuing || activePickupUntilTick <= WorldHelper.getTicks() || closestDrop.isEmpty()) {
            return Optional.empty();
        }

        ItemEntity drop = closestDrop.get();
        if (!isUsableDrop(drop) || drop.getPos().squaredDistanceTo(playerPos) > maxDistanceSq) {
            return Optional.empty();
        }
        return Optional.of(drop);
    }

    boolean shouldContinueActivePickup(boolean pickupTaskContinuing) {
        pruneExpired();
        return pickupTaskContinuing && activePickupUntilTick > WorldHelper.getTicks();
    }

    Vec3d activePickupPos(Vec3d fallback) {
        return activePickupPos == null ? fallback : activePickupPos;
    }

    int miningGraceTicksRemaining() {
        return Math.max(0, miningGraceUntilTick - WorldHelper.getTicks());
    }

    int activePickupTicksRemaining() {
        return Math.max(0, activePickupUntilTick - WorldHelper.getTicks());
    }

    void pruneExpired() {
        int currentTick = WorldHelper.getTicks();
        if (miningOrigin != null && currentTick > miningGraceUntilTick) {
            miningOrigin = null;
            miningGraceUntilTick = 0;
        }
        if (activePickupUntilTick > 0 && currentTick > activePickupUntilTick) {
            activePickupUntilTick = 0;
            activePickupPos = null;
        }
    }

    void reset() {
        miningOrigin = null;
        miningGraceUntilTick = 0;
        activePickupUntilTick = 0;
        activePickupPos = null;
    }

    private boolean isUsableDrop(ItemEntity drop) {
        return drop != null && drop.isAlive() && !drop.getStack().isEmpty();
    }
}
