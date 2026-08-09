package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.connection;

import java.util.Map;

//20260809_kpopmodder: Split the command ownership acceptance timestamp without changing emitted keys.
public final class FabricChatClefCommandOwnershipAcceptedAtPayloadMap {
    private static final String ACCEPTED_AT_MS = "accepted_at_ms";

    private FabricChatClefCommandOwnershipAcceptedAtPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long acceptedAtMs) {
        payload.put(ACCEPTED_AT_MS, acceptedAtMs);
    }
}
