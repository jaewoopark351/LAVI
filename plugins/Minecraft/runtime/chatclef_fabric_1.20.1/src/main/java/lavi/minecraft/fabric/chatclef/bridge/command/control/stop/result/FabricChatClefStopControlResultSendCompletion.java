package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Correlate an async STOP send outcome to its exact immutable delivery.

import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;

public final class FabricChatClefStopControlResultSendCompletion {
    private final FabricChatClefStopControlResultDelivery delivery;
    private final FabricChatClefCommandResultSendOutcome outcome;

    public FabricChatClefStopControlResultSendCompletion(
            FabricChatClefStopControlResultDelivery delivery,
            FabricChatClefCommandResultSendOutcome outcome
    ) {
        this.delivery = delivery;
        this.outcome = outcome;
    }

    public FabricChatClefStopControlResultDelivery delivery() {
        return delivery;
    }

    public FabricChatClefCommandResultSendOutcome outcome() {
        return outcome;
    }
}
