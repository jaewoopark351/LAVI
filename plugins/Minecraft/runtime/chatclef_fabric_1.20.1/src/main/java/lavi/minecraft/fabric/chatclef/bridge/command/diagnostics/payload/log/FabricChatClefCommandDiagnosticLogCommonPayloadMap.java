package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log.common.FabricChatClefCommandDiagnosticLogDetailsPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log.common.FabricChatClefCommandDiagnosticLogEventPayloadMap;

import java.util.Map;

//20260807_kpopmodder: Keep shared diagnostic log fields at the final log Map edge.
final class FabricChatClefCommandDiagnosticLogCommonPayloadMap {
    private FabricChatClefCommandDiagnosticLogCommonPayloadMap() {
    }

    static void putCommonFields(
            Map<String, Object> payload,
            String event,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        FabricChatClefCommandDiagnosticLogEventPayloadMap.writeTo(payload, event);
        FabricChatClefCommandDiagnosticLogDetailsPayloadMap.writeTo(payload, details);
    }
}
