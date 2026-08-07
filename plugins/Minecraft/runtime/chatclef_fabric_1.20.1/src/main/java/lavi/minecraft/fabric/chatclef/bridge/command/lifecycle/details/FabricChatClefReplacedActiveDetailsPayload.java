package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefReplacedActiveDetailsPayloadMap;

import java.util.Map;

//20260806_kpopmodder: Split command lifecycle detail payloads by event while preserving emitted keys.
public final class FabricChatClefReplacedActiveDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    private final String replacedActiveRequestId;

    public FabricChatClefReplacedActiveDetailsPayload(String replacedActiveRequestId) {
        this.replacedActiveRequestId = FabricChatClefLifecycleDetailValues.nullToEmpty(replacedActiveRequestId);
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefReplacedActiveDetailsPayloadMap.toMap(replacedActiveRequestId);
    }
}
