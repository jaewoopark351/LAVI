package lavi.minecraft.fabric.chatclef.bridge.state;

import java.util.Optional;

//20260801_kpopmodder: Store Fabric bridge connection state without touching ChatClef engine state.
public final class FabricChatClefBridgeState {
    private volatile FabricChatClefBridgeLifecycleState lifecycleState = FabricChatClefBridgeLifecycleState.STOPPED;
    private volatile String sessionId;
    private volatile String lastError;
    private volatile boolean handshakeAccepted;

    public FabricChatClefBridgeLifecycleState lifecycleState() {
        return lifecycleState;
    }

    public Optional<String> sessionId() {
        return Optional.ofNullable(sessionId);
    }

    public Optional<String> lastError() {
        return Optional.ofNullable(lastError);
    }

    public boolean handshakeAccepted() {
        return handshakeAccepted;
    }

    public void markConnecting() {
        lifecycleState = FabricChatClefBridgeLifecycleState.CONNECTING;
        handshakeAccepted = false;
        lastError = null;
    }

    public void markConnected() {
        lifecycleState = FabricChatClefBridgeLifecycleState.CONNECTED;
        lastError = null;
    }

    public void markHandshakeAccepted(String newSessionId) {
        sessionId = blankToNull(newSessionId);
        handshakeAccepted = true;
        markConnected();
    }

    public void markDisconnected(String reason) {
        lifecycleState = FabricChatClefBridgeLifecycleState.DISCONNECTED;
        handshakeAccepted = false;
        lastError = blankToNull(reason);
    }

    public void markFailed(String error) {
        lifecycleState = FabricChatClefBridgeLifecycleState.FAILED;
        handshakeAccepted = false;
        lastError = blankToNull(error);
    }

    public void markStopped() {
        lifecycleState = FabricChatClefBridgeLifecycleState.STOPPED;
        handshakeAccepted = false;
        lastError = null;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
