package lavi.minecraft.diagnostics.container.home.fingerprint;

import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

//20260829_kpopmodder: Added this class to isolate version-specific fingerprint metadata reads.
public final class StoreHomeFingerprintMetadataReader {
    public StoreHomeFingerprintMetadataRead read(
            HomeStorageStackFingerprint fingerprint) {
        Objects.requireNonNull(fingerprint, "fingerprint");
        ItemStack exactStack = fingerprint.exactStackCopy();
        if (exactStack == null) {
            return StoreHomeFingerprintMetadataRead.readable(null);
        }
        //#if MC >= 12005
        return StoreHomeFingerprintMetadataRead.unavailable();
        //#else
        //$$ NbtCompound metadata = exactStack.getNbt();
        //$$ return StoreHomeFingerprintMetadataRead.readable(metadata);
        //#endif
    }
}
