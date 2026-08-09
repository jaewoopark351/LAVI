package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.request;

import java.util.Map;

//20260809_kpopmodder: Split request correlation ownership fields without changing emitted keys.
public final class FabricChatClefCommandOwnershipRequestPayloadMap {
    private FabricChatClefCommandOwnershipRequestPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String requestId,
            String correlationId
    ) {
        FabricChatClefCommandOwnershipRequestIdPayloadMap.writeTo(payload, requestId);
        FabricChatClefCommandOwnershipCorrelationPayloadMap.writeTo(payload, correlationId);
    }
}
