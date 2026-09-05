package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Expose the STOP result outbox as a thin send-lifecycle facade.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.function.BooleanSupplier;

public final class FabricChatClefStopControlResultOutbox {
    private final FabricChatClefStopControlResultDeliveryRegistry registry;
    private final FabricChatClefStopControlResultSendCoordinator sendCoordinator;

    public FabricChatClefStopControlResultOutbox(
            FabricChatClefStopControlResultSender resultSender,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.registry = new FabricChatClefStopControlResultDeliveryRegistry();
        this.sendCoordinator = new FabricChatClefStopControlResultSendCoordinator(
                resultSender,
                new FabricChatClefStopControlTransitionEmitter(diagnostics),
                registry
        );
    }

    public FabricChatClefStopControlResultDelivery commitAndSend(
            FabricChatClefStopControlBaseRequest request,
            FabricChatClefCommandResultPayload payload,
            boolean javaBarrierOwned,
            String invalidTargetFieldsMask,
            BooleanSupplier sentCommit
    ) {
        return sendCoordinator.commitAndSend(
                request,
                payload,
                javaBarrierOwned,
                invalidTargetFieldsMask,
                sentCommit
        );
    }

    public void onEndClientTick(long nowMs) {
        sendCoordinator.onEndClientTick(nowMs);
    }

    public int pendingDeliveryCount() {
        return registry.deliveryCount();
    }

    public void resetForShutdown() {
        registry.reset();
        sendCoordinator.resetCompletions();
    }
}
