package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.capture;

import java.util.Map;

public final class FabricChatClefTaskRuntimeObservedAtPayloadMap {
    private static final String OBSERVED_AT_MS = "observed_at_ms";

    private FabricChatClefTaskRuntimeObservedAtPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long observedAtMs) {
        payload.put(OBSERVED_AT_MS, observedAtMs);
    }
}
