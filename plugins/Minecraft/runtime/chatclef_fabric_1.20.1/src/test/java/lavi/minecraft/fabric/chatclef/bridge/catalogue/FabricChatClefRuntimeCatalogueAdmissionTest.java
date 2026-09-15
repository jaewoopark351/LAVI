package lavi.minecraft.fabric.chatclef.bridge.catalogue;

import lavi.minecraft.fabric.chatclef.bridge.catalogue.admission.FabricChatClefRuntimeCatalogueAdmission;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FabricChatClefRuntimeCatalogueAdmissionTest {
    private static final String SHA = "a".repeat(64);
    private final FabricChatClefCatalogueSnapshot snapshot = new FabricChatClefCatalogueSnapshot(SHA, Map.of("available", true));

    @Test void exactSessionAndSnapshotAreRequiredForBoundKoreanRequest() {
        assertNull(FabricChatClefRuntimeCatalogueAdmission.rejectionReason(metadata("session", SHA), "session", snapshot));
        assertEquals("runtime_catalogue_session_changed", FabricChatClefRuntimeCatalogueAdmission.rejectionReason(metadata("previous", SHA), "session", snapshot));
        assertEquals("runtime_catalogue_changed", FabricChatClefRuntimeCatalogueAdmission.rejectionReason(metadata("session", "b".repeat(64)), "session", snapshot));
        assertEquals("runtime_catalogue_unavailable", FabricChatClefRuntimeCatalogueAdmission.rejectionReason(metadata("session", SHA), "session", FabricChatClefCatalogueSnapshot.unavailable("resource_reload_pending")));
    }

    @Test void rawEnglishWithoutCatalogueBindingKeepsExistingPath() {
        assertNull(FabricChatClefRuntimeCatalogueAdmission.rejectionReason(Map.of(), "session", FabricChatClefCatalogueSnapshot.unavailable("capture_pending")));
        assertEquals("runtime_catalogue_binding_invalid", FabricChatClefRuntimeCatalogueAdmission.rejectionReason(metadata("session", "invalid"), "session", snapshot));
    }

    private Map<String, Object> metadata(String session, String digest) {
        return Map.of("natural_language", Map.of("translation", Map.of("data", Map.of("runtime_catalogue",
                Map.of("session_id", session, "catalogue_sha256", digest)))));
    }
}
