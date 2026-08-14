package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture;

import java.util.Map;

public final class FabricChatClefTaskOwnershipCaptureAvailablePayloadMap {
    private static final String AVAILABLE = "available";

    private FabricChatClefTaskOwnershipCaptureAvailablePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean available) {
        payload.put(AVAILABLE, available);
    }
}
