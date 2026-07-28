package adris.altoclef.tasks.resources.mining;

import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

//20260728_kpopmodder: Keep per-task block cooldown bookkeeping separate from mining candidate selection.
final class TemporaryBlockBlacklist {
    private final Map<BlockPos, Integer> skipUntilTick = new HashMap<>();

    void add(BlockPos pos, int ticks) {
        skipUntilTick.put(pos, WorldHelper.getTicks() + ticks);
    }

    boolean contains(BlockPos pos) {
        pruneExpired();
        Integer untilTick = skipUntilTick.get(pos);
        return untilTick != null && untilTick > WorldHelper.getTicks();
    }

    int size() {
        pruneExpired();
        return skipUntilTick.size();
    }

    void pruneExpired() {
        int currentTick = WorldHelper.getTicks();
        skipUntilTick.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
    }
}
