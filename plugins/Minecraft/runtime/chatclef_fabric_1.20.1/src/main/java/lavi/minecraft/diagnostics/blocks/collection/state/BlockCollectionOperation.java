package lavi.minecraft.diagnostics.blocks.collection.state;

//20260913_kpopmodder: Fixed semantic operation names exclude runtime identities from first-event keys.
public enum BlockCollectionOperation {
    READ_QUERY(false), READ_COPY(false), WRITE_ADD(true), WRITE_PUT(true),
    WRITE_CLEAR(true), WRITE_ADD_ALL(true), RESET(true);

    private final boolean writer;
    BlockCollectionOperation(boolean writer) { this.writer = writer; }
    public boolean writer() { return writer; }
}
