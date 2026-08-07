package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolate execution diagnostic log Map assembly without changing emitted keys.
public final class FabricChatClefCommandDiagnosticExecutionLogPayloadMap {
    private FabricChatClefCommandDiagnosticExecutionLogPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String event,
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        Map<String, Object> payload = execution == null
                ? new HashMap<>()
                : execution.diagnosticPayload(event).toMap();
        FabricChatClefCommandDiagnosticLogCommonPayloadMap.putCommonFields(payload, event, details);
        return payload;
    }
}
