package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.terminal.FabricChatClefTerminalResultClearPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.terminal.FabricChatClefTerminalResultSendPayloadMap;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefTerminalResultDetailsPayloadMap {
    private FabricChatClefTerminalResultDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(boolean terminalSent, boolean lifecycleCleared) {
        Map<String, Object> details = new HashMap<>();
        FabricChatClefTerminalResultSendPayloadMap.writeTo(details, terminalSent);
        FabricChatClefTerminalResultClearPayloadMap.writeTo(details, lifecycleCleared);
        return details;
    }
}
