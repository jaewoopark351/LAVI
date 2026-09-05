package lavi.minecraft.fabric.chatclef.bridge.transport.result;

//20260905_kpopmodder: Own WebSocket submission and normalize its synchronous and asynchronous outcomes.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;

import java.net.http.WebSocket;
import java.util.function.Consumer;

public final class FabricChatClefResultTransportSubmission {
    private final FabricChatClefResultSendDiagnostics diagnostics;
    private final FabricChatClefResultAsyncCompletion asyncCompletion;

    public FabricChatClefResultTransportSubmission(
        FabricChatClefResultSendDiagnostics diagnostics,
        FabricChatClefResultSendCompletionRouter completionRouter
    ) {
        this.diagnostics = diagnostics;
        this.asyncCompletion = new FabricChatClefResultAsyncCompletion(
                diagnostics,
                completionRouter
        );
    }

    public FabricChatClefCommandResultSendSubmission submit(
            WebSocket socket,
            String message,
            FabricChatClefCommandContext context,
            Consumer<FabricChatClefCommandResultSendOutcome> stopCompletion
    ) {
        try {
            socket.sendText(message, true)
                    .whenComplete(
                            (ignored, error) -> asyncCompletion.complete(
                                    context,
                                    stopCompletion,
                                    error
                            )
                    );
            return FabricChatClefCommandResultSendSubmission.accepted();
        } catch (Exception error) {
            String detail = diagnostics.sendFailed(error);
            return FabricChatClefCommandResultSendSubmission.failed(
                    FabricChatClefCommandResultSendOutcome.failed(
                            FabricChatClefCommandResultSendStatus.SEND_FAILED,
                            detail
                    )
            );
        }
    }

}
