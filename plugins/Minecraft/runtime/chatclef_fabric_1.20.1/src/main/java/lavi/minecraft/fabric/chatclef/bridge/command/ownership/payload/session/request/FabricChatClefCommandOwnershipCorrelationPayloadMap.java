package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.request;

import java.util.Map;

//20260809_kpopmodder: Split the command ownership correlation id without changing emitted keys.
public final class FabricChatClefCommandOwnershipCorrelationPayloadMap {
    private static final String CORRELATION_ID = "correlation_id";

    private FabricChatClefCommandOwnershipCorrelationPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String correlationId) {
        payload.put(CORRELATION_ID, correlationId);
    }
}
