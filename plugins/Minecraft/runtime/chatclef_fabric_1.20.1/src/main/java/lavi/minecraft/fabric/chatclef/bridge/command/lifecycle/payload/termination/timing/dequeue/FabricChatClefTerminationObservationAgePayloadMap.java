package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.dequeue;

import java.util.Map;

//20260814_kpopmodder: Keep the observation-age key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationObservationAgePayloadMap {
    private static final String OBSERVATION_AGE_MS = "observation_age_ms";

    private FabricChatClefTerminationObservationAgePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long observationAgeMs) {
        payload.put(OBSERVATION_AGE_MS, observationAgeMs);
    }
}
