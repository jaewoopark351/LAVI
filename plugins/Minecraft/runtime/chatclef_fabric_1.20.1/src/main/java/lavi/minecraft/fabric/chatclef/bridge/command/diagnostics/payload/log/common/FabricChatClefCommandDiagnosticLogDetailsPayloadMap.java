package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log.common;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.FabricChatClefCommandDiagnosticDetailsMapPayload;

import java.util.Map;

public final class FabricChatClefCommandDiagnosticLogDetailsPayloadMap {
    private static final String DETAILS = "details";

    private FabricChatClefCommandDiagnosticLogDetailsPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, FabricChatClefCommandDiagnosticDetailsPayload details) {
        payload.put(DETAILS, FabricChatClefCommandDiagnosticDetailsMapPayload.toMap(details));
    }
}
