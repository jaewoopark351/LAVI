package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log.common;

import java.util.Map;

public final class FabricChatClefCommandDiagnosticLogEventPayloadMap {
    private static final String EVENT = "event";

    private FabricChatClefCommandDiagnosticLogEventPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String event) {
        payload.put(EVENT, event);
    }
}
