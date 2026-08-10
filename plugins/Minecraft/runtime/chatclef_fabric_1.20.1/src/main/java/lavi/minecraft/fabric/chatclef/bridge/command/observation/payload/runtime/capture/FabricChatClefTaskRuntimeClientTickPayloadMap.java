package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.capture;

import java.util.Map;

public final class FabricChatClefTaskRuntimeClientTickPayloadMap {
    private static final String CLIENT_TICK_ID = "client_tick_id";

    private FabricChatClefTaskRuntimeClientTickPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long clientTickId) {
        payload.put(CLIENT_TICK_ID, clientTickId);
    }
}
