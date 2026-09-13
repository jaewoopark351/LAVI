//#if MC == 12001
package lavi.minecraft.blocks.scanner.snapshot;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.PalettedContainer;
import java.util.List;

//20260913_kpopmodder: Worker-readable palette copies contain no live world or chunk references.
public final class ScanChunkSnapshot {
    private final ChunkPos position;
    private final int bottomY;
    private final long worldTime;
    private final List<PalettedContainer<BlockState>> sections;

    ScanChunkSnapshot(ChunkPos position, int bottomY, long worldTime,
                             List<PalettedContainer<BlockState>> sections) {
        this.position = position;
        this.bottomY = bottomY;
        this.worldTime = worldTime;
        this.sections = List.copyOf(sections);
    }
    public ChunkPos position() { return position; }
    public int bottomY() { return bottomY; }
    public int topY() { return bottomY + sections.size() * 16; }
    public long worldTime() { return worldTime; }
    public BlockState state(BlockPos pos) {
        return sections.get((pos.getY() - bottomY) >> 4).get(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }
}

//#endif
