package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox;

//20260905_kpopmodder: Own asynchronous terminal-result transport submission effects.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

public final class FabricChatClefTerminalResultTransport {
    private final FabricChatClefCommandResultSender resultSender;
    private final FabricChatClefCommandResultOutboxDiagnostics diagnostics;

    public FabricChatClefTerminalResultTransport(
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandResultOutboxDiagnostics diagnostics
    ) {
        this.resultSender = resultSender;
        this.diagnostics = diagnostics;
    }

    public boolean submit(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload result
    ) {
        FabricChatClefCommandResultSendSubmission submission =
                resultSender.sendTerminalCommandResult(context, result);
        if (!submission.acceptedForAsyncSend()) {
            FabricChatClefCommandResultSendOutcome outcome = submission.immediateOutcome();
            context.completeTerminalSend(outcome);
            diagnostics.submissionFailed(context, outcome);
            return false;
        }
        diagnostics.submissionStarted(context);
        return true;
    }
}
