package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.current;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

import java.util.Map;

public final class FabricChatClefTaskRuntimeOwnershipPayloadMap {
    private static final String OWNERSHIP = "ownership";

    private FabricChatClefTaskRuntimeOwnershipPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, FabricChatClefTaskOwnershipSnapshot ownership) {
        payload.put(OWNERSHIP, ownership.toMap());
    }
}
