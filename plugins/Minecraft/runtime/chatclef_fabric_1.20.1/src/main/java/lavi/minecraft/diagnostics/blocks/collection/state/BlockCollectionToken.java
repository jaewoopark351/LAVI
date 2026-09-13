package lavi.minecraft.diagnostics.blocks.collection.state;

import lavi.minecraft.diagnostics.blocks.collection.context.BlockCollectionContext;

//20260913_kpopmodder: The native caller retains only diagnostic values, never a lock or a game collection.
public record BlockCollectionToken(long scannerSequence, long registryEpoch, long sequence,
        BlockCollectionOperation operation, String boundary, String mapIdentity, String collectionIdentity,
        BlockCollectionContext context, long writeGenerationBefore, long parentReadSequence,
        boolean retained) { }
