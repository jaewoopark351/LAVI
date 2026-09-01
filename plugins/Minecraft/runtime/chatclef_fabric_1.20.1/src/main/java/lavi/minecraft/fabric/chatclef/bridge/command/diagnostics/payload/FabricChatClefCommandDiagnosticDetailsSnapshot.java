package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

//20260901_kpopmodder: Freeze lifecycle details once for authority logging and projections.
public final class FabricChatClefCommandDiagnosticDetailsSnapshot
        implements FabricChatClefCommandDiagnosticDetailsPayload {
    private final Map<String, Object> fields;

    private FabricChatClefCommandDiagnosticDetailsSnapshot(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public static FabricChatClefCommandDiagnosticDetailsSnapshot capture(
            FabricChatClefCommandDiagnosticDetailsPayload details) {
        if (details == null) {
            return new FabricChatClefCommandDiagnosticDetailsSnapshot(Map.of());
        }
        Map<String, Object> captured = details.toMap();
        return new FabricChatClefCommandDiagnosticDetailsSnapshot(
                captured == null ? Map.of() : captured
        );
    }

    @Override
    public Map<String, Object> toMap() {
        return fields;
    }
}
