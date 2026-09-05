package lavi.minecraft.fabric.chatclef.bridge.transport.inbound.stop;

//20260905_kpopmodder: Coordinate strict STOP validation, dedupe reservation, and atomic queue admission.

import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseValidation;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequestValidator;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlValidationDecision;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.util.Optional;

public final class FabricChatClefStopControlInboundAdmissionCoordinator {
    private final FabricChatClefStopControlRequestValidator validator;
    private final FabricChatClefStopControlDedupeRegistry dedupeRegistry;
    private final FabricChatClefStopControlQueue stopQueue;
    private final FabricChatClefSessionGuard sessionGuard;
    private final FabricChatClefStopControlTransitionEmitter transitionEmitter;
    private final FabricChatClefStopControlPreQueueRejectionPublisher rejectionPublisher;
    private final FabricChatClefStopControlAdmissionDiagnosticProjection diagnosticProjection;

    public FabricChatClefStopControlInboundAdmissionCoordinator(
            FabricChatClefStopControlRequestValidator validator,
            FabricChatClefStopControlDedupeRegistry dedupeRegistry,
            FabricChatClefStopControlQueue stopQueue,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefStopControlTransitionEmitter transitionEmitter,
            FabricChatClefStopControlPreQueueRejectionPublisher rejectionPublisher
    ) {
        this(
                validator,
                dedupeRegistry,
                stopQueue,
                sessionGuard,
                transitionEmitter,
                rejectionPublisher,
                new FabricChatClefStopControlAdmissionDiagnosticProjection(stopQueue)
        );
    }

    public FabricChatClefStopControlInboundAdmissionCoordinator(
            FabricChatClefStopControlRequestValidator validator,
            FabricChatClefStopControlDedupeRegistry dedupeRegistry,
            FabricChatClefStopControlQueue stopQueue,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefStopControlTransitionEmitter transitionEmitter,
            FabricChatClefStopControlPreQueueRejectionPublisher rejectionPublisher,
            FabricChatClefStopControlAdmissionDiagnosticProjection diagnosticProjection
    ) {
        this.validator = validator;
        this.dedupeRegistry = dedupeRegistry;
        this.stopQueue = stopQueue;
        this.sessionGuard = sessionGuard;
        this.transitionEmitter = transitionEmitter;
        this.rejectionPublisher = rejectionPublisher;
        this.diagnosticProjection = diagnosticProjection;
    }

    public void handle(JsonNode envelope, long javaSocketGeneration) {
        Optional<FabricChatClefAcceptedSessionIdentity> acceptedIdentity = sessionGuard.acceptedIdentity();
        FabricChatClefStopControlBaseValidation baseValidation =
                validator.validateBase(envelope, javaSocketGeneration);
        if (!baseValidation.valid()) {
            FabricChatClefAcceptedSessionIdentity accepted = acceptedIdentity.orElse(null);
            transitionEmitter.noWire(
                    null,
                    javaSocketGeneration,
                    accepted == null ? null : accepted.sessionId(),
                    accepted == null ? null : accepted.serverConnectionGeneration(),
                    "none",
                    baseValidation.diagnosticDisposition(),
                    diagnosticProjection.barrierState(),
                    diagnosticProjection.ordinaryGateState(),
                    true
            );
            return;
        }
        FabricChatClefStopControlBaseRequest base = baseValidation.request();
        FabricChatClefAcceptedSessionIdentity accepted = acceptedIdentity.orElse(null);
        base = base.withAcceptedValidationIdentity(
                accepted == null ? null : accepted.sessionId(),
                accepted == null ? null : accepted.serverConnectionGeneration()
        );
        acceptedIdentity.ifPresent(identity ->
                dedupeRegistry.selectAcceptedGeneration(identity.serverConnectionGeneration()));
        FabricChatClefStopControlDedupeDecision dedupeDecision = dedupeRegistry.reserve(
                base.identity(),
                base.fingerprint()
        );
        if (dedupeDecision != FabricChatClefStopControlDedupeDecision.RESERVED) {
            transitionEmitter.noWire(
                    base,
                    javaSocketGeneration,
                    base.acceptedSessionIdAtValidation(),
                    base.acceptedServerConnectionGenerationAtValidation(),
                    "none",
                    diagnosticProjection.disposition(dedupeDecision),
                    diagnosticProjection.barrierState(),
                    diagnosticProjection.ordinaryGateState(),
                    diagnosticProjection.quarantineActive()
            );
            return;
        }
        FabricChatClefStopControlValidationDecision decision = validator.validateProfile(
                base,
                envelope,
                acceptedIdentity,
                System.currentTimeMillis()
        );
        if (!decision.accepted()) {
            rejectionPublisher.publish(base, decision, null);
            return;
        }
        FabricChatClefStopControlRequest request = decision.request();
        Optional<FabricChatClefStopControlContext> admitted = stopQueue.offer(request);
        if (admitted.isEmpty()) {
            rejectionPublisher.publish(
                    base,
                    FabricChatClefStopControlValidationDecision.rejected(
                            "stop_control_in_flight",
                            request.targetScope(),
                            "none"
                    ),
                    request
            );
            return;
        }
        transitionEmitter.contextBoundary(
                admitted.get(),
                "queue_admitted",
                "closed",
                "blocked",
                false,
                "none"
        );
    }

}
