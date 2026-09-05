package lavi.minecraft.fabric.chatclef.bridge.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;

import java.io.IOException;
import java.util.Map;

//20260801_kpopmodder: Isolate Fabric ChatClef bridge JSON serialization.
public final class FabricChatClefBridgeJson {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(FabricChatClefBridgeEnvelope envelope) throws IOException {
        return objectMapper.writeValueAsString(envelope);
    }

    public FabricChatClefBridgeEnvelope decode(String text) throws IOException {
        return objectMapper.readValue(text, FabricChatClefBridgeEnvelope.class);
    }

    //20260905_kpopmodder: Preserve raw JSON types and key presence for strict STOP validation.
    public JsonNode decodeTree(String text) throws IOException {
        return objectMapper.readTree(text);
    }

    public FabricChatClefBridgeEnvelope decode(JsonNode tree) throws IOException {
        return objectMapper.treeToValue(tree, FabricChatClefBridgeEnvelope.class);
    }

    public FabricChatClefCommandRequest commandRequest(Map<String, Object> payload) {
        return objectMapper.convertValue(payload, FabricChatClefCommandRequest.class);
    }
}
