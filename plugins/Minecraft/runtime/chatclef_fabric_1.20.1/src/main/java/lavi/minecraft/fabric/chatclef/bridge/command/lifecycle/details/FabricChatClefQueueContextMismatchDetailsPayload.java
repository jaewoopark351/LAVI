package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefQueueContextMismatchDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private static final String QUEUE_ACTIVE_PRESENT = "queue_active_present";
    private static final String QUEUE_ACTIVE_REQUEST_ID = "queue_active_request_id";

    private final boolean queueActivePresent;
    private final String queueActiveRequestId;

    public FabricChatClefQueueContextMismatchDetailsPayload(
            boolean queueActivePresent,
            String queueActiveRequestId
    ) {
        this.queueActivePresent = queueActivePresent;
        this.queueActiveRequestId = FabricChatClefLifecycleDetailValues.nullToEmpty(queueActiveRequestId);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> details = new HashMap<>();
        details.put(QUEUE_ACTIVE_PRESENT, queueActivePresent);
        details.put(QUEUE_ACTIVE_REQUEST_ID, queueActiveRequestId);
        return details;
    }
}
