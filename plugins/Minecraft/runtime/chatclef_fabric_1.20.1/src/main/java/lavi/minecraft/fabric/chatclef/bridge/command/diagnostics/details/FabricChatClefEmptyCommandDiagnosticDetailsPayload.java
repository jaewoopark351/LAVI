package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.details;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.details.payload.FabricChatClefEmptyCommandDiagnosticDetailsPayloadMap;

import java.util.Map;

//20260807_kpopmodder: Keep empty diagnostic details typed before the final log Map edge.
public final class FabricChatClefEmptyCommandDiagnosticDetailsPayload
        implements FabricChatClefCommandDiagnosticDetailsPayload {
    public static FabricChatClefEmptyCommandDiagnosticDetailsPayload create() {
        return new FabricChatClefEmptyCommandDiagnosticDetailsPayload();
    }

    private FabricChatClefEmptyCommandDiagnosticDetailsPayload() {
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefEmptyCommandDiagnosticDetailsPayloadMap.toMap();
    }
}
