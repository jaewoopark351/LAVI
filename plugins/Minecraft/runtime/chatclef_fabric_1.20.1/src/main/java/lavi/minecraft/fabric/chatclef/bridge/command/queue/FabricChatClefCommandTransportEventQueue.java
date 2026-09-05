package lavi.minecraft.fabric.chatclef.bridge.command.queue;

//20260905_kpopmodder: Preserve the legacy transport-event API as a thin facade over two typed queues.

import lavi.minecraft.fabric.chatclef.bridge.command.connection.detach.FabricChatClefConnectionDetachEventQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletionQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;

import java.util.Optional;

public final class FabricChatClefCommandTransportEventQueue {
    private final FabricChatClefConnectionDetachEventQueue detachEvents =
            new FabricChatClefConnectionDetachEventQueue();
    private final FabricChatClefCommandResultSendCompletionQueue resultCompletions =
            new FabricChatClefCommandResultSendCompletionQueue();

    public void enqueueResultCompletion(FabricChatClefCommandResultSendCompletion completion) {
        resultCompletions.enqueue(completion);
    }

    public FabricChatClefCommandResultSendCompletion pollResultCompletion() {
        return resultCompletions.poll();
    }

    public void enqueueConnectionDetached(long connectionGeneration, String reason) {
        detachEvents.enqueue(connectionGeneration, reason);
    }

    public Optional<FabricChatClefConnectionDetachedEvent> pollConnectionDetached() {
        return detachEvents.poll();
    }

    public void clear() {
        detachEvents.clear();
        resultCompletions.clear();
    }
}
