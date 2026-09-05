package lavi.minecraft.fabric.chatclef.bridge.transport.result;

//20260905_kpopmodder: Construct only one protocol command-result envelope.

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.result.FabricChatClefCommandResultEnvelopeFactory;

public final class FabricChatClefResultEnvelopeBuilder {
    private final FabricChatClefCommandResultEnvelopeFactory envelopeFactory =
            new FabricChatClefCommandResultEnvelopeFactory();

    public FabricChatClefBridgeEnvelope build(
            String correlationId,
            String sessionId,
            FabricChatClefCommandResultPayload payload
    ) {
        return envelopeFactory.commandResult(correlationId, sessionId, payload);
    }
}
