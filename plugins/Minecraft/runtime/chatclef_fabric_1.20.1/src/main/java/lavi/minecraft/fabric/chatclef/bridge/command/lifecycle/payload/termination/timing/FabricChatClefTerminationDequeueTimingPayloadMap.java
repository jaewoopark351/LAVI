package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing;

import java.util.Map;

//20260814_kpopmodder: Keep dequeue timing fields separate without changing emitted keys.
public final class FabricChatClefTerminationDequeueTimingPayloadMap {
    private static final String DEQUEUED_AT_MS = "dequeued_at_ms";
    private static final String DEQUEUED_CLIENT_TICK = "dequeued_client_tick";
    private static final String OBSERVATION_AGE_MS = "observation_age_ms";
    private static final String QUEUE_DEPTH_AFTER_DEQUEUE = "queue_depth_after_dequeue";

    private FabricChatClefTerminationDequeueTimingPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            long dequeuedAtMs,
            long dequeuedClientTick,
            long observationAgeMs,
            int queueDepthAfterDequeue
    ) {
        payload.put(DEQUEUED_AT_MS, dequeuedAtMs);
        payload.put(DEQUEUED_CLIENT_TICK, dequeuedClientTick);
        payload.put(OBSERVATION_AGE_MS, observationAgeMs);
        payload.put(QUEUE_DEPTH_AFTER_DEQUEUE, queueDepthAfterDequeue);
    }
}
