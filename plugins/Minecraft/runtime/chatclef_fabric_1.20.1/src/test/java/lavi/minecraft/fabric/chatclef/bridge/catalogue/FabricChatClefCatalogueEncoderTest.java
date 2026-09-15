package lavi.minecraft.fabric.chatclef.bridge.catalogue;

import com.google.gson.JsonParser;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.encoding.FabricChatClefCatalogueEncoder;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;

class FabricChatClefCatalogueEncoderTest {
    @Test void preservesUnicodeNullContextCountsAndVerifiedDigest() throws Exception {
        var document = new LinkedHashMap<String, Object>();
        document.put("schema_version", 1);
        document.put("minecraft_version", "1.20.1");
        document.put("butler_user", null);
        document.put("entries", List.of(Map.of("kind", "item", "id", "minecraft:iron_ingot",
                "translation_key", "item.minecraft.iron_ingot", "korean_name", "\uCCA0\uAD34",
                "tokens", Map.of("give", "iron_ingot"), "capabilities", List.of("give"))));
        var snapshot = new FabricChatClefCatalogueEncoder().encode(document, 1);
        assertEquals(true, snapshot.wireValue().get("available"));
        byte[] decoded;
        try (var input = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(
                (String) snapshot.wireValue().get("payload"))))) { decoded = input.readAllBytes(); }
        assertEquals(decoded.length, snapshot.wireValue().get("uncompressed_bytes"));
        assertEquals(1, snapshot.wireValue().get("entry_count"));
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(decoded)), snapshot.revision());
        var parsed = JsonParser.parseString(new String(decoded, StandardCharsets.UTF_8)).getAsJsonObject();
        assertTrue(parsed.get("butler_user").isJsonNull());
        assertEquals("\uCCA0\uAD34", parsed.getAsJsonArray("entries").get(0).getAsJsonObject().get("korean_name").getAsString());
    }

    @Test void rejectsOversizedDecodedAndEntryCountInsteadOfTruncatingCoverage() {
        var encoder = new FabricChatClefCatalogueEncoder();
        assertEquals("entry_limit", encoder.encode(Map.of(), 65537).wireValue().get("reason"));
        var large = encoder.encode(Map.of("data", "x".repeat(FabricChatClefCatalogueEncoder.MAX_DECODED_BYTES)), 0);
        assertEquals(false, large.wireValue().get("available"));
        assertEquals("decoded_size_limit", large.wireValue().get("reason"));
    }

    @Test void snapshotStoreReplacesRatherThanMergesStaleTranslations() {
        var before = FabricChatClefCatalogueSnapshotStore.current();
        try {
            var first = new FabricChatClefCatalogueEncoder().encode(Map.of("entries", List.of("first")), 1);
            FabricChatClefCatalogueSnapshotStore.publish(first);
            FabricChatClefCatalogueSnapshotStore.publish(FabricChatClefCatalogueSnapshot.unavailable("capture_failed"));
            assertEquals(false, FabricChatClefCatalogueSnapshotStore.current().wireValue().get("available"));
            assertFalse(FabricChatClefCatalogueSnapshotStore.current().wireValue().containsKey("payload"));
        } finally { FabricChatClefCatalogueSnapshotStore.publish(before); }
    }
}
