package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.detach;

import java.util.Map;

//20260809_kpopmodder: Split the command ownership detached flag without changing emitted keys.
public final class FabricChatClefCommandOwnershipDetachedFlagPayloadMap {
    private static final String DETACHED = "detached";

    private FabricChatClefCommandOwnershipDetachedFlagPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean detached) {
        payload.put(DETACHED, detached);
    }
}
