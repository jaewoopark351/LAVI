package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.terminal;

import java.util.Map;

//20260808_kpopmodder: Keep terminal result send fields separate without changing emitted keys.
public final class FabricChatClefTerminalResultSendPayloadMap {
    private static final String TERMINAL_SENT = "terminal_sent";

    private FabricChatClefTerminalResultSendPayloadMap() {
    }

    public static void writeTo(Map<String, Object> details, boolean terminalSent) {
        details.put(TERMINAL_SENT, terminalSent);
    }
}
