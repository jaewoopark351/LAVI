package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.terminal;

import java.util.Map;

//20260808_kpopmodder: Keep terminal lifecycle clear fields separate without changing emitted keys.
public final class FabricChatClefTerminalResultClearPayloadMap {
    private static final String LIFECYCLE_CLEARED = "lifecycle_cleared";

    private FabricChatClefTerminalResultClearPayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, boolean lifecycleCleared) {
        details.put(LIFECYCLE_CLEARED, lifecycleCleared);
    }
}
