package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.FabricChatClefCommandOwnershipDetachPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.FabricChatClefCommandOwnershipSessionPayloadMap;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Folderized command ownership Map serialization without changing emitted diagnostic fields.
public final class FabricChatClefCommandOwnershipPayloadMap {
    private FabricChatClefCommandOwnershipPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String requestId,
            String correlationId,
            String sessionId,
            long connectionGeneration,
            long acceptedAtMs,
            boolean detached,
            String detachedReason
    ) {
        Map<String, Object> payload = new HashMap<>();
        FabricChatClefCommandOwnershipSessionPayloadMap.putSessionFields(
                payload,
                requestId,
                correlationId,
                sessionId,
                connectionGeneration,
                acceptedAtMs
        );
        FabricChatClefCommandOwnershipDetachPayloadMap.putDetachFields(
                payload,
                detached,
                detachedReason
        );
        return payload;
    }
}
