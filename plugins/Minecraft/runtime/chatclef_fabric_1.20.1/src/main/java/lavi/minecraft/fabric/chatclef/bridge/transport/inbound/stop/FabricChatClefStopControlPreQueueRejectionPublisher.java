package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.stop;

//20260905_kpopmodder: Commit pre-queue STOP rejections without exposing queue-admission logic.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlValidationDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

public final class FabricChatClefStopControlPreQueueRejectionPublisher {
    private final FabricChatClefStopControlDedupeRegistry dedupeRegistry;
    private final FabricChatClefStopControlResultFactory resultFactory;
    private final FabricChatClefStopControlResultOutbox resultOutbox;
    private final FabricChatClefStopControlTransitionEmitter transitionEmitter;
    private final FabricChatClefStopControlAdmissionDiagnosticProjection diagnosticProjection;

    public FabricChatClefStopControlPreQueueRejectionPublisher(
            FabricChatClefStopControlDedupeRegistry dedupeRegistry,
            FabricChatClefStopControlQueue stopQueue,
            FabricChatClefStopControlResultFactory resultFactory,
            FabricChatClefStopControlResultOutbox resultOutbox,
            FabricChatClefStopControlTransitionEmitter transitionEmitter
    ) {
        this(
                dedupeRegistry,
                stopQueue,
                resultFactory,
                resultOutbox,
                transitionEmitter,
                new FabricChatClefStopControlAdmissionDiagnosticProjection(stopQueue)
        );
    }

    public FabricChatClefStopControlPreQueueRejectionPublisher(
            FabricChatClefStopControlDedupeRegistry dedupeRegistry,
            FabricChatClefStopControlQueue stopQueue,
            FabricChatClefStopControlResultFactory resultFactory,
            FabricChatClefStopControlResultOutbox resultOutbox,
            FabricChatClefStopControlTransitionEmitter transitionEmitter,
            FabricChatClefStopControlAdmissionDiagnosticProjection diagnosticProjection
    ) {
        this.dedupeRegistry = dedupeRegistry;
        this.resultFactory = resultFactory;
        this.resultOutbox = resultOutbox;
        this.transitionEmitter = transitionEmitter;
        this.diagnosticProjection = diagnosticProjection;
    }

    public void publish(
            FabricChatClefStopControlBaseRequest base,
            FabricChatClefStopControlValidationDecision decision,
            FabricChatClefStopControlRequest request
    ) {
        try {
            FabricChatClefCommandResultPayload payload = resultFactory.rejected(
                    base,
                    decision.rejectionReason(),
                    decision.resultTargetScope(),
                    request,
                    null
            );
            dedupeRegistry.markTerminal(base.identity());
            resultOutbox.commitAndSend(
                    base,
                    payload,
                    false,
                    decision.invalidTargetFieldsMask(),
                    () -> true
            );
        } catch (RuntimeException error) {
            dedupeRegistry.markTerminal(base.identity());
            transitionEmitter.noWire(
                    base,
                    base.javaSocketGeneration(),
                    base.acceptedSessionIdAtValidation(),
                    base.acceptedServerConnectionGenerationAtValidation(),
                    decision.invalidTargetFieldsMask(),
                    "result_profile_construction_failed",
                    diagnosticProjection.barrierState(),
                    diagnosticProjection.ordinaryGateState(),
                    true
            );
        }
    }

}
