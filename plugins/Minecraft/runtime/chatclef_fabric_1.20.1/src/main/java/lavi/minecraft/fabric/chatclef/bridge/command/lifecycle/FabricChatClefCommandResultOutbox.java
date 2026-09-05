package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

//20260905_kpopmodder: Preserve the result outbox API as a thin responsibility-composition facade.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox.FabricChatClefCommandResultCompletionDrainer;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox.FabricChatClefCommandResultOutboxDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox.FabricChatClefCommandResultRetirementCommit;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox.FabricChatClefTerminalResultSubmission;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.function.Supplier;

public final class FabricChatClefCommandResultOutbox {
    private final FabricChatClefTerminalResultSubmission terminalSubmission;
    private final FabricChatClefCommandResultCompletionDrainer completionDrainer;

    public FabricChatClefCommandResultOutbox(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        FabricChatClefCommandResultOutboxDiagnostics outboxDiagnostics =
                new FabricChatClefCommandResultOutboxDiagnostics(diagnostics);
        this.terminalSubmission = new FabricChatClefTerminalResultSubmission(
                commandQueue,
                resultSender,
                outboxDiagnostics
        );
        this.completionDrainer = new FabricChatClefCommandResultCompletionDrainer(
                commandQueue,
                new FabricChatClefCommandResultRetirementCommit(
                        commandQueue,
                        outboxDiagnostics
                ),
                outboxDiagnostics
        );
    }

    public boolean sendTerminal(
            FabricChatClefCommandExecution execution,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return terminalSubmission.submitActive(execution, resultFactory);
    }

    public boolean sendTerminal(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return terminalSubmission.submitActive(context, resultFactory);
    }

    public boolean sendPendingTerminal(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return terminalSubmission.submitPending(context, resultFactory);
    }

    public boolean drainSendCompletions(FabricChatClefCommandExecution activeExecution) {
        return completionDrainer.drain(activeExecution);
    }
}
