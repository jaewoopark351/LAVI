package lavi.minecraft.fabric.chatclef.bridge.transport;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;

import java.net.http.WebSocket;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

//20260803_kpopmodder: Keep command_result envelope construction out of the WebSocket bridge listener.
public final class FabricChatClefResultEnvelopeSender implements FabricChatClefCommandResultSender {
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefBridgeJson json;
    private final Supplier<WebSocket> socketSupplier;
    private final LongSupplier activeGenerationSupplier;

    public FabricChatClefResultEnvelopeSender(
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefBridgeJson json,
            Supplier<WebSocket> socketSupplier,
            LongSupplier activeGenerationSupplier
    ) {
        this.diagnostics = diagnostics;
        this.json = json;
        this.socketSupplier = socketSupplier;
        this.activeGenerationSupplier = activeGenerationSupplier;
    }

    @Override
    public void sendCommandResult(FabricChatClefCommandContext context, Map<String, Object> payload) {
        sendCommandResult(
                context.correlationId(),
                context.sessionId(),
                context.connectionGeneration(),
                payload
        );
    }

    public void sendCommandResult(
            String correlationId,
            String sessionId,
            long generation,
            Map<String, Object> payload
    ) {
        WebSocket socket = socketSupplier.get();
        long activeGeneration = activeGenerationSupplier.getAsLong();
        if (socket == null || generation != activeGeneration) {
            diagnostics.warn(
                    "ignored command_result for inactive generation="
                            + generation
                            + " active_generation="
                            + activeGeneration
            );
            return;
        }
        try {
            socket.sendText(json.encode(envelope(correlationId, sessionId, payload)), true);
        } catch (Exception error) {
            diagnostics.warn("command_result send failed " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private FabricChatClefBridgeEnvelope envelope(String correlationId, String sessionId, Map<String, Object> payload) {
        FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
        envelope.protocolVersion = 1;
        envelope.messageType = "command_result";
        envelope.messageId = "fabric-chatclef-result-" + UUID.randomUUID();
        envelope.correlationId = correlationId;
        envelope.sessionId = sessionId;
        envelope.timestampMs = System.currentTimeMillis();
        envelope.payload = payload;
        return envelope;
    }
}
