package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.FabricChatClefCommandDiagnosticDetailsMapPayload;

import java.util.Map;

//20260807_kpopmodder: Keep shared diagnostic log fields at the final log Map edge.
final class FabricChatClefCommandDiagnosticLogCommonPayloadMap {
    private static final String EVENT = "event";
    private static final String DETAILS = "details";

    private FabricChatClefCommandDiagnosticLogCommonPayloadMap() {
    }

    static void putCommonFields(
            Map<String, Object> payload,
            String event,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        payload.put(EVENT, event);
        payload.put(DETAILS, FabricChatClefCommandDiagnosticDetailsMapPayload.toMap(details));
    }
}
