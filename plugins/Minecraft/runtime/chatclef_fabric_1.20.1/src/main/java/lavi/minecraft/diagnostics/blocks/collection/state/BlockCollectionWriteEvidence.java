package lavi.minecraft.diagnostics.blocks.collection.state;

import lavi.minecraft.diagnostics.blocks.collection.format.BlockCollectionEventFields;

//20260913_kpopmodder: Retain one immutable peer and latest observed write without retaining gameplay data.
public record BlockCollectionWriteEvidence(BlockCollectionToken firstOverlapWrite, long overlapWriteCount,
        BlockCollectionToken latestObservedWrite) {
    public Object[] fields() {
        return BlockCollectionEventFields.writeEvidence(this);
    }
}
