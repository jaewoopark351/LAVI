package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture;

import java.util.Map;

public final class FabricChatClefTaskOwnershipCaptureErrorPayloadMap {
    private static final String ERROR = "error";

    private FabricChatClefTaskOwnershipCaptureErrorPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String error) {
        payload.put(ERROR, error);
    }
}
