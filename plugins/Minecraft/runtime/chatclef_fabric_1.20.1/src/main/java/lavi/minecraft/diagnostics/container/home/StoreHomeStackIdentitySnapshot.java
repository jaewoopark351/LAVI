package lavi.minecraft.diagnostics.container.home;

import lavi.minecraft.diagnostics.container.home.fingerprint.StoreHomeFingerprintMetadataRead;
import lavi.minecraft.diagnostics.container.home.fingerprint.StoreHomeFingerprintMetadataReader;
import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySlotSnapshot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;

import java.util.Objects;

//20260828_kpopmodder: Materialize one immutable stack identity at the stale comparison boundary.
public final class StoreHomeStackIdentitySnapshot {
    public static final String NOT_AVAILABLE = "not_available";
    private static final StoreHomeFingerprintMetadataReader FINGERPRINT_METADATA_READER =
            new StoreHomeFingerprintMetadataReader();

    private final boolean present;
    private final String itemId;
    private final int count;
    private final int damage;
    private final String metadataDigest;
    private final String metadataPresent;
    private final String captureStatus;
    private final String errorClass;

    private StoreHomeStackIdentitySnapshot(
            boolean present,
            String itemId,
            int count,
            int damage,
            String metadataDigest,
            String metadataPresent,
            String captureStatus,
            String errorClass) {
        this.present = present;
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.count = Math.max(0, count);
        this.damage = Math.max(0, damage);
        this.metadataDigest = Objects.requireNonNull(metadataDigest, "metadataDigest");
        this.metadataPresent = Objects.requireNonNull(metadataPresent, "metadataPresent");
        this.captureStatus = Objects.requireNonNull(captureStatus, "captureStatus");
        this.errorClass = Objects.requireNonNull(errorClass, "errorClass");
    }

    public static StoreHomeStackIdentitySnapshot captureActual(ItemStack liveStack) {
        if (liveStack == null || liveStack.isEmpty()) {
            return absent();
        }
        ItemStack frozen = liveStack.copy();
        MetadataRead metadata = readMetadata(frozen);
        return captured(
                String.valueOf(Registries.ITEM.getId(frozen.getItem())),
                frozen.getCount(),
                frozen.getDamage(),
                metadata
        );
    }

    public static StoreHomeStackIdentitySnapshot captureExpected(
            HomeStorageStackFingerprint fingerprint,
            int expectedCount) {
        Objects.requireNonNull(fingerprint, "fingerprint");
        StoreHomeFingerprintMetadataRead fingerprintMetadata =
                FINGERPRINT_METADATA_READER.read(fingerprint);
        MetadataRead metadata = new MetadataRead(
                fingerprintMetadata.readable(),
                fingerprintMetadata.metadataCopy()
        );
        return captured(
                fingerprint.itemId(),
                expectedCount,
                fingerprint.damage(),
                metadata
        );
    }

    //20260828_kpopmodder: Convert the immutable validator evidence without another live read.
    public static StoreHomeStackIdentitySnapshot capture(
            HomeStorageInventorySlotSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return snapshot.occupied()
                ? captureExpected(snapshot.fingerprint().orElseThrow(), snapshot.count())
                : absent();
    }

    static StoreHomeStackIdentitySnapshot captureValues(
            String itemId,
            int count,
            int damage,
            NbtCompound metadata) {
        return captured(itemId, count, damage, new MetadataRead(true, metadata));
    }

    public static StoreHomeStackIdentitySnapshot absent() {
        return new StoreHomeStackIdentitySnapshot(
                false, NOT_AVAILABLE, 0, 0, NOT_AVAILABLE,
                "false", "complete", "none"
        );
    }

    public static StoreHomeStackIdentitySnapshot unavailable(String errorClass) {
        return new StoreHomeStackIdentitySnapshot(
                false, NOT_AVAILABLE, 0, 0, NOT_AVAILABLE,
                NOT_AVAILABLE, "partial", normalizeError(errorClass)
        );
    }

    private static StoreHomeStackIdentitySnapshot captured(
            String itemId,
            int count,
            int damage,
            MetadataRead metadata) {
        if (!metadata.readable()) {
            return new StoreHomeStackIdentitySnapshot(
                    true, itemId, count, damage, NOT_AVAILABLE,
                    NOT_AVAILABLE, "partial", "metadata_unavailable_for_version"
            );
        }
        try {
            return new StoreHomeStackIdentitySnapshot(
                    true,
                    itemId,
                    count,
                    damage,
                    StoreHomeMetadataDigest.digest(metadata.nbt()),
                    Boolean.toString(StoreHomeMetadataDigest.hasMetadata(metadata.nbt())),
                    "complete",
                    "none"
            );
        } catch (RuntimeException | LinkageError error) {
            return new StoreHomeStackIdentitySnapshot(
                    true, itemId, count, damage, NOT_AVAILABLE,
                    NOT_AVAILABLE, "partial", error.getClass().getSimpleName()
            );
        }
    }

    private static MetadataRead readMetadata(ItemStack stack) {
        //#if MC >= 12005
        return new MetadataRead(false, null);
        //#else
        //$$ NbtCompound nbt = stack.getNbt();
        //$$ return new MetadataRead(true, nbt == null ? null : nbt.copy());
        //#endif
    }

    private static String normalizeError(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    public boolean present() {
        return present;
    }

    public String itemId() {
        return itemId;
    }

    public int count() {
        return count;
    }

    public int damage() {
        return damage;
    }

    public String metadataDigest() {
        return metadataDigest;
    }

    public String metadataPresent() {
        return metadataPresent;
    }

    public String captureStatus() {
        return captureStatus;
    }

    public String errorClass() {
        return errorClass;
    }

    public boolean identityObserved() {
        return present || "complete".equals(captureStatus);
    }

    public Object itemIdValue() {
        return present ? itemId : NOT_AVAILABLE;
    }

    public Object countValue() {
        return present ? count : NOT_AVAILABLE;
    }

    public Object damageValue() {
        return present ? damage : NOT_AVAILABLE;
    }

    private record MetadataRead(boolean readable, NbtCompound nbt) {
    }
}
