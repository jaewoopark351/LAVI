package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;

import java.util.HashMap;
import java.util.Map;

//20260806_kpopmodder: Split command lifecycle detail payloads by event while preserving emitted keys.
public final class FabricChatClefReplacedActiveDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private static final String REPLACED_ACTIVE_REQUEST_ID = "replaced_active_request_id";

    private final String replacedActiveRequestId;

    public FabricChatClefReplacedActiveDetailsPayload(String replacedActiveRequestId) {
        this.replacedActiveRequestId = FabricChatClefLifecycleDetailValues.nullToEmpty(replacedActiveRequestId);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> details = new HashMap<>();
        details.put(REPLACED_ACTIVE_REQUEST_ID, replacedActiveRequestId);
        return details;
    }
}
