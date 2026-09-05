package lavi.minecraft.fabric.chatclef.bridge.transport.inbound;

//20260905_kpopmodder: Decode raw and typed inbound envelopes without choosing their route.

import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;

import java.io.IOException;

public final class FabricChatClefInboundEnvelopeDecoder {
    private final FabricChatClefBridgeJson json;

    public FabricChatClefInboundEnvelopeDecoder(FabricChatClefBridgeJson json) {
        this.json = json;
    }

    public JsonNode decodeRaw(String message) throws IOException {
        return json.decodeTree(message);
    }

    public FabricChatClefBridgeEnvelope decodeTyped(JsonNode rawEnvelope) throws IOException {
        return json.decode(rawEnvelope);
    }
}
