package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation;

import java.util.Map;

//20260814_kpopmodder: Keep the post-observation queue-depth key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationObservationQueueDepthAfterPayloadMap {
    private static final String QUEUE_DEPTH_AFTER = "queue_depth_after";

    private FabricChatClefTerminationObservationQueueDepthAfterPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, int queueDepthAfter) {
        payload.put(QUEUE_DEPTH_AFTER, queueDepthAfter);
    }
}
