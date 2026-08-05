package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

//20260803_kpopmodder: Keep terminal outcome decisions separate from task observation.
public final class FabricChatClefCommandTerminalDecision {
    private final boolean terminal;
    private final String reason;
    private final FabricChatClefCommandResultPayload result;

    private FabricChatClefCommandTerminalDecision(
            boolean terminal,
            String reason,
            FabricChatClefCommandResultPayload result
    ) {
        this.terminal = terminal;
        this.reason = reason;
        this.result = result;
    }

    public static FabricChatClefCommandTerminalDecision waiting(String reason) {
        return new FabricChatClefCommandTerminalDecision(false, reason, null);
    }

    public static FabricChatClefCommandTerminalDecision terminal(String reason, FabricChatClefCommandResultPayload result) {
        return new FabricChatClefCommandTerminalDecision(true, reason, result);
    }

    public boolean terminal() {
        return terminal;
    }

    public String reason() {
        return reason;
    }

    public FabricChatClefCommandResultPayload result() {
        return result;
    }
}
