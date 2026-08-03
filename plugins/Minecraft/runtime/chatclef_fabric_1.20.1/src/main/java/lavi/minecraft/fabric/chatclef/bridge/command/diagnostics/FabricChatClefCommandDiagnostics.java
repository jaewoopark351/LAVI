package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.HashMap;
import java.util.Map;

//20260803_kpopmodder: Added diagnostic logging to prove Fabric ChatClef command lifecycle boundaries.
public final class FabricChatClefCommandDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefCommandDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void info(String event, FabricChatClefCommandExecution execution) {
        info(event, execution, new HashMap<>());
    }

    public void warn(String event, FabricChatClefCommandExecution execution) {
        warn(event, execution, new HashMap<>());
    }

    public void info(
            String event,
            FabricChatClefCommandExecution execution,
            Map<String, Object> details
    ) {
        diagnostics.info("command lifecycle " + executionPayload(event, execution, details));
    }

    public void warn(
            String event,
            FabricChatClefCommandExecution execution,
            Map<String, Object> details
    ) {
        diagnostics.warn("command lifecycle " + executionPayload(event, execution, details));
    }

    public void contextInfo(
            String event,
            FabricChatClefCommandContext context,
            Map<String, Object> details
    ) {
        diagnostics.info("command lifecycle " + contextPayload(event, context, details));
    }

    public void contextWarn(
            String event,
            FabricChatClefCommandContext context,
            Map<String, Object> details
    ) {
        diagnostics.warn("command lifecycle " + contextPayload(event, context, details));
    }

    private Map<String, Object> executionPayload(
            String event,
            FabricChatClefCommandExecution execution,
            Map<String, Object> details
    ) {
        Map<String, Object> payload = execution == null
                ? new HashMap<>()
                : execution.diagnosticData(event);
        payload.put("event", event);
        payload.put("details", details == null ? new HashMap<String, Object>() : details);
        return payload;
    }

    private Map<String, Object> contextPayload(
            String event,
            FabricChatClefCommandContext context,
            Map<String, Object> details
    ) {
        Map<String, Object> payload = context == null
                ? new HashMap<>()
                : context.ownershipData();
        payload.put("event", event);
        payload.put("details", details == null ? new HashMap<String, Object>() : details);
        return payload;
    }
}
