package lavi.minecraft.diagnostics.container.home.fingerprint;

import net.minecraft.nbt.NbtCompound;

//20260829_kpopmodder: Added this type file to expose one immutable fingerprint metadata read.
public final class StoreHomeFingerprintMetadataRead {
    private final boolean readable;
    private final NbtCompound metadata;

    private StoreHomeFingerprintMetadataRead(
            boolean readable,
            NbtCompound metadata) {
        this.readable = readable;
        this.metadata = copy(metadata);
    }

    public static StoreHomeFingerprintMetadataRead readable(
            NbtCompound metadata) {
        return new StoreHomeFingerprintMetadataRead(true, metadata);
    }

    public static StoreHomeFingerprintMetadataRead unavailable() {
        return new StoreHomeFingerprintMetadataRead(false, null);
    }

    public boolean readable() {
        return readable;
    }

    public NbtCompound metadataCopy() {
        return copy(metadata);
    }

    private static NbtCompound copy(NbtCompound value) {
        return value == null ? null : value.copy();
    }
}
