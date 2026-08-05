package lavi.minecraft.fabric.chatclef.bridge.command.result;

import java.util.Map;

//20260805_kpopmodder: Type command result data while preserving the existing v1 map edge.
public interface FabricChatClefCommandResultDataPayload {
    Map<String, Object> toMap();

    static FabricChatClefCommandResultDataPayload empty() {
        return FabricChatClefCommandResultDataMapPayload.of(null);
    }

    static FabricChatClefCommandResultDataPayload fromMap(Map<String, Object> data) {
        return FabricChatClefCommandResultDataMapPayload.of(data);
    }
}
