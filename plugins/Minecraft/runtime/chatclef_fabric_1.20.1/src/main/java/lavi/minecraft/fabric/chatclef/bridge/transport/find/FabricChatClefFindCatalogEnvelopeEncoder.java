//#if MC == 12001
//$$ package lavi.minecraft.fabric.chatclef.bridge.transport.find;

//$$ import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeEnvelope;
//$$ import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
//$$ import java.nio.charset.StandardCharsets;
//$$ import java.util.LinkedHashMap;
//$$ import java.util.Map;
//$$ import java.util.UUID;

//$$ //20260914_kpopmodder: Keep bounded catalog serialization separate from transport/session ownership.
//$$ public final class FabricChatClefFindCatalogEnvelopeEncoder {
//$$     private final FabricChatClefBridgeJson json;
//$$     public FabricChatClefFindCatalogEnvelopeEncoder(FabricChatClefBridgeJson json) { this.json = json; }
//$$     public String encode(String sessionId, long connectionGeneration, String event, Map<String, Object> payload)
//$$             throws java.io.IOException {
//$$         var envelope = new FabricChatClefBridgeEnvelope();
//$$         envelope.messageType = "event";
//$$         envelope.messageId = "fabric-find-" + UUID.randomUUID();
//$$         envelope.sessionId = sessionId;
//$$         envelope.timestampMs = System.currentTimeMillis();
//$$         var wire = new LinkedHashMap<String, Object>(payload);
//$$         wire.put("event", event);
//$$         wire.put("connection_generation", connectionGeneration);
//$$         envelope.payload = wire;
//$$         String encoded = json.encode(envelope);
//$$         if (encoded.getBytes(StandardCharsets.UTF_8).length > 65536)
//$$             throw new IllegalArgumentException("catalog_wire_page_bound");
//$$         return encoded;
//$$     }
//$$ }
//#endif
