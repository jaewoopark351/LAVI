package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.capture;

import java.util.Map;

public final class FabricChatClefTaskOwnershipCaptureThreadPayloadMap {
    private static final String CAPTURE_THREAD = "capture_thread";

    private FabricChatClefTaskOwnershipCaptureThreadPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String captureThread) {
        payload.put(CAPTURE_THREAD, captureThread);
    }
}
