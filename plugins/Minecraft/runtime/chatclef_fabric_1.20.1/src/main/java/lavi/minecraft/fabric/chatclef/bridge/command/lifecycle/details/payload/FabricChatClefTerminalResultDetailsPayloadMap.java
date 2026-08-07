package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefTerminalResultDetailsPayloadMap {
    private static final String TERMINAL_SENT = "terminal_sent";
    private static final String LIFECYCLE_CLEARED = "lifecycle_cleared";

    private FabricChatClefTerminalResultDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(boolean terminalSent, boolean lifecycleCleared) {
        Map<String, Object> details = new HashMap<>();
        details.put(TERMINAL_SENT, terminalSent);
        details.put(LIFECYCLE_CLEARED, lifecycleCleared);
        return details;
    }
}
