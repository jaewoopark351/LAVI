package lavi.minecraft.diagnostics.blocks.collection.state;

//20260913_kpopmodder: Immutable output proposals do not select or alter the native operation.
public record BlockCollectionEvent(String wrapper, String semantic, BlockCollectionToken token,
        Object[] fields) { }
