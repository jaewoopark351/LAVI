package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Own current-socket text-frame assembly and inbound delivery.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.transport.connection.FabricChatClefWebSocketConnectionState;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefInboundMessageHandler;
import lavi.minecraft.fabric.chatclef.bridge.transport.inbound.FabricChatClefInboundTextFrameAssembler;

import java.net.http.WebSocket;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public final class FabricChatClefConnectionTextReceiver {
    private final FabricChatClefWebSocketConnectionState connectionState;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefInboundMessageHandler inboundMessageHandler;
    private final FabricChatClefInboundTextFrameAssembler frameAssembler;

    public FabricChatClefConnectionTextReceiver(
            FabricChatClefWebSocketConnectionState connectionState,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefInboundMessageHandler inboundMessageHandler
    ) {
        this.connectionState = connectionState;
        this.diagnostics = diagnostics;
        this.inboundMessageHandler = inboundMessageHandler;
        this.frameAssembler = new FabricChatClefInboundTextFrameAssembler();
    }

    public void resetFrames() {
        frameAssembler.reset();
    }

    public CompletionStage<?> onText(
            WebSocket webSocket,
            CharSequence data,
            boolean last
    ) {
        if (!connectionState.isCurrentSocket(webSocket)) {
            diagnostics.warn(
                    "ignored text from stale WebSocket generation="
                            + connectionState.activeConnectionGeneration()
            );
            webSocket.request(1);
            return null;
        }
        long generation = connectionState.activeConnectionGeneration();
        Optional<String> completeMessage = frameAssembler.append(data, last);
        completeMessage.ifPresent(message -> deliverCurrent(webSocket, generation, message));
        webSocket.request(1);
        return null;
    }

    private void deliverCurrent(WebSocket webSocket, long generation, String message) {
        if (!connectionState.isCurrentSocket(webSocket)
                || generation != connectionState.activeConnectionGeneration()) {
            diagnostics.warn("ignored message from stale WebSocket generation=" + generation);
            return;
        }
        inboundMessageHandler.handle(generation, message);
    }
}
