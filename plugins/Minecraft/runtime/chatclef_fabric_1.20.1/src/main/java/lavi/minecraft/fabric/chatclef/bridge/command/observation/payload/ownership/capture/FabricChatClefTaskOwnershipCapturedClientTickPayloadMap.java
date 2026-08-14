package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture;

import java.util.Map;

public final class FabricChatClefTaskOwnershipCapturedClientTickPayloadMap {
    private static final String CAPTURED_CLIENT_TICK = "captured_client_tick";

    private FabricChatClefTaskOwnershipCapturedClientTickPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long capturedClientTick) {
        payload.put(CAPTURED_CLIENT_TICK, capturedClientTick);
    }
}
