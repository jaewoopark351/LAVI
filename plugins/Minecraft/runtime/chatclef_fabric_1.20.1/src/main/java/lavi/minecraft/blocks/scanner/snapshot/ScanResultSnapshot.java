//#if MC == 12001
package lavi.minecraft.blocks.scanner.snapshot;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.*;

//20260913_kpopmodder: A completed worker offers frozen data; only the owning client tick may commit it to the index.
public final class ScanResultSnapshot {
    private final Map<Block, Set<BlockPos>> blocks;
    private final Map<ChunkPos, Long> chunks;
    public ScanResultSnapshot(Map<Block, ? extends Set<BlockPos>> blocks, Map<ChunkPos, Long> chunks) {
        HashMap<Block, Set<BlockPos>> frozen = new HashMap<>();
        for (var entry : blocks.entrySet()) frozen.put(entry.getKey(), entry.getValue().stream()
                .map(BlockPos::toImmutable).collect(java.util.stream.Collectors.toUnmodifiableSet()));
        this.blocks = Map.copyOf(frozen);
        this.chunks = Map.copyOf(chunks);
    }
    public Map<Block, Set<BlockPos>> blocks() { return blocks; }
    public Map<ChunkPos, Long> chunks() { return chunks; }
}
//#endif
