package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Add a dedicated diagnostic details map boundary before log serialization.
public final class FabricChatClefCommandDiagnosticDetailsMapPayload {
    private FabricChatClefCommandDiagnosticDetailsMapPayload() {
    }

    public static Map<String, Object> toMap(FabricChatClefCommandDiagnosticDetailsPayload details) {
        if (details == null) {
            return new HashMap<>();
        }
        Map<String, Object> payload = details.toMap();
        return payload == null ? new HashMap<>() : payload;
    }
}
