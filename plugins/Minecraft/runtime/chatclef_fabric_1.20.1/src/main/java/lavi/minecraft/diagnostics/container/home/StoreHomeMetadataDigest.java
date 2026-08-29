package lavi.minecraft.diagnostics.container.home;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//20260828_kpopmodder: Hash canonical STORE_HOME metadata without item id, count, or damage.
public final class StoreHomeMetadataDigest {
    public static final String ALGORITHM = "sha256-128";
    public static final int CANONICALIZATION_VERSION = 1;
    private static final int OUTPUT_BYTES = 16;

    private StoreHomeMetadataDigest() {
    }

    public static String digest(NbtCompound source) {
        NbtCompound metadata = withoutDamage(source);
        String canonical = metadata == null || metadata.isEmpty()
                ? "metadata:none"
                : canonical(metadata);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(OUTPUT_BYTES * 2);
            for (int index = 0; index < OUTPUT_BYTES; index++) {
                int value = digest[index] & 0xff;
                result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(value & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static boolean hasMetadata(NbtCompound source) {
        NbtCompound metadata = withoutDamage(source);
        return metadata != null && !metadata.isEmpty();
    }

    private static NbtCompound withoutDamage(NbtCompound source) {
        if (source == null) {
            return null;
        }
        NbtCompound copy = source.copy();
        copy.remove("Damage");
        return copy;
    }

    private static String canonical(NbtElement element) {
        StringBuilder result = new StringBuilder();
        appendCanonical(result, element);
        return result.toString();
    }

    private static void appendCanonical(StringBuilder target, NbtElement element) {
        if (element == null) {
            target.append("null");
            return;
        }
        target.append('t').append(element.getType()).append(':');
        if (element instanceof NbtCompound compound) {
            List<String> keys = new ArrayList<>(compound.getKeys());
            Collections.sort(keys);
            target.append('{').append(keys.size()).append(':');
            for (String key : keys) {
                appendToken(target, key);
                appendCanonical(target, compound.get(key));
            }
            target.append('}');
            return;
        }
        if (element instanceof NbtList list) {
            target.append('[').append(list.size()).append(':');
            for (int index = 0; index < list.size(); index++) {
                appendCanonical(target, list.get(index));
            }
            target.append(']');
            return;
        }
        appendToken(target, element.toString());
    }

    private static void appendToken(StringBuilder target, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        target.append(bytes.length).append(':').append(value).append(';');
    }
}
