package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session;

import java.util.Map;

//20260808_kpopmodder: Keep command ownership detach fields separate at the same Map edge.
public final class FabricChatClefCommandOwnershipDetachPayloadMap {
    private static final String DETACHED = "detached";
    private static final String DETACHED_REASON = "detached_reason";

    private FabricChatClefCommandOwnershipDetachPayloadMap() {
    }

    public static void putDetachFields(
            Map<String, Object> payload,
            boolean detached,
            String detachedReason
    ) {
        payload.put(DETACHED, detached);
        payload.put(DETACHED_REASON, detachedReason);
    }
}
