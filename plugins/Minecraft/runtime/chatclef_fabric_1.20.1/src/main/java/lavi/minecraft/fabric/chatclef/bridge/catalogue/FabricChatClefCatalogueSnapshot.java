package lavi.minecraft.fabric.chatclef.bridge.catalogue;

import java.util.Map;

//20260915_kpopmodder: Immutable transport-only snapshot; contains no game objects.
public record FabricChatClefCatalogueSnapshot(String revision, Map<String, Object> wireValue) {
    public FabricChatClefCatalogueSnapshot {
        wireValue = Map.copyOf(wireValue);
    }

    public static FabricChatClefCatalogueSnapshot unavailable(String reason) {
        return new FabricChatClefCatalogueSnapshot("unavailable:" + reason,
                Map.of("available", false, "reason", reason));
    }
}
