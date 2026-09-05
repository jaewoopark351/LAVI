package lavi.minecraft.fabric.chatclef.bridge.command.result.send;

//20260905_kpopmodder: Own async completion events for ordinary command result sends.

import java.util.ArrayDeque;
import java.util.Deque;

public final class FabricChatClefCommandResultSendCompletionQueue {
    private final Deque<FabricChatClefCommandResultSendCompletion> completions = new ArrayDeque<>();

    public synchronized void enqueue(FabricChatClefCommandResultSendCompletion completion) {
        if (completion != null) {
            completions.offer(completion);
        }
    }

    public synchronized FabricChatClefCommandResultSendCompletion poll() {
        return completions.poll();
    }

    public synchronized void clear() {
        completions.clear();
    }
}
