package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import java.util.Map;

//20260805_kpopmodder: Keep command diagnostic details typed until the existing log Map edge.
public interface FabricChatClefCommandDiagnosticDetailsPayload {
    Map<String, Object> toMap();
}
