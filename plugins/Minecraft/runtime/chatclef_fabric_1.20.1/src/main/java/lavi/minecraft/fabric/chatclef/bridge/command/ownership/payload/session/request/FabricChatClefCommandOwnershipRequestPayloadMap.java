package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.request;

import java.util.Map;

//20260809_kpopmodder: Split request correlation ownership fields without changing emitted keys.
public final class FabricChatClefCommandOwnershipRequestPayloadMap {
    private static final String REQUEST_ID = "request_id";
    private static final String CORRELATION_ID = "correlation_id";

    private FabricChatClefCommandOwnershipRequestPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String requestId,
            String correlationId
    ) {
        payload.put(REQUEST_ID, requestId);
        payload.put(CORRELATION_ID, correlationId);
    }
}
