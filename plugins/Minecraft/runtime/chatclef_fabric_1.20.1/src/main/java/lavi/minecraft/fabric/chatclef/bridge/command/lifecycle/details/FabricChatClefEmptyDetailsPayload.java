package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefEmptyDetailsPayload implements FabricChatClefLifecycleDetailsPayload {
    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>();
    }
}
