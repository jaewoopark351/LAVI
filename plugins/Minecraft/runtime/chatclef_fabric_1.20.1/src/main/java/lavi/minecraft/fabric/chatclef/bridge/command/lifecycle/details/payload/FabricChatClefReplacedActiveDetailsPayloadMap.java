package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefReplacedActiveDetailsPayloadMap {
    private static final String REPLACED_ACTIVE_REQUEST_ID = "replaced_active_request_id";

    private FabricChatClefReplacedActiveDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(String replacedActiveRequestId) {
        Map<String, Object> details = new HashMap<>();
        details.put(REPLACED_ACTIVE_REQUEST_ID, replacedActiveRequestId);
        return details;
    }
}
