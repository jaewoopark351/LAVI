package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.delivery;

//20260905_kpopmodder: Register one immutable STOP result delivery before its first send attempt.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDelivery;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultDeliveryRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.function.BooleanSupplier;

public final class FabricChatClefStopControlResultDeliveryCommitStage {
    private final FabricChatClefStopControlResultDeliveryRegistry registry;
    private final FabricChatClefStopControlResultDeliveryTransitionPublisher transitions;

    public FabricChatClefStopControlResultDeliveryCommitStage(
            FabricChatClefStopControlResultDeliveryRegistry registry,
            FabricChatClefStopControlResultDeliveryTransitionPublisher transitions
    ) {
        this.registry = registry;
        this.transitions = transitions;
    }

    public FabricChatClefStopControlResultDelivery commit(
            FabricChatClefStopControlBaseRequest request,
            FabricChatClefCommandResultPayload payload,
            boolean javaBarrierOwned,
            String invalidTargetFieldsMask,
            BooleanSupplier sentCommit
    ) {
        FabricChatClefStopControlResultDelivery delivery = new FabricChatClefStopControlResultDelivery(
                request,
                payload,
                javaBarrierOwned,
                invalidTargetFieldsMask,
                sentCommit
        );
        registry.add(delivery);
        transitions.pending(delivery);
        return delivery;
    }
}
