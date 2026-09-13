//#if MC == 12001
package lavi.minecraft.blocks.protection.calculation;

import lavi.minecraft.blocks.protection.state.ProtectionSnapshot;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.function.Predicate;

//20260913_kpopmodder: Client-owned bounded calculation keeps unvisited regions explicit and patches observed block changes.
public final class ProtectionCalculation {
    public static final int POSITIONS_PER_TICK = 4_096;
    private final Set<BlockPos> markers = new HashSet<>();
    private final Set<BlockPos> protectedBlocks = new HashSet<>();
    private final LinkedHashSet<BlockPos> pending = new LinkedHashSet<>();
    private final Set<BlockPos> repeatAfterCurrent = new HashSet<>();
    private ProtectionVolumeCursor cursor;
    private long revision;

    public boolean indicators(Set<BlockPos> next) {
        if (markers.equals(next)) return false;
        for (BlockPos marker : next) if (!markers.contains(marker)) pending.add(marker.toImmutable());
        markers.clear(); markers.addAll(next);
        pending.retainAll(markers);
        repeatAfterCurrent.retainAll(markers);
        if (cursor != null && !markers.contains(cursor.marker())) cursor = null;
        protectedBlocks.removeIf(pos -> !nearAny(pos));
        revision++;
        return true;
    }
    public boolean indicatorChanged(BlockPos pos, boolean present) {
        HashSet<BlockPos> next = new HashSet<>(markers);
        if (present) next.add(pos.toImmutable()); else next.remove(pos);
        return indicators(next);
    }
    public int advance(Predicate<BlockPos> isProtectedType) {
        int visited = 0;
        while (visited < POSITIONS_PER_TICK && !pending.isEmpty()) {
            if (cursor == null) cursor = new ProtectionVolumeCursor(pending.iterator().next());
            BlockPos pos = cursor.next();
            if (isProtectedType.test(pos)) protectedBlocks.add(pos); else protectedBlocks.remove(pos);
            visited++;
            if (!cursor.hasNext()) {
                BlockPos finished = cursor.marker();
                pending.remove(finished);
                if (repeatAfterCurrent.remove(finished)) pending.add(finished);
                cursor = null;
            }
        }
        if (visited > 0) revision++;
        return visited;
    }
    public boolean blockChanged(BlockPos pos, boolean isProtectedType) {
        if (!nearAny(pos)) return false;
        boolean changed = isProtectedType ? protectedBlocks.add(pos.toImmutable()) : protectedBlocks.remove(pos);
        if (changed) revision++;
        return changed;
    }
    public void chunkChanged(int x, int z) {
        for (BlockPos marker : markers) {
            if ((marker.getX() - 16 >> 4) <= x && x <= (marker.getX() + 16 >> 4)
                    && (marker.getZ() - 16 >> 4) <= z && z <= (marker.getZ() + 16 >> 4)) {
                if (cursor != null && cursor.marker().equals(marker)) repeatAfterCurrent.add(marker);
                else pending.add(marker);
            }
        }
        revision++;
    }
    private boolean nearAny(BlockPos pos) {
        for (BlockPos marker : markers) if (ProtectionSnapshot.inRange(marker, pos)) return true;
        return false;
    }
    public Set<BlockPos> completeBlocks() {
        HashSet<BlockPos> result = new HashSet<>(protectedBlocks);
        // Incomplete regions are represented only as uncertainty, never as a completed partial result.
        result.removeIf(pos -> pending.stream().anyMatch(marker -> ProtectionSnapshot.inRange(marker, pos)));
        return result;
    }
    public Set<BlockPos> pending() { return Set.copyOf(pending); }
    public long revision() { return revision; }
}

//#endif
