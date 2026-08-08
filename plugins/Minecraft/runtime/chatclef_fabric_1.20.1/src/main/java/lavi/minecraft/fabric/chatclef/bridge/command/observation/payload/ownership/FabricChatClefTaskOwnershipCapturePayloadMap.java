package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership;

import java.util.Map;

//20260808_kpopmodder: Split task ownership capture fields from the snapshot Map edge without changing keys.
public final class FabricChatClefTaskOwnershipCapturePayloadMap {
    private static final String AVAILABLE = "available";
    private static final String ERROR = "error";
    private static final String CAPTURED_AT_MS = "captured_at_ms";
    private static final String CAPTURED_CLIENT_TICK = "captured_client_tick";
    private static final String CAPTURE_THREAD = "capture_thread";

    private FabricChatClefTaskOwnershipCapturePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean available,
            String error,
            long capturedAtMs,
            long capturedClientTick,
            String captureThread
    ) {
        payload.put(AVAILABLE, available);
        payload.put(ERROR, error);
        payload.put(CAPTURED_AT_MS, capturedAtMs);
        payload.put(CAPTURED_CLIENT_TICK, capturedClientTick);
        payload.put(CAPTURE_THREAD, captureThread);
    }
}
