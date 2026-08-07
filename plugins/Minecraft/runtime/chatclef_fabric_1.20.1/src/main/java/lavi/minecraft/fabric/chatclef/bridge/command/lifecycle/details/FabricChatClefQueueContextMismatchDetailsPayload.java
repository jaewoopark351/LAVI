package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefQueueContextMismatchDetailsPayloadMap;

import java.util.Map;

public final class FabricChatClefQueueContextMismatchDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
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
        return FabricChatClefQueueContextMismatchDetailsPayloadMap.toMap(queueActivePresent, queueActiveRequestId);
    }
}
