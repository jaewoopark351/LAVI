package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.queue.FabricChatClefQueueActivePresencePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.queue.FabricChatClefQueueActiveRequestPayloadMap;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefQueueContextMismatchDetailsPayloadMap {
    private FabricChatClefQueueContextMismatchDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(boolean queueActivePresent, String queueActiveRequestId) {
        Map<String, Object> details = new HashMap<>();
        FabricChatClefQueueActivePresencePayloadMap.writeTo(details, queueActivePresent);
        FabricChatClefQueueActiveRequestPayloadMap.writeTo(details, queueActiveRequestId);
        return details;
    }
}
