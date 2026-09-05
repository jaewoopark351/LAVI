package lavi.minecraft.fabric.chatclef.bridge.transport.session;

//20260905_kpopmodder: Own mutable handshake expectations and the accepted session identity.

import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;

import java.util.Optional;

public final class FabricChatClefMutableSessionState {
    private final FabricChatClefBridgeState bridgeState;
    private FabricChatClefAcceptedSessionIdentity acceptedIdentity;
    private String expectedHandshakeMessageId = "";
    private long expectedJavaSocketGeneration;

    public FabricChatClefMutableSessionState(FabricChatClefBridgeState bridgeState) {
        this.bridgeState = bridgeState;
    }

    public synchronized void beginHandshake(String messageId, long javaSocketGeneration) {
        expectedHandshakeMessageId = messageId == null ? "" : messageId;
        expectedJavaSocketGeneration = javaSocketGeneration;
        acceptedIdentity = null;
    }

    public synchronized FabricChatClefAcceptedSessionIdentity acceptedIdentityOrNull() {
        return acceptedIdentity;
    }

    public synchronized Optional<FabricChatClefAcceptedSessionIdentity> acceptedIdentity() {
        return Optional.ofNullable(acceptedIdentity);
    }

    public synchronized String expectedHandshakeMessageId() {
        return expectedHandshakeMessageId;
    }

    public synchronized long expectedJavaSocketGeneration() {
        return expectedJavaSocketGeneration;
    }

    public synchronized void accept(FabricChatClefAcceptedSessionIdentity identity) {
        acceptedIdentity = identity;
        expectedHandshakeMessageId = "";
        expectedJavaSocketGeneration = 0L;
        bridgeState.markHandshakeAccepted(identity.sessionId());
    }

    public synchronized boolean handshakeAccepted() {
        return bridgeState.handshakeAccepted() && acceptedIdentity != null;
    }

    public synchronized boolean isActiveSession(String sessionId) {
        return sessionId != null && activeSessionId().equals(sessionId);
    }

    public synchronized String activeSessionId() {
        return acceptedIdentity == null ? "" : acceptedIdentity.sessionId();
    }

    public synchronized boolean matches(
            String sessionId,
            long serverConnectionGeneration,
            long javaSocketGeneration
    ) {
        return handshakeAccepted()
                && acceptedIdentity.sessionId().equals(sessionId)
                && acceptedIdentity.serverConnectionGeneration() == serverConnectionGeneration
                && acceptedIdentity.javaSocketGeneration() == javaSocketGeneration;
    }

    public synchronized void markConnectionDetached(long javaSocketGeneration) {
        if (acceptedIdentity != null
                && acceptedIdentity.javaSocketGeneration() == javaSocketGeneration) {
            acceptedIdentity = null;
        }
        if (expectedJavaSocketGeneration == javaSocketGeneration) {
            expectedHandshakeMessageId = "";
            expectedJavaSocketGeneration = 0L;
        }
    }
}
