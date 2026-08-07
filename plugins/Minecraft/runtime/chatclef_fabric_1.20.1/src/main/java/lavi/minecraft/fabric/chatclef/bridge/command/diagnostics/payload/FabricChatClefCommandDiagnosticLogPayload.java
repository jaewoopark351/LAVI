package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.details.FabricChatClefEmptyCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log.FabricChatClefCommandDiagnosticContextLogPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.payload.log.FabricChatClefCommandDiagnosticExecutionLogPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;

import java.util.Map;

//20260807_kpopmodder: Split command diagnostic log payload assembly from log emission without changing keys.
public final class FabricChatClefCommandDiagnosticLogPayload {
    private FabricChatClefCommandDiagnosticLogPayload() {
    }

    public static Map<String, Object> execution(
            String event,
            FabricChatClefCommandExecution execution
    ) {
        return execution(event, execution, FabricChatClefEmptyCommandDiagnosticDetailsPayload.create());
    }

    public static Map<String, Object> execution(
            String event,
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        return FabricChatClefCommandDiagnosticExecutionLogPayloadMap.toMap(event, execution, details);
    }

    public static Map<String, Object> context(
            String event,
            FabricChatClefCommandContext context
    ) {
        return context(event, context, FabricChatClefEmptyCommandDiagnosticDetailsPayload.create());
    }

    public static Map<String, Object> context(
            String event,
            FabricChatClefCommandContext context,
            FabricChatClefCommandDiagnosticDetailsPayload details
    ) {
        return FabricChatClefCommandDiagnosticContextLogPayloadMap.toMap(event, context, details);
    }
}
