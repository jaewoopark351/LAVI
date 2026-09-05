package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command;

//20260905_kpopmodder: Send command_request rejection envelopes without owning admission policy.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefResultEnvelopeSender;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission.FabricChatClefCommandRequestAdmissionDecision;

public final class FabricChatClefCommandRequestRejectionSender {
    private final FabricChatClefResultEnvelopeSender resultEnvelopeSender;

    public FabricChatClefCommandRequestRejectionSender(
            FabricChatClefResultEnvelopeSender resultEnvelopeSender
    ) {
        this.resultEnvelopeSender = resultEnvelopeSender;
    }

    public void send(
            FabricChatClefBridgeEnvelope envelope,
            long generation,
            String requestId,
            FabricChatClefCommandRequestAdmissionDecision decision
    ) {
        send(
                envelope,
                generation,
                requestId,
                decision.rejectionCode(),
                decision.rejectionMessage()
        );
    }

    public void sendQueueOccupied(
            FabricChatClefBridgeEnvelope envelope,
            long generation,
            String requestId,
            String activeRequest
    ) {
        send(
                envelope,
                generation,
                requestId,
                "invalid_request",
                "Fabric ChatClef command already pending or active: " + activeRequest
        );
    }

    private void send(
            FabricChatClefBridgeEnvelope envelope,
            long generation,
            String requestId,
            String code,
            String message
    ) {
        resultEnvelopeSender.sendCommandResult(
                envelope.messageId,
                envelope.sessionId,
                generation,
                FabricChatClefCommandResult.rejected(requestId, code, message)
        );
    }
}
