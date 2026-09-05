package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Keep the WebSocket listener as a focused lifecycle delegation facade.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeMessageFactory;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefReconnectScheduler;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.handshake.FabricChatClefConnectionHandshakeCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.handshake.FabricChatClefHandshakeSender;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.startup.FabricChatClefConnectionAttempt;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.startup.FabricChatClefConnectionStartup;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefInboundMessageHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public final class FabricChatClefWebSocketConnectionLifecycle implements WebSocket.Listener {
    private final FabricChatClefConnectionStartup startup;
    private final FabricChatClefConnectionAttempt connectionAttempt;
    private final FabricChatClefConnectionHandshakeCoordinator handshakeCoordinator;
    private final FabricChatClefConnectionTextReceiver textReceiver;
    private final FabricChatClefConnectionDetachHandler detachHandler;
    private final FabricChatClefConnectionReconnectCoordinator reconnectCoordinator;
    private final FabricChatClefConnectionShutdown shutdown;

    public FabricChatClefWebSocketConnectionLifecycle(
            FabricChatClefBridgeConfig config,
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefBridgeState bridgeState,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            FabricChatClefBridgeMessageFactory messageFactory,
            FabricChatClefReconnectScheduler reconnectScheduler,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefStopControlQueue stopControlQueue,
            FabricChatClefStopControlDedupeRegistry stopControlDedupeRegistry,
            FabricChatClefStopControlResultOutbox stopControlResultOutbox,
            FabricChatClefInboundMessageHandler inboundMessageHandler,
            FabricChatClefWebSocketConnectionState connectionState
    ) {
        this.reconnectCoordinator = new FabricChatClefConnectionReconnectCoordinator(
                config,
                reconnectScheduler,
                connectionState
        );
        this.startup = new FabricChatClefConnectionStartup(
                config,
                diagnostics,
                connectionState,
                reconnectCoordinator
        );
        this.connectionAttempt = new FabricChatClefConnectionAttempt(
                config,
                bridgeState,
                diagnostics,
                connectionState,
                reconnectCoordinator
        );
        this.textReceiver = new FabricChatClefConnectionTextReceiver(
                connectionState,
                diagnostics,
                inboundMessageHandler
        );
        FabricChatClefHandshakeSender handshakeSender = new FabricChatClefHandshakeSender(
                bridgeState,
                diagnostics,
                json,
                messageFactory,
                sessionGuard,
                connectionState
        );
        this.handshakeCoordinator = new FabricChatClefConnectionHandshakeCoordinator(
                bridgeState,
                diagnostics,
                connectionState,
                textReceiver,
                handshakeSender
        );
        this.detachHandler = new FabricChatClefConnectionDetachHandler(
                commandQueue,
                bridgeState,
                diagnostics,
                sessionGuard,
                connectionState,
                reconnectCoordinator
        );
        this.shutdown = new FabricChatClefConnectionShutdown(
                commandQueue,
                bridgeState,
                diagnostics,
                reconnectCoordinator,
                sessionGuard,
                stopControlQueue,
                stopControlDedupeRegistry,
                stopControlResultOutbox,
                connectionState
        );
    }

    public void start() {
        startup.start(this::connectIfRunning);
    }

    public void stop() {
        shutdown.stop();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        handshakeCoordinator.onOpen(webSocket, this::scheduleReconnect);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        return textReceiver.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        detachHandler.onClose(webSocket, statusCode, reason, this::connectIfRunning);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        detachHandler.onError(webSocket, error, this::connectIfRunning);
    }

    private void connectIfRunning() {
        connectionAttempt.connectIfRunning(this, this::connectIfRunning);
    }

    private void scheduleReconnect() {
        reconnectCoordinator.schedule(this::connectIfRunning);
    }
}
