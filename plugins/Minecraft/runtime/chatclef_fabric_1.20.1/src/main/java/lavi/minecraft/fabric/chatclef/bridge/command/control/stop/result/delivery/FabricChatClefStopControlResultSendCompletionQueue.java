package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Own async completion events for STOP result sends.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDelivery;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;

import java.util.ArrayDeque;
import java.util.Deque;

public final class FabricChatClefStopControlResultSendCompletionQueue {
    private final Deque<FabricChatClefStopControlResultSendCompletion> completions = new ArrayDeque<>();

    public synchronized void enqueue(
            FabricChatClefStopControlResultDelivery delivery,
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        completions.offer(new FabricChatClefStopControlResultSendCompletion(delivery, outcome));
    }

    public synchronized FabricChatClefStopControlResultSendCompletion poll() {
        return completions.poll();
    }

    public synchronized void clear() {
        completions.clear();
    }
}
