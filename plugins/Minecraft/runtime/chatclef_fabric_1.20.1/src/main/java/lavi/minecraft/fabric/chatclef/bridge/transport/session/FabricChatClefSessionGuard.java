package lavi.minecraft.fabric.chatclef.bridge.transport.session;

//20260905_kpopmodder: Preserve the session guard API as a synchronized responsibility facade.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.validation.FabricChatClefHandshakeAckValidator;

import java.util.Optional;

public final class FabricChatClefSessionGuard {
    private final FabricChatClefMutableSessionState sessionState;
    private final FabricChatClefHandshakeAckCoordinator handshakeCoordinator;

    public FabricChatClefSessionGuard(
            FabricChatClefBridgeState state,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.sessionState = new FabricChatClefMutableSessionState(state);
        this.handshakeCoordinator = new FabricChatClefHandshakeAckCoordinator(
                sessionState,
                new FabricChatClefHandshakeAckValidator(),
                new FabricChatClefSessionDiagnostics(diagnostics)
        );
    }

    public synchronized void beginHandshake(String messageId, long javaSocketGeneration) {
        handshakeCoordinator.beginHandshake(messageId, javaSocketGeneration);
    }

    public synchronized boolean acceptHandshake(FabricChatClefBridgeEnvelope envelope, long javaSocketGeneration) {
        return handshakeCoordinator.accept(envelope, javaSocketGeneration);
    }

    public synchronized void acceptHandshake(FabricChatClefBridgeEnvelope envelope) {
        handshakeCoordinator.accept(envelope, sessionState.expectedJavaSocketGeneration());
    }

    public synchronized boolean handshakeAccepted() {
        return sessionState.handshakeAccepted();
    }

    public synchronized boolean isActiveSession(String sessionId) {
        return sessionState.isActiveSession(sessionId);
    }

    public synchronized String activeSessionId() {
        return sessionState.activeSessionId();
    }

    public synchronized Optional<FabricChatClefAcceptedSessionIdentity> acceptedIdentity() {
        return sessionState.acceptedIdentity();
    }

    public synchronized boolean matches(
            String sessionId,
            long serverConnectionGeneration,
            long javaSocketGeneration
    ) {
        return sessionState.matches(
                sessionId,
                serverConnectionGeneration,
                javaSocketGeneration
        );
    }

    public synchronized void markConnectionDetached(long javaSocketGeneration) {
        sessionState.markConnectionDetached(javaSocketGeneration);
    }
}
