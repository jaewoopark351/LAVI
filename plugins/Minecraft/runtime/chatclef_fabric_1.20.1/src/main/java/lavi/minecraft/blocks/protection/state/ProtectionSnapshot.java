//#if MC == 12001
package lavi.minecraft.blocks.protection.state;

import net.minecraft.util.math.BlockPos;
import java.util.Set;

//20260913_kpopmodder: One safely published value binds protection and uncertainty to the same world lifetime.
public record ProtectionSnapshot(Object world, Object player, Object dimension, long sourceRevision,
                                 long revision, String status, Set<BlockPos> protectedBlocks,
                                 Set<BlockPos> pendingIndicators) {
    public ProtectionSnapshot {
        protectedBlocks = protectedBlocks.stream().map(BlockPos::toImmutable).collect(java.util.stream.Collectors.toUnmodifiableSet());
        pendingIndicators = pendingIndicators.stream().map(BlockPos::toImmutable).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public boolean avoids(BlockPos pos) {
        if (pos == null || "UNAVAILABLE".equals(status) || "FAILED".equals(status)) return true;
        if (protectedBlocks.contains(pos)) return true;
        for (BlockPos marker : pendingIndicators) if (inRange(marker, pos)) return true;
        return false;
    }
    public static boolean inRange(BlockPos marker, BlockPos pos) {
        return Math.abs((long) marker.getX() - pos.getX()) <= 16
                && Math.abs((long) marker.getY() - pos.getY()) <= 16
                && Math.abs((long) marker.getZ() - pos.getZ()) <= 16;
    }
    public static ProtectionSnapshot unavailable(long revision) {
        return new ProtectionSnapshot(null, null, null, -1, revision, "UNAVAILABLE", Set.of(), Set.of());
    }
}

//#endif
