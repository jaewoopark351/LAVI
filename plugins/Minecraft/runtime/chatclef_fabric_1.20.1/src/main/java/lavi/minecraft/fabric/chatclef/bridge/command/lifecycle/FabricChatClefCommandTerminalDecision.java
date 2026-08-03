package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import java.util.HashMap;
import java.util.Map;

//20260803_kpopmodder: Keep terminal outcome decisions separate from task observation.
public final class FabricChatClefCommandTerminalDecision {
    private final boolean terminal;
    private final String reason;
    private final Map<String, Object> result;

    private FabricChatClefCommandTerminalDecision(
            boolean terminal,
            String reason,
            Map<String, Object> result
    ) {
        this.terminal = terminal;
        this.reason = reason;
        this.result = result;
    }

    public static FabricChatClefCommandTerminalDecision waiting(String reason) {
        return new FabricChatClefCommandTerminalDecision(false, reason, new HashMap<>());
    }

    public static FabricChatClefCommandTerminalDecision terminal(String reason, Map<String, Object> result) {
        return new FabricChatClefCommandTerminalDecision(true, reason, result);
    }

    public boolean terminal() {
        return terminal;
    }

    public String reason() {
        return reason;
    }

    public Map<String, Object> result() {
        return result;
    }
}
