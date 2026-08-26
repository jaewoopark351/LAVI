package lavi.minecraft.diagnostics.container.store.deposit.candidate.range;

import adris.altoclef.multiversion.blockpos.BlockPosVer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record StoreContainerRangeSnapshot(Vec3d playerPosition,
                                          BlockPos rawTarget,
                                          boolean rawRangeEvaluated,
                                          boolean rawWithin50,
                                          Double rawDistanceSquared,
                                          BlockPos currentTryTarget,
                                          boolean currentTryRangeEvaluated,
                                          boolean currentTryWithin70,
                                          Double currentTryDistanceSquared) {
    public static final double RAW_RANGE_THRESHOLD_SQUARED = 50.0 * 50.0;
    public static final double CURRENT_TRY_RANGE_THRESHOLD_SQUARED = 70.0 * 70.0;

    public static StoreContainerRangeSnapshot capture(Vec3d playerPosition,
                                                      BlockPos rawTarget,
                                                      boolean rawRangeEvaluated,
                                                      boolean rawWithin50,
                                                      BlockPos currentTryTarget,
                                                      boolean currentTryRangeEvaluated,
                                                      boolean currentTryWithin70) {
        return new StoreContainerRangeSnapshot(
                playerPosition,
                immutable(rawTarget),
                rawRangeEvaluated,
                rawWithin50,
                distanceSquared(rawTarget, playerPosition),
                immutable(currentTryTarget),
                currentTryRangeEvaluated,
                currentTryWithin70,
                distanceSquared(currentTryTarget, playerPosition)
        );
    }

    public static StoreContainerRangeSnapshot unavailable() {
        return capture(null, null, false, false, null, false, false);
    }

    private static Double distanceSquared(BlockPos target, Vec3d playerPosition) {
        if (target == null || playerPosition == null) {
            return null;
        }
        try {
            return BlockPosVer.getSquaredDistance(target, playerPosition);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static BlockPos immutable(BlockPos position) {
        return position == null ? null : position.toImmutable();
    }
}
