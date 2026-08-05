package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefTerminalDecisionDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private static final String DECISION_REASON = "decision_reason";

    private final String decisionReason;

    public FabricChatClefTerminalDecisionDetailsPayload(String decisionReason) {
        this.decisionReason = FabricChatClefLifecycleDetailValues.nullToEmpty(decisionReason);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> details = new HashMap<>();
        details.put(DECISION_REASON, decisionReason);
        return details;
    }
}
