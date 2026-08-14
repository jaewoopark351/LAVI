package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.state;

import java.util.Map;

//20260814_kpopmodder: Keep the stop-state availability key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationStopStateAvailablePayloadMap {
    private static final String STOP_STATE_AVAILABLE = "stop_state_available";

    private FabricChatClefTerminationStopStateAvailablePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean stopStateAvailable) {
        payload.put(STOP_STATE_AVAILABLE, stopStateAvailable);
    }
}
