package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Apply the exact STOP barrier release callback after confirmed wire delivery.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDelivery;

public final class FabricChatClefStopControlSentBarrierCommit {
    private final FabricChatClefStopControlResultStatusInspector statusInspector;

    public FabricChatClefStopControlSentBarrierCommit(
            FabricChatClefStopControlResultStatusInspector statusInspector
    ) {
        this.statusInspector = statusInspector;
    }

    public boolean apply(FabricChatClefStopControlResultDelivery delivery) {
        if (statusInspector.isUnknown(delivery.payload())) {
            return false;
        }
        try {
            return delivery.sentCommit().getAsBoolean();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
