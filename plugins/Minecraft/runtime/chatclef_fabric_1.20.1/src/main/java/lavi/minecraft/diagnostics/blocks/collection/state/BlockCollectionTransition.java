package lavi.minecraft.diagnostics.blocks.collection.state;

import java.util.List;

//20260913_kpopmodder: Return bounded observations separately from the caller's native result.
public record BlockCollectionTransition(BlockCollectionToken token, BlockCollectionReadOrigin readOrigin,
        List<BlockCollectionEvent> events) {
    public static BlockCollectionTransition empty() { return new BlockCollectionTransition(null, null, List.of()); }
}
