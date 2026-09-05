package lavi.minecraft.fabric.chatclef.bridge.transport.connection.handshake;

//20260905_kpopmodder: Own handshake-session admission and WebSocket submission.

import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.net.http.WebSocket;

public final class FabricChatClefHandshakeSubmission {
    private final FabricChatClefSessionGuard sessionGuard;
    private final FabricChatClefWebSocketConnectionState connectionState;

    public FabricChatClefHandshakeSubmission(
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefWebSocketConnectionState connectionState
    ) {
        this.sessionGuard = sessionGuard;
        this.connectionState = connectionState;
    }

    public void submit(
            WebSocket socket,
            FabricChatClefBridgeEnvelope envelope,
            String encodedEnvelope
    ) {
        sessionGuard.beginHandshake(
                envelope.messageId,
                connectionState.activeConnectionGeneration()
        );
        socket.sendText(encodedEnvelope, true);
    }
}
