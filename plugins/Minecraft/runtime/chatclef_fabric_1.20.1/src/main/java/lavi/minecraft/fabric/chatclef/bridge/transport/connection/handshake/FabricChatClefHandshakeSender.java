package lavi.minecraft.fabric.chatclef.bridge.transport.connection.handshake;

//20260905_kpopmodder: Sequence focused handshake construction and submission owners.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeMessageFactory;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.net.http.WebSocket;

public final class FabricChatClefHandshakeSender {
    private final FabricChatClefHandshakeEnvelopeFactory envelopeFactory;
    private final FabricChatClefHandshakeEncoder encoder;
    private final FabricChatClefHandshakeSubmission submission;
    private final FabricChatClefHandshakeFailureHandler failureHandler;

    public FabricChatClefHandshakeSender(
            FabricChatClefBridgeState bridgeState,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            FabricChatClefBridgeMessageFactory messageFactory,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefWebSocketConnectionState connectionState
    ) {
        this.envelopeFactory = new FabricChatClefHandshakeEnvelopeFactory(
                bridgeState,
                messageFactory
        );
        this.encoder = new FabricChatClefHandshakeEncoder(json);
        this.submission = new FabricChatClefHandshakeSubmission(
                sessionGuard,
                connectionState
        );
        this.failureHandler = new FabricChatClefHandshakeFailureHandler(
                bridgeState,
                diagnostics
        );
    }

    public void send(WebSocket socket, Runnable reconnectAction) {
        try {
            var envelope = envelopeFactory.create();
            submission.submit(socket, envelope, encoder.encode(envelope));
        } catch (Exception error) {
            failureHandler.handle(error, reconnectAction);
        }
    }
}
