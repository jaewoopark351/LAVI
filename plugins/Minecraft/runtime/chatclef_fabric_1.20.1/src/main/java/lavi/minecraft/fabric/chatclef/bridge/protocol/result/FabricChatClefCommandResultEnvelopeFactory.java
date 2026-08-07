package lavi.minecraft.fabric.chatclef.bridge.protocol.result;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;

import java.util.UUID;

public final class FabricChatClefCommandResultEnvelopeFactory {
    public FabricChatClefBridgeEnvelope commandResult(
            String correlationId,
            String sessionId,
            FabricChatClefCommandResultPayload payload
    ) {
        FabricChatClefBridgeEnvelope envelope = new FabricChatClefBridgeEnvelope();
        envelope.protocolVersion = 1;
        envelope.messageType = "command_result";
        envelope.messageId = "fabric-chatclef-result-" + UUID.randomUUID();
        envelope.correlationId = correlationId;
        envelope.sessionId = sessionId;
        envelope.timestampMs = System.currentTimeMillis();
        envelope.payload = payload.toMap();
        return envelope;
    }
}
