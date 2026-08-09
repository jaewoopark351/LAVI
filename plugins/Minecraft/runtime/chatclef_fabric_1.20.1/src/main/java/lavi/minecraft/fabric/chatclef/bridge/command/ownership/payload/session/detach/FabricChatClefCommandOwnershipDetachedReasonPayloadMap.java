package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.detach;

import java.util.Map;

//20260809_kpopmodder: Split the command ownership detach reason without changing emitted keys.
public final class FabricChatClefCommandOwnershipDetachedReasonPayloadMap {
    private static final String DETACHED_REASON = "detached_reason";

    private FabricChatClefCommandOwnershipDetachedReasonPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String detachedReason) {
        payload.put(DETACHED_REASON, detachedReason);
    }
}
