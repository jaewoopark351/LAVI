package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefTerminalResultDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private static final String TERMINAL_SENT = "terminal_sent";
    private static final String LIFECYCLE_CLEARED = "lifecycle_cleared";

    private final boolean terminalSent;
    private final boolean lifecycleCleared;

    public FabricChatClefTerminalResultDetailsPayload(boolean terminalSent, boolean lifecycleCleared) {
        this.terminalSent = terminalSent;
        this.lifecycleCleared = lifecycleCleared;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> details = new HashMap<>();
        details.put(TERMINAL_SENT, terminalSent);
        details.put(LIFECYCLE_CLEARED, lifecycleCleared);
        return details;
    }
}
