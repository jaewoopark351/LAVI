package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.connection;

import java.util.Map;

//20260809_kpopmodder: Split the command ownership session id without changing emitted keys.
public final class FabricChatClefCommandOwnershipSessionIdPayloadMap {
    private static final String SESSION_ID = "session_id";

    private FabricChatClefCommandOwnershipSessionIdPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String sessionId) {
        payload.put(SESSION_ID, sessionId);
    }
}
