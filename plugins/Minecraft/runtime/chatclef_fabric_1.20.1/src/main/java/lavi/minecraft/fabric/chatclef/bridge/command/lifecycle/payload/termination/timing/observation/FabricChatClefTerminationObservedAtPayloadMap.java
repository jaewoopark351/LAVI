package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.timing.observation;

import java.util.Map;

//20260814_kpopmodder: Keep the observation timestamp key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationObservedAtPayloadMap {
    private static final String OBSERVED_AT_MS = "observed_at_ms";

    private FabricChatClefTerminationObservedAtPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long observedAtMs) {
        payload.put(OBSERVED_AT_MS, observedAtMs);
    }
}
