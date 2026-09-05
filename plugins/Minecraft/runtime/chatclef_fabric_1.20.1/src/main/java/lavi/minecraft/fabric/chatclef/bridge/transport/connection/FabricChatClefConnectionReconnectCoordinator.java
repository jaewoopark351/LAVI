package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Own WebSocket reconnect scheduling and scheduler shutdown.

import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefReconnectScheduler;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;

public final class FabricChatClefConnectionReconnectCoordinator {
    private final FabricChatClefBridgeConfig config;
    private final FabricChatClefReconnectScheduler reconnectScheduler;
    private final FabricChatClefWebSocketConnectionState connectionState;

    public FabricChatClefConnectionReconnectCoordinator(
            FabricChatClefBridgeConfig config,
            FabricChatClefReconnectScheduler reconnectScheduler,
            FabricChatClefWebSocketConnectionState connectionState
    ) {
        this.config = config;
        this.reconnectScheduler = reconnectScheduler;
        this.connectionState = connectionState;
    }

    public void runNow(Runnable connectAction) {
        reconnectScheduler.runNow(connectAction);
    }

    public void schedule(Runnable connectAction) {
        if (!connectionState.running()) {
            return;
        }
        reconnectScheduler.runLater(connectAction, config.reconnectDelay());
    }

    public void stop() {
        reconnectScheduler.stop();
    }
}
