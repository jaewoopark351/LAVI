package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Drain a bounded number of asynchronous STOP send completions per tick.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultSendCompletion;

public final class FabricChatClefStopControlResultCompletionDrainer {
    private static final int MAX_COMPLETIONS_PER_TICK = 32;

    private final FabricChatClefStopControlResultSendCompletionQueue completionQueue;
    private final FabricChatClefStopControlResultCompletionStage completionStage;

    public FabricChatClefStopControlResultCompletionDrainer(
            FabricChatClefStopControlResultSendCompletionQueue completionQueue,
            FabricChatClefStopControlResultCompletionStage completionStage
    ) {
        this.completionQueue = completionQueue;
        this.completionStage = completionStage;
    }

    public void drain(long nowMs) {
        for (int index = 0; index < MAX_COMPLETIONS_PER_TICK; index++) {
            FabricChatClefStopControlResultSendCompletion completion = completionQueue.poll();
            if (completion == null) {
                return;
            }
            completionStage.complete(completion.delivery(), completion.outcome(), nowMs);
        }
    }
}
