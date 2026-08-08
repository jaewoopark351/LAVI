package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.queue;

import java.util.Map;

//20260809_kpopmodder: Keep queue active-presence fields separate without changing emitted keys.
public final class FabricChatClefQueueActivePresencePayloadMap {
    private static final String QUEUE_ACTIVE_PRESENT = "queue_active_present";

    private FabricChatClefQueueActivePresencePayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, boolean queueActivePresent) {
        details.put(QUEUE_ACTIVE_PRESENT, queueActivePresent);
    }
}
