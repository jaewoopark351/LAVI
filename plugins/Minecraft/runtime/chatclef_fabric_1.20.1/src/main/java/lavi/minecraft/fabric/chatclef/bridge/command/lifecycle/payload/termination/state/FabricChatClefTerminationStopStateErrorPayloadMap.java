package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.termination.state;

import java.util.Map;

//20260814_kpopmodder: Keep the stop-state error key isolated without changing emitted diagnostics.
public final class FabricChatClefTerminationStopStateErrorPayloadMap {
    private static final String STOP_STATE_ERROR = "stop_state_error";

    private FabricChatClefTerminationStopStateErrorPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String stopStateError) {
        payload.put(STOP_STATE_ERROR, stopStateError);
    }
}
