package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefTerminalDecisionDetailsPayloadMap {
    private static final String DECISION_REASON = "decision_reason";

    private FabricChatClefTerminalDecisionDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(String decisionReason) {
        Map<String, Object> details = new HashMap<>();
        details.put(DECISION_REASON, decisionReason);
        return details;
    }
}
