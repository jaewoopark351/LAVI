package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime;

import java.util.Map;

//20260808_kpopmodder: Split runtime observation capture fields without changing emitted keys.
public final class FabricChatClefTaskRuntimeCapturePayloadMap {
    private static final String THREAD_NAME = "thread_name";
    private static final String OBSERVED_AT_MS = "observed_at_ms";
    private static final String CLIENT_TICK_ID = "client_tick_id";

    private FabricChatClefTaskRuntimeCapturePayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String threadName,
            long observedAtMs,
            long clientTickId
    ) {
        payload.put(THREAD_NAME, threadName);
        payload.put(OBSERVED_AT_MS, observedAtMs);
        payload.put(CLIENT_TICK_ID, clientTickId);
    }
}
