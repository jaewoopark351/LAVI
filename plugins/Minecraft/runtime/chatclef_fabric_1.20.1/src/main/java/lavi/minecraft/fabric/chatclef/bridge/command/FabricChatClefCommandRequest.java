package lavi.minecraft.fabric.chatclef.bridge.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import lavi.minecraft.fabric.chatclef.bridge.command.request.FabricChatClefCommandRequestFields;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Represent one LAVI Fabric ChatClef command request.
public final class FabricChatClefCommandRequest {
    @JsonProperty(FabricChatClefCommandRequestFields.REQUEST_ID)
    public String requestId = "";

    @JsonProperty(FabricChatClefCommandRequestFields.COMMAND)
    public String command = "";

    @JsonProperty(FabricChatClefCommandRequestFields.SOURCE)
    public String source = "";

    @JsonProperty(FabricChatClefCommandRequestFields.DEADLINE_MS)
    public Long deadlineMs;

    @JsonProperty(FabricChatClefCommandRequestFields.METADATA)
    public Map<String, Object> metadata = new HashMap<>();

    public boolean isValid() {
        return requestId != null && !requestId.isBlank()
                && command != null && !command.isBlank();
    }

    public boolean isDeadlineExceeded(long nowMs) {
        return deadlineMs != null && nowMs > deadlineMs;
    }
}
