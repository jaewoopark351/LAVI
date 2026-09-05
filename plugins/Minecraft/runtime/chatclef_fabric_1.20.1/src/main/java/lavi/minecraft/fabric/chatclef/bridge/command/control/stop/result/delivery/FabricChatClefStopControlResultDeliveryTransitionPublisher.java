package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Publish canonical wire-delivery transitions for STOP results.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDelivery;

public final class FabricChatClefStopControlResultDeliveryTransitionPublisher {
    private final FabricChatClefStopControlTransitionEmitter transitionEmitter;
    private final FabricChatClefStopControlResultStatusInspector statusInspector;

    public FabricChatClefStopControlResultDeliveryTransitionPublisher(
            FabricChatClefStopControlTransitionEmitter transitionEmitter,
            FabricChatClefStopControlResultStatusInspector statusInspector
    ) {
        this.transitionEmitter = transitionEmitter;
        this.statusInspector = statusInspector;
    }

    public void pending(FabricChatClefStopControlResultDelivery delivery) {
        transitionEmitter.wireResult(
                delivery.request(),
                delivery.payload(),
                delivery.invalidTargetFieldsMask(),
                "pending",
                delivery.javaBarrierOwned() ? "closed" : "none",
                delivery.javaBarrierOwned() ? "blocked" : "unchanged",
                delivery.javaBarrierOwned() && statusInspector.isUnknown(delivery.payload())
        );
    }

    public void sent(FabricChatClefStopControlResultDelivery delivery, boolean releaseApplied) {
        transitionEmitter.wireResult(
                delivery.request(),
                delivery.payload(),
                delivery.invalidTargetFieldsMask(),
                "sent",
                sentBarrierState(delivery, releaseApplied),
                sentOrdinaryGateState(delivery, releaseApplied),
                statusInspector.isUnknown(delivery.payload())
                        || (delivery.javaBarrierOwned() && !releaseApplied)
        );
    }

    public void failed(FabricChatClefStopControlResultDelivery delivery) {
        transitionEmitter.wireResult(
                delivery.request(),
                delivery.payload(),
                delivery.invalidTargetFieldsMask(),
                "failed",
                delivery.javaBarrierOwned() ? "quarantined" : "none",
                delivery.javaBarrierOwned() ? "blocked" : "unchanged",
                true
        );
    }

    private String sentBarrierState(
            FabricChatClefStopControlResultDelivery delivery,
            boolean releaseApplied
    ) {
        if (!delivery.javaBarrierOwned()) {
            return "none";
        }
        return releaseApplied ? "released" : "quarantined";
    }

    private String sentOrdinaryGateState(
            FabricChatClefStopControlResultDelivery delivery,
            boolean releaseApplied
    ) {
        if (!delivery.javaBarrierOwned()) {
            return "unchanged";
        }
        return releaseApplied ? "open" : "blocked";
    }
}
