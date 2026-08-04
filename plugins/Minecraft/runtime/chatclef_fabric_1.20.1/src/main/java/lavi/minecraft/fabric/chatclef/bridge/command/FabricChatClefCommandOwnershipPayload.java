package lavi.minecraft.fabric.chatclef.bridge.command;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Isolate command ownership diagnostic payload keys without changing their map shape.
final class FabricChatClefCommandOwnershipPayload {
    private static final String REQUEST_ID = "request_id";
    private static final String CORRELATION_ID = "correlation_id";
    private static final String SESSION_ID = "session_id";
    private static final String CONNECTION_GENERATION = "connection_generation";
    private static final String ACCEPTED_AT_MS = "accepted_at_ms";
    private static final String DETACHED = "detached";
    private static final String DETACHED_REASON = "detached_reason";

    private final String requestId;
    private final String correlationId;
    private final String sessionId;
    private final long connectionGeneration;
    private final long acceptedAtMs;
    private final boolean detached;
    private final String detachedReason;

    FabricChatClefCommandOwnershipPayload(
            String requestId,
            String correlationId,
            String sessionId,
            long connectionGeneration,
            long acceptedAtMs,
            boolean detached,
            String detachedReason
    ) {
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.sessionId = sessionId;
        this.connectionGeneration = connectionGeneration;
        this.acceptedAtMs = acceptedAtMs;
        this.detached = detached;
        this.detachedReason = detachedReason;
    }

    Map<String, Object> toMap() {
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
