package lavi.minecraft.fabric.chatclef.bridge.transport;

import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeMessageFactory;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefCommandRequestHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefInboundMessageHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

//20260801_kpopmodder: Keep WebSocket callbacks to transport parsing and command queueing only.
public final class FabricChatClefBridgeClient implements WebSocket.Listener, FabricChatClefCommandResultSender {
    private final FabricChatClefBridgeConfig config;
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefBridgeState state;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefBridgeJson json;
    private final FabricChatClefBridgeMessageFactory messageFactory;
    private final FabricChatClefReconnectScheduler reconnectScheduler;
    private final FabricChatClefResultEnvelopeSender resultEnvelopeSender;
    private final FabricChatClefInboundMessageHandler inboundMessageHandler;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicLong connectionGenerations = new AtomicLong(0);
    private final StringBuilder incomingText = new StringBuilder();
    private volatile WebSocket webSocket;
    private volatile long activeConnectionGeneration;
    private volatile String lastLoggedConnectFailure;

    public FabricChatClefBridgeClient(
            FabricChatClefBridgeConfig config,
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefBridgeState state,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            FabricChatClefBridgeMessageFactory messageFactory,
            FabricChatClefReconnectScheduler reconnectScheduler
    ) {
        this.config = config;
        this.commandQueue = commandQueue;
        this.state = state;
        this.diagnostics = diagnostics;
        this.json = json;
        this.messageFactory = messageFactory;
        this.reconnectScheduler = reconnectScheduler;
        this.resultEnvelopeSender = new FabricChatClefResultEnvelopeSender(
                diagnostics,
                json,
                () -> this.webSocket,
                () -> this.activeConnectionGeneration
        );
        FabricChatClefSessionGuard sessionGuard = new FabricChatClefSessionGuard(state, diagnostics);
        this.inboundMessageHandler = new FabricChatClefInboundMessageHandler(
                diagnostics,
                json,
                sessionGuard,
                new FabricChatClefCommandRequestHandler(
                        commandQueue,
                        diagnostics,
                        json,
                        resultEnvelopeSender,
                        sessionGuard
                )
        );
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        diagnostics.info("starting WebSocket client endpoint=" + config.endpoint());
        reconnectScheduler.runNow(this::connectIfRunning);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "LAVI Fabric ChatClef bridge stopping");
        }
        reconnectScheduler.stop();
        commandQueue.clear("bridge_stopping");
        state.markStopped();
        diagnostics.info("stopped");
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        long generation = connectionGenerations.incrementAndGet();
        this.webSocket = webSocket;
        activeConnectionGeneration = generation;
        incomingText.setLength(0);
        connecting.set(false);
        lastLoggedConnectFailure = null;
        state.markConnected();
        diagnostics.info("connected generation=" + generation + "; sending handshake");
        sendHandshake(webSocket);
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        if (!isCurrentSocket(webSocket)) {
            diagnostics.warn("ignored text from stale WebSocket generation=" + activeConnectionGeneration);
            webSocket.request(1);
            return null;
        }
        long generation = activeConnectionGeneration;
        incomingText.append(data);
        if (last) {
            String message = incomingText.toString();
            incomingText.setLength(0);
            if (!isCurrentSocket(webSocket) || generation != activeConnectionGeneration) {
                diagnostics.warn("ignored message from stale WebSocket generation=" + generation);
            } else {
                inboundMessageHandler.handle(generation, message);
            }
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        long generation = activeConnectionGeneration;
        if (!isCurrentSocket(webSocket)) {
            diagnostics.warn("ignored close from stale WebSocket status=" + statusCode + " reason=" + reason);
            return null;
        }
        this.webSocket = null;
        activeConnectionGeneration = 0;
        connecting.set(false);
        commandQueue.enqueueConnectionDetached(generation, "websocket_closed");
        state.markDisconnected("closed status=" + statusCode + " reason=" + reason);
        diagnostics.warn(
                "closed generation="
                        + generation
                        + " status="
                        + statusCode
                        + " reason="
                        + reason
        );
        scheduleReconnect();
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        long generation = activeConnectionGeneration;
        if (!isCurrentSocket(webSocket)) {
            diagnostics.warn("ignored error from stale WebSocket " + error.getClass().getSimpleName() + ": " + error.getMessage());
            return;
        }
        this.webSocket = null;
        activeConnectionGeneration = 0;
        connecting.set(false);
        commandQueue.enqueueConnectionDetached(generation, "websocket_error");
        String message = error.getClass().getSimpleName() + ": " + error.getMessage();
        state.markFailed(message);
        diagnostics.warn("connection error generation=" + generation + " " + message);
        scheduleReconnect();
    }

    private void connectIfRunning() {
        if (!running.get() || !connecting.compareAndSet(false, true)) {
            return;
        }
        state.markConnecting();
        httpClient.newWebSocketBuilder()
                .buildAsync(config.endpoint(), this)
                .whenComplete((socket, error) -> {
                    if (error == null) {
                        return;
                    }
                    connecting.set(false);
                    String message = error.getClass().getSimpleName() + ": " + error.getMessage();
                    state.markFailed(message);
                    warnConnectFailure(message);
                    scheduleReconnect();
                });
    }

    private void sendHandshake(WebSocket socket) {
        try {
            String message = json.encode(messageFactory.handshake(state));
            socket.sendText(message, true);
        } catch (Exception error) {
            String message = error.getClass().getSimpleName() + ": " + error.getMessage();
            state.markFailed(message);
            diagnostics.warn("handshake send failed " + message);
            scheduleReconnect();
        }
    }

    @Override
    public FabricChatClefCommandResultSendOutcome sendCommandResult(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload payload
    ) {
        return resultEnvelopeSender.sendCommandResult(context, payload);
    }

    private boolean isCurrentSocket(WebSocket socket) {
        return socket != null && socket == webSocket;
    }

    private void scheduleReconnect() {
        if (!running.get()) {
            return;
        }
        reconnectScheduler.runLater(this::connectIfRunning, config.reconnectDelay());
    }

    private void warnConnectFailure(String message) {
        if (message.equals(lastLoggedConnectFailure)) {
            return;
        }
        lastLoggedConnectFailure = message;
        diagnostics.warn("connect failed " + message);
    }
}
