package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;

import java.util.HashMap;
import java.util.Map;

//20260807_kpopmodder: Isolated raw diagnostic detail maps behind the typed log detail payload boundary.
public final class FabricChatClefCommandDiagnosticDetailsMapPayload
        implements FabricChatClefCommandDiagnosticDetailsPayload {
    private final Map<String, Object> details;

    private FabricChatClefCommandDiagnosticDetailsMapPayload(Map<String, Object> details) {
        this.details = details == null ? new HashMap<>() : new HashMap<>(details);
    }

    public static FabricChatClefCommandDiagnosticDetailsMapPayload from(Map<String, Object> details) {
        return new FabricChatClefCommandDiagnosticDetailsMapPayload(details);
    }

    public static FabricChatClefCommandDiagnosticDetailsMapPayload empty() {
        return new FabricChatClefCommandDiagnosticDetailsMapPayload(null);
    }

    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>(details);
    }
}
