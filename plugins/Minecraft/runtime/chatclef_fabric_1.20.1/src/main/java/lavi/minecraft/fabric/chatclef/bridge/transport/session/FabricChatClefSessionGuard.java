package lavi.minecraft.fabric.chatclef.bridge.transport.session;

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;

import java.util.Map;

//20260804_kpopmodder: Keep Fabric ChatClef handshake and session checks out of the WebSocket listener.
public final class FabricChatClefSessionGuard {
    private final FabricChatClefBridgeState state;
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefSessionGuard(
            FabricChatClefBridgeState state,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.state = state;
        this.diagnostics = diagnostics;
    }

    public void acceptHandshake(FabricChatClefBridgeEnvelope envelope) {
        Object accepted = envelope.payload.get("accepted");
        if (!Boolean.TRUE.equals(accepted)) {
            diagnostics.warn("handshake rejected payload=" + envelope.payload);
            return;
        }
        String sessionId = stringPayload(envelope.payload, "session_id");
        if (sessionId == null) {
            sessionId = envelope.sessionId;
        }
        state.markHandshakeAccepted(sessionId);
        diagnostics.info("handshake accepted session=" + state.sessionId().orElse("<none>"));
    }

    public boolean handshakeAccepted() {
        return state.handshakeAccepted();
    }

    public boolean isActiveSession(String sessionId) {
        return sessionId != null && activeSessionId().equals(sessionId);
    }

    public String activeSessionId() {
        return state.sessionId().orElse("");
    }

    private String stringPayload(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
