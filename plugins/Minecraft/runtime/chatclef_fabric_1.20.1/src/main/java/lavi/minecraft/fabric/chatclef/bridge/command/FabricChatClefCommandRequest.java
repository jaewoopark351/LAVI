package lavi.minecraft.fabric.chatclef.bridge.command;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Represent one LAVI Fabric ChatClef command request.
public final class FabricChatClefCommandRequest {
    @JsonProperty("request_id")
    public String requestId = "";

    @JsonProperty("command")
    public String command = "";

    @JsonProperty("source")
    public String source = "";

    @JsonProperty("deadline_ms")
    public Long deadlineMs;

    @JsonProperty("metadata")
    public Map<String, Object> metadata = new HashMap<>();

    public boolean isValid() {
        return requestId != null && !requestId.isBlank()
                && command != null && !command.isBlank();
    }

    public boolean isDeadlineExceeded(long nowMs) {
        return deadlineMs != null && nowMs > deadlineMs;
    }
}
