package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.request;

import java.util.Map;

//20260809_kpopmodder: Split the command ownership request id without changing emitted keys.
public final class FabricChatClefCommandOwnershipRequestIdPayloadMap {
    private static final String REQUEST_ID = "request_id";

    private FabricChatClefCommandOwnershipRequestIdPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String requestId) {
        payload.put(REQUEST_ID, requestId);
    }
}
