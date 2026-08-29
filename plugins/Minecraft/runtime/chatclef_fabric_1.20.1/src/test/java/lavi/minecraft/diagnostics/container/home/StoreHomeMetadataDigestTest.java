package lavi.minecraft.diagnostics.container.home;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StoreHomeMetadataDigestTest {
    @Test
    void excludesDamageFromTheMetadataDigest() {
        NbtCompound first = new NbtCompound();
        first.putInt("Damage", 3);
        NbtCompound second = new NbtCompound();
        second.putInt("Damage", 17);

        assertEquals(
                StoreHomeMetadataDigest.digest(first),
                StoreHomeMetadataDigest.digest(second)
        );
    }

    @Test
    void distinguishesMetadataAndIgnoresCompoundInsertionOrder() {
        NbtCompound first = new NbtCompound();
        first.putString("alpha", "one");
        first.putInt("beta", 2);
        NbtCompound reordered = new NbtCompound();
        reordered.putInt("beta", 2);
        reordered.putString("alpha", "one");
        NbtCompound changed = reordered.copy();
        changed.putString("alpha", "two");

        assertEquals(
                StoreHomeMetadataDigest.digest(first),
                StoreHomeMetadataDigest.digest(reordered)
        );
        assertNotEquals(
                StoreHomeMetadataDigest.digest(first),
                StoreHomeMetadataDigest.digest(changed)
        );
        assertEquals(32, StoreHomeMetadataDigest.digest(first).length());
    }
}
