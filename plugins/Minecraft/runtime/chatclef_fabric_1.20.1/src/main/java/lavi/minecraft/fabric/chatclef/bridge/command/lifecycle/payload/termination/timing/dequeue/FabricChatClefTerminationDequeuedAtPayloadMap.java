package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.dequeue;

import java.util.Map;

//20260814_kpopmodder: Keep the dequeue timestamp key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationDequeuedAtPayloadMap {
    private static final String DEQUEUED_AT_MS = "dequeued_at_ms";

    private FabricChatClefTerminationDequeuedAtPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long dequeuedAtMs) {
        payload.put(DEQUEUED_AT_MS, dequeuedAtMs);
    }
}
