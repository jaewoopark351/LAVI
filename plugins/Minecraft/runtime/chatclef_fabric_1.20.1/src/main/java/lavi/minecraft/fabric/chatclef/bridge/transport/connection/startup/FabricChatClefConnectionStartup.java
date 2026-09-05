package lavi.minecraft.fabric.chatclef.bridge.transport.connection.startup;

//20260905_kpopmodder: Own the one-time transition into the running connection state.

import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefConnectionReconnectCoordinator;

public final class FabricChatClefConnectionStartup {
    private final FabricChatClefBridgeConfig config;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefWebSocketConnectionState connectionState;
    private final FabricChatClefConnectionReconnectCoordinator reconnectCoordinator;

    public FabricChatClefConnectionStartup(
            FabricChatClefBridgeConfig config,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefWebSocketConnectionState connectionState,
            FabricChatClefConnectionReconnectCoordinator reconnectCoordinator
    ) {
        this.config = config;
        this.diagnostics = diagnostics;
        this.connectionState = connectionState;
        this.reconnectCoordinator = reconnectCoordinator;
    }

    public void start(Runnable connectAction) {
        if (!connectionState.beginRunning()) {
            return;
        }
        diagnostics.info("starting WebSocket client endpoint=" + config.endpoint());
        reconnectCoordinator.runNow(connectAction);
    }
}
