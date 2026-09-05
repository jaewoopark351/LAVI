package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

//20260905_kpopmodder: Preserve STOP inbound demux as a thin candidate/admission facade.

import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlCandidateDetector;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequestValidator;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.stop.FabricChatClefStopControlInboundAdmissionCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.stop.FabricChatClefStopControlAdmissionDiagnosticProjection;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.stop.FabricChatClefStopControlPreQueueRejectionPublisher;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

public final class FabricChatClefStopControlInboundHandler {
    private final FabricChatClefStopControlCandidateDetector candidateDetector;
    private final FabricChatClefStopControlInboundAdmissionCoordinator admissionCoordinator;

    public FabricChatClefStopControlInboundHandler(
            FabricChatClefStopControlDedupeRegistry dedupeRegistry,
            FabricChatClefStopControlQueue stopQueue,
            FabricChatClefStopControlResultOutbox resultOutbox,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.candidateDetector = new FabricChatClefStopControlCandidateDetector();
        FabricChatClefStopControlTransitionEmitter transitionEmitter =
                new FabricChatClefStopControlTransitionEmitter(diagnostics);
        FabricChatClefStopControlRequestValidator validator =
                new FabricChatClefStopControlRequestValidator();
        FabricChatClefStopControlResultFactory resultFactory =
                new FabricChatClefStopControlResultFactory();
        FabricChatClefStopControlAdmissionDiagnosticProjection diagnosticProjection =
                new FabricChatClefStopControlAdmissionDiagnosticProjection(stopQueue);
        FabricChatClefStopControlPreQueueRejectionPublisher rejectionPublisher =
                new FabricChatClefStopControlPreQueueRejectionPublisher(
                        dedupeRegistry,
                        stopQueue,
                        resultFactory,
                        resultOutbox,
                        transitionEmitter,
                        diagnosticProjection
                );
        this.admissionCoordinator = new FabricChatClefStopControlInboundAdmissionCoordinator(
                validator,
                dedupeRegistry,
                stopQueue,
                sessionGuard,
                transitionEmitter,
                rejectionPublisher,
                diagnosticProjection
        );
    }

    public boolean isCandidate(JsonNode envelope) {
        return candidateDetector.isCandidate(envelope);
    }

    public void handle(JsonNode envelope, long javaSocketGeneration) {
        admissionCoordinator.handle(envelope, javaSocketGeneration);
    }
}
