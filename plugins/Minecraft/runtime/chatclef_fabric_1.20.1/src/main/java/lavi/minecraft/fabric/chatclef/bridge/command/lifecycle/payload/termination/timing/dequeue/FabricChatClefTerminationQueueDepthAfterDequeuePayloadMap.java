package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.dequeue;

import java.util.Map;

//20260814_kpopmodder: Keep the post-dequeue queue-depth key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationQueueDepthAfterDequeuePayloadMap {
    private static final String QUEUE_DEPTH_AFTER_DEQUEUE = "queue_depth_after_dequeue";

    private FabricChatClefTerminationQueueDepthAfterDequeuePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, int queueDepthAfterDequeue) {
        payload.put(QUEUE_DEPTH_AFTER_DEQUEUE, queueDepthAfterDequeue);
    }
}
