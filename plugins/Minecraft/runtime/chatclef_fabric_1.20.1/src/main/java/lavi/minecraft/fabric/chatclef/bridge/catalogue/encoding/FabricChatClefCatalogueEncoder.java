package lavi.minecraft.fabric.chatclef.bridge.catalogue.encoding;

import com.google.gson.GsonBuilder;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.FabricChatClefCatalogueSnapshot;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

//20260915_kpopmodder: Bound both decoded data and the additive handshake wire profile.
public final class FabricChatClefCatalogueEncoder {
    public static final int MAX_ENTRIES = 65536;
    public static final int MAX_DECODED_BYTES = 16 * 1024 * 1024;
    public static final int MAX_ENCODED_BYTES = 768 * 1024;

    public FabricChatClefCatalogueSnapshot encode(Map<String, Object> document, int entryCount) {
        if (entryCount < 0 || entryCount > MAX_ENTRIES)
            return FabricChatClefCatalogueSnapshot.unavailable("entry_limit");
        try {
            byte[] raw = new GsonBuilder().serializeNulls().create().toJson(document).getBytes(StandardCharsets.UTF_8);
            if (raw.length > MAX_DECODED_BYTES)
                return FabricChatClefCatalogueSnapshot.unavailable("decoded_size_limit");
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) { gzip.write(raw); }
            String encoded = Base64.getEncoder().encodeToString(compressed.toByteArray());
            if (encoded.length() > MAX_ENCODED_BYTES)
                return FabricChatClefCatalogueSnapshot.unavailable("encoded_size_limit");
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
            return new FabricChatClefCatalogueSnapshot(digest, Map.of(
                    "available", true, "encoding", "gzip+base64", "sha256", digest,
                    "uncompressed_bytes", raw.length, "entry_count", entryCount, "payload", encoded));
        } catch (Exception error) {
            return FabricChatClefCatalogueSnapshot.unavailable("encoding_failed");
        }
    }
}
