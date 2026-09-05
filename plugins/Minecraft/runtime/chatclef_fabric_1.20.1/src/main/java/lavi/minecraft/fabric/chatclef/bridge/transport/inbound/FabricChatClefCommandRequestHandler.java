package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

//20260905_kpopmodder: Preserve command_request handling as a thin admission-and-delivery sequence.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefResultEnvelopeSender;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission.FabricChatClefCommandRequestAdmission;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.admission.FabricChatClefCommandRequestAdmissionDecision;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.FabricChatClefCommandContextFactory;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.FabricChatClefCommandRequestDecoder;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.FabricChatClefCommandRequestDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.FabricChatClefCommandRequestEnqueuer;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.command.FabricChatClefCommandRequestRejectionSender;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefCommandRequestHandler {
    private final FabricChatClefCommandRequestAdmission admission;
    private final FabricChatClefCommandRequestDecoder decoder;
    private final FabricChatClefCommandContextFactory contextFactory;
    private final FabricChatClefCommandRequestEnqueuer enqueuer;
    private final FabricChatClefCommandRequestRejectionSender rejectionSender;
    private final FabricChatClefCommandRequestDiagnostics diagnostics;

    public FabricChatClefCommandRequestHandler(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            FabricChatClefResultEnvelopeSender resultEnvelopeSender,
            FabricChatClefSessionGuard sessionGuard
    ) {
        this.admission = new FabricChatClefCommandRequestAdmission(sessionGuard);
        this.decoder = new FabricChatClefCommandRequestDecoder(json);
        this.contextFactory = new FabricChatClefCommandContextFactory();
        this.enqueuer = new FabricChatClefCommandRequestEnqueuer(commandQueue);
        this.rejectionSender = new FabricChatClefCommandRequestRejectionSender(resultEnvelopeSender);
        this.diagnostics = new FabricChatClefCommandRequestDiagnostics(diagnostics);
    }

    public void handle(FabricChatClefBridgeEnvelope envelope, long generation) {
        FabricChatClefCommandRequestAdmissionDecision sessionDecision =
                admission.admitSession(envelope, generation);
        if (!sessionDecision.accepted()) {
            rejectionSender.send(envelope, generation, "", sessionDecision);
            return;
        }
        FabricChatClefCommandRequest request = decoder.decode(envelope.payload);
        FabricChatClefCommandRequestAdmissionDecision requestDecision =
                admission.admitPayload(request);
        if (!requestDecision.accepted()) {
            rejectionSender.send(envelope, generation, request.requestId, requestDecision);
            return;
        }
        if (!enqueuer.offer(contextFactory.create(
                request,
                envelope,
                sessionDecision.acceptedIdentity(),
                generation
        ))) {
            String activeRequest = enqueuer.activeRequestLabel();
            diagnostics.rejectedByQueue(request, activeRequest, generation);
            rejectionSender.sendQueueOccupied(envelope, generation, request.requestId, activeRequest);
            return;
        }
        diagnostics.queued(request, generation);
    }
}
