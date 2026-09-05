package lavi.minecraft.fabric.chatclef.bridge.transport;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeMessageFactory;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionLifecycle;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefCommandRequestHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefInboundMessageHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefStopControlInboundHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefSessionGuard;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

//20260801_kpopmodder: Keep the public bridge client as a thin transport composition facade.
public final class FabricChatClefBridgeClient implements WebSocket.Listener, FabricChatClefCommandResultSender {
    private final FabricChatClefResultEnvelopeSender resultEnvelopeSender;
    private final FabricChatClefStopControlResultOutbox stopControlResultOutbox;
    private final FabricChatClefWebSocketConnectionLifecycle connectionLifecycle;

    public FabricChatClefBridgeClient(
            FabricChatClefBridgeConfig config,
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefBridgeState state,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            FabricChatClefBridgeMessageFactory messageFactory,
            FabricChatClefReconnectScheduler reconnectScheduler,
            FabricChatClefSessionGuard sessionGuard,
            FabricChatClefStopControlQueue stopControlQueue,
            FabricChatClefStopControlDedupeRegistry stopControlDedupeRegistry
    ) {
        FabricChatClefWebSocketConnectionState connectionState =
                new FabricChatClefWebSocketConnectionState();
        this.resultEnvelopeSender = new FabricChatClefResultEnvelopeSender(
                diagnostics,
                json,
                connectionState::webSocket,
                connectionState::activeConnectionGeneration,
                commandQueue::enqueueCommandResultSendCompletion
        );
        this.stopControlResultOutbox = new FabricChatClefStopControlResultOutbox(
                resultEnvelopeSender,
                diagnostics
        );
        FabricChatClefInboundMessageHandler inboundMessageHandler = new FabricChatClefInboundMessageHandler(
                diagnostics,
                json,
                sessionGuard,
                new FabricChatClefCommandRequestHandler(
                        commandQueue,
                        diagnostics,
                        json,
                        resultEnvelopeSender,
                        sessionGuard
                ),
                new FabricChatClefStopControlInboundHandler(
                        stopControlDedupeRegistry,
                        stopControlQueue,
                        stopControlResultOutbox,
                        sessionGuard,
                        diagnostics
                )
        );
        this.connectionLifecycle = new FabricChatClefWebSocketConnectionLifecycle(
                config,
                commandQueue,
                state,
                diagnostics,
                json,
                messageFactory,
                reconnectScheduler,
                sessionGuard,
                stopControlQueue,
                stopControlDedupeRegistry,
                stopControlResultOutbox,
                inboundMessageHandler,
                connectionState
        );
    }

    public void start() {
        connectionLifecycle.start();
    }

    public void stop() {
        connectionLifecycle.stop();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        connectionLifecycle.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        return connectionLifecycle.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        return connectionLifecycle.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        connectionLifecycle.onError(webSocket, error);
    }

    @Override
    public FabricChatClefCommandResultSendSubmission sendCommandResult(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload payload
    ) {
        return resultEnvelopeSender.sendCommandResult(context, payload);
    }

    @Override
    public FabricChatClefCommandResultSendSubmission sendTerminalCommandResult(
            FabricChatClefCommandContext context,
            FabricChatClefCommandResultPayload payload
    ) {
        return resultEnvelopeSender.sendTerminalCommandResult(context, payload);
    }

    //20260905_kpopmodder: Expose only the STOP result lifecycle needed by the client-tick owner.
    public FabricChatClefStopControlResultOutbox stopControlResultOutbox() {
        return stopControlResultOutbox;
    }
}
