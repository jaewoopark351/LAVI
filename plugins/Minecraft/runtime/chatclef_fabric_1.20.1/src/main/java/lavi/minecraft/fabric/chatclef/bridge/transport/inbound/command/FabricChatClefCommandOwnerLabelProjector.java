package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command;

//20260905_kpopmodder: Project only the current ordinary command owner into a diagnostic label.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;

public final class FabricChatClefCommandOwnerLabelProjector {
    private final FabricChatClefCommandQueue commandQueue;

    public FabricChatClefCommandOwnerLabelProjector(FabricChatClefCommandQueue commandQueue) {
        this.commandQueue = commandQueue;
    }

    public String activeRequestLabel() {
        return commandQueue.activeRequestId().orElse("<pending>");
    }
}
