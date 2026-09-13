package lavi.minecraft.diagnostics.blocks.collection.state;

//20260913_kpopmodder: Bind an already-returned list to its read interval without rescanning its contents.
public record BlockCollectionReadOrigin(BlockCollectionToken query, long writeGenerationAfter,
        long copiedSources, boolean crossThreadOverlapObserved, BlockCollectionWriteEvidence writeEvidence) { }
