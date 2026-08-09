package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session;

import lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.connection.FabricChatClefCommandOwnershipConnectionPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.request.FabricChatClefCommandOwnershipRequestPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Keep command ownership request/session fields grouped without changing emitted keys.
public final class FabricChatClefCommandOwnershipSessionPayloadMap {
    private FabricChatClefCommandOwnershipSessionPayloadMap() {
    }

    public static void putSessionFields(
            Map<String, Object> payload,
            String requestId,
            String correlationId,
            String sessionId,
            long connectionGeneration,
            long acceptedAtMs
    ) {
        FabricChatClefCommandOwnershipRequestPayloadMap.writeTo(
                payload,
                requestId,
                correlationId
        );
        FabricChatClefCommandOwnershipConnectionPayloadMap.writeTo(
                payload,
                sessionId,
                connectionGeneration,
                acceptedAtMs
        );
    }
}
