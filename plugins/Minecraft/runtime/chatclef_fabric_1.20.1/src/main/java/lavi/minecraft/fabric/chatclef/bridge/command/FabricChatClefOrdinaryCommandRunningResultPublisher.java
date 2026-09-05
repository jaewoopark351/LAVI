package lavi.minecraft.fabric.chatclef.bridge.command;

//20260905_kpopmodder: Publish only the initial running result for an ordinary command.

import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

public final class FabricChatClefOrdinaryCommandRunningResultPublisher {
    private final FabricChatClefCommandResultSender resultSender;
    private final FabricChatClefOrdinaryCommandDispatchDiagnostics diagnostics;

    public FabricChatClefOrdinaryCommandRunningResultPublisher(
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefOrdinaryCommandDispatchDiagnostics diagnostics
    ) {
        this.resultSender = resultSender;
        this.diagnostics = diagnostics;
    }

    public void publish(
            FabricChatClefCommandContext context,
            FabricChatClefCommandExecution execution
    ) {
        FabricChatClefCommandResultSendSubmission submission =
                resultSender.sendCommandResult(context, execution.runningResult());
        if (!submission.acceptedForAsyncSend()) {
            diagnostics.runningResultSubmissionFailed(context, submission);
        }
    }
}
