package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation;

import java.util.Map;

//20260814_kpopmodder: Keep the observation sequence key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationObservationSequencePayloadMap {
    private static final String OBSERVATION_SEQUENCE = "observation_sequence";

    private FabricChatClefTerminationObservationSequencePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long observationSequence) {
        payload.put(OBSERVATION_SEQUENCE, observationSequence);
    }
}
