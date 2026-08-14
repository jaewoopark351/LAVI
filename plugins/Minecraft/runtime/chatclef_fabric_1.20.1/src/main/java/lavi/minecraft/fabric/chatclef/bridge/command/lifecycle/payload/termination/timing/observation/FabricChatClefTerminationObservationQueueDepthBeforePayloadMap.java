package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation;

import java.util.Map;

//20260814_kpopmodder: Keep the pre-observation queue-depth key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationObservationQueueDepthBeforePayloadMap {
    private static final String QUEUE_DEPTH_BEFORE = "queue_depth_before";

    private FabricChatClefTerminationObservationQueueDepthBeforePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, int queueDepthBefore) {
        payload.put(QUEUE_DEPTH_BEFORE, queueDepthBefore);
    }
}
