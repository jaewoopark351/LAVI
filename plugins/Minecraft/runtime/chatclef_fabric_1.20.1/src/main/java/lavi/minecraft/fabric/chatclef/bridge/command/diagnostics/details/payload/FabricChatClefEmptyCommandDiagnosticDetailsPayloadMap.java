package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.details.payload;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolate empty diagnostic detail Map serialization without changing emitted fields.
public final class FabricChatClefEmptyCommandDiagnosticDetailsPayloadMap {
    private FabricChatClefEmptyCommandDiagnosticDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap() {
        return new HashMap<>();
    }
}
