package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.queue;

import java.util.Map;

//20260809_kpopmodder: Keep queue active-request fields separate without changing emitted keys.
public final class FabricChatClefQueueActiveRequestPayloadMap {
    private static final String QUEUE_ACTIVE_REQUEST_ID = "queue_active_request_id";

    private FabricChatClefQueueActiveRequestPayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, String queueActiveRequestId) {
        details.put(QUEUE_ACTIVE_REQUEST_ID, queueActiveRequestId);
    }
}
