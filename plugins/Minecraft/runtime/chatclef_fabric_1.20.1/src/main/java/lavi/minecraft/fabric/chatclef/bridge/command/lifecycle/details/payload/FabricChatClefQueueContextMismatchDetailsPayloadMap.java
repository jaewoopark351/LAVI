package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefQueueContextMismatchDetailsPayloadMap {
    private static final String QUEUE_ACTIVE_PRESENT = "queue_active_present";
    private static final String QUEUE_ACTIVE_REQUEST_ID = "queue_active_request_id";

    private FabricChatClefQueueContextMismatchDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(boolean queueActivePresent, String queueActiveRequestId) {
        Map<String, Object> details = new HashMap<>();
        details.put(QUEUE_ACTIVE_PRESENT, queueActivePresent);
        details.put(QUEUE_ACTIVE_REQUEST_ID, queueActiveRequestId);
        return details;
    }
}
