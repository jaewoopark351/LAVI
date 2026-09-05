package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox;

//20260905_kpopmodder: Drain and apply bounded asynchronous terminal-send completions on the tick owner.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;

public final class FabricChatClefCommandResultCompletionDrainer {
    private static final int MAX_SEND_COMPLETIONS_PER_TICK = 32;

    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandResultRetirementCommit retirementCommit;
    private final FabricChatClefCommandResultOutboxDiagnostics diagnostics;

    public FabricChatClefCommandResultCompletionDrainer(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultRetirementCommit retirementCommit,
            FabricChatClefCommandResultOutboxDiagnostics diagnostics
    ) {
        this.commandQueue = commandQueue;
        this.retirementCommit = retirementCommit;
        this.diagnostics = diagnostics;
    }

    public boolean drain(FabricChatClefCommandExecution activeExecution) {
        boolean activeTerminalCompleted = false;
        for (int index = 0; index < MAX_SEND_COMPLETIONS_PER_TICK; index++) {
            FabricChatClefCommandResultSendCompletion completion =
                    commandQueue.pollCommandResultSendCompletion();
            if (completion == null) {
                return activeTerminalCompleted;
            }
            activeTerminalCompleted |= apply(activeExecution, completion);
        }
        return activeTerminalCompleted;
    }

    private boolean apply(
            FabricChatClefCommandExecution activeExecution,
            FabricChatClefCommandResultSendCompletion sendCompletion
    ) {
        FabricChatClefCommandContext context = sendCompletion.context();
        FabricChatClefCommandResultSendOutcome outcome = sendCompletion.outcome();
        if (context == null || outcome == null) {
            diagnostics.malformedCompletion();
            return false;
        }
        boolean terminalMarked = context.completeTerminalSend(outcome);
        if (!outcome.succeeded()) {
            diagnostics.asyncSendFailed(context, outcome);
            return false;
        }
        return retirementCommit.retire(
                activeExecution,
                context,
                terminalMarked,
                sendCompletion.completedAtMs()
        );
    }
}
