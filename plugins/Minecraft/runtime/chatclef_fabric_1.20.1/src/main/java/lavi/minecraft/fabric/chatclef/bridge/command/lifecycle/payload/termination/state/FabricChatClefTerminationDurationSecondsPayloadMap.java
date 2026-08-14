package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.state;

import java.util.Map;

//20260814_kpopmodder: Keep the termination duration key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationDurationSecondsPayloadMap {
    private static final String DURATION_SECONDS = "duration_seconds";

    private FabricChatClefTerminationDurationSecondsPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, double durationSeconds) {
        payload.put(DURATION_SECONDS, durationSeconds);
    }
}
