package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.connection;

import java.util.Map;

//20260809_kpopmodder: Split connection acceptance ownership fields without changing emitted keys.
public final class FabricChatClefCommandOwnershipConnectionPayloadMap {
    private FabricChatClefCommandOwnershipConnectionPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String sessionId,
            long connectionGeneration,
            long acceptedAtMs
    ) {
        FabricChatClefCommandOwnershipSessionIdPayloadMap.writeTo(payload, sessionId);
        FabricChatClefCommandOwnershipConnectionGenerationPayloadMap.writeTo(payload, connectionGeneration);
        FabricChatClefCommandOwnershipAcceptedAtPayloadMap.writeTo(payload, acceptedAtMs);
    }
}
