package lavi.minecraft.fabric.chatclef.bridge.transport.session;

//20260905_kpopmodder: Coordinate exactly one validated ACK against the current handshake expectation.

import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.validation.FabricChatClefHandshakeAckValidation;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.validation.FabricChatClefHandshakeAckValidator;

public final class FabricChatClefHandshakeAckCoordinator {
    private final FabricChatClefMutableSessionState sessionState;
    private final FabricChatClefHandshakeAckValidator validator;
    private final FabricChatClefSessionDiagnostics diagnostics;

    public FabricChatClefHandshakeAckCoordinator(
            FabricChatClefMutableSessionState sessionState,
            FabricChatClefHandshakeAckValidator validator,
            FabricChatClefSessionDiagnostics diagnostics
    ) {
        this.sessionState = sessionState;
        this.validator = validator;
        this.diagnostics = diagnostics;
    }

    public synchronized void beginHandshake(String messageId, long javaSocketGeneration) {
        sessionState.beginHandshake(messageId, javaSocketGeneration);
    }

    public synchronized boolean accept(
            FabricChatClefBridgeEnvelope envelope,
            long javaSocketGeneration
    ) {
        if (sessionState.acceptedIdentityOrNull() != null) {
            diagnostics.duplicateAcceptedAck();
            return false;
        }
        FabricChatClefHandshakeAckValidation validation = validator.validate(
                envelope,
                javaSocketGeneration,
                sessionState.expectedHandshakeMessageId(),
                sessionState.expectedJavaSocketGeneration()
        );
        if (!validation.accepted()) {
            diagnostics.rejectedAck(validation.rejectionCode(), javaSocketGeneration);
            return false;
        }
        sessionState.accept(validation.identity());
        diagnostics.accepted(validation.identity());
        return true;
    }
}
