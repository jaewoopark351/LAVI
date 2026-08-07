package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefTerminalResultDetailsPayloadMap;

import java.util.Map;

public final class FabricChatClefTerminalResultDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private final boolean terminalSent;
    private final boolean lifecycleCleared;

    public FabricChatClefTerminalResultDetailsPayload(boolean terminalSent, boolean lifecycleCleared) {
        this.terminalSent = terminalSent;
        this.lifecycleCleared = lifecycleCleared;
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefTerminalResultDetailsPayloadMap.toMap(terminalSent, lifecycleCleared);
    }
}
