package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Preserve STOP result delivery API over immutable descriptor and send-state owners.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;

import java.util.function.BooleanSupplier;

public final class FabricChatClefStopControlResultDelivery {
    private final FabricChatClefStopControlResultDescriptor descriptor;
    private final FabricChatClefStopControlResultSendState sendState;

    public FabricChatClefStopControlResultDelivery(
            FabricChatClefStopControlBaseRequest request,
            FabricChatClefCommandResultPayload payload,
            boolean javaBarrierOwned,
            String invalidTargetFieldsMask,
            BooleanSupplier sentCommit
    ) {
        this.descriptor = new FabricChatClefStopControlResultDescriptor(
                request,
                payload,
                javaBarrierOwned,
                invalidTargetFieldsMask,
                sentCommit
        );
        this.sendState = new FabricChatClefStopControlResultSendState(
                new FabricChatClefStopControlResultRetryPolicy()
        );
    }

    public FabricChatClefStopControlBaseRequest request() {
        return descriptor.request();
    }

    public FabricChatClefCommandResultPayload payload() {
        return descriptor.payload();
    }

    public boolean javaBarrierOwned() {
        return descriptor.javaBarrierOwned();
    }

    public String invalidTargetFieldsMask() {
        return descriptor.invalidTargetFieldsMask();
    }

    public BooleanSupplier sentCommit() {
        return descriptor.sentCommit();
    }

    public synchronized boolean beginSend(long nowMs) {
        return sendState.begin(nowMs);
    }

    public synchronized boolean complete(FabricChatClefCommandResultSendOutcome outcome, long nowMs) {
        return sendState.complete(outcome, nowMs);
    }

    public synchronized boolean ready(long nowMs) {
        return sendState.ready(nowMs);
    }

    public synchronized boolean sent() {
        return sendState.sent();
    }

    public synchronized boolean quarantined() {
        return sendState.quarantined();
    }

    public synchronized int sendAttempts() {
        return sendState.attempts();
    }
}
