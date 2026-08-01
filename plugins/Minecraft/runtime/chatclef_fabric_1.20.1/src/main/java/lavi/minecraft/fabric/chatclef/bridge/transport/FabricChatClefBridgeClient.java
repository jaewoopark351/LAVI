package lavi.minecraft.fabric.chatclef.bridge.transport;

import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeMessageFactory;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

//20260801_kpopmodder: Keep WebSocket callbacks to transport parsing and command queueing only.
public final class FabricChatClefBridgeClient implements WebSocket.Listener, FabricChatClefCommandResultSender {
    private final FabricChatClefBridgeConfig config;
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefBridgeState state;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefBridgeJson json;
    private final FabricChatClefBridgeMessageFactory messageFactory;
    private final FabricChatClefReconnectScheduler reconnectScheduler;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final StringBuilder incomingText = new StringBuilder();
    private volatile WebSocket webSocket;
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
        commandQueue.clear();
        state.markStopped();
        diagnostics.info("stopped");
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        connecting.set(false);
        lastLoggedConnectFailure = null;
        state.markConnected();
        diagnostics.info("connected; sending handshake");
        sendHandshake(webSocket);
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        incomingText.append(data);
        if (last) {
            String message = incomingText.toString();
            incomingText.setLength(0);
            handleMessage(message);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        this.webSocket = null;
        connecting.set(false);
        commandQueue.clear();
        state.markDisconnected("closed status=" + statusCode + " reason=" + reason);
        diagnostics.warn("closed status=" + statusCode + " reason=" + reason);
        scheduleReconnect();
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        this.webSocket = null;
        connecting.set(false);
        commandQueue.clear();
        String message = error.getClass().getSimpleName() + ": " + error.getMessage();
        state.markFailed(message);
        diagnostics.warn("connection error " + message);
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

    private void handleMessage(String message) {
        try {
            FabricChatClefBridgeEnvelope envelope = json.decode(message);
            if (envelope.protocolVersion != 1) {
                diagnostics.warn("ignored unsupported protocol_version=" + envelope.protocolVersion);
                return;
            }
            if ("handshake_ack".equals(envelope.messageType)) {
                handleHandshakeAck(envelope);
                return;
            }
            if ("status_snapshot".equals(envelope.messageType)) {
                diagnostics.info("received status_snapshot");
                return;
            }
            if ("command_request".equals(envelope.messageType)) {
                handleCommandRequest(envelope);
                return;
            }
            if ("error".equals(envelope.messageType)) {
                diagnostics.warn("received error envelope payload=" + envelope.payload);
                return;
            }
            diagnostics.warn("ignored unsupported message_type=" + envelope.messageType);
        } catch (Exception error) {
            diagnostics.warn("message decode failed " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private void handleHandshakeAck(FabricChatClefBridgeEnvelope envelope) {
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

    private void handleCommandRequest(FabricChatClefBridgeEnvelope envelope) {
        FabricChatClefCommandRequest request = json.commandRequest(envelope.payload);
        if (!request.isValid()) {
            sendCommandResult(
                    envelope.messageId,
                    FabricChatClefCommandResult.rejected(
                            request.requestId,
                            "invalid_request",
                            "Fabric ChatClef command_request requires request_id and command."
                    )
            );
            return;
        }
        if (!commandQueue.offer(request)) {
            sendCommandResult(
                    envelope.messageId,
                    FabricChatClefCommandResult.rejected(
                            request.requestId,
                            "invalid_request",
                            "Fabric ChatClef command already pending or active: "
                                    + commandQueue.activeRequestId().orElse("<pending>")
                    )
            );
            return;
        }
        diagnostics.info("queued command request=" + request.requestId);
    }

    @Override
    public void sendCommandResult(String correlationId, Map<String, Object> payload) {
        WebSocket socket = webSocket;
        if (socket == null) {
            diagnostics.warn("cannot send command_result because WebSocket is disconnected");
            return;
        }
        FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
        envelope.protocolVersion = 1;
        envelope.messageType = "command_result";
        envelope.messageId = "fabric-chatclef-result-" + java.util.UUID.randomUUID();
        envelope.correlationId = correlationId;
        envelope.sessionId = state.sessionId().orElse(null);
        envelope.timestampMs = System.currentTimeMillis();
        envelope.payload = payload;
        try {
            socket.sendText(json.encode(envelope), true);
        } catch (Exception error) {
            diagnostics.warn("command_result send failed " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private String stringPayload(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
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
