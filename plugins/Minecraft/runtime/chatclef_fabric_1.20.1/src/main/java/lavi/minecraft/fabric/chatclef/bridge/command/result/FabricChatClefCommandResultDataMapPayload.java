package lavi.minecraft.fabric.chatclef.bridge.command.result;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Keep legacy map-backed result data isolated at the result boundary.
final class FabricChatClefCommandResultDataMapPayload implements FabricChatClefCommandResultDataPayload {
    private final Map<String, Object> data;

    private FabricChatClefCommandResultDataMapPayload(Map<String, Object> data) {
        this.data = data == null ? new HashMap<>() : data;
    }

    static FabricChatClefCommandResultDataMapPayload of(Map<String, Object> data) {
        return new FabricChatClefCommandResultDataMapPayload(data);
    }

    @Override
    public Map<String, Object> toMap() {
        return data;
    }
}
