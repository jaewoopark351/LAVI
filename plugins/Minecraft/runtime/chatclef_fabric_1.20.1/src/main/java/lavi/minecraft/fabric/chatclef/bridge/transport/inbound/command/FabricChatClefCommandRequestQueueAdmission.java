package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command;

//20260905_kpopmodder: Submit only an admitted ordinary command context to its queue.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;

public final class FabricChatClefCommandRequestQueueAdmission {
    private final FabricChatClefCommandQueue commandQueue;

    public FabricChatClefCommandRequestQueueAdmission(FabricChatClefCommandQueue commandQueue) {
        this.commandQueue = commandQueue;
    }

    public boolean offer(FabricChatClefCommandContext context) {
        return commandQueue.offer(context);
    }
}
