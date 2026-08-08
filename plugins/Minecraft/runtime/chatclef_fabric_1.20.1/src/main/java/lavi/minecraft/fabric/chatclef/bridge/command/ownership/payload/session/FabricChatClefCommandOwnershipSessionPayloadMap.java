package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session;

import java.util.Map;

//20260808_kpopmodder: Keep command ownership request/session fields grouped without changing emitted keys.
public final class FabricChatClefCommandOwnershipSessionPayloadMap {
    private static final String REQUEST_ID = "request_id";
    private static final String CORRELATION_ID = "correlation_id";
    private static final String SESSION_ID = "session_id";
    private static final String CONNECTION_GENERATION = "connection_generation";
    private static final String ACCEPTED_AT_MS = "accepted_at_ms";

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
        payload.put(REQUEST_ID, requestId);
        payload.put(CORRELATION_ID, correlationId);
        payload.put(SESSION_ID, sessionId);
        payload.put(CONNECTION_GENERATION, connectionGeneration);
        payload.put(ACCEPTED_AT_MS, acceptedAtMs);
    }
}
