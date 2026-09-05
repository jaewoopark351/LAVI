package lavi.minecraft.fabric.chatclef.bridge.transport.result;

//20260905_kpopmodder: Normalize asynchronous send completion before routing it to the owning lifecycle.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;

import java.util.function.Consumer;

public final class FabricChatClefResultAsyncCompletion {
    private final FabricChatClefResultSendDiagnostics diagnostics;
    private final FabricChatClefResultSendCompletionRouter completionRouter;

    public FabricChatClefResultAsyncCompletion(
            FabricChatClefResultSendDiagnostics diagnostics,
            FabricChatClefResultSendCompletionRouter completionRouter
    ) {
        this.diagnostics = diagnostics;
        this.completionRouter = completionRouter;
    }

    public void complete(
            FabricChatClefCommandContext context,
            Consumer<FabricChatClefCommandResultSendOutcome> stopCompletion,
            Throwable error
    ) {
        FabricChatClefCommandResultSendOutcome outcome;
        if (error == null) {
            outcome = FabricChatClefCommandResultSendOutcome.sent();
        } else {
            String detail = diagnostics.asyncSendFailed(error);
            outcome = FabricChatClefCommandResultSendOutcome.failed(
                    FabricChatClefCommandResultSendStatus.ASYNC_SEND_FAILED,
                    detail
            );
        }
        completionRouter.complete(context, stopCompletion, outcome);
    }
}
