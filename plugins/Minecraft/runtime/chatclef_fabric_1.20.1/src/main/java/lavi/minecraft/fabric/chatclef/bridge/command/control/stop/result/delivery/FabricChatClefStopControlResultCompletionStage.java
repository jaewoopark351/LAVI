package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Apply one STOP send completion and retire its live delivery when terminal.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDelivery;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDeliveryRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;

public final class FabricChatClefStopControlResultCompletionStage {
    private final FabricChatClefStopControlResultDeliveryRegistry registry;
    private final FabricChatClefStopControlSentBarrierCommit sentBarrierCommit;
    private final FabricChatClefStopControlResultDeliveryTransitionPublisher transitions;

    public FabricChatClefStopControlResultCompletionStage(
            FabricChatClefStopControlResultDeliveryRegistry registry,
            FabricChatClefStopControlSentBarrierCommit sentBarrierCommit,
            FabricChatClefStopControlResultDeliveryTransitionPublisher transitions
    ) {
        this.registry = registry;
        this.sentBarrierCommit = sentBarrierCommit;
        this.transitions = transitions;
    }

    public void complete(
            FabricChatClefStopControlResultDelivery delivery,
            FabricChatClefCommandResultSendOutcome outcome,
            long nowMs
    ) {
        boolean sent = delivery.complete(outcome, nowMs);
        if (sent) {
            try {
                transitions.sent(delivery, sentBarrierCommit.apply(delivery));
            } finally {
                registry.remove(delivery);
            }
            return;
        }
        if (delivery.quarantined()) {
            transitions.failed(delivery);
            registry.remove(delivery);
        }
    }
}
