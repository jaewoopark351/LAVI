package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefTerminalDecisionDetailsPayloadMap;

import java.util.Map;

public final class FabricChatClefTerminalDecisionDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private final String decisionReason;

    public FabricChatClefTerminalDecisionDetailsPayload(String decisionReason) {
        this.decisionReason = FabricChatClefLifecycleDetailValues.nullToEmpty(decisionReason);
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefTerminalDecisionDetailsPayloadMap.toMap(decisionReason);
    }
}
