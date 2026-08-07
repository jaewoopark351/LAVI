package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Folderized command ownership Map serialization without changing emitted diagnostic fields.
public final class FabricChatClefCommandOwnershipPayloadMap {
    private static final String REQUEST_ID = "request_id";
    private static final String CORRELATION_ID = "correlation_id";
    private static final String SESSION_ID = "session_id";
    private static final String CONNECTION_GENERATION = "connection_generation";
    private static final String ACCEPTED_AT_MS = "accepted_at_ms";
    private static final String DETACHED = "detached";
    private static final String DETACHED_REASON = "detached_reason";

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
        payload.put(REQUEST_ID, requestId);
        payload.put(CORRELATION_ID, correlationId);
        payload.put(SESSION_ID, sessionId);
        payload.put(CONNECTION_GENERATION, connectionGeneration);
        payload.put(ACCEPTED_AT_MS, acceptedAtMs);
        payload.put(DETACHED, detached);
        payload.put(DETACHED_REASON, detachedReason);
        return payload;
    }
}
