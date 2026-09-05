package lavi.minecraft.fabric.chatclef.bridge.transport.result;

//20260905_kpopmodder: Route asynchronous result completion to its ordinary or STOP owner.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;

import java.util.function.Consumer;

public final class FabricChatClefResultSendCompletionRouter {
    private final Consumer<FabricChatClefCommandResultSendCompletion> ordinaryCompletionSink;

    public FabricChatClefResultSendCompletionRouter(
            Consumer<FabricChatClefCommandResultSendCompletion> ordinaryCompletionSink
    ) {
        this.ordinaryCompletionSink = ordinaryCompletionSink;
    }

    public void complete(
            FabricChatClefCommandContext context,
            Consumer<FabricChatClefCommandResultSendOutcome> stopCompletion,
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        if (context != null && ordinaryCompletionSink != null) {
            ordinaryCompletionSink.accept(FabricChatClefCommandResultSendCompletion.of(context, outcome));
        }
        if (stopCompletion != null) {
            stopCompletion.accept(outcome);
        }
    }
}
