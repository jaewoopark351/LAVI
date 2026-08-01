package lavi.minecraft.fabric.chatclef.bridge.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Mirror the Fabric ChatClef v1 envelope without creating shared Java protocol code.
public final class FabricChatClefBridgeEnvelope {
    @JsonProperty("protocol_version")
    public int protocolVersion = 1;

    @JsonProperty("message_type")
    public String messageType = "error";

    @JsonProperty("message_id")
    public String messageId = "";

    @JsonProperty("correlation_id")
    public String correlationId;

    @JsonProperty("session_id")
    public String sessionId;

    @JsonProperty("timestamp_ms")
    public long timestampMs;

    @JsonProperty("payload")
    public Map<String, Object> payload = new HashMap<>();
}
