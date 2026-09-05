package lavi.minecraft.fabric.chatclef.bridge.transport.connection.startup;

//20260905_kpopmodder: Own each guarded asynchronous WebSocket connection attempt.

import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefConnectionReconnectCoordinator;

import java.net.http.HttpClient;
import java.net.http.WebSocket;

public final class FabricChatClefConnectionAttempt {
    private final FabricChatClefBridgeConfig config;
    private final FabricChatClefBridgeState bridgeState;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefWebSocketConnectionState connectionState;
    private final FabricChatClefConnectionReconnectCoordinator reconnectCoordinator;
    private final HttpClient httpClient;

    public FabricChatClefConnectionAttempt(
            FabricChatClefBridgeConfig config,
            FabricChatClefBridgeState bridgeState,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefWebSocketConnectionState connectionState,
            FabricChatClefConnectionReconnectCoordinator reconnectCoordinator
    ) {
        this.config = config;
        this.bridgeState = bridgeState;
        this.diagnostics = diagnostics;
        this.connectionState = connectionState;
        this.reconnectCoordinator = reconnectCoordinator;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void connectIfRunning(
            WebSocket.Listener listener,
            Runnable reconnectAction
    ) {
        if (!connectionState.running() || !connectionState.beginConnecting()) {
            return;
        }
        bridgeState.markConnecting();
        httpClient.newWebSocketBuilder()
                .buildAsync(config.endpoint(), listener)
                .whenComplete((socket, error) -> {
                    if (error == null) {
                        return;
                    }
                    connectionState.endConnecting();
                    String message = error.getClass().getSimpleName() + ": " + error.getMessage();
                    bridgeState.markFailed(message);
                    warnConnectFailure(message);
                    reconnectCoordinator.schedule(reconnectAction);
                });
    }

    private void warnConnectFailure(String message) {
        if (connectionState.shouldLogConnectFailure(message)) {
            diagnostics.warn("connect failed " + message);
        }
    }
}
