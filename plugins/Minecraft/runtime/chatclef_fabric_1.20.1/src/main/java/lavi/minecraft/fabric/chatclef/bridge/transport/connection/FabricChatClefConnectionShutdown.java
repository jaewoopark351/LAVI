package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Own WebSocket shutdown and operation-owned state resets.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.net.http.WebSocket;

public final class FabricChatClefConnectionShutdown {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefBridgeState bridgeState;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefConnectionReconnectCoordinator reconnectCoordinator;
    private final FabricChatClefSessionGuard sessionGuard;
    private final FabricChatClefStopControlQueue stopControlQueue;
    private final FabricChatClefStopControlDedupeRegistry stopControlDedupeRegistry;
    private final FabricChatClefStopControlResultOutbox stopControlResultOutbox;
    private final FabricChatClefWebSocketConnectionState connectionState;

    public FabricChatClefConnectionShutdown(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefBridgeState bridgeState,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefConnectionReconnectCoordinator reconnectCoordinator,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefStopControlQueue stopControlQueue,
            FabricChatClefStopControlDedupeRegistry stopControlDedupeRegistry,
            FabricChatClefStopControlResultOutbox stopControlResultOutbox,
            FabricChatClefWebSocketConnectionState connectionState
    ) {
        this.commandQueue = commandQueue;
        this.bridgeState = bridgeState;
        this.diagnostics = diagnostics;
        this.reconnectCoordinator = reconnectCoordinator;
        this.sessionGuard = sessionGuard;
        this.stopControlQueue = stopControlQueue;
        this.stopControlDedupeRegistry = stopControlDedupeRegistry;
        this.stopControlResultOutbox = stopControlResultOutbox;
        this.connectionState = connectionState;
    }

    public void stop() {
        if (!connectionState.endRunning()) {
            return;
        }
        WebSocket socket = connectionState.webSocket();
        long generation = connectionState.detachCurrent();
        sessionGuard.markConnectionDetached(generation);
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "LAVI Fabric ChatClef bridge stopping");
        }
        reconnectCoordinator.stop();
        commandQueue.clear("bridge_stopping");
        stopControlQueue.resetForShutdown();
        stopControlDedupeRegistry.resetForShutdown();
        stopControlResultOutbox.resetForShutdown();
        bridgeState.markStopped();
        diagnostics.info("stopped");
    }
}
