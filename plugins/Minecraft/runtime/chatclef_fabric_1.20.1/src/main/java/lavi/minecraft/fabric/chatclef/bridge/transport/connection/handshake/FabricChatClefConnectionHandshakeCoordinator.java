package lavi.minecraft.fabric.chatclef.bridge.transport.connection.handshake;

//20260905_kpopmodder: Coordinate connection-open state with one handshake send.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefConnectionTextReceiver;

import java.net.http.WebSocket;

public final class FabricChatClefConnectionHandshakeCoordinator {
    private final FabricChatClefBridgeState bridgeState;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefWebSocketConnectionState connectionState;
    private final FabricChatClefConnectionTextReceiver textReceiver;
    private final FabricChatClefHandshakeSender handshakeSender;

    public FabricChatClefConnectionHandshakeCoordinator(
            FabricChatClefBridgeState bridgeState,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefWebSocketConnectionState connectionState,
            FabricChatClefConnectionTextReceiver textReceiver,
            FabricChatClefHandshakeSender handshakeSender
    ) {
        this.bridgeState = bridgeState;
        this.diagnostics = diagnostics;
        this.connectionState = connectionState;
        this.textReceiver = textReceiver;
        this.handshakeSender = handshakeSender;
    }

    public void onOpen(WebSocket webSocket, Runnable reconnectAction) {
        long generation = connectionState.acceptOpen(webSocket);
        textReceiver.resetFrames();
        bridgeState.markConnected();
        diagnostics.info("connected generation=" + generation + "; sending handshake");
        handshakeSender.send(webSocket, reconnectAction);
        webSocket.request(1);
    }
}
