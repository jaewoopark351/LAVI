package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command;

//20260905_kpopmodder: Preserve request-enqueuer API over queue admission and owner-label projection.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;

public final class FabricChatClefCommandRequestEnqueuer {
    private final FabricChatClefCommandRequestQueueAdmission queueAdmission;
    private final FabricChatClefCommandOwnerLabelProjector ownerLabelProjector;

    public FabricChatClefCommandRequestEnqueuer(FabricChatClefCommandQueue commandQueue) {
        this.queueAdmission = new FabricChatClefCommandRequestQueueAdmission(commandQueue);
        this.ownerLabelProjector = new FabricChatClefCommandOwnerLabelProjector(commandQueue);
    }

    public boolean offer(FabricChatClefCommandContext context) {
        return queueAdmission.offer(context);
    }

    public String activeRequestLabel() {
        return ownerLabelProjector.activeRequestLabel();
    }
}
