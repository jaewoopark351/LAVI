package lavi.minecraft.fabric.chatclef.bridge.catalogue.admission;

import lavi.minecraft.fabric.chatclef.bridge.catalogue.FabricChatClefCatalogueSnapshot;
import java.util.Map;

//20260915_kpopmodder: Revalidate the exact compiler snapshot before the existing native invocation.
public final class FabricChatClefRuntimeCatalogueAdmission {
    private FabricChatClefRuntimeCatalogueAdmission() { }

    public static String rejectionReason(Map<String, Object> metadata, String sessionId,
                                         FabricChatClefCatalogueSnapshot snapshot) {
        Object natural = metadata == null ? null : metadata.get("natural_language");
        if (!(natural instanceof Map<?, ?> naturalMap)) return null;
        Object translation = naturalMap.get("translation");
        if (!(translation instanceof Map<?, ?> translationMap)) return null;
        Object data = translationMap.get("data");
        if (!(data instanceof Map<?, ?> dataMap) || !dataMap.containsKey("runtime_catalogue")) return null;
        if (!(dataMap.get("runtime_catalogue") instanceof Map<?, ?> binding)) return "runtime_catalogue_binding_invalid";
        if (!sessionId.equals(binding.get("session_id"))) return "runtime_catalogue_session_changed";
        if (snapshot == null || !Boolean.TRUE.equals(snapshot.wireValue().get("available")))
            return "runtime_catalogue_unavailable";
        Object digest = binding.get("catalogue_sha256");
        if (!(digest instanceof String text) || !text.matches("[a-f0-9]{64}")) return "runtime_catalogue_binding_invalid";
        return snapshot.revision().equals(digest) ? null : "runtime_catalogue_changed";
    }
}
