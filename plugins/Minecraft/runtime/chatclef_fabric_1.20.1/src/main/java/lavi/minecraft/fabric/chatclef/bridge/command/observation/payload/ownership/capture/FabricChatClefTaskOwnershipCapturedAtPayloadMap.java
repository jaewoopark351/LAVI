package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture;

import java.util.Map;

public final class FabricChatClefTaskOwnershipCapturedAtPayloadMap {
    private static final String CAPTURED_AT_MS = "captured_at_ms";

    private FabricChatClefTaskOwnershipCapturedAtPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long capturedAtMs) {
        payload.put(CAPTURED_AT_MS, capturedAtMs);
    }
}
