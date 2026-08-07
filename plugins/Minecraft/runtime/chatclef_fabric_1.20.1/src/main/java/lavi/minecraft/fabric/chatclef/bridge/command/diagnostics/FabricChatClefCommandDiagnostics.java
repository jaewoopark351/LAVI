package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.FabricChatClefCommandDiagnosticLogPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

//20260803_kpopmodder: Added diagnostic logging to prove Fabric ChatClef command lifecycle boundaries.
public final class FabricChatClefCommandDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefCommandDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void info(String event, FabricChatClefCommandExecution execution) {
        diagnostics.info(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.execution(event, execution)
        );
    }

    public void warn(String event, FabricChatClefCommandExecution execution) {
        diagnostics.warn(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.execution(event, execution)
        );
    }

    public void info(
            String event,
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        diagnostics.info(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.execution(event, execution, details)
        );
    }

    public void warn(
            String event,
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        diagnostics.warn(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.execution(event, execution, details)
        );
    }

    public void contextInfo(
            String event,
            FabricChatClefCommandContext context,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        diagnostics.info(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.context(event, context, details)
        );
    }

    public void contextWarn(
            String event,
            FabricChatClefCommandContext context,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        diagnostics.warn(
                "command lifecycle "
                        + FabricChatClefCommandDiagnosticLogPayload.context(event, context, details)
        );
    }
}
