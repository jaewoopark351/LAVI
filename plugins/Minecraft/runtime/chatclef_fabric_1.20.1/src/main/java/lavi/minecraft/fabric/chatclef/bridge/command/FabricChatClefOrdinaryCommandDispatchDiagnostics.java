package lavi.minecraft.fabric.chatclef.bridge.command;

//20260905_kpopmodder: Render only bounded ordinary-command dispatch diagnostics.

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefOrdinaryCommandDispatchDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefOrdinaryCommandDispatchDiagnostics(
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.diagnostics = diagnostics;
    }

    public void started(
            FabricChatClefCommandContext context,
            FabricChatClefCommandRequest request,
            String command
    ) {
        diagnostics.info(
                "dispatch command request="
                        + context.requestId()
                        + " source="
                        + request.source
                        + " normalized_command="
                        + command
                        + " generation="
                        + context.connectionGeneration()
        );
    }

    public void runningResultSubmissionFailed(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultSendSubmission submission
    ) {
        diagnostics.warn(
                "running result send submit failed request="
                        + context.requestId()
                        + " outcome="
                        + submission.diagnosticMessage()
        );
    }
}
