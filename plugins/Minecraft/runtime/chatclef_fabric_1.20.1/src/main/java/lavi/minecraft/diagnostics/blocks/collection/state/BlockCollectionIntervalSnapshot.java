package lavi.minecraft.diagnostics.blocks.collection.state;

//20260913_kpopmodder: Pass interval counts to formatting without exposing mutable tracking state.
public record BlockCollectionIntervalSnapshot(long entered, long normalExits, long abnormalExits,
        long overlaps, long nulls, int activeMarkers, long activeOmitted) { }
