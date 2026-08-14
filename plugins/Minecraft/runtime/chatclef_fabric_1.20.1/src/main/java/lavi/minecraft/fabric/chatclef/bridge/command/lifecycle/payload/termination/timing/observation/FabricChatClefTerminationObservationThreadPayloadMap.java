package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation;

import java.util.Map;

//20260814_kpopmodder: Keep the observation thread key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationObservationThreadPayloadMap {
    private static final String OBSERVATION_THREAD = "observation_thread";

    private FabricChatClefTerminationObservationThreadPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String observationThread) {
        payload.put(OBSERVATION_THREAD, observationThread);
    }
}
