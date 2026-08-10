package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.capture;

import java.util.Map;

public final class FabricChatClefTaskRuntimeThreadNamePayloadMap {
    private static final String THREAD_NAME = "thread_name";

    private FabricChatClefTaskRuntimeThreadNamePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String threadName) {
        payload.put(THREAD_NAME, threadName);
    }
}
