package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.FabricChatClefEmptyDetailsPayloadMap;

import java.util.Map;

public final class FabricChatClefEmptyDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefEmptyDetailsPayloadMap.toMap();
    }
}
