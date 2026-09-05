package lavi.minecraft.fabric.chatclef.bridge.transport.result;

//20260905_kpopmodder: Preserve result-envelope encoding API over focused build and JSON stages.

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;

public final class FabricChatClefResultEnvelopeEncoder {
    private final FabricChatClefResultEnvelopeBuilder envelopeBuilder;
    private final FabricChatClefResultJsonEncoder jsonEncoder;

    public FabricChatClefResultEnvelopeEncoder(
            FabricChatClefBridgeJson json,
            FabricChatClefResultSendDiagnostics diagnostics
    ) {
        this.envelopeBuilder = new FabricChatClefResultEnvelopeBuilder();
        this.jsonEncoder = new FabricChatClefResultJsonEncoder(json, diagnostics);
    }

    public FabricChatClefResultEnvelopeEncoding encode(
            String correlationId,
            String sessionId,
            FabricChatClefCommandResultPayload payload
    ) {
        return jsonEncoder.encode(envelopeBuilder.build(correlationId, sessionId, payload));
    }
}
