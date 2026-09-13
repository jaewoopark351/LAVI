//#if MC == 12001
package lavi.minecraft.blocks.scanner.snapshot;

import net.minecraft.util.math.BlockPos;
import java.util.List;

//20260913_kpopmodder: World identity, content revision and copied locations cross the read boundary together.
public record BlockLocationSnapshot(Object world, Object player, Object dimension, long revision, List<BlockPos> positions) {
    public BlockLocationSnapshot { positions = positions.stream().map(BlockPos::toImmutable).toList(); }
}

//#endif
