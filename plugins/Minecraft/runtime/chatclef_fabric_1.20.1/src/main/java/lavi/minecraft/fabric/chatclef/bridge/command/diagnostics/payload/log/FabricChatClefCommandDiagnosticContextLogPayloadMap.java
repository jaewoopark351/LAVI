package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolate context diagnostic log Map assembly without changing emitted keys.
public final class FabricChatClefCommandDiagnosticContextLogPayloadMap {
    private FabricChatClefCommandDiagnosticContextLogPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String event,
            FabricChatClefCommandContext context,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        Map<String, Object> payload = context == null
                ? new HashMap<>()
                : context.ownershipPayload().toMap();
        FabricChatClefCommandDiagnosticLogCommonPayloadMap.putCommonFields(payload, event, details);
        return payload;
    }
}
