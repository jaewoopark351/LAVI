package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.connection;

import java.util.Map;

//20260809_kpopmodder: Split connection acceptance ownership fields without changing emitted keys.
public final class FabricChatClefCommandOwnershipConnectionPayloadMap {
    private static final String SESSION_ID = "session_id";
    private static final String CONNECTION_GENERATION = "connection_generation";
    private static final String ACCEPTED_AT_MS = "accepted_at_ms";

    private FabricChatClefCommandOwnershipConnectionPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String sessionId,
            long connectionGeneration,
            long acceptedAtMs
    ) {
        payload.put(SESSION_ID, sessionId);
        payload.put(CONNECTION_GENERATION, connectionGeneration);
        payload.put(ACCEPTED_AT_MS, acceptedAtMs);
    }
}
