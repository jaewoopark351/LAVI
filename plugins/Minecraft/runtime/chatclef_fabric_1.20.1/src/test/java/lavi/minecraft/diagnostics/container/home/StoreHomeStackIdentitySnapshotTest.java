package lavi.minecraft.diagnostics.container.home;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreHomeStackIdentitySnapshotTest {
    @Test
    void freezesMetadataBeforeTheMutableSourceChanges() {
        NbtCompound metadata = metadata("efficiency", 4);
        StoreHomeStackIdentitySnapshot snapshot =
                StoreHomeStackIdentitySnapshot.captureValues(
                        "minecraft:diamond_pickaxe", 1, 7, metadata
                );
        String frozenDigest = snapshot.metadataDigest();

        metadata.putInt("level", 5);

        assertEquals(frozenDigest, snapshot.metadataDigest());
        assertNotEquals(frozenDigest, StoreHomeMetadataDigest.digest(metadata));
    }

    @Test
    void separatesItemDamageMetadataAndCountComparisons() {
        NbtCompound metadata = metadata("efficiency", 4);
        StoreHomeStackIdentitySnapshot expected = identity(
                "minecraft:diamond_pickaxe", 1, 7, metadata
        );

        StoreHomeManifestMismatchSnapshot itemChanged = compare(
                expected,
                identity("minecraft:iron_pickaxe", 1, 7, metadata),
                false
        );
        StoreHomeManifestMismatchSnapshot damageChanged = compare(
                expected,
                identity("minecraft:diamond_pickaxe", 1, 8, metadata),
                false
        );
        StoreHomeManifestMismatchSnapshot metadataChanged = compare(
                expected,
                identity("minecraft:diamond_pickaxe", 1, 7,
                        metadata("efficiency", 5)),
                false
        );
        StoreHomeManifestMismatchSnapshot countChanged = compare(
                expected,
                identity("minecraft:diamond_pickaxe", 2, 7, metadata),
                true
        );

        assertEquals("false", itemChanged.itemIdEqual());
        assertEquals(
                expected.metadataDigest(),
                itemChanged.actual().metadataDigest()
        );
        assertEquals("true", damageChanged.metadataDigestEqual());
        assertEquals("false", damageChanged.damageEqual());
        assertEquals("false", metadataChanged.metadataDigestEqual());
        assertEquals("false", countChanged.countEqual());
        assertEquals(
                expected.metadataDigest(),
                countChanged.actual().metadataDigest()
        );
        assertEquals("true", countChanged.metadataDigestEqual());
        assertEquals("true", countChanged.fullFingerprintEqual());
        assertFalse(countChanged.actual().metadataDigest().isBlank());
        assertTrue(countChanged.actual().identityObserved());
    }

    private static StoreHomeManifestMismatchSnapshot compare(
            StoreHomeStackIdentitySnapshot expected,
            StoreHomeStackIdentitySnapshot actual,
            boolean fingerprintEqual) {
        return StoreHomeManifestMismatchSnapshot.fromSnapshots(
                0,
                8,
                expected.count(),
                "STORE_HOME",
                "test",
                "test_fixture",
                expected,
                actual,
                Boolean.toString(fingerprintEqual)
        );
    }

    private static StoreHomeStackIdentitySnapshot identity(
            String itemId,
            int count,
            int damage,
            NbtCompound metadata) {
        return StoreHomeStackIdentitySnapshot.captureValues(
                itemId, count, damage, metadata.copy()
        );
    }

    private static NbtCompound metadata(String enchantment, int level) {
        NbtCompound metadata = new NbtCompound();
        metadata.putString("enchantment", enchantment);
        metadata.putInt("level", level);
        return metadata;
    }
}
