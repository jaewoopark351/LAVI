package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

//20260815_kpopmodder: Return WebSocket send completion to the client tick before clearing command ownership.
public final class FabricChatClefCommandResultSendCompletion {
    private final FabricChatClefCommandContext context;
    private final FabricChatClefCommandResultSendOutcome outcome;
    private final long completedAtMs;

    private FabricChatClefCommandResultSendCompletion(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultSendOutcome outcome,
            long completedAtMs
    ) {
        this.context = context;
        this.outcome = outcome;
        this.completedAtMs = completedAtMs;
    }

    public static FabricChatClefCommandResultSendCompletion of(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        return new FabricChatClefCommandResultSendCompletion(
                context,
                outcome,
                System.currentTimeMillis()
        );
    }

    public FabricChatClefCommandContext context() {
        return context;
    }

    public FabricChatClefCommandResultSendOutcome outcome() {
        return outcome;
    }

    public long completedAtMs() {
        return completedAtMs;
    }
}
