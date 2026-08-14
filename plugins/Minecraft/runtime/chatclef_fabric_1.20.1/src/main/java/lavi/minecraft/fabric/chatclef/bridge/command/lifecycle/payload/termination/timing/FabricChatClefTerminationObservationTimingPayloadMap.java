package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing;

import java.util.Map;

//20260814_kpopmodder: Keep observation capture timing fields separate without changing emitted keys.
public final class FabricChatClefTerminationObservationTimingPayloadMap {
    private static final String OBSERVATION_SEQUENCE = "observation_sequence";
    private static final String OBSERVED_AT_MS = "observed_at_ms";
    private static final String OBSERVED_CLIENT_TICK = "observed_client_tick";
    private static final String OBSERVATION_THREAD = "observation_thread";
    private static final String QUEUE_DEPTH_BEFORE = "queue_depth_before";
    private static final String QUEUE_DEPTH_AFTER = "queue_depth_after";

    private FabricChatClefTerminationObservationTimingPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            long observationSequence,
            long observedAtMs,
            long observedClientTick,
            String observationThread,
            int queueDepthBefore,
            int queueDepthAfter
    ) {
        payload.put(OBSERVATION_SEQUENCE, observationSequence);
        payload.put(OBSERVED_AT_MS, observedAtMs);
        payload.put(OBSERVED_CLIENT_TICK, observedClientTick);
        payload.put(OBSERVATION_THREAD, observationThread);
        payload.put(QUEUE_DEPTH_BEFORE, queueDepthBefore);
        payload.put(QUEUE_DEPTH_AFTER, queueDepthAfter);
    }
}
